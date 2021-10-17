package com.example.maxchat;

import com.example.maxchat.EventHandlers.OnCloseHandler;
import com.example.maxchat.Server.Server;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        File toDelete = new File(System.getProperty("user.dir") + "/FILE_TO_DELETE.txt");
        if (toDelete.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(toDelete))) {
                String fileName = reader.readLine();
                if (fileName != null && new File(fileName).exists())
                    new File(fileName).delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                toDelete.delete();
            }
        }
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 800);
        stage.setTitle("Chat");
        stage.setScene(scene);
        stage.show();
        Server server = new Server((TextArea)scene.lookup("#chatArea"), (TextArea)scene.lookup("#messageArea"));
        stage.setOnCloseRequest(new OnCloseHandler(server));
        server.start();
    }

    public static void main(String[] args) {
        launch();
    }
}