package com.aliucord.plugins;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.aliucord.CollectionUtils;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.DangerButton;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class SettingsShit extends SettingsPage {

    Context ctx;
    static List<Pattern> patternList;
    static List<Pattern> adapterList;
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        ctx = view.getContext();
        removeScrollView();

        patternList = Vibrator.patternlist;
        if (patternList.size() == 0) {patternList.add(new Pattern(0,"Sample Pattern",new long[]{1000,0,5000,100},true));}

        var stopVibration = new DangerButton(ctx);
        stopVibration.setText("Stop Vibrator");
        stopVibration.setOnClickListener(v -> Vibrator.stop());
        addView(stopVibration);

        var addPatternButton = new Button(ctx);
        addPatternButton.setText("Add New Pattern");
        addView(addPatternButton);

        ListView lw = new ListView(ctx);
        adapterList = new ArrayList<>();
        adapterList.addAll(patternList);
        var adapter = new BaseAdapter() {
            @Override public int getCount() { return adapterList.size(); }
            @Override public Pattern getItem(int position) { return adapterList.get(position); }
            @Override public long getItemId(int position) { return 0; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                var v = (EpicPatternCard) convertView;
                var pattern = getItem(position);

                if (v == null) v = new EpicPatternCard(ctx,pattern);

                v.deleteButton.setOnClickListener(v1 -> {
                    Vibrator.deletePattern(pattern);

                    var filtered = CollectionUtils.filter(patternList,pattern1 -> pattern1.ID == pattern.ID);
                    if (filtered.size() != 0 ) patternList.remove(filtered.get(0));
                    adapterList.clear();
                    adapterList.addAll(patternList);

                    notifyDataSetChanged();
                });

                return v;
            }
        };
        addPatternButton.setOnClickListener(v -> {
            int ID = 0;
            if (patternList.size() > 0 ) ID = patternList.get(patternList.size() - 1).ID + 1;
            patternList.add(new Pattern(ID,"Pattern Name(Click to edit)",null,true));
            adapterList.clear();
            adapterList.addAll(patternList);
            adapter.notifyDataSetChanged();
        });

        lw.setAdapter(adapter);
        addView(lw);
        var layParams  = lw.getLayoutParams();
        layParams.height = MATCH_PARENT;
        lw.setLayoutParams(layParams);
    }
}
