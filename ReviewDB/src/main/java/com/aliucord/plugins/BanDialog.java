package com.aliucord.plugins;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.aliucord.Utils;
import com.aliucord.fragments.ConfirmDialog;
import com.discord.app.AppDialog;
import com.discord.databinding.LeaveGuildDialogBinding;
import com.discord.views.LoadingButton;
import com.discord.widgets.guilds.leave.WidgetLeaveGuildDialog$binding$2;
import com.google.android.material.button.MaterialButton;

public class BanDialog extends AppDialog {
    // https://github.com/Aliucord/Aliucord/blob/main/Aliucord/src/main/java/com/aliucord/fragments/ConfirmDialog.java
    // because original method didnt support custom button texts I recreated it
    private static final int resId = Utils.getResId("leave_guild_dialog", "layout");
    private LeaveGuildDialogBinding binding;
    private CharSequence title;
    private View.OnClickListener onCancelListener;
    private CharSequence description;
    private final boolean isDangerous = false;
    private View.OnClickListener onOkListener;

    public BanDialog() {
        super(resId);
    }

    public void onViewBound(View view) {
        super.onViewBound(view);
        binding = WidgetLeaveGuildDialog$binding$2.INSTANCE.invoke(view);
        LoadingButton okButton = this.getOKButton();
        okButton.setText("Ok");
        okButton.setIsLoading(false);
        okButton.setOnClickListener((e) -> {
            this.dismiss();
        });
        this.getCancelButton().setText("Appeal");
        getCancelButton().setOnClickListener(onCancelListener);
        okButton.setBackgroundColor(ContextCompat.getColor(view.getContext(), com.lytefast.flexinput.R.c.uikit_btn_bg_color_selector_red));
        getHeader().setText(title);
        getBody().setText(description);

        this.getBody().setMovementMethod(LinkMovementMethod.getInstance());
    }

    public final LinearLayout getRoot() {
        return this.binding.a;
    }

    public final MaterialButton getCancelButton() {
        return this.binding.b;
    }

    public final LoadingButton getOKButton() {
        return this.binding.c;
    }

    public final TextView getBody() {
        return this.binding.d;
    }

    public final TextView getHeader() {
        return binding.e;
    }

    public BanDialog setTitle(CharSequence title) {
        this.title = title;
        return this;
    }

    public BanDialog setDescription(CharSequence description) {
        this.description = description;
        return this;
    }

    public BanDialog setOnCancelListener(View.OnClickListener listener) {
        onCancelListener = listener;
        return this;
    }
}
