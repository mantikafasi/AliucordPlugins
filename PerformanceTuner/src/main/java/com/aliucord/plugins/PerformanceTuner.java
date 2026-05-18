package com.aliucord.plugins;

import android.content.Context;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.Hook;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.PreHook;
import com.discord.api.message.embed.EmbedType;
import com.discord.embed.RenderableEmbedMedia;
import com.discord.stores.StoreStream;
import com.discord.views.typing.TypingDots;
import com.discord.widgets.chat.list.WidgetChatList;
import com.discord.widgets.chat.list.InlineMediaView;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapter;
import com.discord.widgets.chat.list.model.WidgetChatListModel;
import com.discord.widgets.chat.list.model.WidgetChatListModelMessages;

import java.lang.reflect.Method;
import java.util.WeakHashMap;

@SuppressWarnings("unused")
@AliucordPlugin
public class PerformanceTuner extends Plugin {
    public static final String KEY_DIAGNOSTICS = "diagnostics";
    public static final String KEY_HOLD_LAST_ROWS_DURING_LOAD = "holdLastRowsDuringLoad";
    public static final String KEY_PRESERVE_LOADER_TOUCH_STATE = "preserveLoaderTouchState";
    public static final String KEY_RECYCLER_REUSE = "recyclerReuse";
    public static final String KEY_DISABLE_TYPING_DOTS = "disableTypingDots";
    public static final String KEY_PREVENT_INLINE_PLAYERS = "preventInlinePlayers";

    private final ThreadLocal<Long> channelSwitchStart = new ThreadLocal<>();
    private final ThreadLocal<Long> adapterSetDataStart = new ThreadLocal<>();
    private final ThreadLocal<Long> messageModelBuildStart = new ThreadLocal<>();
    private final WeakHashMap<WidgetChatList, WidgetChatListModel> lastNonEmptyChatModels = new WeakHashMap<>();
    private long lastInlineMediaLogAt = 0L;
    private long lastTypingDotsLogAt = 0L;
    private long lastLoaderStateLogAt = 0L;
    private long lastHeldRowsLogAt = 0L;

