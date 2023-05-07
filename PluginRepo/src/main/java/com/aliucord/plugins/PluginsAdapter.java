package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.CollectionUtils;
import com.aliucord.Constants;
import com.aliucord.PluginManager;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.utils.ChangelogUtils;
import com.discord.app.AppFragment;
import com.discord.widgets.user.usersheet.WidgetUserSheet;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginsAdapter extends RecyclerView.Adapter<PluginsAdapter.ViewHolder> {
    //com.aliucord.settings.Adapter
    private final AppFragment fragment;
    private final Context ctx;
    public List<Plugin> data;

    public PluginsAdapter(AppFragment fragment, Collection<Plugin> plugins) {
        super();
        this.fragment = fragment;
        ctx = fragment.requireContext();
        data = (List<Plugin>) plugins;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(this, new PluginCard(ctx));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        CollectionUtils.filter(data,plugin -> plugin.getName().toLowerCase().contains("pog"));

        Plugin p = data.get(position);
        Plugin.Manifest manifest = p.getManifest();

        if (PluginManager.plugins.containsKey(p.getName())) {
            holder.card.installButton.setVisibility(View.GONE);
            holder.card.uninstallButton.setVisibility(View.VISIBLE);
        } else {
            holder.card.installButton.setVisibility(View.VISIBLE);
            holder.card.uninstallButton.setVisibility(View.GONE);
        }

        holder.card.descriptionView.setText(p.getManifest().description);
        holder.card.changeLogButton.setVisibility(!p.getManifest().changelog.equals("null") ? View.VISIBLE : View.GONE);

        if (manifest.authors == null) return;
        String title = String.format("%s v%s by %s", p.getName(), manifest.version, TextUtils.join(", ", manifest.authors));

        SpannableString spannableTitle = new SpannableString(title);
        for (Plugin.Manifest.Author author : manifest.authors) {
            if (author.id < 1) continue;
            int i = title.indexOf(author.name, p.getName().length() + 2 + manifest.version.length() + 3);
            spannableTitle.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    WidgetUserSheet.Companion.show(author.id, fragment.getParentFragmentManager());
                }
            }, i, i + author.name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        holder.card.titleView.setText(spannableTitle);
    }

    private String getGithubUrl(Plugin plugin) {
        return plugin
                .getManifest().updateUrl
                .replace("raw.githubusercontent.com", "github.com")
                .replaceFirst("/builds.*", "");
    }

    public void onGithubClick(int position) {
        Utils.launchUrl(getGithubUrl(data.get(position)));
    }

    public void onChangeLogClick(int position) {
        Plugin p = data.get(position);
        Plugin.Manifest manifest = p.getManifest();
        if (manifest.changelog != null) {
            String url = getGithubUrl(p);
            ChangelogUtils.show(ctx, p.getName() + " v" + manifest.version, manifest.changelogMedia, manifest.changelog, new ChangelogUtils.FooterAction(com.lytefast.flexinput.R.e.ic_account_github_white_24dp, url));
        }
    }

    public void onInstallClick(int position) {
        Plugin p = data.get(position);
        Utils.threadPool.execute(() -> {
            if (PluginRepoAPI.installPlugin(p.getName(), p.getManifest().updateUrl)) {
                Utils.mainThread.post(() -> {
                    Utils.showToast("Successfully installed " + p.getName());
                    notifyItemChanged(position);
                });

            }
        });
    }

    public void onUninstallClick(int position) {
        Plugin p = data.get(position);

        ConfirmDialog dialog = new ConfirmDialog()
                .setIsDangerous(true)
                .setTitle("Delete " + p.getName())
                .setDescription("Are you sure you want to delete this plugin? This action cannot be undone.");
        dialog.setOnOkListener(e -> {
            File pluginFile = new File(Constants.BASE_PATH + "/plugins/" + p.__filename + ".zip");
            if (pluginFile.exists() && !pluginFile.delete()) {
                PluginManager.logger.errorToast("Failed to delete plugin " + p.getName(), null);
                return;
            }

            PluginManager.stopPlugin(p.getName());
            PluginManager.plugins.remove(p.getName());
            notifyItemChanged(position);
            PluginManager.logger.infoToast("Successfully deleted " + p.getName());


            dialog.dismiss();


            if (p.requiresRestart()) Utils.promptRestart();
        });

        dialog.show(fragment.getParentFragmentManager(), "Confirm Plugin Uninstall");
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        public final PluginCard card;
        private final PluginsAdapter adapter;

        @SuppressLint("SetTextI18n")
        public ViewHolder(PluginsAdapter adapter, PluginCard card) {
            super(card);
            this.adapter = adapter;
            this.card = card;

            card.repoButton.setOnClickListener(this::onGithubClick);
            card.changeLogButton.setOnClickListener(this::onChangeLogClick);
            card.installButton.setOnClickListener(this::onInstallClick);
            card.uninstallButton.setOnClickListener(this::onUninstallClick);
        }

        public void onUninstallClick(View v) {
            adapter.onUninstallClick(getAdapterPosition());
        }

        public void onGithubClick(View view) {
            adapter.onGithubClick(getAdapterPosition());
        }

        public void onChangeLogClick(View view) {
            adapter.onChangeLogClick(getAdapterPosition());
        }

        public void onInstallClick(View view) {
            adapter.onInstallClick(getAdapterPosition());
        }
    }
}
