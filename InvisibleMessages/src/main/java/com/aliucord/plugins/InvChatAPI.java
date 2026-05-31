package com.aliucord.plugins;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class InvChatAPI {
    private static final char[] ZWC = {
            '\u200c',
            '\u200d',
            '\u2061',
            '\u2062',
            '\u2063',
            '\u2064'
    };
    private static final String[] HUFFMAN_TABLE = {
            "" + ZWC[0] + ZWC[1],
            "" + ZWC[0] + ZWC[2],
            "" + ZWC[0] + ZWC[3],
            "" + ZWC[1] + ZWC[2],
            "" + ZWC[1] + ZWC[3],
            "" + ZWC[2] + ZWC[3]
    };
    private static final SecureRandom RANDOM = new SecureRandom();

    public static boolean containsInvisibleMessage(String message) {
        if (message == null) return false;
        for (char zwc : ZWC) {
            if (message.indexOf(zwc) >= 0) return true;
        }
        return false;
    }

    public static boolean containsAny(String string, String searchChars) {
        if (string == null || searchChars == null) return false;
        for (var a : searchChars.toCharArray()) {
            if (string.contains(String.valueOf(a))) return true;
        }
        return false;
    }

    public static String encrypt(String password, String secret, String cover) throws IOException {
        if (cover == null || cover.split(" ").length < 2) {
            throw new IOException("Minimum two words required");
        }

        byte[] compressedSecret = compress(secret);
        byte[] payload = encryptPayload(password, complement(compressedSecret), false);
        String invisibleStream = shrink(toConceal(payload, true, false));
        return embed(cover, invisibleStream);
    }

    public static String decrypt(String message, String password) throws IOException {
        try {
            String expanded = expand(detach(message));
            ConcealedData concealed = concealToData(expanded);
            byte[] decrypted = concealed.encrypt
                    ? decryptPayload(password, concealed.data, concealed.integrity)
                    : concealed.data;
            return decompress(complement(decrypted));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    private static byte[] encryptPayload(String password, byte[] secret, boolean integrity) throws IOException {
        try {
            byte[] salt = new byte[8];
            RANDOM.nextBytes(salt);
            KeyMaterial keyMaterial = createKeyMaterial(password, salt);
            byte[] encrypted = aesCtr(secret, keyMaterial, Cipher.ENCRYPT_MODE);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(salt);
            if (integrity) output.write(hmacSha256(keyMaterial.key, secret));
            output.write(encrypted);
            return output.toByteArray();
        } catch (GeneralSecurityException e) {
            throw new IOException(e);
        }
    }

    private static byte[] decryptPayload(String password, byte[] payload, boolean integrity) throws GeneralSecurityException, IOException {
        int hmacLength = integrity ? 32 : 0;
        if (payload.length < 8 + hmacLength) throw new IOException("Invalid StegCloak payload");

        byte[] salt = Arrays.copyOfRange(payload, 0, 8);
        byte[] expectedHmac = integrity ? Arrays.copyOfRange(payload, 8, 40) : null;
        byte[] encrypted = Arrays.copyOfRange(payload, 8 + hmacLength, payload.length);
        KeyMaterial keyMaterial = createKeyMaterial(password, salt);
        byte[] decrypted = aesCtr(encrypted, keyMaterial, Cipher.DECRYPT_MODE);

        if (integrity && !MessageDigest.isEqual(expectedHmac, hmacSha256(keyMaterial.key, decrypted))) {
            throw new IOException("Wrong password or invalid payload");
        }

        return decrypted;
    }

    private static byte[] aesCtr(byte[] input, KeyMaterial keyMaterial, int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CTR/NoPadding");
        cipher.init(mode, new SecretKeySpec(keyMaterial.key, "AES"), new IvParameterSpec(keyMaterial.iv));
        return cipher.doFinal(input);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static KeyMaterial createKeyMaterial(String password, byte[] salt) throws GeneralSecurityException {
        byte[] key = pbkdf2Sha512(password.getBytes(StandardCharsets.UTF_8), salt, 10000, 48);
        return new KeyMaterial(Arrays.copyOfRange(key, 0, 16), Arrays.copyOfRange(key, 16, 48));
    }

    private static byte[] pbkdf2Sha512(byte[] password, byte[] salt, int iterations, int keyLength) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(password, "HmacSHA512"));

        int hLen = mac.getMacLength();
        int blockCount = (int) Math.ceil((double) keyLength / hLen);
        byte[] derived = new byte[blockCount * hLen];
        int offset = 0;

        for (int block = 1; block <= blockCount; block++) {
            mac.reset();
            mac.update(salt);
            mac.update((byte) (block >>> 24));
            mac.update((byte) (block >>> 16));
            mac.update((byte) (block >>> 8));
            mac.update((byte) block);
            byte[] u = mac.doFinal();
            byte[] t = u.clone();

            for (int i = 1; i < iterations; i++) {
                mac.reset();
                u = mac.doFinal(u);
                for (int j = 0; j < hLen; j++) t[j] ^= u[j];
            }

            System.arraycopy(t, 0, derived, offset, hLen);
            offset += hLen;
        }

        return Arrays.copyOf(derived, keyLength);
    }

    private static String toConceal(byte[] payload, boolean encrypt, boolean integrity) {
        StringBuilder result = new StringBuilder();
        result.append(integrity && encrypt ? ZWC[0] : encrypt ? ZWC[1] : ZWC[2]);

        for (byte value : payload) {
            int unsigned = value & 0xff;
            for (int shift = 6; shift >= 0; shift -= 2) {
                result.append(ZWC[(unsigned >>> shift) & 3]);
            }
        }

        return result.toString();
    }

    private static ConcealedData concealToData(String stream) {
        if (stream == null || stream.length() < 2) throw new IllegalArgumentException("Invalid invisible stream");

        int flag = zwcIndex(stream.charAt(0));
        boolean encrypt;
        boolean integrity;
        if (flag == 0) {
            encrypt = true;
            integrity = true;
        } else if (flag == 1) {
            encrypt = true;
            integrity = false;
        } else if (flag == 2) {
            encrypt = false;
            integrity = false;
        } else {
            throw new IllegalArgumentException("Unknown StegCloak payload flag");
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int bitCount = 0;
        int current = 0;
        for (int i = 1; i < stream.length(); i++) {
            int value = zwcIndex(stream.charAt(i));
            if (value < 0 || value > 3) throw new IllegalArgumentException("Invalid payload character");
            current = (current << 2) | value;
            bitCount += 2;
            if (bitCount == 8) {
                output.write(current);
                current = 0;
                bitCount = 0;
            }
        }

        return new ConcealedData(output.toByteArray(), encrypt, integrity);
    }

    private static String shrink(String secret) {
        char[] repeatChars = findOptimal(secret);
        String invisibleStream = replaceAll(secret, "" + repeatChars[1] + repeatChars[1], String.valueOf(ZWC[5]));
        invisibleStream = replaceAll(invisibleStream, "" + repeatChars[0] + repeatChars[0], String.valueOf(ZWC[4]));
        return getCompressFlag(repeatChars[0], repeatChars[1]) + invisibleStream;
    }

    private static String expand(String secret) {
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("Invalid invisible stream");
        char[] repeatChars = extractCompressFlag(secret.charAt(0));
        String invisibleStream = secret.substring(1);
        invisibleStream = replaceAll(invisibleStream, String.valueOf(ZWC[5]), "" + repeatChars[1] + repeatChars[1]);
        return replaceAll(invisibleStream, String.valueOf(ZWC[4]), "" + repeatChars[0] + repeatChars[0]);
    }

    private static char[] findOptimal(String secret) {
        Map<Character, Map<Integer, Integer>> dict = new HashMap<>();
        for (int i = 0; i < 4; i++) dict.put(ZWC[i], new HashMap<>());

        for (int j = 0; j < secret.length(); j++) {
            int count = 1;
            while (j < secret.length() - 1 && secret.charAt(j) == secret.charAt(j + 1)) {
                count++;
                j++;
            }
            if (count >= 2 && dict.containsKey(secret.charAt(j))) {
                for (int itr = count; itr >= 2; itr--) {
                    Map<Integer, Integer> charStats = dict.get(secret.charAt(j));
                    int value = charStats.getOrDefault(itr, 0) + (count / itr) * (itr - 1);
                    charStats.put(itr, value);
                }
            }
        }

        List<RankedRepeat> ranked = new ArrayList<>();
        for (Map.Entry<Character, Map<Integer, Integer>> entry : dict.entrySet()) {
            Integer score = entry.getValue().get(2);
            if (score != null) ranked.add(new RankedRepeat(entry.getKey(), score));
        }
        ranked.sort(Comparator.comparingInt((RankedRepeat repeat) -> repeat.score).reversed());

        List<Character> required = new ArrayList<>();
        for (RankedRepeat repeat : ranked) {
            if (required.size() == 2) break;
            required.add(repeat.character);
        }
        for (int i = 0; i < 4 && required.size() < 2; i++) {
            if (!required.contains(ZWC[i])) required.add(ZWC[i]);
        }

        char[] result = { required.get(0), required.get(1) };
        Arrays.sort(result);
        return result;
    }

    private static char getCompressFlag(char zwc1, char zwc2) {
        String value = "" + zwc1 + zwc2;
        for (int i = 0; i < HUFFMAN_TABLE.length; i++) {
            if (HUFFMAN_TABLE[i].equals(value)) return ZWC[i];
        }
        throw new IllegalArgumentException("Invalid compression flags");
    }

    private static char[] extractCompressFlag(char flag) {
        int index = zwcIndex(flag);
        if (index < 0 || index >= HUFFMAN_TABLE.length) throw new IllegalArgumentException("Invalid compression flag");
        return HUFFMAN_TABLE[index].toCharArray();
    }

    private static String detach(String message) {
        String[] words = message.split(" ");
        String detached = "";
        for (String word : words) {
            int limit = -1;
            for (int i = 0; i < word.length(); i++) {
                if (zwcIndex(word.charAt(i)) < 0) {
                    limit = i;
                    break;
                }
            }
            if (limit > 0) detached = word.substring(0, limit);
        }

        if (detached.isEmpty()) {
            throw new IllegalArgumentException("Invisible stream not detected");
        }

        return detached;
    }

    private static String embed(String cover, String secret) {
        String[] words = cover.split(" ");
        int targetIndex = RANDOM.nextInt(Math.max(1, words.length / 2));
        words[targetIndex + 1] = secret + words[targetIndex + 1];
        return String.join(" ", words);
    }

    private static byte[] compress(String input) {
        return input.getBytes(StandardCharsets.UTF_8);
    }

    private static String decompress(byte[] input) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(input.length * 4, 1024));

        for (int readPosition = 0; readPosition < input.length; readPosition++) {
            int inputValue = input[readPosition] & 0xff;
            if (inputValue >>> 6 != 3) {
                output.write(inputValue);
                continue;
            }

            int sequenceLengthIdentifier = inputValue >>> 5;
            if (readPosition == input.length - 1 || (readPosition == input.length - 2 && sequenceLengthIdentifier == 7)) {
                output.write(inputValue);
                continue;
            }

            int next = input[readPosition + 1] & 0xff;
            if (next >>> 7 == 1) {
                output.write(inputValue);
                continue;
            }

            int matchLength = inputValue & 31;
            int matchDistance;
            if (sequenceLengthIdentifier == 6) {
                matchDistance = next;
                readPosition += 1;
            } else {
                matchDistance = (next << 8) | (input[readPosition + 2] & 0xff);
                readPosition += 2;
            }

            byte[] currentOutput = output.toByteArray();
            int matchPosition = currentOutput.length - matchDistance;
            if (matchPosition < 0) throw new IllegalArgumentException("Invalid LZUTF8 back-reference");
            for (int offset = 0; offset < matchLength; offset++) {
                output.write(currentOutput[matchPosition + offset]);
                if (matchPosition + offset == currentOutput.length - 1) currentOutput = output.toByteArray();
            }
        }

        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static byte[] complement(byte[] data) {
        byte[] output = new byte[data.length];
        for (int i = 0; i < data.length; i++) output[i] = (byte) ~data[i];
        return output;
    }

    private static int zwcIndex(char character) {
        for (int i = 0; i < ZWC.length; i++) {
            if (ZWC[i] == character) return i;
        }
        return -1;
    }

    private static String replaceAll(String input, String target, String replacement) {
        return input.replace(target, replacement);
    }

    private static class ConcealedData {
        final byte[] data;
        final boolean encrypt;
        final boolean integrity;

        ConcealedData(byte[] data, boolean encrypt, boolean integrity) {
            this.data = data;
            this.encrypt = encrypt;
            this.integrity = integrity;
        }
    }

    private static class KeyMaterial {
        final byte[] iv;
        final byte[] key;

        KeyMaterial(byte[] iv, byte[] key) {
            this.iv = iv;
            this.key = key;
        }
    }

    private static class RankedRepeat {
        final char character;
        final int score;

        RankedRepeat(char character, int score) {
            this.character = character;
            this.score = score;
        }
    }
}
