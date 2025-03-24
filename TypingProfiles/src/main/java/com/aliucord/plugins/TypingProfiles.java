package com.aliucord.plugins;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Build;
import android.util.Pair;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.discord.databinding.WidgetChatOverlayBinding;
import com.discord.models.member.GuildMember;
import com.discord.models.user.User;
import com.discord.stores.StoreSlowMode;
import com.discord.stores.StoreStream;
import com.discord.utilities.view.extensions.ViewExtensions;
import com.discord.views.typing.TypingDots;
import com.discord.widgets.chat.overlay.ChatTypingModel;
import com.discord.widgets.chat.overlay.WidgetChatOverlay;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodReplacement;
import rx.Subscription;

@SuppressWarnings("unused")
@AliucordPlugin
public class TypingProfiles extends Plugin {

    public static Resources staticresources;
    static Subscription cooldownSub;
    Method getTypingString;
    Method getSlowmodeText;
    Subscription channelSelectedSub;

    {
        try {
            getTypingString = WidgetChatOverlay.TypingIndicatorViewHolder.class.getDeclaredMethod("getTypingString", Resources.class, List.class);
            getSlowmodeText = WidgetChatOverlay.TypingIndicatorViewHolder.class.getDeclaredMethod("getSlowmodeText", int.class, int.class, boolean.class);
            getTypingString.setAccessible(true);
            getSlowmodeText.setAccessible(true);
        } catch (NoSuchMethodException e) {
            logger.error(e);
        }
    }

    public TypingProfiles() {
        needsResources = true;
    }

