package com.aliucord.plugins;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.lytefast.flexinput.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Page extends SettingsPage {
    float scale;
    List<SeekBar> seekBarList = new ArrayList<>();
    SeekBar r;
    SeekBar g;
    SeekBar b;
    SeekBar a;
    LinearLayout lay;
    Context ctx;
    Button button;

    public int getColor(Context context) {
        //return ColorCompat.getThemedColor(context, Utils.getResId("primary_dark_200","color"));
        return context.getResources().getColor(R.c.primary_dark_200);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        ctx = view.getContext();
        scale = ctx.getResources().getDisplayMetrics().density;
        lay = getLinearLayout();
        button = new Button(ctx);
        r = createSeekBar("Red");
        b = createSeekBar("Blue");
        g = createSeekBar("Green");
        a = createSeekBar("Alpha");
        seekBarList.addAll(Arrays.asList(r, b, g, a));

        button.setText("Reset");
        button.setTextColor(getColor(ctx));
        button.setOnClickListener(v -> {
            for (SeekBar e : seekBarList) {
                e.setProgress(0);
                button.setBackgroundColor(1677721600);
                HighLightReplies.setting.setInt("colorInt", 1677721600);
            }
            a.setProgress(100);
        });
        button.setWidth(200);
        button.setHeight(200);
        lay.addView(button);
        LinearLayout.LayoutParams param = (LinearLayout.LayoutParams) button.getLayoutParams();
        param.bottomMargin = 20;
        button.setLayoutParams(param);

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
        cancel.setOnClickListener(v -> {
            close();
        });

        optLay.addView(save);
        optLay.addView(cancel);
        lay.addView(optLay);
        updateButtonColor();

    }

    public int getColors() {
        return Color.argb(a.getProgress(), r.getProgress(), g.getProgress(), b.getProgress());
    }

    public void setSettings() {
        SettingsAPI api = HighLightReplies.setting;
        api.setInt("colorInt", getColors());
        api.setInt("Red", r.getProgress());
        api.setInt("Green", g.getProgress());
        api.setInt("Blue", b.getProgress());
        api.setInt("Alpha", a.getProgress());
    }

    public SeekBar createSeekBar(String name) {
        LinearLayout upperLayout = new LinearLayout(ctx);
        upperLayout.setOrientation(LinearLayout.VERTICAL);

        TextView tw = new TextView(ctx);
        tw.setText(name);
        tw.setTextColor(getColor(ctx));
        upperLayout.addView(tw);


        LinearLayout lay = new LinearLayout(ctx);

        upperLayout.addView(lay);

        SeekBar sb = new SeekBar(ctx);

        int defvalue = 125;
        sb.setMax(255);
        if (name.equals("Alpha")) {
            defvalue = 100;
            sb.setMax(100);
        }
        sb.setProgress(HighLightReplies.setting.getInt(name, defvalue));

        EditText et = new EditText(ctx);
        et.setInputType(EditorInfo.TYPE_CLASS_NUMBER);


        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                et.setText(String.valueOf(seekBar.getProgress()));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

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
                String progs = String.valueOf(prog);

                if (name.equals("Alpha") && prog > 100) {
                    prog = 100;
                    et.setText(progs);
                }
                if (prog > 255) {
                    prog = 255;
                    et.setText(progs);
                } else if (prog < 0) {
                    prog = 0;
                    et.setText(progs);
                }
                sb.setProgress(prog);
                updateButtonColor();

            }
        });


        et.setEnabled(false); //disabled for now


        lay.addView(sb);
        lay.addView(et);
        this.lay.addView(upperLayout);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) sb.getLayoutParams();
        params.width = 0;
        et.setLayoutParams(params);
        //Logger logger = new Logger("Highlight replies");
        //logger.info(String.valueOf(sb.getLayoutParams().width ) + " " + upperLayout.getLayoutParams().width);
        params.width = ActionBar.LayoutParams.MATCH_PARENT;
        sb.setLayoutParams(params);


        return sb;
    }

    public void updateButtonColor() {
        if (button != null) {
            button.setBackgroundColor(getColors());
        }

    }
}
