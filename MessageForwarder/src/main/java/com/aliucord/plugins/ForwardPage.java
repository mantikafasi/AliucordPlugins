package com.aliucord.plugins;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.wrappers.ChannelWrapper;
import com.aliucord.views.TextInput;
import com.discord.api.channel.Channel;
import com.discord.api.channel.ChannelUtils;
import com.discord.models.message.Message;
import com.discord.models.user.User;
import com.discord.stores.StoreStream;
import com.discord.utilities.icon.IconUtils;
import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForwardPage extends SettingsPage {
    private final Message originalMessage;
    private final List<SearchEntry> channelsList = new ArrayList<>();
    private final List<SearchEntry> filteredList = new ArrayList<>();
    private ChannelAdapter adapter;

    public static class SearchEntry {
        public final Channel channel;
        public final long channelId;
        public final long guildId;
        public final long lastMessageId;
        public final String displayName;
        public final String groupText;
        public final String normalizedName;
        public final String normalizedGroup;
        public final List<String> normalizedUsernames;

        public SearchEntry(Channel channel, long channelId, long guildId, long lastMessageId,
                           String displayName, String groupText, List<String> normalizedUsernames) {
            this.channel = channel;
            this.channelId = channelId;
            this.guildId = guildId;
            this.lastMessageId = lastMessageId;
            this.displayName = displayName;
            this.groupText = groupText;
            this.normalizedUsernames = normalizedUsernames;
            this.normalizedName = normalizeForSearch(displayName);
            this.normalizedGroup = normalizeForSearch(groupText);
        }
    }

    public ForwardPage(Message message) {
        this.originalMessage = message;
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        
        // Remove default scroll container to let RecyclerView occupy full page and scroll natively
        removeScrollView();

        setActionBarTitle("Forward Message");
        setActionBarSubtitle("Select target DM or channel...");

        Context context = view.getContext();
        
        // 1. Search Text Input
        TextInput searchInput = new TextInput(context);
        searchInput.setHint("Search channels or DMs...");
        addView(searchInput);

        // 2. Setup RecyclerView to occupy remaining screen area
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
        
        // Constrain RecyclerView to occupy all remaining vertical space natively
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1.0f
        );
        recyclerView.setLayoutParams(params);
        recyclerView.setNestedScrollingEnabled(false); // Since we removed outer scroll, RecyclerView handles native scroll directly

        adapter = new ChannelAdapter(context);
        recyclerView.setAdapter(adapter);
        addView(recyclerView);

        // 3. Query DMs and Guild Channels asynchronously in a background thread
        Utils.threadPool.execute(() -> {
            List<SearchEntry> tempEntries = new ArrayList<>();

            // Get DMs and Group DMs (guildId = 0L)
            try {
                Map<Long, Channel> dms = StoreStream.getChannels().getChannelsForGuild(0L);
                if (dms != null) {
                    for (Channel channel : dms.values()) {
                        if (channel != null) {
                            int type = channel.D();
                            if (type == 1 || type == 3) { // 1: DM, 3: Group DM
                                String displayName = ChannelUtils.c(channel);
                                String groupText = (type == 1) ? "Direct Message" : "Group DM";

                                 List<String> normalizedUsernames = new ArrayList<>();
                                 try {
                                     List<com.discord.api.user.User> recipients = ChannelWrapper.getRecipients(channel);
                                     if (recipients != null) {
                                         for (com.discord.api.user.User user : recipients) {
                                             if (user != null) {
                                                 String username = user.getUsername();
                                                 if (username != null) {
                                                     normalizedUsernames.add(normalizeForSearch(username));
                                                 }
                                             }
                                         }
                                     }
                                 } catch (Throwable ignored) {}

                                 if (normalizedUsernames.isEmpty()) {
                                     try {
                                         List<Long> recipientIds = ChannelWrapper.getRecipientIds(channel);
                                         if (recipientIds != null) {
                                             for (Long rId : recipientIds) {
                                                 if (rId != null) {
                                                     User user = StoreStream.getUsers().getUsers().get(rId);
                                                     if (user != null) {
                                                         String username = user.getUsername();
                                                         if (username != null) {
                                                             normalizedUsernames.add(normalizeForSearch(username));
                                                         }
                                                     }
                                                 }
                                             }
                                         }
                                     } catch (Throwable ignored) {}
                                 }

                                long lastMsgId = channel.l();
                                if (lastMsgId == 0) lastMsgId = channel.k();

                                tempEntries.add(new SearchEntry(
                                    channel,
                                    ChannelWrapper.getId(channel),
                                    0L,
                                    lastMsgId,
                                    displayName,
                                    groupText,
                                    normalizedUsernames
                                ));
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Get Guild Channels
            try {
                Map<Long, com.discord.models.guild.Guild> guilds = StoreStream.getGuilds().getGuilds();
                if (guilds != null) {
                    for (com.discord.models.guild.Guild guild : guilds.values()) {
                        if (guild != null) {
                            long guildId = guild.getId();
                            String guildName = guild.getName();
                            if (guildName == null) guildName = "Guild";

                            Map<Long, Channel> guildChannels = StoreStream.getChannels().getChannelsForGuild(guildId);
                            if (guildChannels != null) {
                                for (Channel channel : guildChannels.values()) {
                                    if (channel != null) {
                                        int type = channel.D();
                                        // 0: Text, 5: News, 10/11/12: Threads
                                        if (type == 0 || type == 5 || type == 10 || type == 11 || type == 12) {
                                            String name = ChannelWrapper.getName(channel);
                                            String displayName = "#" + (name != null ? name : "unknown");

                                            long lastMsgId = channel.l();
                                            if (lastMsgId == 0) lastMsgId = channel.k();

                                            tempEntries.add(new SearchEntry(
                                                channel,
                                                ChannelWrapper.getId(channel),
                                                guildId,
                                                lastMsgId,
                                                displayName,
                                                guildName,
                                                new ArrayList<>()
                                            ));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Retrieve Discord's native frecency sorted keys
            List<Long> frecentIds = new ArrayList<>();
            try {
                var frecency = StoreStream.getChannelsSelected().getFrecency();
                if (frecency != null) {
                    java.util.Collection<?> keys = frecency.getSortedKeys(System.currentTimeMillis());
                    if (keys != null) {
                        for (Object key : keys) {
                            if (key instanceof Long) {
                                frecentIds.add((Long) key);
                            } else if (key instanceof Number) {
                                frecentIds.add(((Number) key).longValue());
                            } else if (key != null) {
                                try {
                                    frecentIds.add(Long.parseLong(key.toString()));
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}

            // Usage-Based Sorting (Frecency/Usage sorting) based on Discord's native Suggestions
            tempEntries.sort((e1, e2) -> {
                int idx1 = frecentIds.indexOf(e1.channelId);
                int idx2 = frecentIds.indexOf(e2.channelId);
                if (idx1 != -1 && idx2 != -1) {
                    return Integer.compare(idx1, idx2); // Lower index in sorted keys is more frecent
                }
                if (idx1 != -1) return -1;
                if (idx2 != -1) return 1;

                // Fallback to chronological (last message ID descending)
                return Long.compare(e2.lastMessageId, e1.lastMessageId);
            });

            // Post the sorted results to main thread and populate adapter
            Utils.mainThread.post(() -> {
                channelsList.clear();
                channelsList.addAll(tempEntries);
                
                // Initialize/Update filtered list with current query
                EditText searchBox = searchInput.getEditText();
                String query = (searchBox != null) ? searchBox.getText().toString() : "";
                filterList(query);
                
                setActionBarSubtitle(tempEntries.size() + " Targets");
            });
        });

        // 4. Setup Real-time Search Filter
        EditText searchBox = searchInput.getEditText();
        if (searchBox != null) {
            searchBox.setMaxLines(1);
            searchBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    filterList(s.toString());
                }
            });
        }
    }

    public static String normalizeForSearch(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\-_#@.]", "").toLowerCase();
    }

    private void filterList(String query) {
        filteredList.clear();
        String normalizedQuery = normalizeForSearch(query);
        if (normalizedQuery.isEmpty()) {
            filteredList.addAll(channelsList);
        } else {
            for (SearchEntry entry : channelsList) {
                if (entry.normalizedName.contains(normalizedQuery) || entry.normalizedGroup.contains(normalizedQuery)) {
                    filteredList.add(entry);
                } else {
                    boolean matchedUsername = false;
                    for (String username : entry.normalizedUsernames) {
                        if (username.contains(normalizedQuery)) {
                            matchedUsername = true;
                            break;
                        }
                    }
                    if (matchedUsername) {
                        filteredList.add(entry);
                    }
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void forwardMessage(long targetChannelId, long guildId) {
        Utils.threadPool.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                JSONObject ref = new JSONObject();
                ref.put("type", 1); // Native forward type
                ref.put("channel_id", String.valueOf(originalMessage.getChannelId()));
                ref.put("message_id", String.valueOf(originalMessage.getId()));
                body.put("message_reference", ref);

                var req = Http.Request.newDiscordRNRequest("/channels/" + targetChannelId + "/messages", "POST")
                        .setHeader("content-type", "application/json");
                var res = req.executeWithBody(body.toString());
                if (res.ok()) {
                    Utils.showToast("Message forwarded!");
                    
                    // Track selection in Discord's native frecency tracker (updates suggestions)
                    try {
                        var frecency = StoreStream.getChannelsSelected().getFrecency();
                        if (frecency != null) {
                            frecency.track(targetChannelId, System.currentTimeMillis());
                        }
                    } catch (Throwable ignored) {}

                    Utils.mainThread.post(() -> {
                        try {
                            var store = StoreStream.getChannelsSelected();
                            java.lang.reflect.Method targetMethod = null;
                            Class<?> clazz = store.getClass();
                            while (clazz != null && targetMethod == null) {
                                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                                    if (m.getName().equals("trySelectChannel")) {
                                        targetMethod = m;
                                        break;
                                    }
                                }
                                clazz = clazz.getSuperclass();
                            }
                            if (targetMethod != null) {
                                targetMethod.setAccessible(true);
                                int paramCount = targetMethod.getParameterTypes().length;
                                if (paramCount == 4) {
                                    Class<?> analyticsClass = targetMethod.getParameterTypes()[3];
                                    Object[] constants = analyticsClass.getEnumConstants();
                                    Object analyticsVal = (constants != null && constants.length > 0) ? constants[0] : null;
                                    targetMethod.invoke(store, guildId, targetChannelId, null, analyticsVal);
                                } else if (paramCount == 3) {
                                    targetMethod.invoke(store, guildId, targetChannelId, null);
                                } else if (paramCount == 2) {
                                    targetMethod.invoke(store, guildId, targetChannelId);
                                }
                            }
                        } catch (Throwable t) {
                            Utils.showToast("Failed to switch channel: " + t.getMessage());
                        }
                    });
                } else {
                    Utils.showToast("Failed to forward: " + res.text());
                }
            } catch (Exception e) {
                Utils.showToast("Error: " + e.getMessage());
            }
        });
    }

    private void disableRounding(SimpleDraweeView iconView) {
        if (iconView == null) return;
        try {
            Object hierarchy = iconView.getHierarchy();
            Class<?> genericHierarchyClass = hierarchy.getClass();
            
            // Try standard method first
            try {
                Class<?> roundingParamsClass = Class.forName("com.facebook.drawee.generic.RoundingParams");
                java.lang.reflect.Method m = genericHierarchyClass.getMethod("setRoundingParams", roundingParamsClass);
                m.invoke(hierarchy, (Object) null);
                return;
            } catch (Throwable ignored) {}
            
            // Try obfuscated method names like "s", "t", "r"
            for (String methodName : new String[]{"s", "t", "r"}) {
                try {
                    for (java.lang.reflect.Method m : genericHierarchyClass.getMethods()) {
                        if (m.getName().equals(methodName) && m.getParameterTypes().length == 1) {
                            m.invoke(hierarchy, (Object) null);
                            return;
                        }
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }

    private class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {
        private final Context context;
        private final int layoutId;
        private final int iconId;
        private final int nameId;
        private final int groupId;
        private final int descriptionId;
        private final int unreadId;
        private final int mentionsId;

        public ChannelAdapter(Context context) {
            this.context = context;
            this.layoutId = Utils.getResId("view_global_search_item", "layout");
            this.iconId = Utils.getResId("item_icon_iv", "id");
            this.nameId = Utils.getResId("item_name_tv", "id");
            this.groupId = Utils.getResId("item_group_tv", "id");
            this.descriptionId = Utils.getResId("item_description_tv", "id");
            this.unreadId = Utils.getResId("item_unread", "id");
            this.mentionsId = Utils.getResId("item_mentions_tv", "id");
        }

        @Override
        public int getItemViewType(int position) {
            SearchEntry entry = filteredList.get(position);
            if (entry != null && entry.channel != null) {
                int type = entry.channel.D();
                if (type == 1 || type == 3) {
                    return 1; // VIEW_TYPE_DM
                }
            }
            return 2; // VIEW_TYPE_GUILD_CHANNEL
        }

        @Override
        public int getItemCount() {
            return filteredList.size();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ConstraintLayout itemView = new ConstraintLayout(context);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            itemView.setLayoutParams(params);
            itemView.setMinimumHeight(DimenUtils.dpToPx(56));

            try {
                android.util.TypedValue outValue = new android.util.TypedValue();
                context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                itemView.setBackgroundResource(outValue.resourceId);
            } catch (Throwable ignored) {}

            LayoutInflater.from(context).inflate(layoutId, itemView, true);

            if (viewType == 2) {
                try {
                    SimpleDraweeView draweeIcon = itemView.findViewById(iconId);
                    if (draweeIcon != null) {
                        ViewGroup.LayoutParams lp = draweeIcon.getLayoutParams();
                        ImageView newIconView = new ImageView(context);
                        newIconView.setId(iconId);
                        newIconView.setLayoutParams(lp);
                        newIconView.setPadding(draweeIcon.getPaddingLeft(), draweeIcon.getPaddingTop(), draweeIcon.getPaddingRight(), draweeIcon.getPaddingBottom());
                        newIconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        
                        itemView.removeView(draweeIcon);
                        itemView.addView(newIconView);
                    }
                } catch (Throwable ignored) {}
            }

            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SearchEntry entry = filteredList.get(position);
            if (entry == null) return;
            Channel channel = entry.channel;
            if (channel == null) return;

            if (holder.unreadView != null) holder.unreadView.setVisibility(View.GONE);
            if (holder.mentionsView != null) holder.mentionsView.setVisibility(View.GONE);
            if (holder.descriptionView != null) holder.descriptionView.setVisibility(View.GONE);

            int type = channel.D();
            long targetChannelId = entry.channelId;
            long guildId = entry.guildId;

            if (type == 1 || type == 3) {
                if (holder.iconView instanceof SimpleDraweeView) {
                    SimpleDraweeView draweeIcon = (SimpleDraweeView) holder.iconView;
                    
                    try {
                        Object hierarchy = draweeIcon.getHierarchy();
                        Class<?> genericHierarchyClass = hierarchy.getClass();
                        Class<?> roundingParamsClass = Class.forName("com.facebook.drawee.generic.RoundingParams");
                        Object roundingParams = roundingParamsClass.getMethod("asCircle").invoke(null);
                        genericHierarchyClass.getMethod("setRoundingParams", roundingParamsClass).invoke(hierarchy, roundingParams);
                    } catch (Throwable ignored) {}

                    try {
                        IconUtils.setIcon(draweeIcon, channel);
                    } catch (Throwable t) {
                        disableRounding(draweeIcon);
                        draweeIcon.setImageResource(Utils.getResId("ic_my_account_24dp", "drawable"));
                    }
                }
            } else {
                int iconRes;
                if (ChannelUtils.w(channel)) {
                    iconRes = Utils.getResId("ic_channel_voice_grey_18dp", "drawable");
                } else if (ChannelUtils.H(channel)) {
                    iconRes = Utils.getResId("ic_thread_grey_18dp", "drawable");
                    if (iconRes == 0) {
                        iconRes = Utils.getResId("ic_channel_thread_grey_18dp", "drawable");
                    }
                } else {
                    iconRes = Utils.getResId("ic_channel_text_grey_18dp", "drawable");
                }

                if (iconRes != 0) {
                    holder.iconView.setImageResource(iconRes);
                } else {
                    holder.iconView.setImageResource(Utils.getResId("ic_channel_text_grey_18dp", "drawable"));
                }
            }

            if (holder.nameView != null) holder.nameView.setText(entry.displayName);
            if (holder.groupView != null) {
                holder.groupView.setText(entry.groupText);
                holder.groupView.setVisibility(View.VISIBLE);
            }

            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> {
                close(); // Pop full-screen fragment stack transition
                forwardMessage(targetChannelId, guildId);
            });
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public View itemView;
            public ImageView iconView;
            public TextView nameView;
            public TextView groupView;
            public TextView descriptionView;
            public ImageView unreadView;
            public TextView mentionsView;

            public ViewHolder(ConstraintLayout itemView) {
                super(itemView);
                this.itemView = itemView;
                this.iconView = itemView.findViewById(iconId);
                this.nameView = itemView.findViewById(nameId);
                this.groupView = itemView.findViewById(groupId);
                this.descriptionView = itemView.findViewById(descriptionId);
                this.unreadView = itemView.findViewById(unreadId);
                this.mentionsView = itemView.findViewById(mentionsId);
            }
        }
    }
}
