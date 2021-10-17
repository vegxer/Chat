package com.example.maxchat.EventHandlers;

import com.example.maxchat.Threads.KickChecking;
import com.example.maxchat.Threads.MessageAreaDisabling;
import com.example.maxchat.Threads.MessageGetting;
import com.example.maxchat.Server.Server;
import com.example.maxchat.Users.AdminUser;
import com.example.maxchat.Users.DefaultUser;
import com.example.maxchat.Users.User;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.EventObject;

public record OnKeyPressedHandler(Server client) implements EventHandler<KeyEvent> {

    @Override
    public void handle(KeyEvent keyEvent) {
        if (keyEvent.getCode() == KeyCode.ENTER) {
            if (keyEvent.isShiftDown()) {
                client.messageArea.appendText("\r\n");
                return;
            }
            String command = client.messageArea.getText().substring(0, client.messageArea.getText().length() - 1);
            client.messageArea.clear();
            try {
                if (command.charAt(0) == '/') {
                    client.user.executeCommand(client, command, client.mainMenu);

                    if (command.equals("/quit")) {
                        if (client.mainMenu) {
                            client.mainMenu = false;
                            client.user = new User("DefaultUser", client.decode(Server.ANON_PASS), "", 100, 18736637);
                            client.chatArea.setText("\nМЕНЮ АВТОРИЗАЦИИ\n/help - для просмотра команд");
                        } else if (client.user instanceof DefaultUser || client.user instanceof AdminUser) {
                            client.mainMenu = true;
                            client.chatArea.setText("\nГЛАВНОЕ МЕНЮ\n/help - для просмотра команд");
                        } else {
                            if (client.isConnected())
                                client.disconnect();
                            Platform.exit();
                            System.exit(0);
                        }
                    } else if (command.equals("/deleteAccount")) {
                        client.mainMenu = false;
                        client.chatArea.clear();
                        client.user = new User("DefaultUser", client.decode(Server.ANON_PASS), "", 100, 18736637);
                        client.chatArea.appendText("\nМЕНЮ АВТОРИЗАЦИИ\n/help - для просмотра команд");
                    } else {
                        if (command.startsWith("/join ")) {
                            client.mainMenu = false;
                            client.chatArea.setText(client.readFTPFile(Server.DIALOGS_PATH + client.user.getDialogName() + "/history.txt"));
                            client.kickChecking = new Thread(new KickChecking(client));
                            client.messageGetting = new Thread(new MessageGetting(client));
                            client.messageGetting.start();
                            client.kickChecking.start();
                        } else if (command.startsWith("/login ") || command.startsWith("/register ")) {
                            client.mainMenu = true;
                            client.chatArea.setText("\nГЛАВНОЕ МЕНЮ\n/help - для просмотра команд");
                        }
                    }
                } else if ((client.user instanceof DefaultUser || client.user instanceof AdminUser) && !client.mainMenu)
                    client.sendMessage(client.user, client.user.getDialogName(), command);
                else
                    throw new IllegalArgumentException("Неверная команда");
                client.messageAreaDisabling = new Thread(new MessageAreaDisabling(client, command));
                client.messageAreaDisabling.start();
            } catch (Exception e) {
                client.chatArea.appendText("\n" + e.getMessage());
            }
        }
    }
}
