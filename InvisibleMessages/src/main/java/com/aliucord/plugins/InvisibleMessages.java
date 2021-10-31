package com.aliucord.plugins;

import static java.util.Collections.emptyList;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.utils.RxUtils;
import com.discord.api.message.Message;
import com.discord.api.message.embed.EmbedField;
import com.discord.api.message.embed.MessageEmbed;
import com.discord.databinding.WidgetChatInputBinding;
import com.discord.models.domain.NonceGenerator;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreStream;
import com.discord.utilities.message.MessageUtils;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.time.ClockFactory;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.chat.input.MessageDraftsRepo;
import com.discord.widgets.chat.input.WidgetChatInput;
import com.discord.widgets.chat.input.WidgetChatInputEditText;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.io.IOException;
import java.util.Collections;
import com.lytefast.flexinput.R;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.widget.FlexEditText;

import c.b.a.e.a;

@SuppressWarnings("unused")
@AliucordPlugin
public class InvisibleMessages extends Plugin {

    Logger logger = new Logger("InvisibleMessage");
    int viewID= View.generateViewId();
    Drawable lockIcon;

    @Override
    public void start(Context context) throws NoSuchMethodException {



        settingsTab = new SettingsTab(BottomShit.class,SettingsTab.Type.BOTTOM_SHEET);



        lockIcon = ContextCompat.getDrawable(context, R.d.ic_channel_text_locked);
        lockIcon = lockIcon.mutate();



        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf)->{
                        var modal = (WidgetChatListActions.Model)cf.args[0];
                        var message = modal.getMessage();
                        var actions = (WidgetChatListActions)cf.thisObject;
                        logger.info(actions.getView().getClass().getName());
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
                                        if (!dialog.getInput().isEmpty())settings.setString("password", dialog.getInput());
                                        String input = dialog.getInput().isEmpty()?settings.getString("password","Password"):dialog.getInput();

                                        String decrypedMessage = null;
                                        try {
                                            decrypedMessage = InvChatAPI.decrypt(message.getContent(), input);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            decrypedMessage = "Message Couldnt decrypted";
                                        }


                                        var embed = new MessageEmbedBuilder().setTitle("Decrypted Message").setDescription(decrypedMessage).build();
                                        message.getEmbeds().add(embed);

                                        StoreStream.getMessages().handleMessageUpdate(message.synthesizeApiMessage());


                                    }).start();
                                    dialog.dismiss();
                                });
                                dialog.show(Utils.getAppActivity().getSupportFragmentManager(),"a");
                            });
                        }




                    }));



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
                                    new Thread(()->{
                                        try{
                                            String encryptedMessage = InvChatAPI.encrypt(settings.getString("encryptionPassword","Password"),input,text);
                                            var message = createMessage(encryptedMessage);
                                            var obs =RestAPI.getApi().sendMessage(StoreStream.getChannelsSelected().getId(), message);
                                            RxUtils.subscribe(obs,message1 -> {
                                                Utils.mainThread.post(()->{Toast.makeText(context, "Send Success", Toast.LENGTH_SHORT).show();
                                                a.q.setText("");
                                                });
                                                return null;
                                            });  

                                        } catch (IOException e){
                                            Toast.makeText(context, "An Error Occured,Message Couldnt send", Toast.LENGTH_SHORT).show();
                                            logger.error(e);
                                        }
                                        }).start();

                                    dialog.dismiss();
                                }
                            }));
                            dialog.show(Utils.getAppActivity().getSupportFragmentManager(),"b");
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

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
