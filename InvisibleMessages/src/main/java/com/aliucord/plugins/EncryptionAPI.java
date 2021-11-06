package com.aliucord.plugins;

import static com.aliucord.plugins.InvChatAPI.logger;

import com.aliucord.plugins.EncryptionUtils.AES;
import com.aliucord.plugins.EncryptionUtils.Huffman;
import com.aliucord.plugins.EncryptionUtils.LZW;

import java.math.BigInteger;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.util.Arrays;

public class EncryptionAPI {

    public static String encrypt(String message,String password){

       // String hm = Huffman.encode("EPIC COMPRESSED MESSAGE");
        String compMes = LZW.compress(message);

        String encrypted = convertStringToBinary(AES.encrypt(compMes,password));



        String a = Huffman.encode(encrypted);
        //byte[] b = binToByte(a);


        String convertedStr = convertToStupitThing(a);
        var c =  convertToZWC(convertedStr);
        return c;

    }

    public static String convertToStupitThing(String in){
        StringBuilder res = new StringBuilder();
        String b = "";
        for (char a : in.toCharArray()) {
            b+=a;

            switch (b){
                case "00":
                    res.append("A");b="";break;
                case "01":
                    res.append("B");b="";break;
                case "10":
                    res.append("C");b="";break;
                case "11":
                    res.append("D");b="";break;

            }
        }
        logger.info(in);


        return res.toString();
    }
    public static byte[] binToByte(String in){
        return new BigInteger(in, 2).toByteArray();
    }

    public static String convertStringToBinary(String input) {

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();
        for (char aChar : chars) {
            result.append(
                    String.format("%8s", Integer.toBinaryString(aChar))   // char -> int, auto-cast
                            .replaceAll(" ", "0")                         // zero pads
            );
        }
        return result.toString();

    }
    //converts binary to string
    public static String convertToString(String a){
        StringBuilder sb = new StringBuilder(); // Some place to store the chars

        Arrays.stream( // Create a Stream
                a.split("(?<=\\G.{8})") // Splits the input string into 8-char-sections (Since a char has 8 bits = 1 byte)
        ).forEach(s -> // Go through each 8-char-section...
                sb.append((char) Integer.parseInt(s, 2)) // ...and turn it into an int and then to a char
        );
    return sb.toString();
    }




    public static String decrypt(String message,String password){

        new StringBuffer("aaa");

        return "";
    }



    static String[] zwc =new String[]{"‌", "‍", "⁡", "⁢", "⁣", "⁤"}; // 200c,200d,2061,2062,2063,2064 Where the magic happens !



    public static String convertToZWC(String in){
        StringBuilder b = new StringBuilder();
        for (char c : in.toCharArray()) {
            switch (c){
                case 'A':b.append(zwc[0]);break;
                case 'B':b.append(zwc[1]);break;
                case 'C':b.append(zwc[2]);break;
                case 'D':b.append(zwc[3]);break;

            }
        }
        return b.toString();
    }

    public static String putToString(String message,String invisText){
        return message.replaceFirst(" ",invisText +" ");

    }
}
