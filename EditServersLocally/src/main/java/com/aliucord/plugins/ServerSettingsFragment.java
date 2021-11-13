package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.DataClasses.GuildData;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.views.Button;
import com.aliucord.views.Divider;
import com.discord.models.guild.Guild;
import com.discord.stores.StoreStream;
import com.discord.utilities.guilds.GuildUtilsKt;
import com.discord.utilities.icon.IconUtils;
import com.discord.utilities.images.MGImages;
import com.facebook.drawee.view.SimpleDraweeView;
import com.lytefast.flexinput.R;

public class ServerSettingsFragment extends SettingsPage {

    Context ctx;
    Guild guild;
    EditServersLocally plugin;
    GuildData data;

    // NSTW !!! DO NOT READ !!!

    public ServerSettingsFragment(Guild guild, EditServersLocally plugin) {
        this.guild = guild;
        this.plugin = plugin;

        data = plugin.getGuildData(guild);
        if (data.orginalName == null && data.serverName == null) {
            data.orginalName = guild.getName();
        }

    }

    enum optionType {SERVERNAME, SERVERIMAGE}

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        this.ctx = view.getContext();
        var dp = DimenUtils.getDefaultPadding();
        setActionBarTitle(String.format("Edit \"%s\"", guild.getName()));
        setActionBarSubtitle("EditServersLocally");

        createOption(optionType.SERVERNAME, getLinearLayout());
        addView(new Divider(ctx));

        createOption(optionType.SERVERIMAGE, getLinearLayout());

        Button saveButton = new Button(ctx);
        saveButton.setText("Save");
        saveButton.setGravity(View.FOCUS_RIGHT);
        saveButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        saveButton.setWidth(DimenUtils.dpToPx(50));
        saveButton.setOnClickListener(v -> setSettings());
        saveButton.setPadding(dp, dp, dp, dp);

        Button cancelButton = new Button(ctx);
        cancelButton.setText("Cancel");
        cancelButton.setGravity(View.FOCUS_RIGHT);
        cancelButton.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        cancelButton.setWidth(DimenUtils.dpToPx(50));
        cancelButton.setOnClickListener(v -> close());

        addView(saveButton);
        addView(cancelButton);

    }

    @SuppressLint("SetTextI18n")
    public void createOption(optionType optionName, LinearLayout viewLayout) {

        TextView tw = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label);
        EditText et = new EditText(ctx);
        tw.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
        et.setTextColor(ContextCompat.getColor(ctx, R.c.primary_000));
        et.setHintTextColor(ContextCompat.getColor(ctx, R.c.grey_2));
        var dp = DimenUtils.getDefaultPadding();
        tw.setPadding(dp, dp, dp, dp);
        et.setPadding(dp, dp, dp, dp);

        tw.setTextSize(16);


        switch (optionName) {
            case SERVERNAME:

                et.setHint(data.orginalName == null ? guild.getName() : data.orginalName);
                tw.setText("Server Name (If changes are not shown after saving, try restarting discord)");
                if (data.serverName != null) {
                    et.setText(data.serverName);
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
                        data.serverName = s.toString().isEmpty() ? null : s.toString();
                    }
                });
                viewLayout.addView(tw);
                viewLayout.addView(et);
                break;

            case SERVERIMAGE:
                SimpleDraweeView imageView = new SimpleDraweeView(ctx);
                viewLayout.addView(tw);
                addView(imageView);
                ViewGroup.LayoutParams params = imageView.getLayoutParams();
                params.width = DimenUtils.dpToPx(70);
                params.height = DimenUtils.dpToPx(70);
                imageView.setLayoutParams(params);
                int mediaProxySize = IconUtils.getMediaProxySize(imageView.getLayoutParams().width);
                MGImages.setImage$default(imageView, data.imageURL == null ? data.orginalURL : data.imageURL + "?size=" + mediaProxySize, imageView.getWidth(), imageView.getHeight(), true, null, (o, o1) -> true, 112, null);
                // SimpleDraweeViewExtensionsKt.setGuildIcon(view,true,guild,(float) 500f,20,0,0,360f,true,imageRequestBuilder -> { return null; });
                //MGImages.setRoundingParams(view,100,false,0,0,100f);

                var iconURL = data.orginalURL == null ? IconUtils.getForGuild(guild) : data.orginalURL;

                tw.setText("Server Image (switch servers or restart discord for changes to take effect)");
                et.setText(data.imageURL != null ? data.imageURL : "");
                et.setHint(iconURL);
                et.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                    }

                    @Override
                    public void afterTextChanged(Editable s) {
                        data.imageURL = s.toString().trim().isEmpty() ? null : s.toString();

                        MGImages.setImage$default(imageView, data.imageURL == null ? data.orginalURL : data.imageURL + "?size=" + mediaProxySize, imageView.getWidth(), imageView.getHeight(), true, null, (o, o1) -> true, 112, null);
                    }
                });
                viewLayout.addView(et);
                break;
        }
    }

    public void setSettings() {
        plugin.updateGuildData(data);
        Toast.makeText(ctx, "Settings Saved", Toast.LENGTH_SHORT).show();
        var guild2 = GuildUtilsKt.createApiGuild(StoreStream.getGuilds().getGuild(guild.getId()));
        try {

            ReflectUtils.setField(guild2, "icon", data.imageURL == null ? data.orginalURL : data.imageURL);

            ReflectUtils.setField(guild2, "name", data.serverName == null ? data.orginalName : data.serverName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        if (data.serverName != null || data.imageURL != null) {
            StoreStream.access$handleGuildUpdate(StoreStream.getPresences().getStream(), guild2);
        } else {
            plugin.removeGuildData(data.guildID);
        }

        getActivity().onBackPressed();
    }

}
