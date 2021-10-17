package com.example.maxchat.Users;

import com.example.maxchat.HelloApplication;
import com.example.maxchat.Server.Server;

import java.io.IOException;

public class User {
    public boolean isHistoryWriter;
    protected String dialogName;
    protected final Nickname nickname;
    protected final Password password;

    public User(String nickname, String password, String dialogName, int multiplier, int primeNumber) {
        isHistoryWriter = false;
        this.dialogName = dialogName;
        this.nickname = new Nickname(nickname);
        this.password = new Password(password, multiplier, primeNumber);
    }

    public static User createUser(String nickname, String password, String dialogName, int multiplier, int primeNumber)
    {
        if (nickname.equals("Maxim"))
            return new AdminUser(nickname, password, dialogName, multiplier, primeNumber);
        else if (!nickname.equals("DefaultUser"))
            return new DefaultUser(nickname, password, dialogName, multiplier, primeNumber);
        else
            throw new IllegalArgumentException("Нельзя подключиться к чату с учётной записи DefaultUser");
    }

    public String getCommands() {
        return "/login <Имя пользователя> <Пароль> - авторизоваться\n" +
                "/register <Имя пользователя> <Пароль> <Пароль> - зарегистрироваться\n/quit - выйти из чата";
    }

    public boolean executeCommand(Server server, String command, boolean mainMenu) throws IOException, InterruptedException {
        if (command.replace(" ", "").equals("/help")) {
            server.chatArea.appendText("\n" + getCommands());
        }
        else if (command.startsWith("/login ")) {
            String[] login = command.split(" ");
            if (login.length != 3)
                throw new IllegalArgumentException("Неверный формат");
            else {
                server.login(createUser(login[1], login[2], "", 100, 18736637));
            }
        }
        else if (command.startsWith("/register ")) {
            String[] register = command.split(" ");
            if (register.length != 4)
                throw new IllegalArgumentException("Неверный формат");
            else {
                if (!register[2].equals(register[3]))
                    throw new IllegalArgumentException("Пароли не совпадают");
                if (server.getUserPassword(register[1]) != null)
                    throw new IllegalArgumentException("Такой пользователь уже существует");
                String text = server.readFTPFile("chat/users/users.available.txt");
                if (text.isBlank() || text.isEmpty())
                    throw new IllegalArgumentException("Свободные места на сервер закончились");
                String firstLine = text.substring(0, text.indexOf('\n'));
                String username = firstLine;
                User user = User.createUser(register[1], register[2], "", 100, 18736637);
                server.writeFTPFile("chat/users/users.existing.txt", register[1] + " " + user.getHashedPassword() +
                        " " + username + "\n");
                server.deleteFTPFile("chat/users/users.available.txt");
                server.writeFTPFile("chat/users/users.available.txt", text.substring(text.indexOf('\n') + 1));
                server.login(user);
            }
        }
        else if (command.replace(" ", "").equals("/quit")) {
            server.disconnect();
            return false;
        }
        else
            throw new IllegalArgumentException("Команды не существует или у вас нет прав на эту команду");
        return true;
    }


    public String getNickname()
    {
        return nickname.getNickname();
    }

    public void setNickname(String nickname)
    {
        this.nickname.setNickname(nickname);
    }

    public void setPassword(String password)
    {
        this.password.setPassword(password);
    }

    public int getHashedPassword() {
        return password.getHashedPassword();
    }

    public String getPassword() {
        return password.getPassword();
    }

    public String getDialogName() {
        return dialogName;
    }

    public void setDialogName(String dialogName) {
        this.dialogName = dialogName;
    }
}
