package com.example.maxchat.Threads;

import com.example.maxchat.Server.Server;

public record MessageAreaDisabling(Server client, String message) implements Runnable {

    @Override
    public void run() {
        client.messageArea.setEditable(false);
        client.messageArea.setEditable(false);
        if (message.charAt(0) == '/')
            client.messageArea.setText("...КОМАНДА ОБРАБАТЫВАЕТСЯ...");
        else
            client.messageArea.setText("...СООБЩЕНИЕ ОБРАБАТЫВАЕТСЯ...");
        try {
            Thread.sleep(Server.QUIT_DELAY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        client.messageArea.clear();
        client.messageArea.setEditable(true);
    }
}
