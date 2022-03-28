package com.aliucord.plugins.filtering;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;

import com.aliucord.plugins.ThemeRepoAPI;
import com.aliucord.plugins.ThemesPage;
import com.discord.app.AppFragment;
import com.discord.utilities.color.ColorCompat;
import com.discord.views.CheckedSetting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {
    AppFragment fragment;
    Context ctx;
    List<String> filters = new ArrayList<>(Arrays.asList("Author", "Show Installed Themes"));
    List<Developer> developers;
    int viewCount = -2;

    public FilterAdapter(AppFragment fragment) {
        this.fragment = fragment;
        ctx = fragment.requireContext();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        viewCount += 1;
        return new ViewHolder(new AdapterItem(ctx, viewCount));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.item.type = FilterType.values()[position];
        switch (holder.item.type) {
            case Author:
                Utils.threadPool.execute(() -> {
                    developers = null; //TODO GUH
                    developers.add(0, new Developer("None", 0));
                    Utils.mainThread.post(() -> {
                        ((Spinner) holder.item.setting).setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, developers));
                    });
                });
                break;

        }
        holder.item.textView.setText(filters.get(position));
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    public enum FilterType {
        Author,
        show_installed_plugins
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AdapterItem item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            item = (AdapterItem) itemView;


            switch (item.viewType) {
                case -1:
                case 0:
                    var spinner = (Spinner) item.setting;
                    spinner.setSelection(0, false);

                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            boolean refresh = true;
                            ((TextView) parent.getChildAt(0)).setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
                            switch (item.type) {
                                case Author:
                                    if (developers != null && developers.get(position).ID != 0)
                                        ThemeRepoAPI.filters.put("author", String.valueOf(developers.get(position).ID));
                                    else
                                        refresh = !(null == ThemeRepoAPI.filters.remove("author"));
                                    break;
                            }
                            //TODO GUH
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                    break;
                case 1:
                    ((CheckedSetting) item.setting).setOnCheckedListener(aBoolean -> {
                        ThemeRepoAPI.localFilters.put("showInstalledThemes", aBoolean);
                        //TODO guh
                    });
                    break;


            }
        }

    }
}
