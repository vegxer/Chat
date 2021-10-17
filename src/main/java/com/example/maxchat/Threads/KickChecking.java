package com.example.maxchat.Threads;

import com.example.maxchat.Server.Server;

import java.io.IOException;

public record KickChecking(Server client) implements Runnable {

    @Override
    public void run() {
        int count = 0;
        while (count < 2) {
            try {
                Thread.sleep(Server.QUIT_DELAY);
                if (client.isConnected() && !client.existsUser(client.user, client.user.getDialogName()))
                    ++count;
                else
                    count = 0;
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
        client.messageGetting.stop();
        client.mainMenu = true;
        client.chatArea.setText("\nГЛАВНОЕ МЕНЮ\n/help - для просмотра команд");
    }
}
