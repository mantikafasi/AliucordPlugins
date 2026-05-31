package com.aliucord.plugins;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.api.SettingsAPI;
import com.aliucord.fragments.ConfirmDialog;
import com.aliucord.views.Button;
import com.aliucord.views.DangerButton;
import com.aliucord.widgets.BottomSheet;
import com.discord.utilities.color.ColorCompat;

import androidx.core.content.res.ResourcesCompat;

import java.security.KeyPair;

public class EncryptDMsSettings extends BottomSheet {
    private final SettingsAPI settings;

    public EncryptDMsSettings(SettingsAPI settings) {
        this.settings = settings;
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        setPadding(20);

        TextView title = new TextView(requireContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_Settings_Item_Header);
        title.setText("EncryptDMs");
        title.setTypeface(ResourcesCompat.getFont(requireContext(), Constants.Fonts.whitney_semibold));
        addView(title);

        TextView summary = new TextView(requireContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_TextView);
        summary.setText("Accepted keys: " + settings.getObject("userKeys", new java.util.HashMap<>()).size());
        summary.setTextColor(ColorCompat.getThemedColor(requireContext(), com.lytefast.flexinput.R.b.colorInteractiveNormal));
        addView(summary);

        Button copyKey = new Button(requireContext());
        copyKey.setText("Copy Public Key");
        copyKey.setOnClickListener(v -> {
            Utils.setClipboard("EncryptDMs public key", settings.getString("publicKey", ""));
            Utils.showToast("Public key copied");
        });
        addView(copyKey);

        DangerButton resetKeys = new DangerButton(requireContext());
        resetKeys.setText("Regenerate My Key Pair");
        resetKeys.setOnClickListener(v -> {
            ConfirmDialog dialog = new ConfirmDialog()
                    .setTitle("Regenerate keys?")
                    .setDescription("People with your old public key will need to accept your new key before encrypted chat works again.")
                    .setIsDangerous(true);
            dialog.setOnOkListener(ok -> {
                try {
                    KeyPair pair = RSA.generateKeyPair();
                    settings.setString("publicKey", RSA.encodePublicKey(pair.getPublic()));
                    settings.setString("privateKey", RSA.encodePrivateKey(pair.getPrivate()));
                    Utils.showToast("New encryption keys generated");
                    dialog.dismiss();
                    dismiss();
                } catch (Exception e) {
                    Utils.showToast("Failed to generate keys");
                }
            });
            dialog.show(getParentFragmentManager(), "encryptdms_regenerate_keys");
        });
        addView(resetKeys);
    }
}
