package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.GsonUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.models.domain.NonceGenerator;
import com.discord.models.message.Message;
import com.discord.restapi.PayloadJSON;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;

import java.lang.reflect.Constructor;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

@SuppressWarnings("unused")
@AliucordPlugin
public class EncryptDMs extends Plugin {
    public static final String KEY_PREFIX = "<edm:v1:key>:";
    public static final String ENC_PREFIX = "<edm:v1:enc>:";
    private static final String PUBLIC_KEY = "publicKey";
    private static final String PRIVATE_KEY = "privateKey";
    private static final String USER_KEYS = "userKeys";
    private static final String CHANNEL_KEYS = "channelKeys";
    private static final String ENABLED_CHANNELS = "enabledChannels";

    private final int acceptKeyViewId = View.generateViewId();
    private final int channelActionViewId = View.generateViewId();
    private final int channelToggleViewId = View.generateViewId();

    private long me;
    private String publicKey;
    private String privateKey;
    private Drawable lockIcon;

    private HashMap<Long, String> userKeys;
    private HashMap<Long, HashMap<Long, String>> channelKeys;
    private HashMap<Long, Boolean> enabledChannels;

    public static class Payload {
        public int v = 1;
        public HashMap<String, String> keys = new HashMap<>();
        public String iv;
        public String cipher;
    }

