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
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.discord.widgets.chat.list.actions.WidgetChatListActions;
import com.discord.utilities.color.ColorCompat;

@SuppressWarnings("unused")
@AliucordPlugin
public class MessageForwarder extends Plugin {
    public static com.aliucord.api.SettingsAPI pluginSettings;
    int viewID = View.generateViewId();

    @Override
    public void start(Context context) throws NoSuchMethodException {
        pluginSettings = settings;
        Drawable shareIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_upload_24dp);

        patcher.patch(WidgetChatListActions.class.getDeclaredMethod("configureUI", WidgetChatListActions.Model.class),
                new Hook((cf) -> {
                    var modal = (WidgetChatListActions.Model) cf.args[0];
                    var message = modal.getMessage();
                    var actions = (WidgetChatListActions) cf.thisObject;
                    var scrollView = (NestedScrollView) actions.getView();
                    var lay = (LinearLayout) scrollView.getChildAt(0);

                    if (lay.findViewById(viewID) == null) {
                        TextView tw = new TextView(lay.getContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Icon);
                        tw.setId(viewID);
                        tw.setText("Forward Message");
                        if (shareIcon != null) {
                            shareIcon.mutate().setTint(
                                    ColorCompat.getThemedColor(lay.getContext(), com.lytefast.flexinput.R.b.colorInteractiveNormal)
                            );
                        }
                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(shareIcon, null, null, null);

                        // Injects near other share actions
                        lay.addView(tw, 8);

                        tw.setOnClickListener((v) -> {
                            actions.dismiss();
                            Utils.openPageWithProxy(lay.getContext(), new ForwardPage(message));
                        });
                    }
                }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
