package com.aliucord.plugins;
import javax.crypto.Cipher;
import java.io.InputStream;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import android.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.util.Base64;
import android.util.Pair;

public class RSA {
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair pair = generator.generateKeyPair();

        return pair;
    }

    private static Pair<KeyFactory,X509EncodedKeySpec> generateKeyFactory(String key){
        byte[] data = Base64.decode((key.getBytes()),0);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        try{
            return new Pair<>(KeyFactory.getInstance("RSA"),spec);
        } catch (Exception e){
            return null;
        }

    }

    public static Key loadPublicKey(String stored) {
        try{
            var keyfac = generateKeyFactory(stored);
            return keyfac.first.generatePublic(keyfac.second);
        } catch (Exception e){
            return null;
        }
    }
    public static Key loadPrivateKey(String stored) {
        try{
            var keyfac = generateKeyFactory(stored);
            return keyfac.first.generatePrivate(keyfac.second);
        } catch (Exception e){return null;}
    }


    public static String encrypt(String plainText, PublicKey publicKey) {
        try{
            Cipher encryptCipher = Cipher.getInstance("RSA");
            encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(UTF_8));

            return Base64.encodeToString(cipherText,0);
        } catch (Exception e) {return null;}

    }

    public static String decrypt(String cipherText, PrivateKey privateKey) {
        try {
            byte[] bytes = Base64.decode(cipherText,0);

            Cipher decriptCipher = Cipher.getInstance("RSA");
            decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

            return new String(decriptCipher.doFinal(bytes), UTF_8);
        }catch (Exception e){
            return null;
        }

    }

    public static String sign(String plainText, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance("SHA256withRSA");
        privateSignature.initSign(privateKey);
        privateSignature.update(plainText.getBytes(UTF_8));

        byte[] signature = privateSignature.sign();

        return Base64.encodeToString(signature,0);
    }

    public static boolean verify(String plainText, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(publicKey);
        publicSignature.update(plainText.getBytes(UTF_8));

        byte[] signatureBytes = Base64.decode(signature,0);

        return publicSignature.verify(signatureBytes);
    }

    public static void main(String... argv) throws Exception {
        //First generate a public/private key pair
        KeyPair pair = generateKeyPair();
        //KeyPair pair = getKeyPairFromKeyStore();

        //Our secret message
        String message = "the answer to life the universe and everything";

        //Encrypt the message
        String cipherText = encrypt(message, pair.getPublic());

        //Now decrypt it
        String decipheredMessage = decrypt(cipherText, pair.getPrivate());

        System.out.println(decipheredMessage);

        //Let's sign our message
        String signature = sign("foobar", pair.getPrivate());

        //Let's check the signature
        boolean isCorrect = verify("foobar", signature, pair.getPublic());
        System.out.println("Signature correct: " + isCorrect);
    }
}