    @Override
    public void start(Context context) throws Throwable {
        this.me = StoreStream.getUsers().getMe().getId();
        this.lockIcon = ContextCompat.getDrawable(context, R.e.ic_channel_text_locked);
        if (lockIcon != null) lockIcon = lockIcon.mutate();

        ensureOwnKeyPair();
        loadSettings();

        settingsTab = new SettingsTab(EncryptDMsSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patchOutgoingMessages();
        patchMessageConstruction();
        patchMessageActions();
        patchChannelActions();
        registerCommands();
    }

    private void ensureOwnKeyPair() throws Exception {
        if (!settings.exists(PUBLIC_KEY) || !settings.exists(PRIVATE_KEY)) {
            KeyPair pair = RSA.generateKeyPair();
            settings.setString(PUBLIC_KEY, RSA.encodePublicKey(pair.getPublic()));
            settings.setString(PRIVATE_KEY, RSA.encodePrivateKey(pair.getPrivate()));
        }
        publicKey = settings.getString(PUBLIC_KEY, "");
        privateKey = settings.getString(PRIVATE_KEY, "");
    }

    private void loadSettings() {
        userKeys = settings.getObject(USER_KEYS, new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType());
        channelKeys = settings.getObject(CHANNEL_KEYS, new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType()).getType());
        enabledChannels = settings.getObject(ENABLED_CHANNELS, new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, Boolean.class).getType());
    }

    private void patchOutgoingMessages() throws NoSuchMethodException {
        patcher.patch(RestAPI.class.getDeclaredMethod("sendMessage", long.class, RestAPIParams.Message.class), new PreHook(cf -> {
            try {
                long channelId = (long) cf.args[0];
                RestAPIParams.Message message = (RestAPIParams.Message) cf.args[1];
                updateOutgoingMessage(channelId, message);
            } catch (Throwable e) {
                logger.error(e);
            }
        }));

        patcher.patch(RestAPI.class.getDeclaredMethod("editMessage", long.class, long.class, RestAPIParams.Message.class), new PreHook(cf -> {
            try {
                long channelId = (long) cf.args[0];
                RestAPIParams.Message message = (RestAPIParams.Message) cf.args[2];
                updateOutgoingMessage(channelId, message);
            } catch (Throwable e) {
                logger.error(e);
            }
        }));

        patcher.patch(RestAPI.class.getDeclaredMethod("sendMessage", long.class, PayloadJSON.class, okhttp3.MultipartBody.Part[].class), new PreHook(cf -> {
            try {
                Object data = ReflectUtils.getField(cf.args[1], "data");
                if (data instanceof RestAPIParams.Message) updateOutgoingMessage((long) cf.args[0], (RestAPIParams.Message) data);
            } catch (Throwable e) {
                logger.error(e);
            }
        }));
    }

    private void patchMessageConstruction() {
        for (Constructor<?> constructor : Message.class.getConstructors()) {
            patcher.patch(constructor, new Hook(cf -> decryptConstructedMessage(cf.thisObject)));
        }

        for (Constructor<?> constructor : com.discord.api.message.Message.class.getConstructors()) {
            patcher.patch(constructor, new Hook(cf -> decryptConstructedMessage(cf.thisObject)));
        }
    }

    private void patchMessageActions() throws NoSuchMethodException {
        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class), new Hook(cf -> {
            WidgetChatListActions actions = (WidgetChatListActions) cf.thisObject;
            Message message = ((WidgetChatListActions.Model) cf.args[0]).getMessage();
            String content = message.getContent();

            if (content == null || !content.startsWith(KEY_PREFIX)) return;
            if (message.getAuthor().getId() == me) return;

            NestedScrollView scrollView = (NestedScrollView) actions.getView();
            LinearLayout layout = (LinearLayout) scrollView.getChildAt(0);
            if (layout.findViewById(acceptKeyViewId) != null) return;

            TextView item = new TextView(layout.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);
            item.setId(acceptKeyViewId);
            item.setText("Accept Encryption Key & Send Mine");
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon, null, null, null);
            item.setOnClickListener(v -> {
                String key = content.substring(KEY_PREFIX.length()).trim();
                acceptKey(message.getChannelId(), message.getAuthor().getId(), key);
                sendMyKey(message.getChannelId());
                actions.dismiss();
            });
            layout.addView(item, Math.min(5, layout.getChildCount()));
        }));
    }

    private void patchChannelActions() throws NoSuchMethodException {
        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class), new Hook(cf -> {
            WidgetChannelsListItemChannelActions actions = (WidgetChannelsListItemChannelActions) cf.thisObject;
            WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];
            long channelId = ChannelWrapper.getId(model.getChannel());

            NestedScrollView scrollView = (NestedScrollView) actions.requireView();
            LinearLayout layout = (LinearLayout) scrollView.getChildAt(0);

            if (layout.findViewById(channelActionViewId) == null) {
                TextView sendKey = new TextView(layout.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);
                sendKey.setId(channelActionViewId);
                sendKey.setText("Send Encryption Key");
                sendKey.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon, null, null, null);
                sendKey.setOnClickListener(v -> {
                    sendMyKey(channelId);
                    actions.dismiss();
                });
                layout.addView(sendKey);
            }

            if (layout.findViewById(channelToggleViewId) == null) {
                TextView toggle = new TextView(layout.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);
                toggle.setId(channelToggleViewId);
                toggle.setText(isEnabled(channelId) ? "Disable Encrypted Chat" : "Enable Encrypted Chat");
                toggle.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon, null, null, null);
                toggle.setOnClickListener(v -> {
                    setEnabled(channelId, !isEnabled(channelId));
                    Utils.showToast(isEnabled(channelId) ? "Encrypted chat enabled" : "Encrypted chat disabled");
                    actions.dismiss();
                });
                layout.addView(toggle);
            }
        }));
    }

    private void registerCommands() {
        commands.registerCommand("encryptkey", "Sends your EncryptDMs public key to this chat", ctx ->
                new CommandsAPI.CommandResult(KEY_PREFIX + settings.getString(PUBLIC_KEY, publicKey), null, true)
        );
    }

    private void updateOutgoingMessage(long channelId, RestAPIParams.Message message) {
        String content = message.getContent();
        if (content == null || content.trim().isEmpty() || isControlMessage(content)) return;

        String encrypted = encryptForChannel(channelId, content);
        if (encrypted == null && isEnabled(channelId)) {
            encrypted = "[EncryptDMs] Could not encrypt this message. Check that this chat has accepted public keys.";
        }

        if (encrypted != null) {
            try {
                ReflectUtils.setFinalField(message, "content", encrypted);
            } catch (ReflectiveOperationException e) {
                logger.error(e);
            }
        }
    }

    private String encryptForChannel(long channelId, String plainText) {
        if (!isEnabled(channelId)) return null;

        HashMap<Long, String> recipients = getKnownKeysForChannel(channelId);
        int peerCount = 0;
        for (Long id : recipients.keySet()) if (id != me) peerCount++;
        if (peerCount == 0) return null;

        try {
            SecretKey aesKey = RSA.generateAesKey();
            Payload payload = new Payload();

            for (Map.Entry<Long, String> entry : recipients.entrySet()) {
                PublicKey key = RSA.loadPublicKey(entry.getValue());
                if (key == null) continue;
                String encryptedKey = RSA.encryptKey(aesKey.getEncoded(), key);
                if (encryptedKey != null) payload.keys.put(String.valueOf(entry.getKey()), encryptedKey);
            }

            PublicKey ownKey = RSA.loadPublicKey(settings.getString(PUBLIC_KEY, publicKey));
            if (ownKey != null) payload.keys.put(String.valueOf(me), RSA.encryptKey(aesKey.getEncoded(), ownKey));
            if (payload.keys.size() <= 1) return null;

            RSA.AesCiphertext encrypted = RSA.encryptMessage(plainText, aesKey);
            payload.iv = encrypted.iv;
            payload.cipher = encrypted.cipher;

            String encoded = ENC_PREFIX + RSA.b64(GsonUtils.toJson(payload).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return encoded.length() > 1900 ? null : encoded;
        } catch (Exception e) {
            logger.error(e);
            return null;
        }
    }

    private HashMap<Long, String> getKnownKeysForChannel(long channelId) {
        HashMap<Long, String> result = new HashMap<>();
        HashMap<Long, String> saved = channelKeys.get(channelId);
        if (saved != null) result.putAll(saved);

        Channel channel = StoreStream.getChannels().findChannelById(channelId);
        if (channel != null) {
            List<com.discord.api.user.User> recipients = channel.z();
            if (recipients != null) {
                for (com.discord.api.user.User user : recipients) {
                    String key = userKeys.get(user.getId());
                    if (key != null) result.put(user.getId(), key);
                }
            }
        }

        return result;
    }

    private void decryptConstructedMessage(Object message) {
        try {
            String content;
            if (message instanceof Message) content = ((Message) message).getContent();
            else content = ((com.discord.api.message.Message) message).i();

            String decrypted = decryptContent(content);
            if (decrypted != null) ReflectUtils.setFinalField(message, "content", decrypted);
        } catch (Throwable e) {
            logger.error(e);
        }
    }

    private String decryptContent(String content) {
        if (content == null || !content.startsWith(ENC_PREFIX)) return null;

        try {
            String json = new String(RSA.b64decode(content.substring(ENC_PREFIX.length())), java.nio.charset.StandardCharsets.UTF_8);
            Payload payload = GsonUtils.fromJson(json, Payload.class);
            if (payload == null || payload.keys == null) return "[EncryptDMs] Encrypted message";

            PrivateKey key = RSA.loadPrivateKey(settings.getString(PRIVATE_KEY, privateKey));
            if (key == null) return "[EncryptDMs] Missing private key";

            String encryptedKey = payload.keys.get(String.valueOf(me));
            if (encryptedKey != null) {
                byte[] rawKey = RSA.decryptKey(encryptedKey, key);
                String decrypted = rawKey == null ? null : RSA.decryptMessage(payload.cipher, payload.iv, rawKey);
                if (decrypted != null) return decrypted;
            }

            for (String value : payload.keys.values()) {
                byte[] rawKey = RSA.decryptKey(value, key);
                String decrypted = rawKey == null ? null : RSA.decryptMessage(payload.cipher, payload.iv, rawKey);
                if (decrypted != null) return decrypted;
            }

            return "[EncryptDMs] Encrypted message (no key)";
        } catch (Exception e) {
            logger.error(e);
            return "[EncryptDMs] Encrypted message (decrypt failed)";
        }
    }

    private void acceptKey(long channelId, long userId, String key) {
        if (RSA.loadPublicKey(key) == null) {
            Utils.showToast("Invalid encryption key");
            return;
        }

        userKeys.put(userId, key);
        HashMap<Long, String> keys = channelKeys.get(channelId);
        if (keys == null) keys = new HashMap<>();
        keys.put(userId, key);
        channelKeys.put(channelId, keys);

        saveKeySettings();
        setEnabled(channelId, true);
        Utils.showToast("Encryption key accepted");
    }

    private void sendMyKey(long channelId) {
        long targetChannelId = channelId > 0 ? channelId : StoreStream.getChannelsSelected().getId();
        if (targetChannelId <= 0) {
            Utils.showToast("Open a chat before sending your key");
            return;
        }

        Utils.threadPool.execute(() -> {
            RestAPIParams.Message message = createMessage(KEY_PREFIX + settings.getString(PUBLIC_KEY, publicKey));
            RxUtils.subscribe(RestAPI.getApi().sendMessage(targetChannelId, message), RxUtils.createActionSubscriber(
                    ignored -> Utils.mainThread.post(() -> Utils.showToast("Encryption key sent")),
                    error -> {
                        logger.error(error);
                        Utils.mainThread.post(() -> Utils.showToast("Failed to send encryption key"));
                    }
            ));
        });
    }

    private boolean isControlMessage(String content) {
        return content.startsWith(KEY_PREFIX) || content.startsWith(ENC_PREFIX);
    }

    private boolean isEnabled(long channelId) {
        Boolean enabled = enabledChannels.get(channelId);
        return enabled != null && enabled;
    }

    private void setEnabled(long channelId, boolean enabled) {
        enabledChannels.put(channelId, enabled);
        settings.setObject(ENABLED_CHANNELS, enabledChannels);
    }

    private void saveKeySettings() {
        settings.setObject(USER_KEYS, userKeys);
        settings.setObject(CHANNEL_KEYS, channelKeys);
    }

    private RestAPIParams.Message createMessage(String content) {
        return new RestAPIParams.Message(
                content,
                String.valueOf(NonceGenerator.computeNonce(ClockFactory.get())),
                null,
                null,
                emptyList(),
                null,
                new RestAPIParams.Message.AllowedMentions(emptyList(), emptyList(), emptyList(), false),
                null,
                null
        );
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
