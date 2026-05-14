package com.aliucord.plugins;

import android.content.Intent;
import android.net.Uri;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.patcher.PreHook;
import com.aliucord.plugins.dataclasses.BaseUser;
import com.aliucord.plugins.dataclasses.Notification;
import com.aliucord.plugins.dataclasses.Response;
import com.aliucord.plugins.dataclasses.Review;
import com.aliucord.plugins.dataclasses.ReviewDBSettings;
import com.aliucord.plugins.dataclasses.ReviewVote;
import com.aliucord.plugins.dataclasses.User;
import com.aliucord.utils.GsonUtils;
import com.aliucord.utils.IOUtils;
import com.discord.restapi.RestAPIParams;
import com.discord.widgets.auth.WidgetOauth2Authorize;
import com.discord.widgets.auth.WidgetOauth2Authorize$authorizeApplication$2;
import com.google.gson.reflect.TypeToken;

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
    private static class RatingResponse {
        int rating;
    }

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

        return new Response(false, false, "Unknown error");
    }

    public static List<Review> getReviews(long userid) {
        var response = getReviewResponse(userid, 0);
        return response == null ? null : response.getReviews();
    }

    public static Response getReviewResponse(long userid, int offset) {
            int flags = 0;
            if (ReviewDB.staticSettings.getBool("disableAds",false))
                flags |= AdFlag;
            if (ReviewDB.staticSettings.getBool("disableWarnings",false))
                flags |= Warning;
            var response = simpleRequest("/api/reviewdb/users/" + userid +"/reviews?flags=" + flags + "&offset=" + offset ,"GET", null);
            if (!response.isSuccessful()) {
                return null;
            }
            return response;
    }

    public static int getRating(long userid) {
        try {
            var json = new Http.Request(API_URL + "/api/reviewdb/users/" + userid + "/rating").execute().json(RatingResponse.class);
            return json.rating;
        } catch (IOException e) {
            logger.error(e);
            return 0;
        }
    }

    public static List<ReviewVote> getVotes(long userid, String token) {
        try {
            var req = new Http.Request(API_URL + "/api/reviewdb/users/" + userid + "/reviews/votes");
            req.setHeader("Authorization", token);
            var response = req.execute().json(Response.class);
            return response.getVotes();
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    public static Response voteReview(String token, int reviewID, boolean isUpvote) {
        try {
            JSONObject json = new JSONObject();
            json.put("isUpvote", isUpvote);
            return authenticatedRequest("/api/reviewdb/reviews/" + reviewID + "/vote", "POST", token, json);
        } catch (JSONException e) {
            logger.error(e);
            return new Response(false, false, "An Error Occured");
        }
    }

    public static ReviewDBSettings getSettings(String token) {
        try {
            var req = new Http.Request(API_URL + "/api/reviewdb/settings");
            req.setHeader("Authorization", token);
            return req.execute().json(ReviewDBSettings.class);
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    public static Response setOptOut(String token, boolean optedOut) {
        try {
            var json = new JSONObject();
            json.put("opt", optedOut);
            return authenticatedRequest("/api/reviewdb/settings", "PATCH", token, json);
        } catch (JSONException e) {
            logger.error(e);
            return new Response(false, false, "An Error Occured");
        }
    }

    public static List<BaseUser> getBlockedUsers(String token) {
        try {
            var req = new Http.Request(API_URL + "/api/reviewdb/blocks");
            req.setHeader("Authorization", token);
            return req.execute().json(TypeToken.getParameterized(List.class, BaseUser.class).type);
        } catch (IOException e) {
            logger.error(e);
            return null;
        }
    }

    public static Response blockUser(String token, long discordID, boolean block) {
        try {
            var json = new JSONObject();
            json.put("action", block ? "block" : "unblock");
            json.put("discordId", Long.toString(discordID));
            return authenticatedRequest("/api/reviewdb/blocks", "PATCH", token, json);
        } catch (JSONException e) {
            logger.error(e);
            return new Response(false, false, "An Error Occured");
        }
    }

    public static Response appeal(String token, String appealText) {
        try {
            var json = new JSONObject();
            json.put("appealText", appealText);
            return authenticatedRequest("/api/reviewdb/appeals", "PUT", token, json);
        } catch (JSONException e) {
            logger.error(e);
            return new Response(false, false, "An Error Occured");
        }
    }

    public static Response markNotificationRead(String token, int notificationID) {
        return authenticatedRequest("/api/reviewdb/notifications?id=" + notificationID, "PATCH", token, null);
    }

    public static List<Notification> searchNotifications(String token) {
        var user = getUser();
        if (user == null || user.getNotification() == null) return null;
        return java.util.Collections.singletonList(user.getNotification());
    }

    public static String redirectUrl(String page) {
        var token = ReviewDB.staticSettings.getString("token", "");
        var uri = Uri.parse("https://reviewdb.mantikafasi.dev").buildUpon();
        if (!token.equals("")) {
            uri.appendPath("api").appendPath("redirect").appendQueryParameter("token", token);
            if (page != null && !page.equals("")) uri.appendQueryParameter("page", page);
        } else if (page != null && !page.equals("")) {
            uri.path(page);
        }
        return uri.build().toString();
    }

    private static Response authenticatedRequest(String endpoint, String method, String token, JSONObject body) {
        try {
            Http.Request request = new Http.Request(API_URL + endpoint, method);
            request.setHeader("Authorization", token);
            Http.Response response = body == null ? request.execute() : request.executeWithBody(body.toString());
            if (response.ok()) {
                try {
                    return response.json(Response.class);
                } catch (Exception ignored) {
                    return new Response(false, true, "Success");
                }
            }
            try (var es = request.conn.getErrorStream()) {
                var errorJson = IOUtils.readAsText(es);
                return GsonUtils.fromJson(GsonUtils.getGson(), errorJson, Response.class);
            }
        } catch (IOException e) {
            logger.error(e);
            return new Response(false, false, e.getMessage());
        }
    }

    public static int getLastReviewID(long userid) {
        try {
            return Integer.parseInt(Http.simpleGet(API_URL +"/getLastReviewID?discordid=" + userid));
        } catch (IOException | NumberFormatException e) {
            ReviewDB.logger.error(e);
            return 0;
        }
    }

    public static User getUser() {
        try {
            var req = new Http.Request(API_URL + "/api/reviewdb/users");
            if (ReviewDB.staticSettings.getString ("token", "").isEmpty()) return null;
            req.setHeader("Authorization", ReviewDB.staticSettings.getString("token", ""));
            var res = req.execute();
            return res.json(User.class);
        } catch (IOException e) {
            logger.error(e);
            return null;
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

                            cf.setResult(null);
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
