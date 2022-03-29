package com.aliucord.plugins;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.views.DangerButton;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class SettingsShit extends SettingsPage {

    Context ctx;
    List<Pattern> patternList;
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        ctx = view.getContext();

        patternList = Vibrator.settings.getObject("patternList", new ArrayList<>(), TypeToken.getParameterized(ArrayList.class,Pattern.class).getType());
        if (patternList.size() == 0) {patternList.add(new Pattern(0,"Sample Pattern",new long[]{1000,0,5000,100},true));}

        var stopVibration = new DangerButton(ctx);
        stopVibration.setText("Stop Vibrator");
        stopVibration.setOnClickListener(v -> Vibrator.stop());
        addView(stopVibration);

        var addPatternButton = new Button(ctx);
        addPatternButton.setText("Add New Pattern");
        addView(addPatternButton);

        ListView lw = new ListView(ctx);
        var adapter = new BaseAdapter() {
            @Override public boolean areAllItemsEnabled() { return true; }
            @Override public boolean isEnabled(int position) { return true; }
            @Override public void registerDataSetObserver(DataSetObserver observer) { }
            @Override public void unregisterDataSetObserver(DataSetObserver observer) { }
            @Override public int getCount() { return patternList.size(); }
            @Override public Pattern getItem(int position) { return patternList.get(position); }
            @Override public long getItemId(int position) { return 0; }
            @Override public boolean hasStableIds() {return false; }
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout v = (LinearLayout) convertView;
                if (v == null) {
                    v = new LinearLayout(ctx);
                    v.addView(new EpicPatternCard(ctx,patternList.get(position)));
                }
                return v;
            }
            @Override public int getItemViewType(int position) { return 0; }
            @Override public int getViewTypeCount() { return 1; }
            @Override public boolean isEmpty() { return false; }
        };
        addPatternButton.setOnClickListener(v -> {
            patternList.add(new Pattern(patternList.size(),"Pattern Name(Click to edit)",null,true));
            adapter.notifyDataSetChanged();
        });
        lw.setAdapter(adapter);
        addView(lw);
    }
}
