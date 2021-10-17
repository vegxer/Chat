package com.example.maxchat.Users;

import java.util.regex.Pattern;

public class Password {
    private int hashedPassword;
    private final int multiplier;
    private final int primeNumber;
    String password;

    public Password(String password, int multiplier, int primeNumber)
    {
        this.password = password;
        this.multiplier = multiplier;
        this.primeNumber = primeNumber;
        setPassword(password);
    }

    private void checkPassword(String password)
    {
        if (password.length() < 8)
            throw new IllegalArgumentException("Длина пароля должна быть не меньше восьми символов");
        if (password.length() > 30)
            throw new IllegalArgumentException("Длина пароля должна быть не больше тридцати символов");
        if (!isCorrectCharsInNickname(password))
            throw new IllegalArgumentException("В пароле могут содержаться только латинские буквы, цифры и знаки подчёркивания\nВ пароле должны быть каким минимум одна большая, одна маленькая буква и цифра");
    }

    private boolean isCorrectCharsInNickname(String password)
    {
        String passwordPattern = "^(?=.{8,})((\\w*[0-9]\\w*[a-z]\\w*[A-Z]\\w*)|(\\w*[0-9]\\w*[A-Z]\\w*[a-z]\\w*)" +
                "|(\\w*[a-z]\\w*[0-9]\\w*[A-Z]\\w*)|(\\w*[a-z]\\w*[A-Z]\\w*[0-9]\\w*)" +
                "|(\\w*[A-Z]\\w*[a-z]\\w*[0-9]\\w*)|(\\w*[A-Z]\\w*[0-9]\\w*[a-z]\\w*))$";

        return Pattern.matches(passwordPattern, password);
    }

    public int getHash(String str, int multiplier, int primeNumber)
    {
        int hash = str.charAt(0);

        for (int i = 1; i < str.length(); ++i)
            hash = (hash * multiplier + str.charAt(i)) % primeNumber;

        return hash;
    }

    public String getPassword() {
        return password;
    }

    public int getHashedPassword() {
        return hashedPassword;
    }

    public void setPassword(String password)
    {
        checkPassword(password);

        hashedPassword = getHash(password, multiplier, primeNumber);
    }
}
