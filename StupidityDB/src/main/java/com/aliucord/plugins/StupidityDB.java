package com.aliucord.plugins;

import static com.lytefast.flexinput.R.i.UiKit_ImageButton;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.discord.models.message.Message;
import com.discord.utilities.color.ColorCompat;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage;
import com.discord.widgets.user.profile.UserProfileHeaderView;
import com.discord.widgets.user.profile.UserProfileHeaderViewModel;
import com.discord.widgets.user.usersheet.WidgetUserSheet;
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel;
import com.lytefast.flexinput.R;

@SuppressWarnings("unused")
@AliucordPlugin
public class StupidityDB extends Plugin {
    Context context;
    int profileheadertextid = View.generateViewId();
    int twid = View.generateViewId();
    Drawable stupidityIcon;
    public static SettingsAPI staticSettings;

    @Override
    public void start(Context context) throws NoSuchMethodException {
        this.context = context;
        staticSettings = settings;
        stupidityIcon = ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_emoji_24dp).mutate();
        stupidityIcon.setTint(ColorCompat.getColor(context, com.lytefast.flexinput.R.c.primary_dark_400));
        settingsTab = new SettingsTab(BottomShit.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);
        var is110 = settings.getBool("is110", true);
        if (is110) {
            settings.setBool("useOAUTH2", !StupidityDBAPI.isUserinServer());
            settings.setBool("is110", false);
        }

        patchProfileHeaderView();
        patchWidgetChatListAdapterItemMessage();
        patchWidgerUserSheet();
    }

    public void patchWidgerUserSheet() throws NoSuchMethodException {
        patcher.patch(WidgetUserSheet.class.getDeclaredMethod("configureProfileActionButtons", WidgetUserSheetViewModel.ViewState.Loaded.class),
                new Hook((cf) -> {
                    var model = (WidgetUserSheetViewModel.ViewState.Loaded) cf.args[0];
                    var thisobj = (WidgetUserSheet) cf.thisObject;
                    var binding = WidgetUserSheet.access$getBinding$p(thisobj);

                    var layout = binding.A;
                    //binding.f.setVisibility(View.VISIBLE);
                    View v = layout.getChildAt(0);

                    if (layout.findViewById(twid) == null) {
                        ViewGroup.LayoutParams param = v.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width, param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);

                        Button button = new Button(v.getContext(), null, 0, UiKit_ImageButton);
                        button.setText("Vote Stupidity");
                        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

                        button.setCompoundDrawablesRelativeWithIntrinsicBounds(null, stupidityIcon, null, null);
                        button.setLayoutParams(layout.getChildAt(0).getLayoutParams());

                        button.setId(twid);
                        button.setClickable(true);
                        button.setOnClickListener(v1 -> {

                            if (settings.getString("token", null) == null && settings.getBool("useOAUTH2", false)) {
                                Utils.openPageWithProxy(Utils.getAppActivity(), new AuthorazationPage());
                                return;
                            }

                            var dialog = new InputDialog().setTitle("Stupidity Level").setDescription("Please Enter Some Number");

                            dialog.setOnDialogShownListener(v2 -> {
                                dialog.getInputLayout().getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
                            });
                            dialog.setOnOkListener(v2 -> {
                                var input = dialog.getInput().length() < 4 ? Integer.parseInt(dialog.getInput()) : -1;
                                if (input > 100 || input < 0) {
                                    Toast.makeText(context, "Input Should Be Between 0 and 100", Toast.LENGTH_SHORT).show();
                                } else {
                                    Utils.threadPool.execute(() -> {
                                        var result = StupidityDBAPI.sendUserData(input, model.getUser().getId());
                                        Utils.getAppActivity().runOnUiThread(() -> {
                                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show();

                                        });
                                    });
                                    dialog.dismiss();
                                }
                            });
                            dialog.show(thisobj.getChildFragmentManager(), "epic");
                        });
                        layout.addView(button);
                    }
                }));
    }

    public void patchWidgetChatListAdapterItemMessage() throws NoSuchMethodException {
        patcher.patch(WidgetChatListAdapterItemMessage.class.getDeclaredMethod("configureItemTag", Message.class),
                new Hook((cf) -> {
                    var thisobj = (WidgetChatListAdapterItemMessage) cf.thisObject;
                    var message = (Message) cf.args[0];
                    new Thread(() -> {
                        try {
                            TextView itemTimestampField = (TextView) ReflectUtils.getField(cf.thisObject, "itemTimestamp");
                            String stupidity = StupidityDBAPI.getUserData(message.getAuthor().i());
                            Utils.mainThread.post(
                                    () -> {
                                        if (itemTimestampField != null && !itemTimestampField.getText().toString().endsWith("Stupit") && stupidity != null && !stupidity.equals("None"))
                                            itemTimestampField.setText(itemTimestampField.getText() + " %" + stupidity + " Stupit");
                                    }
                            );
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            logger.error(e);
                            e.printStackTrace();
                        }
                    }).start();
                }));
    }

    public void patchProfileHeaderView() throws NoSuchMethodException {
        patcher.patch(UserProfileHeaderView.class.getDeclaredMethod("updateViewState", UserProfileHeaderViewModel.ViewState.Loaded.class),
                new Hook((cf) -> {
                    var thisobj = (UserProfileHeaderView) cf.thisObject;
                    var binding = UserProfileHeaderView.access$getBinding$p(thisobj);
                    var customStatusViewId = Utils.getResId("user_profile_header_custom_status", "id");
                    var customStatus = binding.a.findViewById(customStatusViewId); //stole thi from juby, tyyy juby
                    var layout = (LinearLayout) customStatus.getParent();

                    var detailsTW = (TextView) layout.findViewById(profileheadertextid);
                    if (detailsTW == null) {
                        detailsTW = new TextView(context,null, R.i.UiKit_TextView_Semibold);
                        layout.addView(detailsTW);
                        var viewstate = (UserProfileHeaderViewModel.ViewState.Loaded)cf.args[0];
                        var userid= viewstate.component1().getId();
                        detailsTW.setId(profileheadertextid);
                        detailsTW.setTextColor(ColorCompat.getThemedColor(layout.getContext(), R.b.colorTextMuted));
                        var stupidity =StupidityDBAPI.getUserData(userid);
                        if (stupidity!=null && !stupidity.equals("None")) detailsTW.setText("%" + stupidity +" Stupit");

                        detailsTW.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));

                    }
                }));

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
