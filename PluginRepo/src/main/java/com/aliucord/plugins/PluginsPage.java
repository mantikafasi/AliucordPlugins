package com.aliucord.plugins;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.entities.Plugin;
import com.aliucord.fragments.SettingsPage;
import com.aliucord.settings.Plugins;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.GsonUtils;
import com.aliucord.views.TextInput;
import com.aliucord.views.ToolbarButton;
import com.aliucord.widgets.LinearLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginsPage extends SettingsPage {
    private static final int uniqueId = View.generateViewId();
    //com.aliucord.settings.Plugins

    @SuppressLint("NotifyDataSetChanged")
    public void makeSearch(String input,int index){
        if (adapter!=null && !(adapter.data.size() < index)) {
            Utils.threadPool.execute(() -> {
                Utils.mainThread.post(() -> {
                    if (index == 0) {
                        loadingIcon.setVisibility(View.VISIBLE);
                        adapter.data = new ArrayList<>();
                        adapter.notifyDataSetChanged();
                    }
                });
                var plugs = PluginRepoAPI.getPlugins(input,index);
                adapter.data.addAll(plugs);
                Utils.mainThread.post(() -> {
                    loadingIcon.setVisibility(View.GONE);
                    if (plugs.size() > 0) adapter.notifyDataSetChanged();
                    setActionBarSubtitle(adapter.data.size() + " Shown");
                });
                requestMade = false;
            });

        }
    }

    public void makeSearch(String input){makeSearch(input,0);}

    PluginsAdapter adapter;
    ProgressBar loadingIcon;
    int index = 0;
    boolean requestMade = false;

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);
        setActionBarTitle("Plugin Repo");
        var context = view.getContext();
        int padding = DimenUtils.getDefaultPadding();
        int p = padding / 2;
        setActionBarSubtitle("Loading...");

        Utils.threadPool.execute(() -> {

            List<Plugin> plugins = PluginRepoAPI.getPlugins();
            Utils.mainThread.post(() -> {
                setActionBarSubtitle(plugins.size() + " Shown");

                if (getHeaderBar().findViewById(uniqueId) == null) {
                    ToolbarButton pluginFolderBtn = new ToolbarButton(context);
                    pluginFolderBtn.setId(uniqueId);
                    Toolbar.LayoutParams params = new Toolbar.LayoutParams(Toolbar.LayoutParams.WRAP_CONTENT, Toolbar.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.END;
                    params.setMarginEnd(p);
                    pluginFolderBtn.setLayoutParams(params);
                    pluginFolderBtn.setPadding(p, p, p, p);
                    pluginFolderBtn.setImageDrawable(ContextCompat.getDrawable(context, com.lytefast.flexinput.R.e.ic_open_in_new_white_24dp));

                    pluginFolderBtn.setOnClickListener(e -> {
                        File dir = new File(Constants.PLUGINS_PATH);
                        if (!dir.exists() && !dir.mkdir()) {
                            Utils.showToast("Failed to create plugins directory!", true);
                            return;
                        }
                        Utils.launchFileExplorer(dir);
                    });

                    addHeaderButton(pluginFolderBtn);

                    TextInput input = new TextInput(context);
                    input.setHint(context.getString(com.lytefast.flexinput.R.h.search));

                    RecyclerView recyclerView = new RecyclerView(context);
                    recyclerView.setLayoutManager(new LinearLayoutManager(context,RecyclerView.VERTICAL, false));
                    adapter = new PluginsAdapter(this, plugins);
                    recyclerView.setAdapter(adapter);
                    ShapeDrawable shape = new ShapeDrawable(new RectShape());
                    shape.setTint(Color.TRANSPARENT);
                    shape.setIntrinsicHeight(padding);
                    DividerItemDecoration decoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
                    decoration.setDrawable(shape);
                    recyclerView.addItemDecoration(decoration);
                    recyclerView.setPadding(0, padding, 0, 0);
                    recyclerView.setNestedScrollingEnabled(true);
                    loadingIcon = new ProgressBar(context);
                    loadingIcon.setIndeterminate(true);
                    loadingIcon.setVisibility(View.GONE);

                    addView(input);
                    addView(loadingIcon);
                    addView(recyclerView);

                    EditText editText = input.getEditText();

                    editText.setMaxLines(1);
                    editText.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) { }
                        @Override
                        public void afterTextChanged(Editable s) {
                            if (s.toString().isEmpty()) {
                                makeSearch("");
                            }
                        }
                    });
                    editText.setOnEditorActionListener((v, actionId, event) -> {
                        if ( (actionId == EditorInfo.IME_ACTION_DONE) || ((event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN ))){
                            makeSearch(editText.getText().toString());
                        }
                        return false;
                    });
                    var shit = (NestedScrollView)getLinearLayout().getParent();
                    recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                            super.onScrollStateChanged(recyclerView,newState);
                            if (!requestMade &&newState == ViewPager.SCROLL_STATE_IDLE && !shit.canScrollVertically(1)) {
                                index += 50;
                                requestMade = true;
                                makeSearch(editText.getText().toString() , index);

                            }
                        }
                    });
                }
            });
        });
    }
}
