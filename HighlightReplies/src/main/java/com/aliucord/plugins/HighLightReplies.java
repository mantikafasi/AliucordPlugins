package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

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

@SuppressWarnings("unused")
@AliucordPlugin
public class HighLightReplies extends Plugin {

    public static final Logger logger = new Logger("HighlightReplies");

    Context context ;
    Message currentMessage=null;
    WidgetChatListAdapterItemMessage currentView=null;

    @Override
    public void start(Context context) {

        patcher.patch(WidgetChatListAdapterItemMessage$onConfigure$4.class,"invoke",new Class[]{View.class},new PinePatchFn(callFrame -> {
            try {
                //callFrame.invokeOriginalMethod(); for some reason it pop ups context menu second time

                if(currentView !=null){
                    currentView.itemView.setBackgroundColor(0);
                }
                WidgetChatListAdapterItemMessage view = (WidgetChatListAdapterItemMessage) ReflectUtils.getField(callFrame.thisObject,"this$0");
                Message message = (Message) ReflectUtils.getField(callFrame.thisObject,"$message");
                currentMessage= message;
                currentView = view;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }
        ));

        try {
            patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class),new PinePatchFn(callFrame ->{
                // if view gets recycled change its background color again
                try {
                    callFrame.invokeOriginalMethod();
                } catch (InvocationTargetException | IllegalAccessException e) {
                    e.printStackTrace();
                }

                MessageEntry message = (MessageEntry) callFrame.args[1];

                if (message.getMessage().getId() == currentMessage.getId()){
                    WidgetChatListAdapterItemMessage view = (WidgetChatListAdapterItemMessage) callFrame.thisObject;
                    view.itemView.setBackgroundColor(Color.HSVToColor(100,new float[]{0,0,0}));
                    currentView = view;
                }
            } ));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        //Reply Button on Context Menu clicked
        patcher.patch(WidgetChatListActions$configureUI$14.class,"onClick",new Class[]{View.class},
                new PinePatchFn(callFrame -> {
                    try {
                        callFrame.invokeOriginalMethod();
                        WidgetChatListActions.Model model = (WidgetChatListActions.Model) ReflectUtils.getField(callFrame.thisObject,"$data");
                        if (model.getMessage().equals(currentMessage)){
                            //int id = Utils.getResId("selectableItemBackground","attr");

                            currentView.itemView.setBackgroundColor(Color.HSVToColor(100,new float[]{0,0,0}));
                        }
                    } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }));



        //When Close Button Clicked.
        patcher.patch(WidgetChatInput$configureContextBarReplying$3.class,"onClick",new Class[]{View.class},
                new PinePatchFn(callFrame -> {
                    try {
                        callFrame.invokeOriginalMethod();
                        currentView.itemView.setBackgroundColor(0);
                        currentMessage = null;
                        currentView=null;

                    } catch (InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }));


    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
