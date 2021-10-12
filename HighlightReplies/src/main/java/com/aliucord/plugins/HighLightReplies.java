package com.aliucord.plugins;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.patcher.Hook;
import com.discord.api.channel.Channel;
import com.discord.restapi.PayloadJSON;
import com.discord.restapi.RestAPIInterface;
import com.discord.restapi.RestAPIParams;
import com.discord.stores.StoreApplication;
import com.discord.stores.StoreGatewayConnection;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.messagesend.MessageQueue$doSend$2;
import com.discord.utilities.rest.RestAPI;
import com.discord.utilities.rest.SendUtils;
import com.discord.widgets.chat.input.expression.WidgetExpressionPickerAdapter$onAttachedToRecyclerView$1;
import com.discord.widgets.chat.list.adapter.WidgetChatListItem;
import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialog$a;
import com.lytefast.flexinput.R;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;
import com.discord.databinding.WidgetChatInputBinding;
import com.discord.models.message.Message;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.chat.input.WidgetChatInput;
import com.discord.widgets.chat.input.WidgetChatInput$configureContextBarReplying$3;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$configureUI$14;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage$onConfigure$4;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import okhttp3.MultipartBody;

@SuppressWarnings("unused")
@AliucordPlugin
public class HighLightReplies extends Plugin {
    public static final Logger logger = new Logger("HighlightReplies");
    Context context ;
    public static SettingsAPI setting = null;
    Message currentMessage=null;
    WidgetChatListAdapter adapter ;
    WidgetChatListItem currentView=null;


    public int getColor(){return settings.getInt("colorInt",1677721600); }


    @Override
    public void start(Context context) throws NoSuchMethodException {
        setting = settings;
        logger.info(String.valueOf(Color.HSVToColor(100,new float[]{0,0,0})));
        settingsTab = new SettingsTab(Page.class);

        this.context= context;
        try {
            patcher.patch(MessageQueue$doSend$2.class.getDeclaredMethod("call", SendUtils.SendPayload.ReadyToSend.class),new Hook(callFrame -> {
                //if I dont patch this RestAPI.sendMessage doesnt get called for some reason
            }));
            patcher.patch(RestAPI.class.getDeclaredMethod("sendMessage", long.class, RestAPIParams.Message.class),new Hook(callFrame -> {
                unHighLight();
            }));

            patcher.patch(RestAPI.class.getDeclaredMethod("sendMessage", long.class, PayloadJSON.class, MultipartBody.Part[].class),new Hook(callFrame -> {
                unHighLight();
            }));
        } catch (NoSuchMethodException e) {
            logger.error(e);
        }

        try {
            patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),new Hook(callFrame ->{
                // if view gets recycled change its background color again
                WidgetChatListAdapterItemMessage item = (WidgetChatListAdapterItemMessage) callFrame.thisObject;
                adapter = item.adapter;
                MessageEntry message = (MessageEntry) callFrame.args[1];
                if (currentMessage!=null && currentView!=null){
                    if (message.getMessage().getId() == currentMessage.getId()){
                        WidgetChatListAdapterItemMessage view = (WidgetChatListAdapterItemMessage) callFrame.thisObject;
                        view.itemView.setBackgroundColor(getColor());
                        currentView = view;
                    }
                }
            } ));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        //When Close Button Clicked.
        patcher.patch(WidgetChatInput$configureContextBarReplying$3.class,"onClick",new Class[]{View.class},
                new Hook(callFrame -> {unHighLight();}));


        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("access$replyMessage", WidgetChatListActions.class, Message.class, Channel.class),
                new Hook(callFrame -> {
                    unHighLight();

                    Message message = (Message) callFrame.args[1];


                    Utils.mainThread.post(()->{
                        try{
                            List<ChatListEntry> data = adapter.getInternalData();
                            int i = CollectionUtils.findIndex(data, e -> e instanceof MessageEntry && ((MessageEntry) e).getMessage().getId() == message.getId());
                            WidgetChatListItem a= (WidgetChatListItem)adapter.getRecycler().findViewHolderForAdapterPosition(i);
                            currentMessage=message;
                            currentView=a;

                            a.itemView.setBackgroundColor(getColor()); }catch (Exception e){ logger.error(e); }

                    });


                }));

    }
    public void unHighLight(){
        if (currentView!=null){
            currentView.itemView.setBackgroundColor(0);
            currentView=null;
            currentMessage=null;

        }
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
