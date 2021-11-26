package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentManager;

import com.aliucord.Constants;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.plugins.DataClasses.ChannelData;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.app.AppActivity;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.databinding.WidgetChannelsListItemChannelVoiceBinding;
import com.discord.databinding.WidgetGuildContextMenuBinding;
import com.discord.databinding.WidgetGuildProfileSheetBinding;
import com.discord.models.guild.Guild;
import com.discord.stores.StoreStream;
import com.discord.utilities.color.ColorCompat;
import com.discord.utilities.icon.IconUtils;
import com.discord.utilities.permissions.PermissionUtils;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel;
import com.discord.widgets.channels.list.items.ChannelListItemVoiceChannel;
import com.discord.widgets.guilds.contextmenu.GuildContextMenuViewModel;
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheet;
import com.discord.widgets.guilds.profile.WidgetGuildProfileSheetViewModel;
import com.discord.widgets.servers.SettingsChannelListAdapter;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XposedBridge;

@AliucordPlugin
public class EditServersLocally extends Plugin {


    // WARNING : NOT SAFE TO READ

    // i agree this sucked to look at -animal


    AtomicReference<HashMap<Long, View>> channels = new AtomicReference<>(new HashMap<>());
    AtomicLong currentGuild = new AtomicLong();
    Logger logger = new Logger("EditServersLocally");
    public HashMap<Long, ChannelData> channelData = settings.getObject("channelData", new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, ChannelData.class).getType());
    public HashMap<Long, GuildData> guildData = settings.getObject("guildData", new HashMap<>(), TypeToken.getParameterized(HashMap.class, Long.class, GuildData.class).getType());


    Context context;
    Drawable editIcon;
    int serverSettingsID = View.generateViewId();


    @SuppressLint({"ResourceType", "SetTextI18n"})
    @Override
    public void start(Context context) throws Throwable {
        logger.info(channelData.size() + " " + guildData.size());


        this.context = context;
        editIcon = ContextCompat.getDrawable(context, R.e.ic_edit_24dp);
        editIcon = editIcon.mutate();
        settingsTab = new SettingsTab(PluginSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patchChannel();
        patchGuild();
        patchWidgetGuildProfileSheet();
        patchGuildContextMenu();
        patchWidgetChannelLıstItemText();
        patchIconUtils();
        patchChannelActionsMenu();
        patchVoiceChannels();
    }
    public void patchChannel() throws NoSuchMethodException {
        patcher.patch(Channel.class.getDeclaredMethod("m"), new Hook((cf) -> {
            //patching 'getChannelName' method so I can change channels name
            try {
                Channel ch = (Channel) cf.thisObject;
                ChannelData data = getChannelData(ChannelWrapper.getId(ch));
                if (data.channelName != null) {
                    cf.setResult(data.channelName);
                }
            } catch (Exception e) {
                //error handling
            }

        }));
    }

    public void patchGuild() throws NoSuchMethodException {
        patcher.patch(com.discord.models.guild.Guild.class.getDeclaredMethod("getName"), new PreHook((cf) -> {
            com.discord.models.guild.Guild guild = (com.discord.models.guild.Guild) cf.thisObject;
            GuildData data = getGuildData(guild.getId());
            if (data.serverName != null) {
                cf.setResult(data.serverName);
            } else if (data.orginalName != null) {
                cf.setResult(data.orginalName);
            } else {
                try {
                    cf.setResult(XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, cf.args).toString());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }));
    }

    public void patchWidgetGuildProfileSheet() throws NoSuchMethodException {
        patcher.patch(WidgetGuildProfileSheet.class.getDeclaredMethod("configureTabItems", long.class, WidgetGuildProfileSheetViewModel.TabItems.class, boolean.class)
                , new Hook((cf) -> {
                    try {

                        var bindingMethod = ReflectUtils.getMethodByArgs(WidgetGuildProfileSheet.class, "getBinding");
                        var binding = (WidgetGuildProfileSheetBinding) bindingMethod.invoke(cf.thisObject);

                        var lay = (ViewGroup) binding.f.getRootView();
                        var primaryActions = (CardView) lay.findViewById(Utils.getResId("guild_profile_sheet_secondary_actions", "id"));
                        var linearLayout = (LinearLayout) primaryActions.getChildAt(0);
                        if (linearLayout.findViewById(serverSettingsID) != null) {
                            return;
                        }

                        TextView tw = new TextView(lay.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);
                        tw.setId(serverSettingsID);
                        int pd = DimenUtils.getDefaultPadding();
                        tw.setPadding(pd, pd, pd, pd);
                        tw.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));

                        Context ctx = binding.e.getContext();
                        tw.setText("Local Server Settings");
                        tw.setOnClickListener(v1 -> {
                            ServerSettingsFragment page = new ServerSettingsFragment(StoreStream.getGuilds().getGuild((Long) cf.args[0]), EditServersLocally.this);
                            Utils.openPageWithProxy(ctx, page);

                        });

                        tw.setLayoutParams(linearLayout.getChildAt(0).getLayoutParams());
                        linearLayout.addView(tw);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }


                }));
    }

    public void patchGuildContextMenu() throws NoSuchMethodException {
        patcher.patch(WidgetGuildContextMenu.class.getDeclaredMethod("configureUI", GuildContextMenuViewModel.ViewState.class)
                , new Hook((cf) -> {
                    //adding set server name,photo to Guild Settings
                    var thisObject = (WidgetGuildContextMenu) cf.thisObject;

                    try {
                        var state = (GuildContextMenuViewModel.ViewState.Valid) cf.args[0];
                        Method method = ReflectUtils.getMethodByArgs(WidgetGuildContextMenu.class, "getBinding");
                        WidgetGuildContextMenuBinding binding = (WidgetGuildContextMenuBinding) method.invoke(thisObject);
                        LinearLayout v = (LinearLayout) binding.e.getParent();
                        var guild = state.getGuild();

                        if (v.findViewById(serverSettingsID) != null) {
                            return;
                        }
                        TextView tw = new TextView(v.getContext(), null, 0, R.i.ContextMenuTextOption);
                        tw.setLayoutParams(binding.e.getLayoutParams());
                        tw.setId(serverSettingsID);


                        editIcon.setTint(ColorCompat.getThemedColor(v.getContext(), R.b.colorInteractiveNormal));
                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(editIcon, null, null, null);

                        Context ctx = binding.e.getContext();
                        tw.setText("Local Server Settings");
                        tw.setOnClickListener(v1 -> {

                            ServerSettingsFragment page = new ServerSettingsFragment(guild, EditServersLocally.this);
                            Utils.openPageWithProxy(ctx, page);

                        });
                        v.addView(tw);
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));
    }

    public void patchWidgetChannelLıstItemText() throws NoSuchMethodException {
        patcher.patch(WidgetChannelsListAdapter.ItemChannelText.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(
                (cf) -> {
                    WidgetChannelsListAdapter.ItemChannelText thisobj = (WidgetChannelsListAdapter.ItemChannelText) cf.thisObject;
                    try {

                        WidgetChannelsListItemChannelBinding binding = (WidgetChannelsListItemChannelBinding) ReflectUtils.getField(thisobj, "binding");
                        ChannelListItemTextChannel channelListItemTextChannel = (ChannelListItemTextChannel) cf.args[1];
                        long i = ChannelWrapper.getId(channelListItemTextChannel.component1());
                        ChannelWrapper ch = new ChannelWrapper(channelListItemTextChannel.getChannel());
                        var channel = channelListItemTextChannel.getChannel();
                        var chdata = getChannelData(i);
                        if (ch.getGuildId() != currentGuild.get()) {

                            currentGuild.set(ch.getGuildId());
                            channels.set(new HashMap<>());
                        }
                        //saving instances of textviews so we can change them when channel name changed
                        channels.get().put(ch.getId(), binding.e);

                        //getting saved names and changing channel name to it

                        if (chdata.channelName != null) {
                            try {
                                ReflectUtils.setField(channel, "name", chdata.channelName);
                                ReflectUtils.setField(cf.thisObject, "channel", channel);
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                logger.error(e);
                            }

                            binding.e.setText(chdata.channelName);
                        }

                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));

    }

    public void patchIconUtils() throws NoSuchMethodException {
        patcher.patch(IconUtils.class.getDeclaredMethod("getForGuild", Long.class, String.class, String.class, boolean.class, Integer.class),
                new PreHook((cf) -> {
                    long guildID = (long) cf.args[0];
                    // Changing Server Icon to saved one if exists
                    GuildData data = getGuildData(guildID);
                    if (data.orginalURL.equals("") || data.orginalURL == null) {

                        try {
                            data.orginalURL = (XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, cf.args).toString());
                            updateGuildData(data);

                        } catch (IllegalAccessException | InvocationTargetException e) {
                            logger.error(e);
                        }
                    }
                    cf.setResult(data.imageURL != null ? data.imageURL : data.orginalURL);

                }));
    }

    public void patchChannelActionsMenu() throws NoSuchMethodException {
        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
                new Hook((cf) -> {
                    //Putting 'Set Channel Name' button to actions
                    WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];

                    try {
                        WidgetChannelsListItemChannelActions actions = (WidgetChannelsListItemChannelActions) cf.thisObject;

                        var a = (NestedScrollView) actions.requireView();
                        var layout = (LinearLayout) a.getChildAt(0);

                        Method method = ReflectUtils.getMethodByArgs(cf.thisObject.getClass(), "getBinding");
                        WidgetChannelsListItemActionsBinding binding = (WidgetChannelsListItemActionsBinding) method.invoke(cf.thisObject);
                        View v = binding.j;

                        ViewGroup.LayoutParams param = a.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width, param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);


                        TextView tw = new TextView(v.getContext(), null, 0, R.i.UiKit_Settings_Item_Icon);

                        tw.setText("Set Channel Name");

                        ChannelData data = getChannelData(ChannelWrapper.getId(model.getChannel()));
                        if (data.orginalName == null) {
                            data.orginalName = ChannelWrapper.getName(model.getChannel());
                        }

                        editIcon.setTint(ColorCompat.getThemedColor(v.getContext(), R.b.colorInteractiveNormal));
                        tw.setCompoundDrawablesRelativeWithIntrinsicBounds(editIcon, null, null, null);
                        tw.setLayoutParams(v.getLayoutParams());

                        tw.setId(View.generateViewId());
                        tw.setOnClickListener(v1 -> {
                            createDialog("Set Text Channel Name", data, actions.getParentFragmentManager());
                            actions.dismiss();
                        });
                        layout.addView(tw);

                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        logger.error(e);
                    }

                }));
    }

    public void patchVoiceChannels() {
        try {
            var itemClass = WidgetChannelsListAdapter.ItemChannelVoice.class.getDeclaredField("binding");
            itemClass.setAccessible(true);
            patcher.patch(WidgetChannelsListAdapter.ItemChannelVoice.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class), new Hook(callFrame -> {
                var a = (ChannelListItemVoiceChannel) callFrame.args[1];
                if (PermissionUtils.can(16, a.getPermission())) return; //took this from halal
                var channel = ((ChannelListItemVoiceChannel) callFrame.args[1]).getChannel();
                var chWrapped = new ChannelWrapper(channel);
                var chdata = getChannelData(chWrapped.getId());
                try {

                    var binding = (WidgetChannelsListItemChannelVoiceBinding) itemClass.get(callFrame.thisObject);

                    if (chdata.orginalName == null) {
                        chdata.orginalName = chWrapped.getName();
                    }

                    if (chdata.channelName != null) {
                        binding.f.setText(chdata.channelName);

                        try {
                            
                            ReflectUtils.setField(channel, "name", chdata.channelName);
                            ReflectUtils.setField(callFrame.thisObject, "channel", channel);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            logger.error(e);
                        }

                    }



                    binding.a.setOnLongClickListener(view -> {

                        if (currentGuild.get() != chWrapped.getGuildId()) {
                            currentGuild.set(chWrapped.getGuildId());
                            channels.set(new HashMap<>());
                        }

                        channels.get().put(chWrapped.getId(), binding.f);

                        createDialog("Set Voice Channel Name", chdata,Utils.appActivity.getSupportFragmentManager());
                        return true;
                    });

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }));


        } catch (NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }


    }

    public void createDialog(String message, ChannelData data, FragmentManager fm) {

        InputDialog dialog = new InputDialog().setTitle(message).setPlaceholderText(data.orginalName).setDescription("To reset channel name empty the text box and click confirm");


        dialog.setOnDialogShownListener(v -> {
            if (data.channelName != null) dialog.getInputLayout().getEditText().setText(data.channelName);
        });
        dialog.setOnOkListener(v -> {
            var inStr = dialog.getInput();
            if (inStr.isEmpty()) removeData(data.channelID);
            data.channelName = !inStr.isEmpty() ? inStr : null;
            updateChannel(data);
            Channel ch = StoreStream.getChannels().getChannel(data.channelID);
            try {
                ReflectUtils.setField(ch, "name", !inStr.isEmpty() ? inStr : data.orginalName);
                updateTextChannel(data);

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
            StoreStream.getChannels().handleChannelOrThreadCreateOrUpdate(ch);
            updateChannelData(data);
            setChannelData();
            dialog.dismiss();
        });
        dialog.show(fm, "a");

    }

    public void updateChannelData(ChannelData data) {
        channelData.put(data.channelID, data);
        setChannelData();
    }

    public void updateGuildData(GuildData data) {
        guildData.put(data.guildID, data);
        setGuildData();
    }

    public void updateTextChannel(ChannelData data) {
        TextView b = (TextView) channels.get().get(data.channelID);
        if (b!=null) b.setText(data.channelName == null ? data.orginalName : data.channelName);
    }

    public ChannelData getChannelData(long id) {
        return channelData.get(id) != null ? channelData.get(id) : new ChannelData(id);
    }

    public GuildData getGuildData(long id) {
        return guildData.get(id) != null ? guildData.get(id) : new GuildData(id);
    }

    public GuildData getGuildData(Guild guild) {
        return guildData.get(guild.getId()) != null ? guildData.get(guild.getId()) : new GuildData(guild);
    }

    public void setGuildData() {
        settings.setObject("guildData", guildData);
    }

    public void removeData(long channelID) {
        ChannelData data = getChannelData(channelID);
        data.channelName = "";
        updateChannel(data);
        updateTextChannel(data);
        channelData.remove(channelID);
        setChannelData();
    }

    public void updateChannel(ChannelData data) {

        try {

            if (data.channelName != null && data.channelName.isEmpty())
                data.channelName = data.orginalName;

            TextView v = (TextView) channels.get().get(data.channelID);
            if (!data.channelName.isEmpty()) {
                v.setText(data.channelName);
            } else {
                v.setText(ChannelWrapper.getName(StoreStream.getChannels().getChannel(data.channelID)));
            }


        } catch (Exception e) {
            logger.error(e);
        }

        Channel ch = StoreStream.getChannels().getChannel(data.channelID);

        if (ch != null) {
            try {
                ReflectUtils.setField(ch, "name", data.channelName);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
            StoreStream.getChannels().handleChannelOrThreadCreateOrUpdate(ch);

        }

    }

    public void removeGuildData(long id) {
        guildData.remove(id);
        setGuildData();
    }

    public void setChannelData() {
        settings.setObject("channelData", channelData);
    }


    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
