package com.aliucord.plugins;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;
import android.util.TypedValue;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.PreHook;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

@AliucordPlugin
@SuppressWarnings("unused")
public class NewIcons extends Plugin {
    private final SparseIntArray replacements = new SparseIntArray();

    private Resources pluginResources;
    private int loaderResourcesArg = -1;
    private int loaderIdArg = -1;

    public NewIcons() {
        needsResources = true;
    }

    @Override
    public void start(Context context) throws Throwable {
        pluginResources = resources;
        int mappedCount = buildReplacementMap();
        if (mappedCount == 0) {
            logger.warn("No matching Discord drawable resources were found.");
            return;
        }

        try {
            Method loader = findLoadDrawableForCookieMethod();
            patcher.patch(loader, new PreHook(this::replaceFromResourcesImpl));
            logger.info("Patched ResourcesImpl.loadDrawableForCookie for " + mappedCount + " drawables.");
        } catch (Throwable t) {
            logger.warn("ResourcesImpl.loadDrawableForCookie patch failed", t);
        }
    }

    @Override
    public void stop(Context context) throws Throwable {
        patcher.unpatchAll();
        commands.unregisterAll();
        replacements.clear();
        pluginResources = null;
        loaderResourcesArg = -1;
        loaderIdArg = -1;
    }

    private int buildReplacementMap() {
        replacements.clear();

        Resources discordResources = Utils.getAppContext().getResources();
        String pluginPackage = getClass().getPackage().getName();

        for (String name : NewIconNames.NAMES) {
            int pluginId = pluginResources.getIdentifier(name, "drawable", pluginPackage);
            int discordId = findDiscordDrawableId(discordResources, name);
            if (discordId != 0 && pluginId != 0) {
                replacements.put(discordId, pluginId);
            }
        }

        return replacements.size();
    }

    private int findDiscordDrawableId(Resources discordResources, String name) {
        int id = discordResources.getIdentifier(name, "drawable", "com.discord");
        if (id != 0) return id;
        return 0;
    }

    private Method findLoadDrawableForCookieMethod() throws ClassNotFoundException, NoSuchMethodException {
        Class<?> resourcesImplClass = Class.forName("android.content.res.ResourcesImpl");
        for (Method method : resourcesImplClass.getDeclaredMethods()) {
            if (!method.getName().equals("loadDrawableForCookie") || !Drawable.class.isAssignableFrom(method.getReturnType())) {
                continue;
            }

            Class<?>[] params = method.getParameterTypes();
            int resourcesArg = -1;
            int typedValueArg = -1;
            int idArg = -1;

            for (int i = 0; i < params.length; i++) {
                if (Resources.class.isAssignableFrom(params[i])) resourcesArg = i;
                if (TypedValue.class.isAssignableFrom(params[i])) typedValueArg = i;
            }

            if (typedValueArg != -1) {
                for (int i = typedValueArg + 1; i < params.length; i++) {
                    if (params[i] == int.class) {
                        idArg = i;
                        break;
                    }
                }
            }

            if (resourcesArg != -1 && idArg != -1) {
                method.setAccessible(true);
                loaderResourcesArg = resourcesArg;
                loaderIdArg = idArg;
                return method;
            }
        }

        throw new NoSuchMethodException("ResourcesImpl.loadDrawableForCookie(Resources, TypedValue, int, ...)");
    }

    private void replaceFromResourcesImpl(MethodHookParam callFrame) {
        if (loaderResourcesArg < 0 || loaderIdArg < 0) return;

        Object resources = callFrame.args[loaderResourcesArg];
        Object id = callFrame.args[loaderIdArg];
        if (!(resources instanceof Resources) || !(id instanceof Integer)) return;

        setReplacementResult(callFrame, (Resources) resources, (Integer) id);
    }

    private void setReplacementResult(MethodHookParam callFrame, Resources requestResources, int discordId) {
        Drawable replacement = getReplacementDrawable(requestResources, discordId);
        if (replacement != null) {
            callFrame.setResult(replacement);
        }
    }

    private Drawable getReplacementDrawable(Resources requestResources, int discordId) {
        if (pluginResources == null || requestResources == pluginResources) return null;

        int pluginId = replacements.get(discordId, 0);
        if (pluginId == 0) return null;

        try {
            return pluginResources.getDrawable(pluginId, null).mutate();
        } catch (Throwable t) {
            replacements.delete(discordId);
            logger.warn("Disabled broken replacement for drawable id " + discordId, t);
            return null;
        }
    }
}
