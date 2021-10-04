package com.mantikafasi.plugins;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.adjust.sdk.Reflection;
import com.aliucord.Logger;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;

import com.discord.databinding.WidgetChatInputBinding;
import com.discord.models.message.Message;
import com.discord.stores.StoreGatewayConnection;
import com.discord.widgets.chat.input.ChatInputViewModel;
import com.discord.widgets.chat.input.WidgetChatInput;
import com.discord.widgets.chat.input.WidgetChatInput$configureContextBarReplying$3;
import com.discord.widgets.chat.input.WidgetChatInputDiscoveryCommandsModel;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.actions.WidgetChatListActions$configureUI$14;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage$onConfigure$4;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@AliucordPlugin
public class byebyeSlashCommands extends Plugin {
    int tryCount=0;
    public static final Logger logger = new Logger("byebyeSlashCommands");


    Context context ;
    Message currentMessage=null;

    WidgetChatListAdapterItemMessage currentView = null;
    @Override
    public void start(Context context) {
        this.context= context;

        patcher.patch(WidgetChatListAdapterItemMessage$onConfigure$4.class,"invoke",new Class[]{View.class},new PinePatchFn(callFrame -> {
            try {
                WidgetChatListAdapterItemMessage view = (WidgetChatListAdapterItemMessage) ReflectUtils.getField(callFrame.thisObject,"this$0");
                logger.info(view.toString());

                Message message = (Message) ReflectUtils.getField(callFrame.thisObject,"$message");
                currentMessage= message;
                currentView = view;

                currentView.setIsRecyclable(false);

                //view.data
                 } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }
        ));
        patcher.patch(WidgetChatListActions$configureUI$14.class,"onClick",new Class[]{View.class},
                new PinePatchFn(callFrame -> {
                    try {
                        WidgetChatListActions.Model model = (WidgetChatListActions.Model) ReflectUtils.getField(callFrame.thisObject,"$data");
                        //model.getMessage()
                        if (model.getMessage().equals(currentMessage)){
                            //currentView.itemView.setBackgroundColor(Color.parseColor("#FFFFFF"));
                            int id = Utils.getResId("selectableItemBackground","attr");

                            currentView.itemView.setBackgroundColor(Color.HSVToColor(100,new float[]{0,0,0}));




                        }
                        logger.info("sas");
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));

        patcher.patch(WidgetChatInput.class,"configureContextBarReplying",new Class[]{ChatInputViewModel.ViewState.Loaded.PendingReplyState.Replying.class},
        new PinePatchFn(callFrame -> {

            try {
                callFrame.invokeOriginalMethod();
            } catch (InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
            try {
                Method method = ReflectUtils.getMethodByArgs(callFrame.thisObject.getClass(),"getBinding");
                WidgetChatInputBinding binding = (WidgetChatInputBinding) method.invoke(callFrame.thisObject);

                RelativeLayout lay = binding.e;


               // button.setOnClickListener(v -> currentView.itemView.setBackgroundColor(0));

                logger.info("a");
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }


        }));
        patcher.patch(WidgetChatInput$configureContextBarReplying$3.class,"onClick",new Class[]{View.class},
                new PinePatchFn(callFrame -> {
                    try {
                        callFrame.invokeOriginalMethod();
                        currentView.itemView.setBackgroundColor(0);

                    } catch (InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }));



        for (Method m: WidgetChatInputDiscoveryCommandsModel.class.getDeclaredMethods()) {
            patcher.patch("com.discord.widgets.chat.input.WidgetChatInputDiscoveryCommandsModel",m.getName(), m.getParameterTypes() ,new PinePatchFn(callFrame -> {
                callFrame.setResult(null);}));
        }
    }





    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

}
