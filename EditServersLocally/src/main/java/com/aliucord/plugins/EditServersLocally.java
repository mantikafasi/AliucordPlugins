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

import com.aliucord.CollectionUtils;
import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.utils.DimenUtils;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.stores.StoreStream;
import com.discord.stores.StoreStream$initGatewaySocketListeners$18;
import com.discord.widgets.channels.list.WidgetChannelListModel;
import com.discord.widgets.channels.list.WidgetChannelsList;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@AliucordPlugin
public class EditServersLocally extends Plugin {

    ArrayList<ChannelData> dataList =settings.getObject("data",new ArrayList<>(), TypeToken.getParameterized(ArrayList.class, ChannelData.class).getType());
    AtomicReference<HashMap<Long, View>> channels = new AtomicReference<>(new HashMap<>());
    AtomicLong currentGuild= new AtomicLong();
    Logger logger = new Logger("EditServersLocally");
    @SuppressLint("ResourceType")
    @Override
    public void start(Context context) throws Throwable {
        //TODO add opinion to edit server names and logos
        //TODO change channelName in chat too
        settingsTab = new SettingsTab(BottomSheet.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patcher.patch(Channel.class.getDeclaredMethod("m"),new Hook((cf)->{
            Channel ch = (Channel) cf.thisObject;
        }));

        patcher.patch(WidgetChannelsListAdapter.ItemChannelText.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class),new Hook(
                (cf)->{
            WidgetChannelsListAdapter.ItemChannelText thisobj = (WidgetChannelsListAdapter.ItemChannelText) cf.thisObject;
            try {
                WidgetChannelsListItemChannelBinding binding = (WidgetChannelsListItemChannelBinding) ReflectUtils.getField(thisobj,"binding");
                ChannelListItemTextChannel channelListItemTextChannel  = (ChannelListItemTextChannel) cf.args[1];
                ChannelWrapper ch = new ChannelWrapper(channelListItemTextChannel.getChannel());
                if (ch.getGuildId()!= currentGuild.get()){
                    currentGuild.set(ch.getGuildId());
                    channels.set(new HashMap<>());
                }
                channels.get().put(ch.getId(),binding.d);

                //method gets Channel names
                int i =findIndex(ChannelWrapper.getId(channelListItemTextChannel.component1()));
                if (i!=-1){
                    binding.d.setText(dataList.get(i).channelName);;
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }));

        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
                new Hook((cf)->{
                    //Putting ChannelName button to actions
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

                            int index = findIndex(ChannelWrapper.getId(model.getChannel()));
                            if (index!=-1){
                                et.setText(dataList.get(index).getChannelName());
                            }

                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                            builder.setMessage("Set Channel Name")
                                    .setPositiveButton("Set", (dialog, id) -> {
                                        addData(new ChannelData(model.getGuild().getId(),ChannelWrapper.getId(model.getChannel()),et.getText().toString()));

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


    public void addData(ChannelData data){
        int index =findIndex(data.getChannelID());
        if(index!=-1){
            dataList.remove(index);
        }
        dataList.add(data);

        updateChannel(data.getChannelID(),data.getChannelName());
        setData();

    }
    public int findIndex(long channelID){
       return CollectionUtils.findIndex(dataList,channelData -> channelData.getChannelID()==channelID);
    }

    public void removeData(long channelID){
        dataList.remove(findIndex(channelID));
        updateChannel(channelID,"");
        setData();
    }
    public void updateChannel(long channelID,String chname)  {
        try{
            TextView v = (TextView) channels.get().get(channelID);
            if (!chname.isEmpty()){
                v.setText(chname);
            } else{
                v.setText(ChannelWrapper.getName(StoreStream.getChannels().getChannel(channelID)));
            }

        }catch (Exception e){logger.error(e);}
        Channel ch = StoreStream.getChannels().getChannel(channelID);
        try { ReflectUtils.setField(ch,"name",chname); } catch (NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
        StoreStream.getChannels().handleChannelOrThreadCreateOrUpdate(ch);
    }

    public void setData(){
        settings.setObject("data",dataList);
    }


    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }
}
