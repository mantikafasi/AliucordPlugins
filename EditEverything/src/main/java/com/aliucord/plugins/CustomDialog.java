package com.aliucord.plugins;

import android.database.DataSetObserver;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.aliucord.Constants;
import com.aliucord.utils.DimenUtils;
import com.discord.app.AppDialog;
import com.discord.utilities.color.ColorCompat;

import java.util.List;

public class CustomDialog extends AppDialog {
    LinearLayout root;
    List<TextView> textViewList;

    public CustomDialog(List<TextView> textViewList) {
        this.textViewList = textViewList;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return new LinearLayout(inflater.getContext(), null, 0, com.lytefast.flexinput.R.i.UiKit_Dialog_Container);
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        root = ((LinearLayout) view);
        var padding = DimenUtils.getDefaultPadding();

        var ctx = view.getContext();

        var tw = new TextView(ctx, null, 0, com.lytefast.flexinput.R.i.UserProfile_Section_Header);
        tw.setPadding(padding, padding, 0, padding);
        tw.setText("Select the text you want to edit");
        tw.setTextSize(15f);
        tw.setMovementMethod(LinkMovementMethod.getInstance());
        tw.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_bold));

        tw.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_200));


        var lw = new ListView(ctx);
        var adapter = new ListAdapter() {

            @Override
            public boolean areAllItemsEnabled() {
                return true;
            }

            @Override
            public boolean isEnabled(int position) {
                return true;
            }

            @Override
            public void registerDataSetObserver(DataSetObserver observer) {
            }

            @Override
            public void unregisterDataSetObserver(DataSetObserver observer) {
            }

            @Override
            public int getCount() {
                return textViewList.size();
            }

            @Override
            public Object getItem(int position) {
                return textViewList.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                var tw = new TextView(ctx);
                tw.setTypeface(ResourcesCompat.getFont(ctx, Constants.Fonts.whitney_semibold));
                tw.setTextSize(16f);
                tw.setMovementMethod(LinkMovementMethod.getInstance());
                tw.setTextColor(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.primary_dark_300));
                tw.setText(textViewList.get(position).getText());
                tw.setPadding(0, padding / 2, 0, padding / 2);
                tw.setOnClickListener(v -> {
                    EditEverything.openDialog(textViewList.get(position));
                    dismiss();
                });
                return tw;
            }

            @Override
            public int getItemViewType(int position) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

        };
        lw.setAdapter(adapter);
        lw.setPadding(padding, padding, padding, padding);
        lw.setDivider(new ColorDrawable(ColorCompat.getColor(ctx, com.lytefast.flexinput.R.c.white)));
        lw.setDividerHeight(1);

        root.addView(tw);
        root.addView(lw);

    }
}
