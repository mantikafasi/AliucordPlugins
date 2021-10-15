package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.views.Divider;

import com.discord.api.voice.state.StageRequestToSpeakState;
import com.discord.models.domain.ModelApplicationStream;
import com.discord.stores.StoreStream;
import com.discord.utilities.guilds.GuildUtilsKt;
import com.discord.utilities.icon.IconUtils;
import com.lytefast.flexinput.R;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;
import com.discord.app.AppFragment;
import com.discord.models.guild.Guild;

public class ServerSettingsFragment extends AppFragment {

    Context ctx;
    Guild guild;
    EditServersLocally plugin;
    GuildData data ;


    public ServerSettingsFragment(Guild guild, EditServersLocally plugin){
        this.guild=guild;
        this.plugin=plugin;
        data=plugin.getGuildData(guild.getId());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ctx=inflater.getContext();
        return createLayout();
    }

    enum optionType{
        SERVERNAME,
        SERVERIMAGE

    }
    public LinearLayout createLayout(){

        var lay = new LinearLayout(ctx);

        lay.setOrientation(LinearLayout.VERTICAL);
        lay.setBackgroundResource(R.c.primary_dark_600);
        lay.addView(createOption(optionType.SERVERNAME));
        lay.addView(new Divider(ctx));

        lay.addView(createOption(optionType.SERVERIMAGE));


        LinearLayout buttonLay= new LinearLayout(ctx);

        Button saveButton = new Button(ctx);
        saveButton.setTextColor(getColor());
        saveButton.setGravity(View.FOCUS_RIGHT);
        saveButton.setText("Save");
        saveButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        saveButton.setWidth(DimenUtils.dpToPx(50));
        saveButton.setOnClickListener(v -> setSettings());

        Button cancelButton = new Button(ctx);
        cancelButton.setTextColor(getColor());
        cancelButton.setGravity(View.FOCUS_RIGHT);
        cancelButton.setText("Cancel");
        cancelButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        cancelButton.setWidth(DimenUtils.dpToPx(50));
        cancelButton.setOnClickListener(v -> getActivity().onBackPressed());

        buttonLay.addView(saveButton);
        buttonLay.addView(cancelButton);
        buttonLay.setGravity(Gravity.RIGHT);

        buttonLay.setPadding(DimenUtils.dpToPx(15),DimenUtils.dpToPx(15),DimenUtils.dpToPx(15),0);
        lay.addView(buttonLay);


        //lay.setPadding(DimenUtils.dpToPx(30),DimenUtils.dpToPx(60),DimenUtils.dpToPx(10),DimenUtils.dpToPx(10));
        return lay;
    }
    public View createOption(optionType optionName){
        LinearLayout lay = new LinearLayout(ctx);

        lay.setOrientation(LinearLayout.VERTICAL);
        TextView tw = new TextView(ctx);
        EditText et = new EditText(ctx);

        tw.setTextColor(getColor());
        et.setTextColor(getColor());
        et.setHintTextColor(ctx.getResources().getColor(R.c.primary_dark_400));
        tw.setTextSize(16);

        switch (optionName){
            case SERVERNAME:
                et.setHint(data.orginalName==null?guild.getName():data.orginalName);
                tw.setText("Server Name");
                if (data.serverName!=null){
                    et.setText(data.serverName);
                }

                et.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                    @Override public void afterTextChanged(Editable s) { data.serverName = s.toString().isEmpty()?null:s.toString(); }});
                break;

            case SERVERIMAGE:

                var iconURL = data.orginalURL==null?"https://cdn.discordapp.com/icons/"+guild.getId() +"/"+guild.getIcon() +".*":data.orginalURL;
                tw.setText("Server Image (switch servers or restart discord for changes to take effect)");
                et.setText(data.imageURL!=null?data.imageURL:"");
                et.setHint(iconURL);
                et.addTextChangedListener(new TextWatcher() {
                    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                    @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
                    @Override public void afterTextChanged(Editable s) { data.imageURL = s.toString().isEmpty()?null:s.toString(); }});
                break;
        }

        lay.addView(tw);
        lay.addView(et);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tw.getLayoutParams();
        int size = DimenUtils.dpToPx(2);
        int size2 = DimenUtils.dpToPx(10);

        params.setMargins(size,size2,size,size2);
        lay.setLayoutParams(params);
        lay.setPadding(DimenUtils.dpToPx(15),DimenUtils.dpToPx(15),DimenUtils.dpToPx(15),0);

        return lay;
    }

    public void setSettings(){
        plugin.updateGuildData(data);
        Toast.makeText(ctx, "Settings Saved", Toast.LENGTH_SHORT).show();
        var guild2 = GuildUtilsKt.createApiGuild(StoreStream.getGuilds().getGuild(guild.getId()));
        try {
            if(data.imageURL!=null){
                ReflectUtils.setField(guild2,"icon",data.imageURL);
            }
            ReflectUtils.setField(guild2,"name",data.serverName==null?data.orginalName:data.serverName); } catch (NoSuchFieldException | IllegalAccessException e) { e.printStackTrace(); }
        StoreStream.access$handleGuildUpdate(StoreStream.getPresences().getStream(), guild2);
        getActivity().onBackPressed();
    }

    public int getColor(){
        return ctx.getResources().getColor(R.c.primary_dark_200);
    }

}
