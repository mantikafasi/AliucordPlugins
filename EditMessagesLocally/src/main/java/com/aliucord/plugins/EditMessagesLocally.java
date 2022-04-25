package com.aliucord.plugins;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.aliucord.CollectionUtils;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.user.CoreUser;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.messagesend.MessageQueue;
import com.discord.utilities.messagesend.MessageRequest;
import com.discord.utilities.messagesend.MessageResult;
import com.discord.widgets.chat.list.WidgetChatList;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemSystemMessage;
import com.discord.widgets.chat.list.entries.MessageEntry;

import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("unused")
@AliucordPlugin
public class EditMessagesLocally extends Plugin {
    int viewid = View.generateViewId();
    long me = StoreStream.getUsers().getMe().getId();

    @Override
    public void start(Context context) throws NoSuchMethodException {
        patcher.patch(MessageQueue.class.getDeclaredMethod("doEdit", MessageRequest.Edit.class, MessageQueue.DrainListener.class), new InsteadHook((cf) -> {
            var edit = (MessageRequest.Edit) cf.args[0];
            var listener = (MessageQueue.DrainListener) cf.args[1];
            var channelId = edit.getChannelId();
            var messageId = edit.getMessageId();
            var content = edit.getContent();
            var mes = StoreStream.getMessages().getMessage(channelId, messageId);

            if (!(new CoreUser(mes.getAuthor()).getId() == me)) {
                try {
                    ReflectUtils.setField(mes, "content", content);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.error(e);
                }
                listener.complete(new MessageResult.Success(mes.synthesizeApiMessage()));

                Utils.mainThread.post(() -> {
                    var wchlist = WidgetChatList.access$getAdapter$p(Utils.widgetChatList);
                    int i = CollectionUtils.findIndex(wchlist.getInternalData(), e -> e instanceof MessageEntry && ((MessageEntry) e).getMessage().getId() == mes.getId());
                    wchlist.notifyItemChanged(i);
                });
            } else {
                try {
                    XposedBridge.invokeOriginalMethod(cf.method,cf.thisObject,cf.args);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    logger.error(e);
                }
            }
            return null;
        }));
        Drawable editIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_edit_24dp);

        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf) -> {
                    var modal = (WidgetChatListActions.Model) cf.args[0];
                    var message = modal.getMessage();
                    var actions = (WidgetChatListActions) cf.thisObject;
                    var scrollView = (NestedScrollView) actions.getView();
                    var lay = (LinearLayout) scrollView.getChildAt(0);
                    if (lay.findViewById(viewid) == null && message.getAuthor().getId() != StoreStream.getUsers().getMe().getId()) {
                        TextView tw = new TextView(lay.getContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
                        tw.setId(viewid);
                        tw.setText("Edit Message Locally");
                        if (editIcon != null) editIcon.setTint(
                                ColorCompat.getThemedColor(lay.getContext(), com.lytefast.flexinput.R.b.colorInteractiveNormal)
                        );
                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(editIcon, null, null, null);
                        lay.addView(tw, 5);
                        tw.setOnClickListener((v) -> {
                            try {
                                ReflectUtils.invokeMethod(cf.thisObject, "editMessage", message);
                            } catch (ReflectiveOperationException e) {
                                logger.error(e);
                            }
                            actions.dismiss();
                        });
                    }
                }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
