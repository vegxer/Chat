package com.example.maxchat.Threads;

import com.example.maxchat.Server.Server;

import java.io.IOException;

public record DeleteWaiting(Server client, int filesCount, String fileName) implements Runnable {

    @Override
    public void run() {
        try {
            Thread.sleep(Server.READ_DELAY + (filesCount + 1) * 20L);
            if (client.existsFile(fileName))
                client.deleteFile(fileName);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
