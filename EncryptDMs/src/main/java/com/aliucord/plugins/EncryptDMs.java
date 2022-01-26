package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.annotation.SuppressLint;
import android.content.Context;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.discord.models.domain.NonceGenerator;
import com.discord.restapi.RestAPIParams;
import com.discord.utilities.time.ClockFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;

@AliucordPlugin
public class EncryptDMs extends Plugin {

    Context context;
    HashMap<Long,String> userKeys;
    HashMap<Long,String> userPrivateKeys;
    HashMap<Long,Long> userChannelMap;
    String privateKey;
    String publicKey;

    @SuppressLint({"ResourceType", "SetTextI18n"})
    @Override
    public void start(Context context) throws Throwable {
        /*
        this.context=context;

        userKeys = settings.getObject("userKeys",new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType());
        userPrivateKeys = settings.getObject("userPrivateKeys",new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType());
        userChannelMap = settings.getObject("userChannelMap",new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, Long.class).getType());


        if(!settings.exists("publicKey")){
            var keyPair = RSA.generateKeyPair();
            settings.setString("publicKey", Base64.encodeToString(keyPair.getPublic().getEncoded(),0));
            settings.setString("privateKey", Base64.encodeToString(keyPair.getPrivate().getEncoded(),0));
        }
        privateKey = settings.getString("privateKey","");
        publicKey  = settings.getString("publicKey","");

        patcher.patch(WidgetHomeHeaderManager.class.getDeclaredMethod("configure", WidgetHome.class, WidgetHomeModel.class, WidgetHomeBinding.class),
                new Hook((cf)->{
                    var a =(WidgetHome)cf.args[0];
                    var b = a.getToolbar();
                    var menuItem = b.getMenu().add("Encrypt Message");
                    menuItem.setVisible(true);
                    menuItem.setIcon(com.lytefast.flexinput.R.e.ic_perk_lock);
                    menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    menuItem.setOnMenuItemClickListener(item -> {
                         new Thread(()->{
                            var messageE = createMessage("<ewd:publickey>:" + settings.getString("publicKey","") );
                            var obs = RestAPI.getApi().sendMessage(StoreStream.getChannelsSelected().getId(),messageE);
                            RxUtils.subscribe(obs,message1 -> {logger.info(message1.toString());return null;});
                        }).start();
                        return true;
                    });
                }));

        int id = View.generateViewId();
        Drawable lockIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_perk_lock).mutate();
        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf)->{
                    var modal = (WidgetChatListActions.Model)cf.args[0];
                    var message = modal.getMessage();
                    var actions = (WidgetChatListActions)cf.thisObject;
                    var scrollView = (NestedScrollView)actions.getView();
                    var lay = (LinearLayout)scrollView.getChildAt(0);
                    var cont =message.getContent();
                    var isSelf = message.getAuthor().i() == StoreStream.getUsers().getMe().getId();

                    if (lay.findViewById(id)==null && message.getContent().startsWith("<ewd:publickey>:") && !isSelf){
                        TextView tw = new TextView(lay.getContext(),null,0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
                        tw.setId(id);
                        tw.setText("Accept E2E Chat");

                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon,null,null,null);
                        lay.addView(tw,5);
                        tw.setOnClickListener(v -> {

                            addChannelUserMap(message.getChannelId(),message.getAuthor().i());
                            addPublicKey(message.getAuthor().i(),cont.split("<ewd:publickey>:")[1]);
                            var messageE = createMessage("<ewd:publickeyUser>:" + publicKey);
                            var obs = RestAPI.getApi().sendMessage(message.getChannelId(),messageE);
                            RxUtils.subscribe(obs,message1 -> {return null;});

                            var privateKeyMes = createMessage("<ewd:privatekey>:");
                            RxUtils.subscribe(RestAPI.getApi().sendMessage(message.getChannelId(),privateKeyMes),message1 -> null);

                        });
                    }
                }));

        for (Constructor<?> constructor : Message.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{
                Message message = (Message) cf.thisObject;

                String cont = message.getContent();
                var isSelf = message.getAuthor().i() == StoreStream.getUsers().getMe().getId();

                 if (cont.startsWith("<ewd:publickeyUser>:") && !isSelf && !userKeys.containsKey(message.getAuthor().i())){
                     addChannelUserMap(message.getChannelId(),message.getAuthor().i());
                     var publicKey =cont.split("<ewd:publickeyUser>:")[1];
                     addPublicKey(message.getAuthor().i(),publicKey);
                     var messageToSend = createMessage("<ewd:privatekey>:" +privateKey );
                     RxUtils.subscribe(RestAPI.getApi().sendMessage(message.getChannelId(),messageToSend),message1 -> null);
                 } else if (cont.startsWith("<ewd:privatekey>:")&& !isSelf && !userPrivateKeys.containsKey(message.getAuthor().i())){
                     addPrivateKey(message.getAuthor().i(),cont.split("<ewd:privatekey>:")[1]);
                 }

                 if (cont.startsWith("<ewd:enc>:")){
                    if (userKeys.containsKey(message.getAuthor().i()) ) {
                        try {
                            ReflectUtils.setField(message,"content",decrypt(message.getContent()));
                            cf.setResult(null);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else if (isSelf){
                        try {
                            ReflectUtils.setField(message,"content",decrypt(userChannelMap.get(message.getChannelId()),message.getContent()));
                            cf.setResult(null);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }));
        }

        for (Constructor<?> constructor : com.discord.api.message.Message.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{
                var mes = (com.discord.api.message.Message)cf.thisObject;
                if (userKeys.containsKey(mes.e().i())){
                    try {
                        ReflectUtils.setField(mes,"content",encrypt(mes.e().i(),mes.i()));
                        cf.setResult(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }));
        }

         */
    }
    public String decrypt(long channelID,String encryptedText){
        var id = userChannelMap.get(channelID);
        if (id == null) return null;
        return RSA.decrypt(encryptedText, (PrivateKey) RSA.loadPrivateKey(userPrivateKeys.get(id)));
    }
    public String decrypt(String encryptedText){
        return RSA.decrypt(encryptedText, (PrivateKey) RSA.loadPrivateKey(settings.getString("privateKey",null)));
    }
    public String encrypt(long userID,String text){
        if (userKeys.containsKey(userID)) return RSA.encrypt(text, (PublicKey) RSA.loadPrivateKey(userKeys.get(userID)));
        return null;
    }
    public void addChannelUserMap(Long channel,Long user){
        userChannelMap.put(channel,user);
        settings.setObject("userChannelMap",userChannelMap);
    }
    public void addPublicKey(long id,String publicKey){
        userKeys.put(id,publicKey);
        settings.setObject("userKeys",userKeys);
    }
    public void addPrivateKey(long id,String privatekey){
        userPrivateKeys.put(id,privatekey);
        settings.setObject("userPrivateKeys",userPrivateKeys);
    }
    public RestAPIParams.Message createMessage(String message){
        return new RestAPIParams.Message(
                message, // Content
                String.valueOf(NonceGenerator.computeNonce(ClockFactory.get())), // Nonce
                null, // ApplicationId
                null, // Activity
                emptyList(), // stickerIds
                null, // messageReference
                new RestAPIParams.Message.AllowedMentions( // https://discord.com/developers/docs/resources/channel#allowed-mentions-object-allowed-mentions-structure
                        emptyList(), // parse
                        emptyList(), //users
                        emptyList(), // roles
                        false // repliedUser
                ),null
        );

    }
    @Override public void stop(Context context) {
        patcher.unpatchAll();
    }
}
