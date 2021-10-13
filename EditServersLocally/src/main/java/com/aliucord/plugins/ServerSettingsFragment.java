package com.aliucord.plugins;

import android.content.Context;
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

import com.aliucord.plugins.DataClasses.GuildData;
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
    SettingsAPI settings;
    GuildData data ;


    public ServerSettingsFragment(Guild guild, SettingsAPI settings){
        this.guild=guild;
        this.settings=settings;
        data=settings.getObject(String.valueOf(guild.getId()),new GuildData(guild.getId()));
    }

    enum optionType{
        SERVERNAME,
        SERVERIMAGE

    }
    public LinearLayout createLayout(){

        var lay = new LinearLayout(ctx);

        lay.setOrientation(LinearLayout.VERTICAL);

        lay.addView(createOption(optionType.SERVERNAME));
        lay.addView(createOption(optionType.SERVERIMAGE));

        LinearLayout buttonlay = new LinearLayout(ctx);

        Button button = new Button(ctx);
        button.setTextColor(getColor());
        button.setGravity(View.FOCUS_RIGHT);
        button.setText("Save");
        button.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        button.setWidth(DimenUtils.dpToPx(50));
        button.setOnClickListener(v -> setSettings());

        Button cancel = new Button(ctx);
        cancel.setTextColor(getColor());
        cancel.setGravity(View.FOCUS_RIGHT);
        cancel.setText("Cancel");
        cancel.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        cancel.setWidth(DimenUtils.dpToPx(50));
        cancel.setOnClickListener(v -> getActivity().onBackPressed());

        buttonlay.addView(button);
        buttonlay.addView(cancel);

        buttonlay.setGravity(Gravity.RIGHT);

        lay.addView(buttonlay);

        return lay;
    }
    public View createOption(optionType optionName){


        LinearLayout lay = new LinearLayout(ctx);

        lay.setOrientation(LinearLayout.VERTICAL);
        TextView tw = new TextView(ctx);
        EditText et = new EditText(ctx);
        Button button = new Button(ctx);


        tw.setTextColor(getColor());
        et.setTextColor(getColor());
        button.setTextColor(getColor());


        switch (optionName){
            case SERVERNAME:

                button.setOnClickListener(v -> data.serverName=null);

                tw.setText("Server Name");
                if (data.serverName!=null){
                    et.setText(data.serverName);

                } else {
                    et.setText(guild.getName());
                }
                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        data.serverName = s.toString();
                    }

                });
                break;
            case SERVERIMAGE:
                var iconURL = "https://cdn.discordapp.com/icons/"+guild.getId()+"/"+guild.getIcon();
                tw.setText("Server Image");
                et.setText(data.imageURL!=null?data.imageURL:iconURL);

                button.setOnClickListener(v -> data.imageURL=null);

                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {

                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        data.imageURL = s.toString();
                    }

                });

                break;
        }

        button.setText("RESET");



        lay.addView(tw);
        lay.addView(et);
        lay.addView(button);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tw.getLayoutParams();
        int size = DimenUtils.dpToPx(20);
        int size2 = DimenUtils.dpToPx(2);
        params.setMargins(size2,size2,size2,size2);

        et.setLayoutParams(params);
        tw.setLayoutParams(params);

        return lay;
    }

    public void setSettings(){
        settings.setObject(String.valueOf(guild.getId()),data);
        Toast.makeText(ctx, "Settings Saved", Toast.LENGTH_SHORT).show();
        getActivity().onBackPressed();
    }

    public int getColor(){
        return ctx.getResources().getColor(R.c.primary_dark_200);
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ctx=inflater.getContext();
        return createLayout();
    }
}
