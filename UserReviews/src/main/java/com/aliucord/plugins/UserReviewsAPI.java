package com.aliucord.plugins;

import com.aliucord.Http;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.GsonUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UserReviewsAPI {
    public static final String API_URL = "https://mantikralligi1.pythonanywhere.com";
    public static List<Review> getReviews(long userid) {
        try {
            String response = Http.simpleGet(API_URL + "/getUserReviews?discordid=" + userid);
            return GsonUtils.fromJson(response, TypeToken.getParameterized(List.class,Review.class).type);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String addReview(String comment,Long userid,String token){
        try {
            JSONObject json = new JSONObject();
            json.put("comment",comment);
            json.put("star","-1");
            json.put("token",token);
            json.put("userid",userid);
            return Http.simplePost(API_URL + "/addReview",json.toString());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return "An Error Occured";
        }
    }


}
