package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;

import com.aliucord.CollectionUtils;
import com.aliucord.Logger;
import com.aliucord.annotations.AliucordPlugin;
import com.discord.databinding.WidgetChannelsListItemChannelBinding;
import com.discord.stores.StoreApplication;
import com.discord.widgets.channels.list.WidgetChannelsListAdapter;
import com.discord.widgets.channels.list.items.ChannelListItem;
import com.discord.widgets.channels.list.items.ChannelListItemTextChannel;
import com.google.gson.reflect.TypeToken;
import com.lytefast.flexinput.R;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.PinePatchFn;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.databinding.WidgetChannelsListItemActionsBinding;
import com.discord.widgets.channels.list.WidgetChannelsListItemChannelActions;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

@AliucordPlugin
public class EditServersLocally extends Plugin {

    ArrayList<ChannelData> dataList =settings.getObject("data",new ArrayList<>(), TypeToken.getParameterized(ArrayList.class, ChannelData.class).getType());

    Logger logger = new Logger("EditServersLocally");
    @SuppressLint("ResourceType")
    @Override
    public void start(Context context) throws Throwable {
        JSONObject jsonObject = new JSONObject();
        //settings.setObject("data",new ArrayList<ChannelData>());


        logger.info(dataList.toString());
        patcher.patch(Channel.class.getDeclaredMethod("m"), new Hook(cf -> {
            //method gets Channel names
            Channel ch = (Channel) cf.thisObject;
            ChannelWrapper channel = new ChannelWrapper(ch);
            //logger.info(String.valueOf(channel.getId()));
            int i =findIndex(channel.getId());
            logger.info(String.valueOf(i!=-1));
            if (i!=-1){
                cf.setResult(dataList.get(i).channelName);
            }

        }));
        patcher.patch(WidgetChannelsListAdapter.ItemChannelText.class.getDeclaredMethod("onConfigure", int.class, ChannelListItem.class),new Hook((cf)->{
            WidgetChannelsListAdapter.ItemChannelText thisobj = (WidgetChannelsListAdapter.ItemChannelText) cf.thisObject;
            try {
                WidgetChannelsListItemChannelBinding binding = (WidgetChannelsListItemChannelBinding) ReflectUtils.getField(thisobj,"binding");
                ChannelListItemTextChannel channelListItemTextChannel  = (ChannelListItemTextChannel) cf.args[1];

                //method gets Channel names


                int i =findIndex(ChannelWrapper.getId(channelListItemTextChannel.component1()));
                logger.info(String.valueOf(i!=-1));
                if (i!=-1){
                    binding.d.setText(dataList.get(i).channelName);;
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }));



        patcher.patch(WidgetChannelsListItemChannelActions.class.getDeclaredMethod("configureUI", WidgetChannelsListItemChannelActions.Model.class),
                new Hook((cf)->{
                    //Putting ConfigureUI
                    WidgetChannelsListItemChannelActions.Model model = (WidgetChannelsListItemChannelActions.Model) cf.args[0];
                    try {
                        WidgetChannelsListItemChannelActions actions = (WidgetChannelsListItemChannelActions) cf.thisObject;

                        var a = (NestedScrollView)actions.requireView();
                        var layout = (LinearLayout)a.getChildAt(0);

                        Method method = ReflectUtils.getMethodByArgs(cf.thisObject.getClass(),"getBinding");
                        WidgetChannelsListItemActionsBinding binding = (WidgetChannelsListItemActionsBinding) method.invoke(cf.thisObject);
                        View v =  binding.j;

                        EditText et =new EditText(v.getContext());
                        et.setLayoutParams(a.getLayoutParams());

                        TextView tw = new TextView(v.getContext(),null,0,R.h.UiKit_Settings_Item_Icon);
                        tw.setText("Set Channel Name");
                        tw.setLayoutParams(v.getLayoutParams());

                        tw.setId(View.generateViewId());
                        tw.setOnClickListener(v1 -> {
                            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                            builder.setMessage("Set Channel Name")
                                    .setPositiveButton("Set", (dialog, id) -> {
                                        addData(new ChannelData(model.getGuild().getId(),ChannelWrapper.getId(model.getChannel()),et.getText().toString()));
                                    })
                                    .setNegativeButton("Cancel", (dialog, id) -> {

                                    }).setView(et).setNeutralButton("Remove",(dialog, which) -> {
                                        removeData(ChannelWrapper.getId(model.getChannel()));

                            });
                             builder.create().show();
                            // Intent intent = new Intent(tw.getContext(),);
                        });
                        layout.addView(tw);

                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }

                }));

    }

    public void addData(ChannelData data){
        int index =findIndex(data.getChannelID());
        if(index!=-1){
            dataList.remove(index);
        }
        dataList.add(data);
        settings.setObject("data",dataList);

    }
    public int findIndex(long channelID){
       return CollectionUtils.findIndex(dataList,channelData -> channelData.getChannelID()==channelID);
    }

    public void removeData(long channelID){
        dataList.remove(findIndex(channelID));
        settings.setObject("data",dataList);
    }


    @Override
    public void stop(Context context) throws Throwable {

    }
}
