package com.aliucord.plugins;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

import com.aliucord.Utils;
import com.aliucord.fragments.SettingsPage;

import java.util.List;

public class ScreenShotsView extends SettingsPage {
    List<String> screenshots;
    int position;

    public ScreenShotsView(List<String> screenshots, int position) {
        this.screenshots = screenshots;
        this.position = position;
    }

    @Override
    public void onViewBound(View view) {
        super.onViewBound(view);


        var ctx = Utils.getAppContext();

        var viewPager = new ViewPager(ctx);
        viewPager.setAdapter(new EpicViewPager(screenshots, false));
        viewPager.setCurrentItem(position);

        setActionBarTitle("Screenshot " + (position + 1) + "/" + screenshots.size());

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setActionBarTitle("Screenshot " + (position + 1) + "/" + screenshots.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        addView(viewPager);


    }
}