    @Override
    public void start(Context context) throws Throwable {
        settingsTab = new SettingsTab(PerformanceTunerSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        patchChannelSwitchDiagnostics();
        patchLoaderTouchState();
        patchMessageModelDiagnostics();
        patchChatListStateHold();
        patchChatAdapterDiagnostics();
        patchRecyclerReuse();
        patchInlineMediaPlayers();

        if (settings.getBool(KEY_DISABLE_TYPING_DOTS, true))
            patchTypingDots();
    }

    private boolean diagnosticsEnabled() {
        return settings.getBool(KEY_DIAGNOSTICS, true);
    }

    private boolean preventInlinePlayers() {
        return settings.getBool(KEY_PREVENT_INLINE_PLAYERS, true);
    }

    private boolean holdLastRowsDuringLoad() {
        return settings.getBool(KEY_HOLD_LAST_ROWS_DURING_LOAD, true);
    }

    private boolean recyclerReuseEnabled() {
        return settings.getBool(KEY_RECYCLER_REUSE, true);
    }

    private boolean preserveLoaderTouchState() {
        return settings.getBool(KEY_PRESERVE_LOADER_TOUCH_STATE, true);
    }

    private void patchChannelSwitchDiagnostics() throws NoSuchMethodException {
        var method = StoreStream.class.getDeclaredMethod("handleChannelSelected", long.class);
        method.setAccessible(true);

        patcher.patch(method, new PreHook(callFrame -> {
            if (diagnosticsEnabled())
                channelSwitchStart.set(System.nanoTime());
        }));

        patcher.patch(method, new Hook(callFrame -> {
            if (!diagnosticsEnabled())
                return;

            Long start = channelSwitchStart.get();
            channelSwitchStart.remove();
            if (start == null)
                return;

            long channelId = (long) callFrame.args[0];
            logger.info("[PerformanceTuner] channel switch store fanout channel=" + channelId + " took " + elapsedMs(start) + "ms");
        }));
    }

    private void patchLoaderTouchState() throws Throwable {
        if (!preserveLoaderTouchState())
            return;

        Class<?> resetter = Class.forName("com.discord.stores.StoreMessagesLoader$handleChannelSelected$1");
        Method method = resetter.getDeclaredMethod("invoke2", Class.forName("com.discord.stores.StoreMessagesLoader$ChannelLoadedState"));
        method.setAccessible(true);

        patcher.patch(method, new InsteadHook(callFrame -> {
            throttledLoaderStateLog();
            return callFrame.args[0];
        }));
    }

    private void patchMessageModelDiagnostics() throws Throwable {
        Class<?> builder = Class.forName("com.discord.widgets.chat.list.model.WidgetChatListModelMessages$Companion$get$1");
        Method target = null;
        for (Method method : builder.getDeclaredMethods()) {
            if (method.getName().equals("invoke")
                    && method.getParameterTypes().length == 15
                    && method.getReturnType() == WidgetChatListModelMessages.class) {
                target = method;
                break;
            }
        }

        if (target == null) {
            logger.warn("[PerformanceTuner] could not find chat message model builder for diagnostics");
            return;
        }

        target.setAccessible(true);
        patcher.patch(target, new PreHook(callFrame -> {
            if (diagnosticsEnabled())
                messageModelBuildStart.set(System.nanoTime());
        }));

        patcher.patch(target, new Hook(callFrame -> {
            if (!diagnosticsEnabled())
                return;

            Long start = messageModelBuildStart.get();
            messageModelBuildStart.remove();
            if (start == null || !(callFrame.getResult() instanceof WidgetChatListModelMessages))
                return;

            WidgetChatListModelMessages result = (WidgetChatListModelMessages) callFrame.getResult();
            int count = result.getItems() == null ? -1 : result.getItems().size();
            logger.info("[PerformanceTuner] message model rebuild entries=" + count + " took " + elapsedMs(start) + "ms");
        }));
    }

    private void patchChatAdapterDiagnostics() throws NoSuchMethodException {
        var method = WidgetChatListAdapter.class.getDeclaredMethod("setData", WidgetChatListAdapter.Data.class);

        patcher.patch(method, new PreHook(callFrame -> {
            holdPreviousRowsForTransientLoadingModel(callFrame);

            if (diagnosticsEnabled())
                adapterSetDataStart.set(System.nanoTime());
        }));

        patcher.patch(method, new Hook(callFrame -> {
            if (!diagnosticsEnabled())
                return;

            Long start = adapterSetDataStart.get();
            adapterSetDataStart.remove();
            if (start == null)
                return;

            WidgetChatListAdapter.Data data = (WidgetChatListAdapter.Data) callFrame.args[0];
            int count = data.getList() == null ? -1 : data.getList().size();
            logger.info("[PerformanceTuner] chat adapter setData entries=" + count + " dispatch took " + elapsedMs(start) + "ms");
        }));
    }

    private void patchChatListStateHold() throws NoSuchMethodException {
        Method method = WidgetChatList.class.getDeclaredMethod("configureUI", WidgetChatListModel.class);
        method.setAccessible(true);

        patcher.patch(method, new PreHook(callFrame -> {
            if (!holdLastRowsDuringLoad())
                return;

            WidgetChatList widget = (WidgetChatList) callFrame.thisObject;
            WidgetChatListModel nextData = (WidgetChatListModel) callFrame.args[0];
            WidgetChatListModel lastData = lastNonEmptyChatModels.get(widget);

            if (isUsefulChatModel(nextData)) {
                lastNonEmptyChatModels.put(widget, nextData);
                return;
            }

            if (lastData == null)
                return;

            if (nextData == null || isTransientLoadingModel(nextData)) {
                callFrame.args[0] = lastData;
                throttledHeldRowsLog(lastData.getChannelId(), nextData == null ? 0L : nextData.getChannelId());
            }
        }));
    }

    private void holdPreviousRowsForTransientLoadingModel(PreHook.MethodHookParam callFrame) {
        if (!holdLastRowsDuringLoad() || !(callFrame.args[0] instanceof WidgetChatListModel))
            return;

        WidgetChatListAdapter adapter = (WidgetChatListAdapter) callFrame.thisObject;
        WidgetChatListAdapter.Data currentData = adapter.getData();
        WidgetChatListModel nextData = (WidgetChatListModel) callFrame.args[0];
        if (!isUsefulAdapterData(currentData)
                || currentData.getChannelId() == nextData.getChannelId()
                || !isTransientLoadingModel(nextData)) {
            return;
        }

        callFrame.args[0] = currentData;
        throttledHeldRowsLog(currentData.getChannelId(), nextData.getChannelId());
    }

    private boolean isUsefulAdapterData(WidgetChatListAdapter.Data data) {
        return data != null && data.getList() != null && data.getList().size() > 3;
    }

    private boolean isUsefulChatModel(WidgetChatListModel data) {
        return data != null && isUsefulAdapterData(data);
    }

    private boolean isTransientLoadingModel(WidgetChatListModel data) {
        return data != null
                && data.isLoadingMessages()
                && data.getList() != null
                && data.getList().size() <= 3;
    }

    private void patchRecyclerReuse() throws NoSuchMethodException {
        patcher.patch(WidgetChatList.class.getDeclaredMethod("onViewBound", View.class), new Hook(callFrame -> {
            if (!recyclerReuseEnabled())
                return;

            View root = (View) callFrame.args[0];
            RecyclerView recycler = root.findViewById(Utils.getResId("chat_list_recycler", "id"));
            if (recycler == null)
                return;

            recycler.setItemAnimator(null);
            recycler.setItemViewCacheSize(24);

            RecyclerView.RecycledViewPool pool = recycler.getRecycledViewPool();
            for (int type = 0; type <= 64; type++)
                pool.setMaxRecycledViews(type, 24);

            if (diagnosticsEnabled())
                logger.info("[PerformanceTuner] tuned chat RecyclerView cache and recycled view pool");
        }));
    }

    private void patchInlineMediaPlayers() throws NoSuchMethodException {
        var method = InlineMediaView.class.getDeclaredMethod(
                "updateUI",
                RenderableEmbedMedia.class,
                String.class,
                EmbedType.class,
                Integer.class,
                Integer.class,
                String.class
        );
        method.setAccessible(true);

        patcher.patch(method, new PreHook(callFrame -> {
            if (callFrame.args[1] == null)
                return;

            if (diagnosticsEnabled()) {
                EmbedType embedType = (EmbedType) callFrame.args[2];
                String featureTag = String.valueOf(callFrame.args[5]);
                logger.info("[PerformanceTuner] inline media progressive bind type=" + embedType + " feature=" + featureTag);
            }

            if (!preventInlinePlayers())
                return;

            callFrame.args[1] = null;
            throttledInlineMediaLog();
        }));
    }

    private void patchTypingDots() throws NoSuchMethodException {
        patcher.patch(TypingDots.class.getDeclaredMethod("a", boolean.class), new InsteadHook(callFrame -> {
            TypingDots dots = (TypingDots) callFrame.thisObject;
            dots.c();
            dots.setVisibility(View.GONE);
            throttledTypingDotsLog();
            return null;
        }));
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private void throttledInlineMediaLog() {
        if (!diagnosticsEnabled())
            return;

        long now = System.currentTimeMillis();
        if (now - lastInlineMediaLogAt < 5_000L)
            return;

        lastInlineMediaLogAt = now;
        logger.info("[PerformanceTuner] static inline media is active; skipped inline player creation");
    }

    private void throttledTypingDotsLog() {
        if (!diagnosticsEnabled())
            return;

        long now = System.currentTimeMillis();
        if (now - lastTypingDotsLogAt < 5_000L)
            return;

        lastTypingDotsLogAt = now;
        logger.info("[PerformanceTuner] typing dot animation suppressed");
    }

    private void throttledLoaderStateLog() {
        if (!diagnosticsEnabled())
            return;

        long now = System.currentTimeMillis();
        if (now - lastLoaderStateLogAt < 5_000L)
            return;

        lastLoaderStateLogAt = now;
        logger.info("[PerformanceTuner] preserved old-channel loader touch state during channel switch");
    }

    private void throttledHeldRowsLog(long oldChannelId, long newChannelId) {
        if (!diagnosticsEnabled())
            return;

        long now = System.currentTimeMillis();
        if (now - lastHeldRowsLogAt < 5_000L)
            return;

        lastHeldRowsLogAt = now;
        logger.info("[PerformanceTuner] held previous chat rows during transient channel load old="
                + oldChannelId + " new=" + newChannelId);
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
