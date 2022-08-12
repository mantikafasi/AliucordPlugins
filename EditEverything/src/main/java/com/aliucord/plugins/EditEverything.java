package com.aliucord.plugins;

import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.InputDialog;
import com.aliucord.patcher.InsteadHook;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XposedBridge;

@SuppressWarnings("unused")
@AliucordPlugin
public class EditEverything extends Plugin {
    List<TextView> twList = new ArrayList<TextView>();
    private int clicks = 0;

    public static void openDialog(TextView tw) {
        var dialog = new InputDialog().setTitle("Edit Text");
        dialog.setOnDialogShownListener(view -> {
            dialog.getInputLayout().getEditText().setText(tw.getText());
        });
        dialog.setOnOkListener(v -> {
            tw.setText(dialog.getInput());
            dialog.dismiss();
        });
        dialog.show(Utils.getAppActivity().getSupportFragmentManager(), "s");

    }

    public void getTextViews(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            var view = vg.getChildAt(i);
            if (view instanceof ViewGroup) {
                getTextViews((ViewGroup) view);
            } else if (view instanceof TextView) twList.add((TextView) view);
        }
    }

    @Override
    public void start(Context context) throws NoSuchMethodException {
        Handler handler = new Handler();

        patcher.patch(View.class.getDeclaredMethod("performClick"), new InsteadHook(cf -> {
            if (cf.thisObject instanceof TextView || cf.thisObject instanceof ViewGroup) {
                clicks++;
                handler.postDelayed(() -> {
                    if (clicks >= 2) {
                        if (cf.thisObject instanceof ViewGroup) {
                            var a = (ViewGroup) cf.thisObject;
                            twList.clear();
                            getTextViews(a);

                            if (twList.size() == 1) {
                                openDialog(twList.get(0));
                            } else {
                                new CustomDialog(twList).show(Utils.getAppActivity().getSupportFragmentManager(), "a");
                            }
                        }
                        if (cf.thisObject instanceof TextView) openDialog((TextView) cf.thisObject);

                    } else {
                        if (clicks == 1) {
                            try {
                                XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, null);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    clicks = 0;
                }, 200);
            } else {
                try {
                    XposedBridge.invokeOriginalMethod(cf.method, cf.thisObject, null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }));

    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
        commands.unregisterAll();
    }
}
