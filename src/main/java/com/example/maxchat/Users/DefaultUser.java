package com.example.maxchat.Users;

import com.example.maxchat.HelloApplication;
import com.example.maxchat.Server.Server;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class DefaultUser extends User {
    public DefaultUser(String nickname, String password, String dialogName, int multiplier, int primeNumber) {
        super(nickname, password, dialogName, multiplier, primeNumber);
    }

    @Override
    public String getCommands() {
        return "/deleteAccount - удалить аккаунт\n" +
                "/dialog - название текущего диалога\n/list - Список диалогов\n" +
                "/listUsers [<Диалог> <Пароль>]\n/join <Имя диалога> <Пароль> - подключиться к диалогу\n/quit - выйти из чата";
    }

    @Override
    public boolean executeCommand(Server server, String command, boolean mainMenu) throws IOException, InterruptedException {
        User chatInfo = User.createUser("ChatInfo", getPassword(), getDialogName(), 100, 18736637);
        if (command.replace(" ", "").equals("/help")) {
            server.chatArea.appendText("\n" + getCommands());
        }
        else if (command.replace(" ", "").equals("/list"))
            server.printList(server.getDialogs());
        else if (command.startsWith("/join ")) {
            String[] dialog = command.split(" ");
            if (dialog.length == 3) {
                if (!server.getDialogs().contains(dialog[1]))
                    throw new FileNotFoundException("Такого диалога не существует");
                if (!mainMenu) {
                    server.deleteUserFromDialog(this, getDialogName());
                    String message = getNickname() + " покидает чат";
                    ArrayList<String> us = server.getUsers(getDialogName());
                    if (isHistoryWriter && !us.isEmpty()) {
                        message = message.concat(". Он был писателем истории,\nтеперь писателем истории становится " + us.get(0));
                        isHistoryWriter = false;
                    }
                    server.sendMessage(chatInfo, getDialogName(), message);
                    Thread.sleep(Server.QUIT_DELAY);
                    server.kickChecking.stop();
                    server.messageGetting.stop();
                }
                if (server.getUsers(dialog[1]).isEmpty())
                    isHistoryWriter = true;
                setDialogName(dialog[1]);
                server.addUserToDialog(this, dialog[1], new Password(dialog[2], 100, 18736637));
                server.sendMessage(chatInfo, getDialogName(), getNickname() + " подключается к чату");
            }
            else
                throw new IllegalArgumentException("Неверный формат");
        } else if (command.startsWith("/listUsers")) {
            String[] dialog = command.split(" ");
            if (dialog.length == 1) {
                if (!mainMenu)
                    server.printList(server.getUsers(getDialogName()));
                else
                    throw new IllegalArgumentException("У вас нет прав на эту команду");
            }
            else if (dialog.length == 3) {
                if (!server.checkDialogPassword(dialog[1], new Password(dialog[2], 100, 18736637)))
                    throw new IllegalArgumentException("Неверный пароль");
                server.printList(server.getUsers(dialog[1]));
            }
            else if (dialog.length == 2) {
                if (!mainMenu && getDialogName().equals(dialog[1]))
                    server.printList(server.getUsers(getDialogName()));
                else
                    throw new IllegalArgumentException("У вас нет прав на эту команду");
            }
            else
                throw new IllegalArgumentException("Неверный формат");
        } else if (command.replace(" ", "").equals("/quit")) {
            if (!mainMenu) {
                server.deleteUserFromDialog(this, getDialogName());
                String message = getNickname() + " покидает чат";
                ArrayList<String> us = server.getUsers(getDialogName());
                if (isHistoryWriter && !us.isEmpty()) {
                    message = message.concat(". Он был писателем истории,\nтеперь писателем истории становится " + us.get(0));
                    isHistoryWriter = false;
                }
                server.sendMessage(chatInfo, getDialogName(), message);
                Thread.sleep(Server.QUIT_DELAY);
                server.messageGetting.stop();
                server.kickChecking.stop();
            }
            return false;
        }
        else if (command.replace(" ", "").equals("/dialog")) {
            if (mainMenu)
                server.chatArea.appendText("\nВы в главном меню");
            else
                server.chatArea.appendText("\n" + getDialogName());
        }
        else if (command.replace(" ", "").equals("/deleteAccount")) {
            String serverName = server.existsUser(getNickname(), Integer.toString(getHashedPassword()));
            if (serverName == null)
                throw new IOException("Ошибка удаления аккаунта");
            String[] exisitingUsers = server.readFTPFile("chat/users/users.existing.txt").split("\\n");
            String newUsers = "";
            for (String user : exisitingUsers) {
                String[] splittedUser = user.split(" ");
                if (!splittedUser[2].equals(serverName))
                    newUsers = newUsers.concat(user + "\n");
            }
            server.deleteFTPFile("chat/users/users.existing.txt");
            server.writeFTPFile("chat/users/users.existing.txt", newUsers);
            server.writeFTPFile("chat/users/users.available.txt", "\n" + serverName);
            if (!mainMenu) {
                server.deleteUserFromDialog(User.createUser(getNickname(), "1234TtTt", getDialogName(),
                        100, 18736637), getDialogName());
                String message = getNickname() + " удаляет свой аккаунт";
                ArrayList<String> us = server.getUsers(getDialogName());
                if (isHistoryWriter && !us.isEmpty()) {
                    message = message.concat(". Он был писателем истории,\nтеперь писателем истории становится " + us.get(0));
                    isHistoryWriter = false;
                }
                server.sendMessage(chatInfo, getDialogName(), message);
                Thread.sleep(Server.QUIT_DELAY);
                server.kickChecking.stop();
                server.messageGetting.stop();
            }
            return false;
        }
        else
            throw new IllegalArgumentException("Неверная команда или у Вас нет на неё прав");

        return true;
    }
}
