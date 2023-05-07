package com.aliucord.plugins;

import android.content.Intent;
import android.net.Uri;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.plugins.dataclasses.Response;
import com.aliucord.plugins.dataclasses.Review;
import com.discord.app.AppActivity;
import com.discord.restapi.RestAPIParams;
import com.discord.widgets.auth.WidgetOauth2Authorize;
import com.discord.widgets.auth.WidgetOauth2Authorize$authorizeApplication$2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class UserReviewsAPI {

    public static final String API_URL = "https://manti.vendicated.dev";
    public static final int AdFlag = 0b00000001;
    public static final int Warning = 0b00000010;
    public static String AUTH_URL = "https://discord.com/oauth2/authorize?client_id=915703782174752809&redirect_uri=https%3A%2F%2Fmanti.vendicated.dev%2Fapi%2Freviewdb%2Fauth&response_type=code&scope=identify";
    static Logger logger = new Logger("UserReviewsAPI");
    static Long CLIENT_ID = 915703782174752809L;

    public static Response simpleRequest(String endpoint,String method, JSONObject body) {
        try {
            Http.Response response;

            if (body == null)
                response = new Http.Request(API_URL + endpoint, method).execute();
            else
                response = new Http.Request(API_URL + endpoint, method).setFollowRedirects(false).executeWithBody(body.toString());


            var json = response.json(Response.class);
            UserReviews.logger.info(json.toString());
            return json;

        } catch (IOException e) {
            UserReviews.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    public static List<Review> getReviews(long userid) {
            int flags = 0;
            if (UserReviews.staticSettings.getBool("disableAds",false))
                flags |= AdFlag;
            if (UserReviews.staticSettings.getBool("disableWarnings",false))
                flags |= Warning;
            var response = simpleRequest("/api/reviewdb/users/" + userid +"/reviews?flags=" + flags,"GET", null);
            if (!response.isSuccessful()) {
                return null;
            }
            return response.getReviews() ;
    }

    public static int getLastReviewID(long userid) {
        try {
            return Integer.parseInt(Http.simpleGet(API_URL +"/getLastReviewID?discordid=" + userid));
        } catch (IOException | NumberFormatException e) {
            UserReviews.logger.error(e);
            return 0;
        }
    }

    public static void authorize() {

        var intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("REQ_URI", Uri.parse(AUTH_URL));
        intent.addFlags(268468224);

        Utils.openPage(Utils.getAppContext(), WidgetOauth2Authorize.class, intent);

        try {
            UserReviews.staticPatcher.patch(WidgetOauth2Authorize$authorizeApplication$2.class.getDeclaredMethod("invoke", RestAPIParams.OAuth2Authorize.ResponsePost.class),
                    new InsteadHook(cf -> {
                        var thisObject = (WidgetOauth2Authorize$authorizeApplication$2) cf.thisObject;
                        var clientID = thisObject.this$0.getOauth2ViewModel().oauthAuthorize.getClientId();
                        var arg = (RestAPIParams.OAuth2Authorize.ResponsePost) cf.args[0];

                        if (clientID == CLIENT_ID) {
                            Utils.threadPool.execute(() -> {
                                logger.info("Got token: " + arg.getLocation());

                                try {
                                    var response = new Http.Request(arg.getLocation()).execute().json(Response.class);

                                    if (response.isSuccessful()) {
                                        UserReviews.staticSettings.setString("token", response.getToken());
                                        Utils.showToast("Successfully Authorized", false);
                                    }

                                    var i = new Intent(Utils.appActivity, AppActivity.class);
                                    Utils.appActivity.startActivity(i);
                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            });
                        }
                        return "morb";
                    }));
        } catch (NoSuchMethodException e) {
            logger.error(e);
        }
    }

    public static Response reportReview(String token,int reviewID) {
        JSONObject json = new JSONObject();
        try {
            json.put("token",token);
            json.put("reviewid",reviewID);

            return simpleRequest("/api/reviewdb/reports","POST",json);
        } catch (JSONException e) {
            UserReviews.logger.error(e);
            return null;
        }
    }

    public static Response deleteReview(String token,int reviewid) {
        try{
            var json = new JSONObject();
            json.put("token",token);
            json.put("reviewid",reviewid);

            return simpleRequest("/api/reviewdb/users/0/reviews","DELETE",json);

        } catch (JSONException e) {
            UserReviews.logger.error(e);
            return new Response(false,false,"An Error Occured");
        }
    }

    public static Response addReview(String comment, Long userid, String token) {
        try {
            JSONObject json = new JSONObject();
            json.put("comment", comment);
            json.put("token", token);
            return simpleRequest("/api/reviewdb/users/" + userid + "/reviews","PUT",json);

        } catch (JSONException e) {
            e.printStackTrace();
            new Logger("guh").error(e);
            return new Response(false, false, "An Error Occured");
        }
    }


}
