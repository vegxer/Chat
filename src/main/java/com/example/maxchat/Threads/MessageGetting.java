package com.example.maxchat.Threads;

import com.example.maxchat.Server.Server;

import java.io.IOException;

public record MessageGetting(Server client) implements Runnable {

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(Server.READ_DELAY);
                if (client.isConnected()) {
                    String message = client.getMessage();
                    if (message != null) {
                        if (!client.user.isHistoryWriter && message.startsWith("ChatInfo", 21) && message.contains("Он") &&
                                client.user.getNickname().equals(message.substring(message.lastIndexOf(' ') + 1)))
                            client.user.isHistoryWriter = true;

                        client.chatArea.appendText("\r\n" + message);
                        if (client.user.isHistoryWriter)
                            client.writeFTPFile(Server.DIALOGS_PATH + client.user.getDialogName() + "/history.txt",
                                    message + "\n");
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
