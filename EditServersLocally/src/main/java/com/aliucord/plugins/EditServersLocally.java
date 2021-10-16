package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PreHook;
import com.aliucord.plugins.DataClasses.ChannelData;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.databinding.WidgetGuildContextMenuBinding;
import com.discord.stores.StoreStream;
import com.discord.utilities.icon.IconUtils;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel;
import com.discord.widgets.guilds.contextmenu.GuildContextMenuViewModel;
import com.discord.widgets.guilds.contextmenu.WidgetGuildContextMenu;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import de.robv.android.xposed.XposedBridge;

@AliucordPlugin
public class EditServersLocally extends Plugin {


    //WARNING : NOT SAFE TO READ


    AtomicReference<HashMap<Long, View>> channels = new AtomicReference<>(new HashMap<>());
    AtomicLong currentGuild= new AtomicLong();
    Logger logger = new Logger("EditServersLocally");
    public HashMap<Long, ChannelData> channelData = settings.getObject("channelData", new HashMap<>(),TypeToken.getParameterized(HashMap.class, Long.class,ChannelData.class).getType());
    public HashMap<Long,GuildData> guildData = settings.getObject("guildData", new HashMap<>(),TypeToken.getParameterized(HashMap.class, Long.class,GuildData.class).getType());




    Context context;
    @SuppressLint({"ResourceType", "SetTextI18n"})
    @Override
    public void start(Context context) throws Throwable {
        logger.info(channelData.size() + " " + guildData.size());


        this.context= context;

        settingsTab = new SettingsTab(BottomSheet.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        /*
        patcher.patch(Channel.class.getDeclaredMethod("m"),new Hook((cf)->{
            //patching 'getChannelName' method so I can change channels name
            Channel ch = (Channel) cf.thisObject;
            ChannelData data =getChannelData(ChannelWrapper.getId(ch));
            if(data.channelName!=null){cf.setResult(data.channelName);}
        }));

         */


        /*
        patcher.patch(Guild.class.getDeclaredMethod("v"),new PreHook((cf)->{
            var thisobj =(Guild) cf.thisObject;
            try {
                long guildid = (long) ReflectUtils.getField(thisobj,"id");
                GuildData data = guildData.get(guildid);

                //logger.info(String.valueOf(guildid));
                if (data.serverName!=null){
                    cf.setResult(data.serverName);
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e);
            }
        }));

         */


        patcher.patch(com.discord.models.guild.Guild.class.getDeclaredMethod("getName"),new PreHook((cf)->{
            com.discord.models.guild.Guild guild = (com.discord.models.guild.Guild) cf.thisObject;
            GuildData data = getGuildData(guild.getId());
            if(data.serverName!=null){
                cf.setResult(data.serverName);
            }
        }));


        /*
        for (Constructor<?> constructor : com.discord.models.guild.Guild.class.getConstructors()) {
            patcher.patch(constructor,new Hook((cf)->{
                try {
                    com.discord.models.guild.Guild guild = (com.discord.models.guild.Guild) cf.thisObject;

                    GuildData data = getGuildData(guild.getId());
                   // if (data.orginalName==null){data.orginalName= (String) ReflectUtils.getField(cf.thisObject,"name");updateGuildData(data);}
                    if(data.serverName!=null){
                        ReflectUtils.setField(cf.thisObject,"name",data.serverName);
                    }

                } catch (NoSuchFieldException | IllegalAccessException e) {
                    e.printStackTrace();
                }

            }));
        }
         */



        patcher.patch(WidgetGuildContextMenu.class.getDeclaredMethod("configureUI", GuildContextMenuViewModel.ViewState.class)
                ,new Hook((cf)->{
            //adding set server name,photo to Guild Settings
            var thisObject = (WidgetGuildContextMenu)cf.thisObject;

            try {
                var state = (GuildContextMenuViewModel.ViewState.Valid) cf.args[0];
                Method method = ReflectUtils.getMethodByArgs(WidgetGuildContextMenu.class,"getBinding");
                WidgetGuildContextMenuBinding binding = (WidgetGuildContextMenuBinding) method.invoke(thisObject);
                LinearLayout v = (LinearLayout) binding.e.getParent();
                var guild =state.getGuild();

                TextView tw = new TextView(v.getContext(),null,0,R.h.UiKit_Settings_Item_Icon);
                tw.setLayoutParams(binding.e.getLayoutParams());
                Context ctx = binding.e.getContext();
                tw.setText("Local Server Settings");
                tw.setOnClickListener(v1 -> {
                    ServerSettingsFragment page = new ServerSettingsFragment(guild,EditServersLocally.this);
                    Utils.openPageWithProxy(ctx, page);
                    v.removeView(tw);
                });
                v.addView(tw);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }));

        patcher.patch(WidgetChannelsListAdapter.ItemChannelText.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class),new Hook(
                (cf)->  {
            WidgetChannelsListAdapter.ItemChannelText thisobj = (WidgetChannelsListAdapter.ItemChannelText) cf.thisObject;
            try {

                WidgetChannelsListItemChannelBinding binding = (WidgetChannelsListItemChannelBinding) ReflectUtils.getField(thisobj,"binding");
                ChannelListItemTextChannel channelListItemTextChannel  = (ChannelListItemTextChannel) cf.args[1];
                ChannelWrapper ch = new ChannelWrapper(channelListItemTextChannel.getChannel());
                if (ch.getGuildId()!= currentGuild.get()){

                    currentGuild.set(ch.getGuildId());
                    channels.set(new HashMap<>());
                }
                //saving instances of textviews so we can change them when channel name changed
                channels.get().put(ch.getId(),binding.d);

                //getting saved names and changing channel name to it
                long i =ChannelWrapper.getId(channelListItemTextChannel.component1());

                if (channelData.containsKey(i)){
                    var chdata =getChannelData(i);
                    if(chdata.channelName!=null){
                        binding.d.setText(chdata.channelName);
                    }
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }));
        /*
        patcher.patch(GuildListViewHolder.GuildViewHolder.class.getDeclaredMethod("configureGuildIconImage", com.discord.models.guild.Guild.class, boolean.class),
                new Hook((cf)->{
                    var thisobj = (GuildListViewHolder.GuildViewHolder)cf.thisObject;
                    var guild = (com.discord.models.guild.Guild)cf.args[0];
                    try {
                        WidgetGuildsListItemGuildBinding binding = (WidgetGuildsListItemGuildBinding) ReflectUtils.getField(thisobj,"bindingGuild");
                        GuildData data = getGuildData(guild.getId());
                        if (data.imageURL!=null){

                            binding.d.setImageURI(data.imageURL);
                            logger.info(IconUtils.getForGuild(guild));

                            //ReflectUtils.setField(guild,"icon","changed");
                            //StoreStream.access$handleGuildUpdate(StoreStream.getNotices().getStream(),GuildUtilsKt.createApiGuild(guild));
                        }


                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }));

         */

        patcher.patch(IconUtils.class.getDeclaredMethod("getForGuild", Long.class, String.class, String.class, boolean.class, Integer.class),
                new PreHook((cf)->{
                    long guildID = (long) cf.args[0];
                    // Changing Server Icon to saved one if exists
                    GuildData data = getGuildData(guildID);

                    if (data.orginalURL==null){

                        try { data.orginalURL = (XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, cf.args).toString());updateGuildData(data);
                        } catch (IllegalAccessException | InvocationTargetException e) { logger.error(e); }
                    }
                    cf.setResult(data.imageURL!=null?data.imageURL:data.orginalURL);

                }));


        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
                new Hook((cf)->{
                    //Putting 'Set Channel Name' button to actions
                    WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];

                    try {
                        WidgetChannelsListItemChannelActions actions = (WidgetChannelsListItemChannelActions) cf.thisObject;

                        var a = (NestedScrollView)actions.requireView();
                        var layout = (LinearLayout)a.getChildAt(0);

                        Method method = ReflectUtils.getMethodByArgs(cf.thisObject.getClass(),"getBinding");
                        WidgetChannelsListItemActionsBinding binding = (WidgetChannelsListItemActionsBinding) method.invoke(cf.thisObject);
                        View v =  binding.j;

                        ViewGroup.LayoutParams param = a.getLayoutParams();
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(param.width,param.height);
                        params.leftMargin = DimenUtils.dpToPx(20);


                        TextView tw = new TextView(v.getContext(),null,0,R.h.UiKit_Settings_Item_Icon);
                        tw.setText("Set Channel Name");
                        tw.setLayoutParams(v.getLayoutParams());

                        tw.setId(View.generateViewId());
                        tw.setOnClickListener(v1 -> {

                            EditText et =new EditText(v.getContext());
                            et.setSelectAllOnFocus(true);

                            LinearLayout lay = new LinearLayout(v.getContext());
                            lay.addView(et);
                            et.setLayoutParams(params);

                            long chid = ChannelWrapper.getId(model.getChannel());
                            if (channelData.containsKey(chid)){
                                ChannelData chData = getChannelData(chid);
                                et.setHint(chData.orginalName);
                                et.setText(chData.channelName);
                            } else et.setHint(ChannelWrapper.getName(model.getChannel()));

                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                            builder.setMessage("Set Channel Name")
                                    .setPositiveButton("Set", (dialog, id) -> {


                                        ChannelData data = getChannelData(ChannelWrapper.getId(model.getChannel()));
                                        if (data.orginalName==null){
                                            data.orginalName=ChannelWrapper.getName(model.getChannel());
                                        }
                                        data.channelName=et.getText().toString();
                                        updateChannelData(data);
                                        updateChannel(data);


                                    })
                                    .setNegativeButton("Cancel", (dialog, id) -> {}).setView(lay).setNeutralButton("Remove",(dialog, which) -> removeData(ChannelWrapper.getId(model.getChannel())));

                            builder.create().show();

                        });
                        layout.addView(tw);

                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        logger.error(e);
                    }

                }));

    }
    public void updateChannelData(ChannelData data){
        channelData.put(data.channelID,data);
        setChannelData();
    }
    public void updateGuildData(GuildData data){
        guildData.put(data.guildID,data);
        setGuildData();
    }
    public void updateTextChannel(ChannelData data){
        TextView b = (TextView) channels.get().get(data.channelID);
        b.setText(data.channelName==null?data.orginalName:data.channelName);
    }
    public ChannelData getChannelData(long id){ return channelData.get(id)!=null?channelData.get(id):new ChannelData(id); }
    public GuildData getGuildData(long id){ return guildData.get(id)!=null?guildData.get(id):new GuildData(id); }
    public void setGuildData() { settings.setObject("guildData",guildData); }
    public void removeData(long channelID){
        ChannelData data = getChannelData(channelID);
        data.channelName="";
        updateChannel(data);
        updateTextChannel(data);
        channelData.remove(channelID);
        setChannelData();
    }
    public void updateChannel(ChannelData data)  {

        try{
            logger.info(data.orginalName);
            if (data.channelName.isEmpty()) data.channelName=data.orginalName;

            TextView v = (TextView) channels.get().get(data.channelID);
            if (!data.channelName.isEmpty()){
                v.setText(data.channelName);
            } else{
                v.setText(ChannelWrapper.getName(StoreStream.getChannels().getChannel(data.channelID)));
            }



        }catch (Exception e){logger.error(e);}

        Channel ch = StoreStream.getChannels().getChannel(data.channelID);


        try {ReflectUtils.setField(ch,"name",data.channelName); } catch (NoSuchFieldException | IllegalAccessException e) { logger.error(e); }
        StoreStream.getChannels().handleChannelOrThreadCreateOrUpdate(ch);

    }
    public void setChannelData(){ settings.setObject("channelData",channelData); }



    @Override public void stop(Context context) {
        patcher.unpatchAll();
    }
}
