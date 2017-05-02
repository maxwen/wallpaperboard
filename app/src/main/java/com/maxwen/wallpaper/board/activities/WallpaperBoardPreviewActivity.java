package com.maxwen.wallpaper.board.activities;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.transition.Transition;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarCallback;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.kogitune.activitytransition.ActivityTransition;
import com.kogitune.activitytransition.ExitActivityTransition;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.FileHelper;
import com.maxwen.wallpaper.board.helpers.PermissionHelper;
import com.maxwen.wallpaper.board.helpers.ViewHelper;
import com.maxwen.wallpaper.board.helpers.WallpaperHelper;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.Animator;
import com.maxwen.wallpaper.board.utils.Extras;
import com.maxwen.wallpaper.board.utils.ImageConfig;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.senab.photoview.PhotoViewAttacher;

/*
 * Wallpaper Board
 *
 * Copyright (c) 2017 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public class WallpaperBoardPreviewActivity extends AppCompatActivity implements View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    @BindView(R.id.wallpaper)
    ImageView mWallpaper;
    @BindView(R.id.fab)
    FloatingActionButton mFab;
    @BindView(R.id.progress)
    ProgressBar mProgress;

    private Runnable mRunnable;
    private Handler mHandler;
    private PhotoViewAttacher mAttacher;
    private ExitActivityTransition mExitTransition;

    private String mUrl;
    private String mName;
    private String mAuthor;
    private int mColor;
    private boolean mIsEnter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.setTheme(Preferences.getPreferences(this).isDarkTheme() ?
                R.style.WallpaperThemeDark : R.style.WallpaperTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_preview);
        ButterKnife.bind(this);
        ViewHelper.setApplicationWindowColor(this);
        ViewHelper.resetViewBottomMargin(mFab);
        ColorHelper.setTransparentStatusBar(this,
                ContextCompat.getColor(this, R.color.wallpaperStatusBar));
        mIsEnter = true;

        final Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        TextView toolbarTitle = ButterKnife.findById(this, R.id.toolbar_title);
        TextView toolbarSubTitle = ButterKnife.findById(this, R.id.toolbar_subtitle);

        mColor = ColorHelper.getAttributeColor(this, R.attr.colorAccent);
        mProgress.getIndeterminateDrawable().setColorFilter(mColor, PorterDuff.Mode.SRC_IN);

        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(Extras.EXTRA_URL);
            mName = savedInstanceState.getString(Extras.EXTRA_NAME);
            mAuthor = savedInstanceState.getString(Extras.EXTRA_AUTHOR);
        }

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mUrl = bundle.getString(Extras.EXTRA_URL);
            mName = bundle.getString(Extras.EXTRA_NAME);
            mAuthor = bundle.getString(Extras.EXTRA_AUTHOR);
        }

        toolbarTitle.setText(mName);
        toolbarSubTitle.setText(mAuthor);
        toolbar.setTitle("");
        toolbar.setNavigationIcon(R.drawable.ic_toolbar_back);
        setSupportActionBar(toolbar);

        mFab.setOnClickListener(this);

        mExitTransition = ActivityTransition.with(getIntent())
                .to(this, mWallpaper, Extras.EXTRA_IMAGE)
                .duration(300)
                .start(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && savedInstanceState == null) {
            Transition transition = getWindow().getSharedElementEnterTransition();

            if (transition != null) {
                transition.addListener(new Transition.TransitionListener() {
                    @Override
                    public void onTransitionStart(Transition transition) {

                    }

                    @Override
                    public void onTransitionEnd(Transition transition) {
                        if (mIsEnter) {
                            mIsEnter = false;
                            Animator.startSlideDownAnimation(toolbar, View.VISIBLE);
                            loadWallpaper(mUrl);
                        }
                    }

                    @Override
                    public void onTransitionCancel(Transition transition) {

                    }

                    @Override
                    public void onTransitionPause(Transition transition) {

                    }

                    @Override
                    public void onTransitionResume(Transition transition) {

                    }
                });
                return;
            }
        }

        mRunnable = new Runnable() {
            @Override
            public void run() {
                toolbar.setVisibility(View.VISIBLE);
                loadWallpaper(mUrl);
                mRunnable = null;
                mHandler = null;
            }
        };
        mHandler = new Handler();
        mHandler.postDelayed(mRunnable, 700);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewHelper.resetViewBottomMargin(mFab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wallpaper_preview, menu);
        MenuItem save = menu.findItem(R.id.menu_save);
        save.setVisible(getResources().getBoolean(R.bool.enable_wallpaper_download));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_NAME, mName);
        outState.putString(Extras.EXTRA_AUTHOR, mAuthor);
        outState.putString(Extras.EXTRA_URL, mUrl);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mAttacher != null) mAttacher.cleanup();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mHandler != null && mRunnable != null)
            mHandler.removeCallbacks(mRunnable);
        if (mExitTransition != null) mExitTransition.exit(this);
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_save) {
            if (PermissionHelper.isPermissionStorageGranted(this)) {
                File target = new File(WallpaperHelper.getWallpapersDirectory(this).toString()
                        + File.separator + mName + FileHelper.IMAGE_EXTENSION);

                if (target.exists()) {
                    CafeBar.builder(this)
                            .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(this, R.attr.card_background)))
                            .autoDismiss(false)
                            .maxLines(4)
                            .fitSystemWindow(true)
                            .content(String.format(getResources().getString(R.string.wallpaper_download_exist),
                                    ("\"" +mName + FileHelper.IMAGE_EXTENSION+ "\"")))
                            .icon(R.drawable.ic_toolbar_download)
                            .positiveText(R.string.wallpaper_download_exist_replace)
                            .positiveColor(mColor)
                            .onPositive(new CafeBarCallback() {
                                @Override
                                public void OnClick(@NonNull CafeBar cafeBar) {
                                    WallpaperHelper.downloadWallpaper(WallpaperBoardPreviewActivity.this, mColor, mUrl, mName);
                                    cafeBar.dismiss();
                                }
                            })
                            .negativeText(R.string.wallpaper_download_exist_new)
                            .onNegative(new CafeBarCallback() {
                                @Override
                                public void OnClick(@NonNull CafeBar cafeBar) {
                                    WallpaperHelper.downloadWallpaper(WallpaperBoardPreviewActivity.this, mColor, mUrl, mName +"_"+ System.currentTimeMillis());
                                    cafeBar.dismiss();
                                }
                            })
                            .build().show();
                    return true;
                }

                WallpaperHelper.downloadWallpaper(this, mColor, mUrl, mName);
                return true;
            }

            PermissionHelper.requestStoragePermission(this);
            return true;
        } /*else if (id == R.id.menu_wallpaper_settings) {
            WallpaperSettingsFragment.showWallpaperSettings(getSupportFragmentManager());
            return true;
        }*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.fab) {
            WallpaperHelper.applyWallpaper(this, mAttacher.getDisplayRect(), mColor, mUrl, mName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WallpaperHelper.downloadWallpaper(this, mColor, mUrl, mName);
            } else {
                PermissionHelper.showPermissionStorageDenied(this);
            }
        }
    }

    private void loadWallpaper(String url) {
        DisplayImageOptions.Builder options = ImageConfig.getRawDefaultImageOptions();
        options.cacheInMemory(false);
        options.cacheOnDisk(true);

        ImageLoader.getInstance().handleSlowNetwork(true);
        ImageLoader.getInstance().displayImage(url, mWallpaper, options.build(), new SimpleImageLoadingListener() {

            @Override
            public void onLoadingStarted(String imageUri, View view) {
                super.onLoadingStarted(imageUri, view);
                mProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                super.onLoadingFailed(imageUri, view, failReason);
                int text = ColorHelper.getTitleTextColor(mColor);
                onWallpaperLoaded(text);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                if (!Preferences.getPreferences(WallpaperBoardPreviewActivity.this).isScrollWallpaper()) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }

                if (loadedImage != null) {
                    Palette.from(loadedImage).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            int accent = ColorHelper.getAttributeColor(
                                    WallpaperBoardPreviewActivity.this, R.attr.colorAccent);
                            int color = palette.getVibrantColor(accent);
                            mColor = color;
                            int text = ColorHelper.getTitleTextColor(color);
                            mFab.setBackgroundTintList(ColorHelper.getColorStateList(color));
                            onWallpaperLoaded(text);
                        }
                    });
                }
            }
        });
    }

    private void onWallpaperLoaded(@ColorInt int textColor) {
        mAttacher = new PhotoViewAttacher(mWallpaper);
        mAttacher.setScaleType(ImageView.ScaleType.CENTER_CROP);
        mProgress.setVisibility(View.GONE);
        mRunnable = null;
        mHandler = null;

        mFab.setImageDrawable(DrawableHelper.getTintedDrawable(this,
                R.drawable.ic_fab_apply, textColor));
        Animator.showFab(mFab);
    }
}
