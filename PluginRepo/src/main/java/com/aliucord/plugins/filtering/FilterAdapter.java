package com.aliucord.plugins.filtering;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.plugins.PluginRepoAPI;
import com.aliucord.plugins.PluginsPage;
import com.discord.app.AppFragment;
import com.discord.utilities.color.ColorCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterAdapter extends RecyclerView.Adapter<FilterAdapter.ViewHolder> {
    AppFragment fragment;
    Context ctx;
    List<String> filters = new ArrayList<>(Arrays.asList("Author", "Sort By","Download All Plugins"));
    List<Developer> developers;
    List<SortOption> sort_options = new ArrayList<>(Arrays.asList(
            new SortOption("None", ""),
            new SortOption("Last Updated           ", "timestamp"),
            new SortOption("Last Added", "ID"),
            new SortOption("Most Starred", "repo_stars")));

    public FilterAdapter(AppFragment fragment) {
        this.fragment = fragment;
        ctx = fragment.requireContext();
    }

    @NonNull
    @Override
    public FilterAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(new AdapterItem(ctx));
    }

    @Override
    public void onBindViewHolder(@NonNull FilterAdapter.ViewHolder holder, int position) {
        holder.item.type = FilterType.values()[position];
        switch (holder.item.type) {
            case Author:
                Utils.threadPool.execute(() -> {
                    developers = PluginRepoAPI.getDevelopers();
                    developers.add(0, new Developer("None", 0));
                    Utils.mainThread.post(() -> {
                        holder.item.spinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, developers));
                    });
                });
            case sort_by:
                holder.item.spinner.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, sort_options));
        }
        holder.item.textView.setText(filters.get(position));
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    public enum FilterType {
        Author,
        sort_by,
        installAllPlugins
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        AdapterItem item;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            item = (AdapterItem) itemView;

            item.spinner.setSelection(0, false);

            item.textView.setOnClickListener(v -> {
                item.textView.setText("Installing...");
                Utils.showToast("Installing All Plugins...");
                PluginRepoAPI.filters.put("LIMIT",String.valueOf(400));
                Utils.threadPool.execute(() -> {
                    var plugins = PluginRepoAPI.getPlugins();
                    for (var plugin : plugins) {
                        if (plugin.getName().equals("PluginWiper") || PluginManager.plugins.containsKey(plugin.getName())) continue;
                        PluginRepoAPI.installPlugin(plugin.getName(),plugin.getManifest().updateUrl);
                        Utils.showToast("Successfully Installed " + plugin.getName());
                    }
                });

            });
            item.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    boolean refresh = true;
                    ((TextView) parent.getChildAt(0)).setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));
                    switch (item.type) {
                        case sort_by:
                            if (!sort_options.get(position).toString().equals("None"))
                                PluginRepoAPI.filters.put("sort_by", sort_options.get(position).optionValue);
                            else refresh = !(null == PluginRepoAPI.filters.remove("sort_by"));
                            break;
                        case Author:
                            if (developers != null && developers.get(position).ID != 0)
                                PluginRepoAPI.filters.put("author", String.valueOf(developers.get(position).ID));
                            else refresh = !(null == PluginRepoAPI.filters.remove("author"));
                            break;
                    }

                    if (refresh) ((PluginsPage) fragment).makeSearch();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

    }
}
