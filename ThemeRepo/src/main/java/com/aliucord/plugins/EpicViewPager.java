package com.aliucord.plugins;

import static com.aliucord.plugins.ThemeRepoAPI.GITHIB_THEMEREPO_URL;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

import com.aliucord.Utils;
import com.discord.utilities.images.MGImages;
import com.facebook.drawee.drawable.ScalingUtils$ScaleType;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

public class EpicViewPager extends PagerAdapter {

    List<String> screenshots;
    boolean addOnClickListener = true;

    public EpicViewPager(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public EpicViewPager(List<String> screenshots, boolean addOnClickListener) {
        this.screenshots = screenshots;
        this.addOnClickListener = addOnClickListener;
    }

    @Override
    public int getCount() {
        return screenshots.size();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == (object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        var imageView = new SimpleDraweeView(Utils.getAppContext());
        MGImages.setScaleType(imageView, ScalingUtils$ScaleType.c);
        MGImages.setImage(imageView, GITHIB_THEMEREPO_URL + screenshots.get(position));
        imageView.setAdjustViewBounds(true);
        if (addOnClickListener) imageView.setOnClickListener(v -> {
            Utils.openPageWithProxy(Utils.getAppActivity(), new ScreenShotsView(screenshots, position));
        });
        container.addView(imageView);
        return imageView;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

}
