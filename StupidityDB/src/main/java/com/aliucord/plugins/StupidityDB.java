package com.aliucord.plugins;

import static com.lytefast.flexinput.R.i.UiKit_ImageButton;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.viewpager.widget.ViewPager;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.databinding.WidgetUserSheetBinding;
import com.discord.models.message.Message;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.CacheRequest;
import java.util.Collections;

@SuppressWarnings("unused")
@AliucordPlugin
public class StupidityDB extends Plugin {



    @Override
    public void start(Context context) throws NoSuchMethodException {
        Drawable hideIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.abc_seekbar_tick_mark_material).mutate();
        hideIcon.setTint(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.primary_dark_400));

        patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("configureItemTag", Message.class),
                new Hook((cf)->{
                    var thisobj =(WidgetChatListAdapterItemMessage)cf.thisObject;
                    var message = (Message)cf.args[0];
                    new Thread(()->{
                        try {
                            TextView itemTimestampField = (TextView) ReflectUtils.getField(cf.thisObject, "itemTimestamp");
                            String stupidity = StupidityDBAPI.getUserData(message.getAuthor().i());
                            logger.info(stupidity);

                            Utils.mainThread.post(
                                    () -> {
                                        if (itemTimestampField != null && !itemTimestampField.getText().toString().endsWith("Stupit") && stupidity!=null && !stupidity.equals("None"))
                                            itemTimestampField.setText(itemTimestampField.getText() + " %" + stupidity +" Stupit)");
                                    }
                            );
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            logger.error(e);
                            e.printStackTrace();
                        }
                    }).start();
                }));

        int twid = View.generateViewId();
        patcher.patch(WidgetUserSheet.class.getDeclaredMethod("configureProfileActionButtons", WidgetUserSheetViewModel.ViewState.Loaded.class),
                new Hook((cf)->{
                    var model = (WidgetUserSheetViewModel.ViewState.Loaded) cf.args[0];


                    var thisobj = (WidgetUserSheet) cf.thisObject;
                    var nestedScrollView = (NestedScrollView) thisobj.requireView();

                    var binding = WidgetUserSheet.access$getBinding$p(thisobj);


                    var layout = binding.A;
                    binding.f.setVisibility(View.VISIBLE);
                    View v = layout.getChildAt(0);

                    if(layout.findViewById(twid)==null){
                        ViewGroup.LayoutParams param = v.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width, param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);

                        Button button = new Button(v.getContext(), null, 0,UiKit_ImageButton);
                        button.setText("Vote Stupidity");
                        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                        button.setCompoundDrawablesRelativeWithIntrinsicBounds(null, hideIcon, null, null);
                        button.setLayoutParams(layout.getChildAt(0).getLayoutParams());

                        button.setId(twid);
                        button.setClickable(true);
                        button.setOnClickListener(v1 -> {
                            var dialog = new InputDialog().setTitle("Stupidity Level");

                            dialog.setOnDialogShownListener(v2 -> {
                                dialog.getInputLayout().getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
                            });
                            dialog.setOnOkListener(v2 -> {
                                var input = Integer.parseInt(dialog.getInput());
                                if (input>100 || input<0){
                                    Toast.makeText(context, "Input Should Be Between 0 and 100", Toast.LENGTH_SHORT).show();
                                } else {
                                    StupidityDBAPI.sendUserData(input,model.getUser().getId());
                                    dialog.dismiss();
                                }
                            });
                            dialog.show(thisobj.getChildFragmentManager(),"epic");
                        });
                        layout.addView(button);
                    }



                }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
