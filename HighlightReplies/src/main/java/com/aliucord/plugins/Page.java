package com.aliucord.plugins;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;

import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.lytefast.flexinput.R;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.aliucord.fragments.SettingsPage;
import com.discord.utilities.color.ColorCompat;

import java.util.ArrayList;
import java.util.List;

public class Page extends SettingsPage {
    public int getColor(Context context){

        //return ColorCompat.getThemedColor(context, Utils.getResId("primary_dark_200","color"));
        return context.getResources().getColor(R.c.primary_dark_200);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
    }
    List<EditText> editTextList= new ArrayList<>();

    EditText r;
    EditText g;
    EditText b;
    EditText a;

    Context ctx;
    @Override
    public void onViewBound(View view) {

        super.onViewBound(view);

        ctx = ctx;


        r=new EditText(ctx);
        b=new EditText(ctx);
        g=new EditText(ctx);
        a=new EditText(ctx);

        editTextList.add(r);
        editTextList.add(g);
        editTextList.add(b);
        editTextList.add(a);


        LinearLayout lay = getLinearLayout();


        SeekBar red = new SeekBar(ctx);
        lay.addView(red);
        red.setMax(255);

        SeekBar blue = new SeekBar(ctx);
        lay.addView(red);
        red.setMax(255);

        SeekBar green = new SeekBar(ctx);
        lay.addView(red);
        red.setMax(255);

        SeekBar alpha = new SeekBar(ctx);
        lay.addView(red);
        red.setMax(100);



        for (EditText e : editTextList) {
            lay.addView(e);
            e.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
            e.setTextColor(getColor(ctx));
            e.setHintTextColor(getColor(ctx));
            e.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void afterTextChanged(Editable s) {
                    button.setBackgroundColor(getColors());
                }
            });



        }


        r.setHint("Set R Value");
        g.setHint("Set B Value");
        b.setHint("Set G Value");
        a.setHint("Set Alpha");

        button = new Button(ctx);
        button.setText("Reset");
        button.setTextColor(getColor(ctx));
        button.setOnClickListener(v -> {
                    for (EditText e : editTextList) {
                        e.setText("");
                        button.setBackgroundColor(1677721600);
                        HighLightReplies.setting.setInt("colorInt",1677721600);
                    }
        });
        button.setWidth(200);
        button.setHeight(200);
        lay.addView(button);

        LinearLayout optLay = new LinearLayout(ctx);
        optLay.setOrientation(LinearLayout.HORIZONTAL);
        optLay.setGravity(Gravity.RIGHT);
        Button save = new Button(ctx);
        save.setTextColor(getColor(ctx));
        save.setText("Save");
        save.setOnClickListener(v -> {
            setSettings();
            Toast.makeText(ctx, "Settings Saved", Toast.LENGTH_SHORT).show();
            close();
        });

        Button cancel = new Button(ctx);
        cancel.setText("Cancel");
        cancel.setTextColor(getColor(ctx));
        cancel.setOnClickListener(v -> {close();});


        optLay.addView(save);
        optLay.addView(cancel);
        lay.addView(optLay);

    }
    Button button;
    public int getColors(){
        int[] objects = {0,0,0,100};
        try {
            objects[0]=Integer.parseInt(a.getText().toString());
            objects[1]=Integer.parseInt(r.getText().toString());
            objects[2]=Integer.parseInt(g.getText().toString());
            objects[3]=Integer.parseInt(b.getText().toString());

        } catch (Exception e){}
        return Color.argb(objects[0],objects[1],objects[2],objects[3]);

    }
    public void setSettings(){
        SettingsAPI api = HighLightReplies.setting;
        api.setInt("colorInt",getColors());
    }

    public SeekBar createSeekBar(String name){
        LinearLayout upperLayout = new LinearLayout(ctx);
        upperLayout.setOrientation(LinearLayout.VERTICAL);
        TextView tw = new TextView(ctx);
        tw.setText(name);
        tw.setTextColor(getColor(ctx));
        upperLayout.addView(tw);


        LinearLayout lay = new LinearLayout(ctx);

        upperLayout.addView(lay);

        SeekBar sb = new SeekBar(ctx);
        EditText et = new EditText(ctx);
        et.setInputType(EditorInfo.TYPE_CLASS_NUMBER);


        if (name.equals("Alpha")){sb.setMax(100);sb.setProgress(100);} else{sb.setMax(255);}

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                et.setText(seekBar.getProgress());
            }
        });

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int prog = Integer.parseInt(s.toString());

                if (name.equals("Alpha") && prog>100){prog=100;et.setText(prog);}
                if (prog>255){ prog=255;et.setText(prog); } else if(prog>0){ prog=0;et.setText(prog);}
                sb.setProgress(prog);
                updateButtonColor();

            }
        });


        lay.addView(sb);
        lay.addView(et);
        return sb;
    }
    public void updateButtonColor(){
        button.setBackgroundColor(getColors());
    }
}
