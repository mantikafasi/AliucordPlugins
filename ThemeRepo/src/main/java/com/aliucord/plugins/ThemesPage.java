package com.aliucord.plugins;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.aliucord.Constants;
import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.plugins.filtering.FilterAdapter;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.TextInput;
import com.aliucord.views.ToolbarButton;
import com.discord.utilities.color.ColorCompat;
import com.lytefast.flexinput.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ThemesPage extends SettingsPage {
    private static final int uniqueId = View.generateViewId();
    //com.aliucord.settings.Plugins
    ThemesAdapter adapter;
    ProgressBar loadingIcon;
    EditText searchBox;

    @Override
    public void onViewBound(View view) {
        //TODO , ADD IMAGE TO THEMECARD,MAKE FILTERS WORK,MAKE ADD THEME PAGE
        super.onViewBound(view);

        setActionBarTitle("Theme Repo");
        var context = view.getContext();
        int padding = DimenUtils.getDefaultPadding();
        int p = padding / 2;
        ThemeRepoAPI.filters = new HashMap<>();

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

            RecyclerView filterView = new RecyclerView(context);
            filterView.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
            FilterAdapter filterAdapter = new FilterAdapter(this);
            filterView.setAdapter(filterAdapter);
            filterView.addItemDecoration(decoration);
            filterView.setPadding(0, padding, 0, 0);
            filterView.setVisibility(View.GONE);

            var openDrawable = ContextCompat.getDrawable(context, R.e.ic_arrow_down_14dp).mutate();
            openDrawable.setTint(ColorCompat.getThemedColor(context, R.b.colorInteractiveNormal));

            var closedDrawable = new LayerDrawable(new Drawable[]{openDrawable}) {
                @Override
                public void draw(Canvas canvas) {
                    var bounds = openDrawable.getBounds();
                    canvas.save();
                    canvas.rotate(270, bounds.width() / 2f, bounds.height() / 2f);
                    super.draw(canvas);
                    canvas.restore();
                }
            };

            var header = new TextView(context, null, 0, R.i.UiKit_Settings_Item_Header);
            header.setText("Filters");
            header.setTypeface(ResourcesCompat.getFont(context, Constants.Fonts.whitney_semibold));
            header.setOnClickListener(v -> {
                TransitionManager.beginDelayedTransition(getLinearLayout());
                if (filterView.getVisibility() == View.VISIBLE) {
                    filterView.setVisibility(View.GONE);
                    header.setCompoundDrawablesRelativeWithIntrinsicBounds(closedDrawable, null, null, null);
                } else {
                    filterView.setVisibility(View.VISIBLE);
                    header.setCompoundDrawablesRelativeWithIntrinsicBounds(openDrawable, null, null, null);
                }
            });
            header.setCompoundDrawablesRelativeWithIntrinsicBounds(closedDrawable, null, null, null);
            int px = DimenUtils.dpToPx(5);
            header.setPadding(px, px * 3, 0, px * 3);

            addView(input);
            addView(header);
            addView(filterView);
            addView(loadingIcon);
            addView(recyclerView);

            Utils.threadPool.execute(() -> {
                adapter.setData(ThemeRepoAPI.getThemes());
                Utils.mainThread.post(() -> {
                    adapter.notifyDataSetChanged();
                    setActionBarSubtitle(adapter.data.size() + " Themes");
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
