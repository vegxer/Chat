package com.example.maxchat.EventHandlers;

import com.example.maxchat.Server.Server;
import com.example.maxchat.Users.AdminUser;
import com.example.maxchat.Users.DefaultUser;
import com.example.maxchat.Users.User;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.util.ArrayList;

public record OnCloseHandler(Server client) implements EventHandler<WindowEvent> {

    @Override
    public void handle(WindowEvent keyEvent) {
        if (!client.mainMenu && (client.user instanceof AdminUser || client.user instanceof DefaultUser)) {
            try {
                client.deleteUserFromDialog(client.user, client.user.getDialogName());
                String message = client.user.getNickname() + " покидает чат";
                ArrayList<String> us = client.getUsers(client.user.getDialogName());
                if (client.user.isHistoryWriter && !us.isEmpty()) {
                    message = message.concat(". Он был писателем истории,\nтеперь писателем истории становится " + us.get(0));
                    client.user.isHistoryWriter = false;
                }
                client.sendMessage(User.createUser("ChatInfo", client.user.getPassword(), client.user.getDialogName(),
                        100, 18736637), client.user.getDialogName(), message);
                Thread.sleep(Server.QUIT_DELAY);
                if (client.kickChecking != null && client.kickChecking.isAlive())
                    client.kickChecking.stop();
                if (client.messageGetting != null && client.messageGetting.isAlive())
                    client.messageGetting.stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            if (client.isConnected())
                client.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }
}
