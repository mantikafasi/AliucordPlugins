package com.aliucord.plugins;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

public class RSA {
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class AesCiphertext {
        public String iv;
        public String cipher;

        public AesCiphertext(String iv, String cipher) {
            this.iv = iv;
            this.cipher = cipher;
        }
    }

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        return generator.generateKeyPair();
    }

    public static String encodePublicKey(PublicKey key) {
        return b64(key.getEncoded());
    }

    public static String encodePrivateKey(PrivateKey key) {
        return b64(key.getEncoded());
    }

    public static PublicKey loadPublicKey(String stored) {
        try {
            byte[] data = b64decode(stored);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(data));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static PrivateKey loadPrivateKey(String stored) {
        try {
            byte[] data = b64decode(stored);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(data));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static SecretKey generateAesKey() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256, RANDOM);
        return generator.generateKey();
    }

    public static String encryptKey(byte[] key, PublicKey publicKey) {
        try {
            Cipher cipher = getRsaCipher();
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepSpec());
            return b64(cipher.doFinal(key));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static byte[] decryptKey(String cipherText, PrivateKey privateKey) {
        try {
            Cipher cipher = getRsaCipher();
            cipher.init(Cipher.DECRYPT_MODE, privateKey, oaepSpec());
            return cipher.doFinal(b64decode(cipherText));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static AesCiphertext encryptMessage(String plainText, SecretKey secretKey) throws Exception {
        byte[] iv = new byte[12];
        RANDOM.nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));

        return new AesCiphertext(
                b64(iv),
                b64(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)))
        );
    }

    public static String decryptMessage(String cipherText, String iv, byte[] rawKey) {
        try {
            SecretKeySpec key = new SecretKeySpec(rawKey, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, b64decode(iv)));
            return new String(cipher.doFinal(b64decode(cipherText)), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String b64(byte[] data) {
        return android.util.Base64.encodeToString(data, android.util.Base64.NO_WRAP);
    }

    public static byte[] b64decode(String data) {
        return android.util.Base64.decode(data, android.util.Base64.NO_WRAP);
    }

    private static Cipher getRsaCipher() throws Exception {
        return Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    }

    private static OAEPParameterSpec oaepSpec() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }
}
