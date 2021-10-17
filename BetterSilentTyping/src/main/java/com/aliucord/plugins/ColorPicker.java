package com.aliucord.plugins;

import android.app.ActionBar;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.widget.ImageViewCompat;

import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.utils.DimenUtils;
import com.discord.views.CheckedSetting;
import com.discord.views.RadioManager;
import com.lytefast.flexinput.R;

import java.lang.reflect.GenericArrayType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColorPicker extends SettingsPage {
    Logger logger = new Logger("BetterSilentTyping ColorPicker");

    int textColor;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    HashMap<String,SeekBar> seekBarList;
    SeekBar r;
    SeekBar g;
    SeekBar b;
    SeekBar a;
    ImageView keyboardView;
    ImageView disableIconView;
    LinearLayout lay ;

    int[][] defaultValues= {{186,187,191,255},{255,0,0,255}};
    int category=0; //0 means Keyboard Icon, 1 means Disable Icon
    Context ctx;
    @Override
    public void onViewBound(View view) {

        super.onViewBound(view);
        ctx = view.getContext();
        textColor=ctx.getResources().getColor(R.c.primary_dark_200);
        lay = getLinearLayout();


        LinearLayout radioLay = new LinearLayout(ctx);
        radioLay.setOrientation(LinearLayout.VERTICAL);
        CheckedSetting keyboardIcon = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO,"Keyboard Icon","");
        CheckedSetting disableIcon = Utils.createCheckedSetting(ctx, CheckedSetting.ViewType.RADIO,"Disable Icon","");
        List<CheckedSetting> radioList =  Arrays.asList(keyboardIcon,disableIcon);
        RadioManager manager = new RadioManager(radioList);

        for (int i=0;i<radioList.size(); i++) {
            var radio = radioList.get(i);

            int j = i;
            radio.e(v -> {
                setSettings();
                Toast.makeText(ctx, (category==0?"Keyboard Color":"'Disabled Icon' Color") + " Saved", Toast.LENGTH_SHORT).show();
                manager.a(radio);
                category= j;
                changeCategory();

            });
        }


        radioLay.addView(keyboardIcon);
        radioLay.addView(disableIcon);

        lay.addView(radioLay);


        seekBarList= new HashMap<>();
        r=createSeekBar("Red");
        g=createSeekBar("Green");
        b=createSeekBar("Blue");
        a=createSeekBar("Alpha");


        button = new FrameLayout(ctx);
        keyboardView = new ImageView(ctx);
        disableIconView = new ImageView(ctx);
        keyboardView.setImageDrawable(BetterSilentTyping.keyboard);

        disableIconView.setImageDrawable(BetterSilentTyping.disableImage);
        button.addView(keyboardView);
        button.addView(disableIconView);

        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) keyboardView.getLayoutParams();
        params.width=DimenUtils.dpToPx(100);
        params.height=DimenUtils.dpToPx(100);
        params.gravity= Gravity.CENTER;

        keyboardView.setLayoutParams(params);
        disableIconView.setLayoutParams(params);



        lay.addView(button);
        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) button.getLayoutParams();
        param.bottomMargin=20;
        button.setLayoutParams(param);

        LinearLayout optLay = new LinearLayout(ctx);
        optLay.setOrientation(LinearLayout.HORIZONTAL);
        optLay.setGravity(Gravity.RIGHT);

        Button reset = new Button(ctx);
        reset.setTextColor(textColor);
        reset.setText("Reset");

        reset.setOnClickListener(v -> {
            seekBarList.forEach((s, seekBar) -> {
                int progress ;
                int[] vals = defaultValues[category];

                switch (s){
                    case "Red":progress=vals[0];break;
                    case "Blue":progress=vals[1];break;
                    case "Green": progress = vals[2];break;
                    case "Alpha": progress=vals[3];break;
                    default: progress=255;break;
                }

                seekBar.setProgress(progress);
            });



        });

        Button save = new Button(ctx);
        save.setTextColor(textColor);
        save.setText("Save");
        save.setOnClickListener(v -> {
            setSettings();
            Toast.makeText(ctx, (category==0?"Keyboard Color":"Disabled Icon Color") + " Saved", Toast.LENGTH_SHORT).show();
            //close();
        });

        Button cancel = new Button(ctx);
        cancel.setText("Cancel");
        cancel.setTextColor(textColor);
        cancel.setOnClickListener(v -> {close();});

        optLay.addView(reset);
        optLay.addView(save);
        optLay.addView(cancel);
        lay.addView(optLay);
        updateButtonColor();

    }
    FrameLayout button;
    public int getColors(){return Color.argb(a.getProgress(),r.getProgress(),g.getProgress(),b.getProgress()); }
    public void setSettings(){
        SettingsAPI api = BetterSilentTyping.Settings;
        api.setInt(category + "colorInt",getColors());

        api.setInt(category + "Red",r.getProgress());
        api.setInt(category + "Green",g.getProgress());
        api.setInt(category + "Blue",b.getProgress());
        api.setInt(category + "Alpha",a.getProgress());
    }

    public SeekBar createSeekBar(String name){
        LinearLayout upperLayout = new LinearLayout(ctx);
        upperLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tw = new TextView(ctx);
        tw.setText(name);
        tw.setTextColor(textColor);
        upperLayout.addView(tw);


        LinearLayout lay = new LinearLayout(ctx);

        upperLayout.addView(lay);

        SeekBar sb = new SeekBar(ctx);

        int defvalue=125;
        sb.setMax(255);
        //if(name.equals("Alpha")){defvalue=100;sb.setMax(100);}
        sb.setProgress(BetterSilentTyping.Settings.getInt(category  + name,defvalue));

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                updateButtonColor();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });




        lay.addView(sb);
        this.lay.addView(upperLayout);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sb.getLayoutParams();

        params.width = ActionBar.LayoutParams.MATCH_PARENT;
        sb.setLayoutParams(params);


        sb.setPadding(DimenUtils.dpToPx(6), DimenUtils.dpToPx(5),DimenUtils.dpToPx(6),DimenUtils.dpToPx(5));
        seekBarList.put(name,sb);
        return sb;
    }
    public void changeCategory(){
        seekBarList.forEach((s, seekBar) -> {
            var defVal =255;

            seekBar.setProgress(BetterSilentTyping.Settings.getInt(category + s,defVal));
        });


    }

    public enum colorType{
        RED(0),GREEN(1),BLUE(2),ALPHA(3);
        private final int value;
        colorType(int val){
            this.value=val;
        }

        public int getInt(){
            return value;
        }
    }

    @Override
    public void onDestroy() {

        changeCategory(); //

        super.onDestroy();
    }

    public void updateButtonColor(){
        if(button!=null){
            if (category==0){
                ImageViewCompat.setImageTintList(keyboardView,ColorStateList.valueOf(getColors()));
                //keyboardView.setColorFilter(getColors());
            } else {
                ImageViewCompat.setImageTintList(disableIconView,ColorStateList.valueOf(getColors()));
                //disableIconView.setColorFilter(getColors());
            }

        }

    }
}
