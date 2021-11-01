package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.models.domain.NonceGenerator;
import com.discord.models.message.Message;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;

import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.fragment.FlexInputFragment;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;

import c.b.a.e.a;

@SuppressWarnings("unused")
@AliucordPlugin
public class InvisibleMessages extends Plugin {

    Logger logger = new Logger("InvisibleMessage");
    int viewID= View.generateViewId();
    Drawable lockIcon;
    Drawable hideIcon;
    Context context;
    HashMap<Long,String> channelPasswords = settings.getObject("channelPasswords",new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, String.class).getType());

    @Override
    public void start(Context context) throws NoSuchMethodException {
        this.context = context;
        settingsTab = new SettingsTab(BottomShit.class,SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        lockIcon = ContextCompat.getDrawable(context, R.d.ic_channel_text_locked).mutate();
        hideIcon = ContextCompat.getDrawable(context,R.d.avd_show_password).mutate();
        hideIcon.setTint(ColorCompat.getColor(context,R.c.primary_dark_400));

        patchSendButton();
        patchActions();
        patchItemMessage();
        patchChannelActions();
        registerCommand();


    }
    private void patchChannelActions() throws NoSuchMethodException {
        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class)
        ,new Hook((cf)->{
                    var model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];
                    var actions = (WidgetChannelsListItemChannelActions) cf.thisObject;
                    var nestedScrollView = (NestedScrollView) actions.requireView();
                    var layout = (LinearLayout) nestedScrollView.getChildAt(0);
                    var channelId =ChannelWrapper.getId(model.getChannel());
                    Method method;
                    try {
                        method = ReflectUtils.getMethodByArgs(cf.thisObject.getClass(), "getBinding");

                        var binding = (WidgetChannelsListItemActionsBinding) method.invoke(cf.thisObject);
                        View v = binding.j;

                        ViewGroup.LayoutParams param = v.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width, param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);


                        TextView tw = new TextView(v.getContext(), null, 0, R.h.UiKit_Settings_Item_Icon);

                        tw.setText("Set Channel Password (InvisibleMessages)");


                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(hideIcon, null, null, null);
                        tw.setLayoutParams(v.getLayoutParams());

                        tw.setId(View.generateViewId());
                        tw.setOnClickListener(v1 -> {
                            var dialog = new InputDialog().setTitle("Set Password").setDescription("This password will be used for messages sent on this channel").setPlaceholderText(getPassword(channelId));
                            dialog.show(actions.getChildFragmentManager(),"c");

                            dialog.setOnOkListener(v2 -> {
                                String in = dialog.getInput().trim();
                                if (in.isEmpty() || in.equals("Password"))removePassword(channelId); else addPassword(channelId,in);
                                actions.dismiss();
                            });


                        });
                        layout.addView(tw);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) { e.printStackTrace();logger .error(e);}
                }));
    }
    private void addPassword(long channelID,String password){
        channelPasswords.put(channelID,password);
        saveSettings();
    }
    private void removePassword(long channelID){
        channelPasswords.remove(channelID);
        Toast.makeText(context, "Removed Password From Channel", Toast.LENGTH_SHORT).show();
        saveSettings();
    }
    private  void saveSettings(){
        settings.setObject("channelPasswords",channelPasswords);
    }
    private String getPassword(long channelID){
        if (channelPasswords.containsKey(channelID))return channelPasswords.get(channelID);
        return settings.getString("encryptionPassword","Password");
    }
    private void registerCommand(){
        var options = Arrays.asList(
                Utils.createCommandOption(ApplicationCommandType.STRING,"message","This is what normal people see",
                        null,true),Utils.createCommandOption(ApplicationCommandType.STRING,"hiddenMessage","Hidden message,only people that has the password can see this",null,true),
                Utils.createCommandOption(ApplicationCommandType.STRING,"password","Password to encrypt the message,if nothing gets entered default password will be used")
        );


        commands.registerCommand("invis","Send A Invisible Message",options,ctx -> {
            String message = ctx.getString("message");
            var hiddenMessage = ctx.getString("hiddenMessage");
            var password = ctx.getString("password")==null?getPassword(ctx.getChannelId()):ctx.getString("password");

            if (message.split(" ").length<2){
                return new CommandsAPI.CommandResult("Message must contain more than 1 word",null,false);
            }



            try {
                String encryptedMessage = InvChatAPI.encrypt(password,hiddenMessage,message);
                return new CommandsAPI.CommandResult(encryptedMessage,null,true);
            } catch (IOException e) {
                logger.error(e);
                return new CommandsAPI.CommandResult("An Error Occured, Check Debug log for more info",null,false);
            }


        });
    }
    private void patchItemMessage() throws NoSuchMethodException {
        patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("configureItemTag", Message.class),
                new Hook((cf)->{
                    var msg =( Message) cf.args[0] ;
                    var thisobj = (WidgetChatListAdapterItemMessage)cf.thisObject;
                    try {
                        var itemTimestampField =(TextView) ReflectUtils.getField(cf.thisObject,"itemTimestamp");

                        //var tw = (SimpleDraweeSpanTextView)ReflectUtils.getField(thisobj,"itemText");
                        if (itemTimestampField!=null){
                            if (InvChatAPI.containsInvisibleMessage(msg.getContent())){
                                itemTimestampField.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                        hideIcon,
                                        null,
                                        null,
                                        null
                                );

                                itemTimestampField.setCompoundDrawablePadding(DimenUtils.dpToPx(10));
                            } else {
                                itemTimestampField.setCompoundDrawablesRelativeWithIntrinsicBounds(
                                        null,
                                        null,
                                        null,
                                        null
                                );
                            }
                        }

                    }catch (IllegalAccessException  | NoSuchFieldException e) { e.printStackTrace();logger.error(e); } //I hate you reflectutils
                }));
    }
    private void patchActions() throws NoSuchMethodException {

        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf)->{
                    var modal = (WidgetChatListActions.Model)cf.args[0];
                    var message = modal.getMessage();
                    var actions = (WidgetChatListActions)cf.thisObject;
                    var scrollView = (NestedScrollView)actions.getView();
                    var lay = (LinearLayout)scrollView.getChildAt(0);




                    if (lay.findViewById(viewID)==null && InvChatAPI.containsInvisibleMessage(message.getContent())  ){
                        TextView tw = new TextView(lay.getContext(),null,0, R.h.UiKit_Settings_Item_Icon);
                        tw.setId(viewID);
                        tw.setText("Decrypt Message");

                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon,null,null,null);
                        lay.addView(tw,5);
                        //tw.setLayoutParams(lay.getChildAt(3).getLayoutParams());
                        tw.setOnClickListener((v)->{

                            InputDialog dialog = new InputDialog().setTitle("Enter Password");
                            dialog.setPlaceholderText(settings.getString("password","Password"));

                            dialog.setOnOkListener(v1 -> {

                                new Thread(()->{
                                    String dialogIn = dialog.getInput().trim();
                                    if (!dialogIn.isEmpty())settings.setString("password", dialogIn);
                                    String input = dialogIn.isEmpty()?settings.getString("password","Password"):dialogIn;

                                    String decrypedMessage;
                                    try {
                                        decrypedMessage = InvChatAPI.decrypt(message.getContent(), input);
                                    } catch (Exception e) {
                                        logger.error(e);
                                        decrypedMessage = "Message Couldnt decrypted,You can check Debug Log for more info";
                                    }


                                    var embed = new MessageEmbedBuilder().setTitle("Decrypted Message").setDescription(decrypedMessage).build();
                                    message.getEmbeds().add(embed);

                                    StoreStream.getMessages().handleMessageUpdate(message.synthesizeApiMessage());


                                }).start();
                                dialog.dismiss();
                                actions.dismiss();
                            });
                            dialog.show(actions.getChildFragmentManager(),"a");

                        });
                    }




                }));

    }
    private void patchSendButton() throws NoSuchMethodException {
        patcher.patch(FlexInputFragment.class.getDeclaredMethod("onViewCreated", View.class, Bundle.class),
                new Hook((cf)->{

                    var thisObject = (FlexInputFragment)cf.thisObject;
                    a a = thisObject.j();


                    a.o.setOnLongClickListener((v)->{
                        String text =a.q.getText().toString();
                        if (text.split(" ").length<2){
                            Toast.makeText(context, "You need to have at least 2 words to send encrypted", Toast.LENGTH_SHORT).show() ;
                        } else {
                            InputDialog dialog = new InputDialog().setTitle("Write the message you want to hide");
                            dialog.setOnOkListener((v1 -> {
                                String input = dialog.getInput();
                                if (input.isEmpty()){
                                    Toast.makeText(context, "Write Something And Try Again", Toast.LENGTH_SHORT).show();
                                } else {
                                    a.q.setText("");
                                    new Thread(()->{
                                        try{
                                            long chid = StoreStream.getChannelsSelected().getId();
                                            String encryptedMessage = InvChatAPI.encrypt(getPassword(chid),input,text);
                                            var message = createMessage(encryptedMessage);
                                            var obs =RestAPI.getApi().sendMessage(chid, message);
                                            RxUtils.subscribe(obs,message1 -> null);

                                        } catch (Exception e){
                                            Utils.mainThread.post(()->Toast.makeText(context, "An Error Occured,Message Couldn't send", Toast.LENGTH_SHORT).show());
                                            logger.error(e);
                                        }
                                        }).start();
                                    dialog.dismiss();
                                }
                            }));
                            dialog.show(thisObject.getChildFragmentManager(),"b");
                        }
                        return "pogchamp".equals("pogchamp");
                    });

                }));
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
                )
        );

    }
    @Override public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
