package com.example.maxchat.Users;

import java.util.regex.Pattern;

public class Nickname {
    private String nickname;

    public Nickname(String nickname)
    {
        setNickname(nickname);
    }

    private void checkNicknameCorrectness(String nickname)
    {
        if (nickname.length() < 2)
            throw new IllegalArgumentException("Длина имени должна быть больше одного символа");
        if (nickname.length() > 20)
            throw new IllegalArgumentException("Длина имени не должна превышать десять символов");
        if (!isCorrectCharsInNickname(nickname))
            throw new IllegalArgumentException("В имени могут использоваться только латинские" +
                    " буквы, цифры и подчёркивания");
    }

    private boolean isCorrectCharsInNickname(String nickname)
    {
        return !Pattern.compile("\\W").matcher(nickname).find();
    }


    public void setNickname(String nickname)
    {
        checkNicknameCorrectness(nickname);

        this.nickname = nickname;
    }

    public String getNickname()
    {
        return nickname;
    }
}