    public String generateRandomString(int length) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'

        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void start(Context context) throws NoSuchMethodException {
        staticresources = resources;

        AtomicReference<WidgetChatOverlay.TypingIndicatorViewHolder> overlay = new AtomicReference<>();

        var configureTypingMethod = WidgetChatOverlay.TypingIndicatorViewHolder.class.getDeclaredMethod("configureTyping", ChatTypingModel.Typing.class);

        var linearLayoutId = View.generateViewId();

        configureTypingMethod.setAccessible(true);

        AtomicReference<Subscription> old = new AtomicReference<>();


        channelSelectedSub = RxUtils.subscribe(StoreStream.getChannelsSelected().observeId(), channelId -> {
            try {
                var channel = StoreStream.getChannels().getChannel(channelId);
                if (old.get() != null && !old.get().isUnsubscribed()) old.get().unsubscribe();
                old.set(RxUtils.subscribe(StoreStream.getUsersTyping().observeTypingUsers(channelId), _typingUsers -> {

                    if (overlay.get() != null) {
                        var typing = overlay.get();
                        try {
                            var binding = (WidgetChatOverlayBinding) ReflectUtils.getField(typing, "binding");

                            if (cooldownSub != null && !cooldownSub.isUnsubscribed())
                                cooldownSub.unsubscribe();

                            cooldownSub = RxUtils.subscribe(StoreStream.Companion.getSlowMode().observeCooldownSecs(channelId, StoreSlowMode.Type.MessageSend.INSTANCE), cooldownSecs -> {

                                Utils.mainThread.post(() -> {
                                    LinearLayout linearLayout = binding.getRoot().findViewById(linearLayoutId);
                                    linearLayout.removeAllViews();
                                    if (_typingUsers.isEmpty() && channel.x() <= 0) {
                                        // Stop TypingDots
                                        binding.d.c();
                                        // RelativeLayout
                                        binding.c.setVisibility(View.GONE);
                                        return;
                                    }

                                    binding.c.setVisibility(View.VISIBLE);

                                    var users = _typingUsers.stream().map(aLong -> StoreStream.getUsers().getUsers().get(aLong)).collect(Collectors.toList());

                                    // this is to not reimplement discords "and x others" feature
                                    Map<String, User> signatureMap = new HashMap<>();
                                    for (int i = 0; i < users.size(); i++) {
                                        signatureMap.put(generateRandomString(users.get(i).getUsername().length()), users.get(i));
                                    }

                                    String typingString;
                                    String slowmodeText;
                                    try {
                                        typingString = getTypingString.invoke(typing, binding.a.getResources(), signatureMap.keySet().stream().collect(Collectors.toList())).toString();
                                        slowmodeText = getSlowmodeText.invoke(typing, cooldownSecs, channel.x(), !typingString.trim().isEmpty()).toString();
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        logger.error(e);
                                        return;
                                    }

                                    binding.d.setVisibility(!_typingUsers.isEmpty() ? View.VISIBLE : View.GONE);

                                    if (!_typingUsers.isEmpty()) {
                                        TypingDots.b(binding.d, false, 1);
                                    } else {
                                        binding.d.c();
                                    }

                                    List<Pair<Integer, Integer>> pairs = new ArrayList<>();

                                    for (var entry : signatureMap.entrySet()) {
                                        var start = typingString.indexOf(entry.getKey());
                                        var end = start + entry.getKey().length();

                                        if (start == -1) continue;
                                        pairs.add(new Pair<>(start, end));
                                    }

                                    pairs.sort(Comparator.comparingInt(a -> a.first));

                                    var previous = 0;

                                    for (var pair : pairs) {
                                        var start = pair.first;
                                        var end = pair.second;
                                        var beginning = typingString.substring(previous, start);

                                        previous = end;
                                        var beginningTextView = new TextView(context, beginning);

                                        linearLayout.addView(beginningTextView);

                                        var t = typingString.substring(start, end);
                                        var user = signatureMap.get(t);

                                        linearLayout.addView(new ImageView(context, user));

                                        var guild = StoreStream.getGuildSelected().getSelectedGuildId();
                                        var username = "";

                                        if (guild == 0) {
                                            username = user.getUsername();
                                        } else {
                                            var member = StoreStream.getGuilds().getMembers().get(guild).get(user.getId());
                                            username = GuildMember.Companion.getNickOrUsername(member, user);
                                        }

                                        var usernameTextView = new TextView(context, username);
                                        usernameTextView.setTypeface(null, Typeface.BOLD);

                                        linearLayout.addView(usernameTextView);
                                    }

                                    TextView textView = new TextView(context, typingString.substring(previous));
                                    linearLayout.addView(textView);

                                    ViewExtensions.setTextAndVisibilityBy(binding.e, slowmodeText);
                                });
                                return null;
                            });

                        } catch (Exception e) {
                            logger.error(e);
                        }

                    }
                    return null;
                }));
            } catch (Exception e) {
                logger.error(e);
            }
            return null;
        });

        patcher.patch(WidgetChatOverlay.TypingIndicatorViewHolder.class.getDeclaredMethod("configureUI", ChatTypingModel.class), new PreHook(cf -> {
            var view = (WidgetChatOverlay.TypingIndicatorViewHolder) cf.thisObject;
            overlay.set(view);

            try {
                var binding = (WidgetChatOverlayBinding) ReflectUtils.getField(view, "binding");
                LinearLayout linearLayout = new LinearLayout(context);

                linearLayout.setGravity(Gravity.CENTER_VERTICAL);
                linearLayout.setId(linearLayoutId);
                if (binding.c.findViewById(linearLayoutId) == null)
                    binding.c.addView(linearLayout);

                var layparams = binding.e.getLayoutParams();
                layparams.width = ViewGroup.LayoutParams.MATCH_PARENT;
                linearLayout.setLayoutParams(layparams);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }));

        patcher.patch(WidgetChatOverlay.TypingIndicatorViewHolder.class.getDeclaredMethod("configureTyping", ChatTypingModel.Typing.class), XC_MethodReplacement.DO_NOTHING);

        /*
        Utils.mainThread.postDelayed(() -> {
            //var e = ResourcesCompat.getDrawable(resources,,null);

            logger.info(getClass().getPackage().getName());


            //logger.info(Utils.appActivity.getParent().toString());
            var resid = resources.getIdentifier("layout1","layout", getClass().getPackage().getName());

            logger.info(resid + "");


            frameLayout.removeAllViews();


            Thread.setDefaultUncaughtExceptionHandler((t, e) -> {});

            var xml = resources.getLayout(resid);

            Utils.appActivity.getLayoutInflater().inflate(xml, frameLayout, true);

            }, 4000);

         */

    }

    @Override
    public void stop(Context context) {
        channelSelectedSub.unsubscribe();
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}


