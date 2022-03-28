package com.aliucord.plugins;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.SettingsPage;

import java.util.ArrayList;
import java.util.List;

public class SettingsShit extends SettingsPage {
    SettingsAPI settings;
    public SettingsShit(SettingsAPI settingsAPI) {
        settings = settingsAPI;
    }

    Context ctx;
    List<Pattern> patternList;
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        ctx = view.getContext();

        patternList = settings.getObject("patternList",new ArrayList<>());
        patternList.add(new Pattern());
        patternList.add(new Pattern());


        var addPatternButton = new Button(ctx);
        addPatternButton.setText("Add New Pattern");
        addView(addPatternButton);

        ListView lw = new ListView(ctx);
        var adapter = new ListAdapter() {
            @Override
            public boolean areAllItemsEnabled() { return true; }

            @Override public boolean isEnabled(int position) { return true; }

            @Override public void registerDataSetObserver(DataSetObserver observer) { }

            @Override public void unregisterDataSetObserver(DataSetObserver observer) { }

            @Override public int getCount() { return patternList.size(); }

            @Override public Pattern getItem(int position) { return patternList.get(position); }

            @Override public long getItemId(int position) { return 0; }

            @Override public boolean hasStableIds() {return false; }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout v = (LinearLayout) convertView;

                if (v == null) {
                    v = new LinearLayout(ctx);
                    v.addView(new EpicPatternCard(ctx));

                }


                return v;
            }

            @Override public int getItemViewType(int position) { return 0; }

            @Override public int getViewTypeCount() { return 1; }

            @Override public boolean isEmpty() { return false; }
        };
        lw.setAdapter(adapter);
        addView(lw);
    }
}
