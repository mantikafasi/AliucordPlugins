package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class InvChatAPI {
    public static String URL = "https://InvisibleChatAPI.hubertmoszkarel.repl.co";
    static Logger logger = new Logger("InvChatAPI");
    static String regex = "[\u200c\u200d\u2062\u2063\u2063]"; //husk

    public static boolean containsInvisibleMessage(String message) {

        return containsAny(message, "\u200c\u2062\u2063\u2063");
    }

    public static boolean containsAny(String string, String searchChars) {
        for (var a : searchChars.toCharArray()) {
            if (string.contains(String.valueOf(a))) return true;
        }
        return false;
    }


    public static String encrypt(String password, String secret, String cover) throws IOException {

        JSONObject json = new JSONObject();
        try {
            json.put("type", "hide").put("password", password).put("secret", secret + "\u200b").put("cover", cover);
            String encryptedMessage = makeJSONRequest(json);
            return ("\u200b" + encryptedMessage);
        } catch (JSONException e) {
            e.printStackTrace();
            logger.error(e);
        }
        return null; //fail  :sob:

    }

    public static String decrypt(String message, String password) throws IOException {
        JSONObject json = new JSONObject();
        try {
            json.put("type", "reveal").put("password", password).put("secret", message);
            return makeJSONRequest(json);
        } catch (JSONException e) {
            logger.error(e);
            e.printStackTrace();
        }

        return null; //fail :husk:
    }

    public static String makeJSONRequest(JSONObject json) throws IOException, JSONException {

        var request = new Http.Request(URL, "POST");
        request.setHeader("Content-Type", "application/json");
        var res = request.executeWithBody(json.toString());
        String text = res.text();
        //if (text.startsWith("\""))text = text.substring(1, text.length() - 1);
        var jsonObj = new JSONObject(text);
        if (jsonObj.getBoolean("isCorrectPassword")) {
            return new JSONObject(text).getString("response");
        }
        return "Wrong Password";

    }

}
