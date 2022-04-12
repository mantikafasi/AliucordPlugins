package com.aliucord.plugins;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toolbar;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.TextInput;
import com.lytefast.flexinput.R;

import java.util.ArrayList;

public class ThemesPage extends SettingsPage {
    private static final int uniqueId = View.generateViewId();
    //com.aliucord.settings.Plugins
    ThemesAdapter adapter;
    ProgressBar loadingIcon;
    EditText searchBox;

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        removeScrollView();

        setActionBarTitle("Theme Repo");
        var context = view.getContext();
        int padding = DimenUtils.getDefaultPadding();
        int p = padding / 2;

        setActionBarSubtitle("Loading...");

        if (getHeaderBar().findViewById(uniqueId) == null) {
            Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END;
            params.setMarginEnd(p);

            TextInput input = new TextInput(context);
            input.setHint(context.getString(R.h.search));

            RecyclerView recyclerView = new RecyclerView(context);
            recyclerView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
            adapter = new ThemesAdapter(this, new ArrayList<>());
            recyclerView.setAdapter(adapter);
            ShapeDrawable shape = new ShapeDrawable(new RectShape());
            shape.setTint(Color.TRANSPARENT);
            shape.setIntrinsicHeight(padding);
            DividerItemDecoration decoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            decoration.setDrawable(shape);
            recyclerView.addItemDecoration(decoration);
            recyclerView.setPadding(0, padding, 0, 0);
            loadingIcon = new ProgressBar(context);
            loadingIcon.setIndeterminate(true);
            loadingIcon.setVisibility(View.GONE);

            addView(input);
            addView(loadingIcon);
            addView(recyclerView);

            Utils.threadPool.execute(() -> {
                adapter.setData(ThemeRepoAPI.getThemes());
                Utils.mainThread.post(() -> {
                    adapter.notifyDataSetChanged();
                    setActionBarSubtitle(ThemesAdapter.data.size() + " Themes");
                });
            });

            searchBox = input.getEditText();

            searchBox.setMaxLines(1);
            searchBox.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    adapter.getFilter().filter(s.toString());
                }
            });
        }
    }
}
