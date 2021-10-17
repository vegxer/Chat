package com.example.maxchat.Server;

import com.example.maxchat.EventHandlers.OnKeyPressedHandler;
import com.example.maxchat.HelloApplication;
import com.example.maxchat.Threads.DeleteWaiting;
import com.example.maxchat.Users.Password;
import com.example.maxchat.Users.User;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Server {
    public static final String CURR_VERSION = "0.0.1";
    public static final long READ_DELAY = 200;
    public static final long QUIT_DELAY = READ_DELAY * 4;
    public static final String CHAT_PATH = "chat/";
    public static final String DIALOGS_PATH = CHAT_PATH + "dialogs/";
    public static final String USERS_PATH = CHAT_PATH + "users/";
    public static final String UPDATES_PATH = CHAT_PATH + "updates/";
    private static final String STANDARD_PASS = "ʱŢɣԳՄХӁ̐";
    public static final String ANON_PASS = "Ȍȱ˴ѢɊƘŢŗŷŷ";
    private static final String SERVER_ADDRESS_DDNS = "ѲλԣдѲӑϷӁԂϷӁĸХґҡӡґĸґӁЕ";
    private static final String SERVER_ADDRESS_IP = "ŷƍĸŢƂŢĸŗŢƂĸŗŬō";
    public boolean mainMenu = false;
    public User user;
    private FTPClient client;
    public TextArea chatArea, messageArea;
    public Thread kickChecking;
    public Thread messageGetting;
    public Thread messageAreaDisabling;
    public Thread deleteWaiting;

    public Server(TextArea chatArea, TextArea messageArea) {
        this.chatArea = chatArea;
        this.messageArea = messageArea;
    }

    public void start() throws IOException {
        this.user = new User("DefaultUser", decode(ANON_PASS), "", 100, 18736637);
        client = new FTPClient();
        client.connect(decode(SERVER_ADDRESS_IP));
        client.enterLocalPassiveMode();
        client.login(user.getNickname(), decode(ANON_PASS));
        checkUpdates();
        chatArea.setOnInputMethodTextChanged(inputMethodEvent ->
                chatArea.selectRange(chatArea.getText().length(), chatArea.getText().length()));
        chatArea.appendText("\nМЕНЮ АВТОРИЗАЦИИ\n/help - для просмотра команд");
        messageArea.setOnKeyPressed(new OnKeyPressedHandler(this));
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void reconnect() throws IOException {
        if (client.isConnected())
            client.disconnect();
        client.connect(decode(SERVER_ADDRESS_IP));
        client.enterLocalPassiveMode();
    }

    public void disconnect() throws IOException {
        client.disconnect();
    }

    public void login(User user) throws IOException, InterruptedException {
        if (user.getNickname().equals("Maxim") || user.getNickname().equals("DefaultUser")) {
            reconnect();
            if (!client.login(user.getNickname(), user.getPassword())) {
                client.disconnect();
                throw new FTPConnectionClosedException("Неверный логин или пароль");
            }
        }
        else {
            String foundUser = existsUser(user.getNickname(), Integer.toString(user.getHashedPassword()));
            reconnect();
            if (foundUser == null || !client.login(foundUser, decode(STANDARD_PASS) + foundUser.split("_")[1])) {
                client.login("DefaultUser", decode(ANON_PASS));
                throw new FTPConnectionClosedException("Неверный логин или пароль");
            }
        }

        this.user = user;
    }

    public void checkUpdates() throws IOException {
        FTPFile[] update = client.listFiles(UPDATES_PATH);
        if (update.length == 0)
            return;
        if (update.length > 1)
            throw new FileNotFoundException("Ошибка нахождения обновлений");

        String updateVersion = update[0].getName().split("[()]")[1];
        if (!updateVersion.equals(CURR_VERSION)) {

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Обновление");
            alert.setHeaderText(null);
            alert.setContentText("Найдено обновление " + updateVersion + "\nХотите обновить приложение?");
            alert.showAndWait();
            if (alert.getResult() == ButtonType.OK) {
                String runningFileName = HelloApplication.class.getProtectionDomain().getCodeSource().getLocation()
                        .getPath().substring(1);
                System.out.println(runningFileName);
                String updateFile = UPDATES_PATH + update[0].getName();
                File downloadDest = new File(System.getProperty("user.dir") + "/" + update[0].getName());

                OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadDest));
                client.retrieveFile(updateFile, outputStream);
                outputStream.close();
                client.disconnect();
                Platform.exit();
                Runtime.getRuntime().exec(downloadDest.getAbsolutePath());
                writeTo("FILE_TO_DELETE.txt", runningFileName);
                System.exit(0);
            }
        }
    }

    public void deleteFTPFile(String filePath) throws IOException {
        client.deleteFile(filePath);
    }

    public String existsUser(String username, String hashedPassword) throws IOException, InterruptedException {
        String text = readFTPFile("chat/users/users.existing.txt");

        String[] splitted = text.split("\\n");
        for (String user : splitted) {
            String[] splittedUser = user.split(" ");
            if (splittedUser[0].equals(username) && splittedUser[1].equals(hashedPassword))
                return splittedUser[2];
        }

        return null;
    }

    public List<String> getExistingUsers() throws IOException, InterruptedException {
        return Arrays.stream(readFTPFile("chat/users/users.existing.txt").split("\\n")).toList();
    }

    public List<String> getAvailableUsers() throws IOException, InterruptedException {
        return Arrays.stream(readFTPFile("chat/users/users.available.txt").split("\\n")).toList();
    }

    public String getUserPassword(String username) throws IOException, InterruptedException {
        String text = readFTPFile("chat/users/users.existing.txt");

        String[] splitted = text.split("\\n");
        for (String user : splitted) {
            String[] splittedUser = user.split(" ");
            if (splittedUser[0].equals(username))
                return splittedUser[1];
        }

        return null;
    }



    public boolean existsUser(User user, String dialog) throws IOException, InterruptedException {
        return getUsers(dialog).contains(user.getNickname());
    }

    public boolean existsUser(User user) throws IOException, InterruptedException {
        return getUsers().contains(user.getNickname());
    }

    public boolean existsFile(String filePath) throws IOException {
        /*String fileName = filePath.substring(filePath.lastIndexOf('/') + 1);
        FTPFile[] files = client.listFiles(filePath.substring(0, filePath.lastIndexOf('/')));
        for (FTPFile file : files) {
            if (file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;*/
        //client.changeWorkingDirectory("/");
        //System.out.println(client.printWorkingDirectory());
        //String s = client.printWorkingDirectory();
        String na1 = filePath.substring(0, filePath.lastIndexOf('/') + 1), na2 = filePath.substring(filePath.lastIndexOf('/') + 1);
        client.changeWorkingDirectory(na1);
        //String g = client.printWorkingDirectory();

        //client.enterLocalActiveMode();
        String file = client.getModificationTime(na2);
        //client.enterLocalPassiveMode();
        //if (file == null)
        //    System.out.println();
        client.changeWorkingDirectory("/");
        //String s = client.printWorkingDirectory();
        /*if (file == null)
            return client.getModificationTime(filePath) != null;*/
        return file != null;
    }

    public void deleteDialog(String dialogName) throws IOException, InterruptedException {
        sendMessage(User.createUser("ChatInfo", user.getPassword(), user.getDialogName(), 100, 18736637),
                dialogName, "ВНИМАНИЕ! ДИАЛОГ УДАЛЯЕТСЯ!");
        Thread.sleep(QUIT_DELAY);
        deleteDirectory(DIALOGS_PATH + dialogName);
    }

    private void deleteDirectory(String path) throws IOException {
        for (FTPFile file : client.listFiles(path)) {
            String currFile = path + "/" + file.getName();
            if (file.isDirectory())
                deleteDirectory(currFile);
            else
                deleteFile(currFile);
        }

        client.removeDirectory(path);
    }

    public void kickUser(String user, String dialogName, String kickMessage) throws InterruptedException, IOException {
        User u = User.createUser(user, "1234TttT", "", 100, 18736637);
        deleteUserFromDialog(u, dialogName);
        sendMessage(User.createUser("ChatInfo", "1234TttT", "", 100,
                18736637), dialogName, user + " " + kickMessage);
        Thread.sleep(QUIT_DELAY);
    }

    public boolean checkDialogPassword(String dialogName, Password password) throws IOException, InterruptedException {
        if (!client.isConnected())
            Thread.sleep(100);
        if (!getDialogs().contains(dialogName))
            throw new FileNotFoundException("Такого диалога не существует");
        String text = readFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        return text.substring(0, text.indexOf('\n')).equals(Integer.toString(password.getHashedPassword()));
    }

    public<T> void printList(List<T> list) {
        if (list.isEmpty())
            chatArea.appendText("\nНичего не найдено");
        for (T element : list) {
            chatArea.appendText("\n" + element);
        }
    }

    public ArrayList<String> getUsers() throws IOException, InterruptedException {
        ArrayList<String> users = new ArrayList<>();
        ArrayList<String> dialogs = getDialogs();
        for (String dialog : dialogs) {
            ArrayList<String> currUsers = getUsers(dialog);
            for (String user : currUsers) {
                if (!users.contains(user))
                    users.add(user);
            }
        }

        return users;
    }

    public ArrayList<String> getUsers(String dialogName) throws IOException, InterruptedException {
        if (!client.isConnected())
            Thread.sleep(100);
        if (!getDialogs().contains(dialogName)) {
            Thread.sleep(100);
            if (!getDialogs().contains(dialogName))
                throw new FileNotFoundException("Такого диалога не существует");
        }

        String text = readFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        ArrayList<String> users = new ArrayList<>(Arrays.stream(text.split("\\n")).toList());
        users.remove(0);
        return users;
    }

    public void writeTo(String filePath, String message) throws IOException {
        FileWriter writer = new FileWriter(filePath);
        writer.append(message);
        writer.flush();
        writer.close();
    }

    public void sendMessage(User sourceUser, String dialogName, String message) throws IOException {
        writeTo("1message.txt", encode(message));
        FileInputStream input = new FileInputStream("1message.txt");
        client.appendFile(DIALOGS_PATH + dialogName + "/messages/" + sourceUser.getNickname() + ".txt",  input);
        input.close();
        if (!new File("1message.txt").delete())
            throw new IOException("Не удалось удалить временный файл message.txt");
    }

    public String encodeFunction(char letter) {
        return Character.toString((char)((int)Math.round(Math.pow((int)letter, 1.5d))));
    }

    public String decodeFunction(char letter) {
        return Character.toString((char)((int)Math.round(Math.pow((int)letter, 1d / 1.5d))));
    }

    public String encode(String message) {
        String encoded = "";

        for (int i = 0; i < message.length(); ++i) {
            encoded = encoded.concat(encodeFunction(message.charAt(i)));
        }

        return encoded;
    }

    public String decode(String message) {
        String decoded = "";

        for (int i = 0; i < message.length(); ++i) {
            decoded = decoded.concat(decodeFunction(message.charAt(i)));
        }

        return decoded;
    }

    public ArrayList<String> getDialogs() throws IOException {
        ArrayList<String> dialogs = new ArrayList<>();
        FTPFile[] files = client.listDirectories(DIALOGS_PATH);
        for (FTPFile file : files)
            dialogs.add(file.getName());

        return dialogs;
    }

    public String getMessage() throws IOException, InterruptedException {
        FTPFile[] files = client.listFiles(DIALOGS_PATH + user.getDialogName() + "/messages/");
        for (FTPFile file : files) {
            if (file.isFile()) {
                String lastFile = DIALOGS_PATH + user.getDialogName() + "/messages/" + file.getName();
                deleteWaiting = new Thread(new DeleteWaiting(this, files.length, lastFile));
                deleteWaiting.start();
                String fileText = decode(readFTPFile(lastFile));
                return file.getTimestamp().getTime().toLocaleString().replace(" г.", "")
                        .replaceFirst(":00$", "") + "->" +
                        file.getName().substring(0, file.getName().indexOf('.')) + ": " + fileText;
            }
        }

        return null;
    }

    public void deleteFile(String path) throws IOException {
        client.deleteFile(path);
    }

    public void createDialog(String name, Password password) throws IOException {
        writeTo(name + ".txt", password.getHashedPassword() + "\n\n");
        FileInputStream input = new FileInputStream(name + ".txt");
        client.makeDirectory(DIALOGS_PATH + name);
        client.makeDirectory(DIALOGS_PATH + name + "/messages");
        client.appendFile(DIALOGS_PATH + name + "/" + name + ".txt",  input);
        client.appendFile(DIALOGS_PATH + name + "/" + "history.txt", InputStream.nullInputStream());
        input.close();
        if (!new File(name + ".txt").delete())
            throw new IOException("Не удалось удалить временный файл message.txt");
    }

    public String readFTPFile(String filePath) throws IOException {
        if (client.isConnected()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            String na = filePath.substring(filePath.lastIndexOf('/') + 1);
            client.changeWorkingDirectory(filePath.substring(0, filePath.lastIndexOf('/')));
            //client.enterLocalActiveMode();
            client.retrieveFile(na, outputStream);
            //client.enterLocalPassiveMode();
            client.changeWorkingDirectory("/");

            //client.enterLocalPassiveMode();
            InputStreamReader inputReader = new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray()));
            BufferedReader buffReader = new BufferedReader(inputReader);
            String fileText = buffReader.lines().collect(Collectors.joining("\n"));
            outputStream.close();
            inputReader.close();
            buffReader.close();
            return fileText;
        }
        return null;
    }

    public void writeFTPFile(String filePath, String text) throws IOException {
        String path = filePath.substring(filePath.lastIndexOf('/') + 1);
        writeTo(path, text);
        FileInputStream input = new FileInputStream(path);
        client.appendFile(filePath, input);
        input.close();
        if (!new File(path).delete())
            throw new IOException("Не удалось удалить временный файл");
    }

    public void addUserToDialog(User user, String dialogName) throws IOException, InterruptedException {
        if (!client.isConnected())
            Thread.sleep(100);
        if (!getDialogs().contains(dialogName))
            throw new FileNotFoundException("Такого диалога не существует");
        String text = readFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        client.deleteFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        writeFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt", text + user.getNickname() + "\n\n");
    }

    public void addUserToDialog(User user, String dialogName, Password password) throws IOException, InterruptedException {
        if (!client.isConnected())
            Thread.sleep(100);
        if (!getDialogs().contains(dialogName))
            throw new FileNotFoundException("Такого диалога не существует");
        String text = readFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        if (!text.substring(0, text.indexOf('\n')).equals(Integer.toString(password.getHashedPassword())))
            throw new FTPConnectionClosedException("Неверный пароль диалога");
        client.deleteFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        writeFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt", text + user.getNickname() + "\n\n");
    }

    public void deleteUserFromDialog(User user, String dialogName) throws IOException, InterruptedException {
        if (!client.isConnected())
            Thread.sleep(100);
        if (!getDialogs().contains(dialogName)) {
            Thread.sleep(100);
            if (!getDialogs().contains(dialogName))
                throw new FileNotFoundException("Ошибка нахождения диалога");
        }
        else if (!existsUser(user, dialogName)) {
            if (messageGetting.isAlive())
                messageGetting.stop();
            mainMenu = true;
            chatArea.setText("\nГЛАВНОЕ МЕНЮ\n/help - для просмотра команд");
        }

        String text = readFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        writeTo(dialogName + ".txt", text);
        BufferedReader reader = new BufferedReader(new FileReader(dialogName + ".txt"));
        String line, users = "";
        while ((line = reader.readLine()) != null) {
            if (!line.equals(user.getNickname()))
                users = users.concat(line + "\n");
        }
        users += "\n";
        reader.close();
        if (!new File(dialogName + ".txt").delete())
            throw new IOException("Не удалось удалить временный файл message.txt");
        client.deleteFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt");
        writeFTPFile(DIALOGS_PATH + dialogName + "/" + dialogName + ".txt", users);
    }
}
