package com.aliucord.plugins;

import android.content.Intent;
import android.net.Uri;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.patcher.PreHook;
import com.aliucord.plugins.dataclasses.Response;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.utils.GsonUtils;
import com.aliucord.utils.IOUtils;
import com.discord.restapi.RestAPIParams;
import com.discord.widgets.auth.WidgetOauth2Authorize;
import com.discord.widgets.auth.WidgetOauth2Authorize$authorizeApplication$2;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import kotlin.Unit;

public class ReviewDBAPI {

    public static final String API_URL = "https://manti.vendicated.dev";
    public static final int AdFlag = 0b00000001;
    public static final int Warning = 0b00000010;
    public static String AUTH_URL = "https://discord.com/oauth2/authorize?client_id=915703782174752809&redirect_uri=https%3A%2F%2Fmanti.vendicated.dev%2Fapi%2Freviewdb%2Fauth&response_type=code&scope=identify";
    static Logger logger = new Logger("ReviewDBAPI");
    static Long CLIENT_ID = 915703782174752809L;

    public static Response simpleRequest(String endpoint,String method, JSONObject body) {
        try {
            Http.Request request = new Http.Request(API_URL + endpoint, method);
            Http.Response response;

            if (body == null)
                response = request.execute();
            else
                response = request.setFollowRedirects(false).executeWithBody(body.toString());

            Response json;
            if (response.ok()) {
                json = response.json(Response.class);
            } else {
                try (var es = request.conn.getErrorStream()) {
                    var errorJson = IOUtils.readAsText(es);
                    json = GsonUtils.fromJson(GsonUtils.getGson(),errorJson, Response.class);
                } catch (IOException exploded) {
                    logger.error(exploded);
                    json = new Response(false, false, exploded.getMessage());
                }
            }

            return json;

        } catch (IOException e) {
            ReviewDB.logger.error(e);
            e.printStackTrace();
        }
        return null;
    }

    public static List<Review> getReviews(long userid) {
            int flags = 0;
            if (ReviewDB.staticSettings.getBool("disableAds",false))
                flags |= AdFlag;
            if (ReviewDB.staticSettings.getBool("disableWarnings",false))
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
            ReviewDB.logger.error(e);
            return 0;
        }
    }

    public static Runnable unpatch;

    public static void authorize() {

        var intent = new Intent("android.intent.action.VIEW");
        intent.putExtra("REQ_URI", Uri.parse(AUTH_URL));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Utils.openPage(Utils.getAppContext(), WidgetOauth2Authorize.class, intent);

        try {
            if (unpatch == null) unpatch = ReviewDB.staticPatcher.patch(WidgetOauth2Authorize$authorizeApplication$2.class.getDeclaredMethod("invoke", RestAPIParams.OAuth2Authorize.ResponsePost.class),
                    new PreHook(cf -> {
                        var thisObject = (WidgetOauth2Authorize$authorizeApplication$2) cf.thisObject;
                        var clientID = thisObject.this$0.getOauth2ViewModel().oauthAuthorize.getClientId();
                        var arg = (RestAPIParams.OAuth2Authorize.ResponsePost) cf.args[0];

                        if (clientID == CLIENT_ID) {
                            if (unpatch != null) {
                                unpatch.run();
                                unpatch = null;
                            }

                            Utils.threadPool.execute(() -> {
                                logger.info("Got token: " + arg.getLocation());

                                try {
                                    var response = new Http.Request(arg.getLocation()).execute().json(Response.class);

                                    if (response.isSuccessful()) {
                                        ReviewDB.staticSettings.setString("token", response.getToken());
                                        Utils.showToast("Successfully Authorized", false);
                                    }
                                } catch (IOException e) {
                                    logger.error(e);
                                }
                            });
                            thisObject.this$0.getAppActivity().onBackPressed();

                            cf.setResult(Unit.a);
                        }
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
            ReviewDB.logger.error(e);
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
            ReviewDB.logger.error(e);
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
