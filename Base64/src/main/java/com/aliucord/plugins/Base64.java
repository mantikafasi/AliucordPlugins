package com.aliucord.plugins;

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
import com.aliucord.entities.MessageEmbedBuilder;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.stores.StoreStream;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
@AliucordPlugin
public class Base64 extends Plugin {
    int viewID = View.generateViewId();

    @Override
    public void start(Context context) throws NoSuchMethodException {
        Drawable lockIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_channel_text_locked).mutate();

        commands.registerCommand("base64", "Encrypts Message Using Base64", Utils.createCommandOption(ApplicationCommandType.STRING, "message", "Message you want to encrypt"), commandContext -> {
            String input = commandContext.getString("message");
            if (input != null && !input.isEmpty()) {
                return new CommandsAPI.CommandResult(android.util.Base64.encodeToString(input.getBytes(StandardCharsets.UTF_8), 0));
            }
            return new CommandsAPI.CommandResult("Message shouldnt be empty", null, false);
        });
        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf) -> {
                    var modal = (WidgetChatListActions.Model) cf.args[0];
                    var message = modal.getMessage();
                    var actions = (WidgetChatListActions) cf.thisObject;
                    var scrollView = (NestedScrollView) actions.getView();
                    var lay = (LinearLayout) scrollView.getChildAt(0);
                    if (lay.findViewById(viewID) == null && !message.getContent().contains(" ")) {
                        TextView tw = new TextView(lay.getContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
                        tw.setId(viewID);
                        tw.setText("Base64 Decode Message");
                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(lockIcon, null, null, null);
                        lay.addView(tw, 8);
                        tw.setOnClickListener((v) -> {
                            var embed = new MessageEmbedBuilder().setTitle("Base64 Decoded Message").setDescription(new String(android.util.Base64.decode(message.getContent(), 0))).build();
                            message.getEmbeds().add(embed);
                            StoreStream.getMessages().handleMessageUpdate(message.synthesizeApiMessage());
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
