package com.example.maxchat.Users;

import com.example.maxchat.HelloApplication;
import com.example.maxchat.Server.Server;
import com.example.maxchat.Strings.ComfortableString;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class AdminUser extends User {
    protected AdminUser(String nickname, String password, String dialogName, int multiplier, int primeNumber) {
        super(nickname, password, dialogName, multiplier, primeNumber);
    }

    @Override
    public String getCommands() {
        return "/deleteAccount <Имя пользователя> - удалить пользователя\n" +
                "/dialog - название текущего диалога\n/list - Список диалогов\n" +
                "/listUsers [<Диалог>]\n/join <Имя диалога> - подключиться к диалогу" +
        "\n/create <Название группы> <Пароль> - создать диалог\n/delete [<Название группы>] - удалить беседу\n" +
                "/kick <Имя пользователя> [<Название диалога>] - выгнать пользователя из диалога \n" +
                "/all <Сообщение> - отправить сообщение во все\n/availableUsers - доступные пользователи\n" +
                "/existingUsers - зарегистрированные пользователи\n/quit - выйти из чата";
    }

    @Override
    public boolean executeCommand(Server server, String command, boolean mainMenu) throws IOException, InterruptedException {
        User chatInfo = User.createUser("ChatInfo", getPassword(), getDialogName(), 100, 18736637);
        if (command.replace(" ", "").equals("/help")) {
            server.chatArea.appendText("\n" + getCommands());
        }
        else if (command.replace(" ", "").equals("/list"))
            server.printList(server.getDialogs());
        else if (command.startsWith("/create ")) {
            String[] dialog = command.split(" ");
            if (dialog.length == 3) {
                server.createDialog(dialog[1], new Password(dialog[2], 100, 18736637));
                server.chatArea.appendText("\nДиалог успешно создан");
            } else
                throw new IllegalArgumentException("Неверный формат");
        } else if (command.startsWith("/join ")) {
            String[] dialog = command.split(" ");
            if (dialog.length == 2) {
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
                server.addUserToDialog(this, dialog[1]);
                server.sendMessage(chatInfo, getDialogName(), getNickname() + " подключается к чату");
            } else
                throw new IllegalArgumentException("Неверный формат");
        } else if (command.startsWith("/listUsers")) {
            String dialog = command.substring(10);
            if (dialog.isEmpty()) {
                server.printList(server.getUsers());
            }
            else if (ComfortableString.count(dialog, chr -> chr == ' ') == 1) {
                server.printList(server.getUsers(dialog.replace(" ", "")));
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
        else if (command.startsWith("/delete ")) {
            if (!server.getDialogs().contains(command.substring(8)))
                throw new FileNotFoundException("Такого диалога нет");
            if (!mainMenu && command.substring(8).equals(getDialogName()))
                server.kickChecking.stop();
            server.deleteDialog(command.substring(8));
            server.chatArea.appendText("\nДиалог успешно удалён");
        }
        else if (command.startsWith("/all ")) {
            String message = command.substring(5);
            for (String dialog : server.getDialogs())
                server.sendMessage(this, dialog, message);
        }
        else if (command.startsWith("/kick ")) {
            String[] kick = command.split(" ");
            if (kick.length == 2) {
                if (mainMenu)
                    throw new IllegalArgumentException("Вы находитесь в главном меню, укажите диалог для кика пользователя");
                server.kickUser(kick[1], getDialogName(), "был кикнут администратором");
                server.chatArea.appendText("\nПользователь успешно кикнут");
            } else if (kick.length == 3) {
                server.kickUser(kick[1], kick[2], "был кикнут администратором");
                server.chatArea.appendText("\nПользователь успешно кикнут");
            } else
                throw new IllegalArgumentException("Неверный формат");
        }
        else if (command.replace(" ", "").equals("/dialog")) {
            if (mainMenu)
                server.chatArea.appendText("\nВы в главном меню");
            else
                server.chatArea.appendText("\n" + getDialogName());
        }
        else if (command.startsWith("/deleteAccount ")) {
            String[] deleteLine = command.split(" ");
            if (deleteLine.length != 2)
                throw new IllegalArgumentException("Неверный формат ввода");
            String serverName = server.existsUser(deleteLine[1], server.getUserPassword(deleteLine[1]));
            if (serverName == null)
                throw new IOException("Такого пользователя не существует");
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

            ArrayList<String> dialogs = server.getDialogs();
            User u = User.createUser(deleteLine[1], "1234TtTt", "", 100, 18736637);
            for (String dialog : dialogs) {
                if (server.existsUser(u, dialog))
                    server.kickUser(deleteLine[1], dialog, "аккаунт был удалён");
            }
            server.chatArea.appendText("\nПользователь успешно удалён");
        }
        else if (command.replace(" ", "").equals("/availableUsers"))
            server.printList(server.getAvailableUsers());
        else if (command.replace(" ", "").equals("/existingUsers"))
            server.printList(server.getExistingUsers());
        else
            throw new IllegalArgumentException("Неверная команда");

        return true;
    }
}
