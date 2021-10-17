package com.example.maxchat.Strings;

public class ComfortableString
{
    static public boolean all(String str, CharCompare compare)
    {
        for (int i = 0; i < str.length(); ++i)
            if (!compare.unaryCompare(str.charAt(i)))
                return false;

        return true;
    }

    static public int count(String str, CharCompare compare)
    {
        int count = 0;

        for (int i = 0; i < str.length(); ++i)
            if (compare.unaryCompare(str.charAt(i)))
                ++count;

        return count;
    }

    static public int tryParseToUint(String str)
    {
        try
        {
            int number = Integer.parseInt(str);
            return number >= 0 ? number : -1;
        }
        catch (Exception exc)
        {
            return -1;
        }
    }
}