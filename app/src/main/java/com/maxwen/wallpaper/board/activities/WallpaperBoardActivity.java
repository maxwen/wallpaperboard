package com.maxwen.wallpaper.board.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.drawable.DrawerArrowDrawable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.fragments.AboutFragment;
import com.maxwen.wallpaper.board.fragments.FavoritesFragment;
import com.maxwen.wallpaper.board.fragments.SettingsFragment;
import com.maxwen.wallpaper.board.fragments.WallpapersFragment;
import com.maxwen.wallpaper.board.fragments.widgets.BottomNavigationViewNew;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.InAppBillingHelper;
import com.maxwen.wallpaper.board.helpers.LicenseHelper;
import com.maxwen.wallpaper.board.helpers.PermissionHelper;
import com.maxwen.wallpaper.board.helpers.SoftKeyboardHelper;
import com.maxwen.wallpaper.board.helpers.ViewHelper;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.items.InAppBilling;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.receivers.WallpaperBoardReceiver;
import com.maxwen.wallpaper.board.services.WallpaperBoardService;
import com.maxwen.wallpaper.board.utils.Extras;
import com.maxwen.wallpaper.board.utils.ImageConfig;
import com.maxwen.wallpaper.board.utils.LogUtil;
import com.maxwen.wallpaper.board.utils.listeners.FragmentListener;
import com.maxwen.wallpaper.board.utils.listeners.InAppBillingListener;
import com.maxwen.wallpaper.board.utils.listeners.SearchListener;
import com.maxwen.wallpaper.board.utils.listeners.WallpaperBoardListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

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

public class WallpaperBoardActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        WallpaperBoardListener, InAppBillingListener, SearchListener, FragmentListener {

    @BindView(R.id.toolbar_title)
    TextView mToolbarTitle;
    @BindView(R.id.navigation_view)
    NavigationView mNavigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.appbar)
    AppBarLayout mAppBar;
    @BindView(R.id.navigation_bottom)
    BottomNavigationViewNew mBottomNavigationView;

    private static final int MENU_CATEGORY_START = Integer.MAX_VALUE - 10;

    private BillingProcessor mBillingProcessor;
    private ActionBarDrawerToggle mDrawerToggle;
    private FragmentManager mFragManager;
    private WallpaperBoardReceiver mReceiver;

    private String mFragmentTag;
    private int mPosition;

    private String mLicenseKey;
    private String[] mDonationProductsId;

    public void initMainActivity(@Nullable Bundle savedInstanceState, boolean isLicenseCheckerEnabled,
                                 @NonNull byte[] salt, @NonNull String licenseKey,
                                 @NonNull String[] donationProductsId) {
        super.setTheme(Preferences.getPreferences(this).isDarkTheme() ?
                R.style.AppThemeDark : R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_board);
        ButterKnife.bind(this);

        ViewHelper.resetNavigationBarTranslucent(this,
                getResources().getConfiguration().orientation);
        ColorHelper.setStatusBarIconColor(this);
        registerBroadcastReceiver();

        SoftKeyboardHelper softKeyboardHelper = new SoftKeyboardHelper(this,
                findViewById(R.id.container));
        softKeyboardHelper.enable();

        mFragManager = getSupportFragmentManager();
        mLicenseKey = licenseKey;
        mDonationProductsId = donationProductsId;

        Toolbar toolbar = ButterKnife.findById(this, R.id.toolbar);
        toolbar.setTitle("");

        ViewHelper.setupToolbar(toolbar, true);
        setSupportActionBar(toolbar);

        mBottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationViewNew.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.navigation_view_wallpapers) {
                    setMenuChecked(false);
                    mPosition = 0;
                    setFragment(getFragment(mPosition));
                    item.setChecked(true);
                    mToolbarTitle.setText(getToolbarTitle());
                    return true;
                }
                if (id == R.id.navigation_view_favorites) {
                    setMenuChecked(false);
                    mPosition = 2;
                    setFragment(getFragment(mPosition));
                    item.setChecked(true);
                    mToolbarTitle.setText(getToolbarTitle());
                    return true;
                }
                if (id == R.id.navigation_view_categories) {
                    setMenuChecked(false);
                    mPosition = 1;
                    setFragment(getFragment(mPosition));
                    item.setChecked(true);
                    mToolbarTitle.setText(getToolbarTitle());
                    return true;
                }
                if (id == R.id.menu_filter) {
                    final PopupMenu popup = new PopupMenu(WallpaperBoardActivity.this, ((ViewGroup) mBottomNavigationView.getChildAt(0)).getChildAt(2));
                    fileCategoryMenu(popup.getMenu());
                    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            item.setChecked(!item.isChecked());
                            new Database(WallpaperBoardActivity.this).selectCategory(item.getItemId() - MENU_CATEGORY_START, item.isChecked());
                            if (mPosition == 0) {
                                WallpapersFragment fragment = (WallpapersFragment) mFragManager.findFragmentByTag(Extras.TAG_WALLPAPERS);
                                if (fragment != null) {
                                    fragment.filterWallpapers();
                                }
                            }
                            if (mPosition == 1) {
                                WallpapersFragment fragment = (WallpapersFragment) mFragManager.findFragmentByTag(Extras.TAG_CATEGORIES);
                                if (fragment != null) {
                                    fragment.filterWallpapers();
                                }
                            }
                            if (mPosition == 2) {
                                FavoritesFragment fragment = (FavoritesFragment) mFragManager.findFragmentByTag(Extras.TAG_FAVORITES);
                                if (fragment != null) {
                                    fragment.filterWallpapers();
                                }
                            }
                            return true;
                        }
                    });
                    popup.show();
                    return false;
                }
                return false;
            }
        });

        initNavigationView(toolbar);
        initNavigationViewHeader();
        initInAppBilling();

        mPosition = 0;
        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(Extras.EXTRA_POSITION, 0);
        }
        setMenuChecked(false);
        setFragment(getFragment(mPosition));
        MenuItem item = mBottomNavigationView.getMenu().findItem(mapPositionToMenuId());
        if (item == null) {
            item = mNavigationView.getMenu().findItem(mapPositionToMenuId());
        }
        if (item != null) {
            item.setChecked(true);
        }
        mToolbarTitle.setText(getToolbarTitle());


        if (Preferences.getPreferences(this).isFirstRun() && isLicenseCheckerEnabled) {
            LicenseHelper.getLicenseChecker(this).checkLicense(mLicenseKey, salt);
            return;
        }

        checkWallpapers();

        if (isLicenseCheckerEnabled && !Preferences.getPreferences(this).isLicensed()) {
            finish();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(Extras.EXTRA_POSITION, mPosition);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (mBillingProcessor != null) {
            mBillingProcessor.release();
            mBillingProcessor = null;
        }
        if (mReceiver != null) unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        resetNavigationView(newConfig.orientation);
        ViewHelper.resetNavigationBarTranslucent(this, newConfig.orientation);
    }

    @Override
    public void onBackPressed() {
        if (mFragManager.getBackStackEntryCount() > 0) {
            clearBackStack();
            return;
        }

        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        if (!mFragmentTag.equals(Extras.TAG_WALLPAPERS)) {
            mPosition = 0;
            setFragment(getFragment(mPosition));
            MenuItem item = mBottomNavigationView.getMenu().findItem(mapPositionToMenuId());
            if (item == null) {
                item = mNavigationView.getMenu().findItem(mapPositionToMenuId());
            }
            if (item != null) {
                item.setChecked(true);
            }
            mToolbarTitle.setText(getToolbarTitle());
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (!mBillingProcessor.handleActivityResult(requestCode, resultCode, data))
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.PERMISSION_STORAGE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WallpapersFragment fragment = (WallpapersFragment) mFragManager
                        .findFragmentByTag(Extras.TAG_WALLPAPERS);
                if (fragment != null) {
                    fragment.downloadWallpaper();
                }
            } else {
                PermissionHelper.showPermissionStorageDenied(this);
            }
        }
    }

    @Override
    public void onWallpapersChecked(@Nullable Intent intent) {
        if (intent != null) {
            String packageName = intent.getStringExtra("packageName");
            //LogUtil.d("Broadcast received from service with packageName: " + packageName);

            if (packageName == null)
                return;

            if (!packageName.equals(getPackageName())) {
                LogUtil.d("Received broadcast from different packageName, expected: " + getPackageName());
                return;
            }

            int size = intent.getIntExtra(Extras.EXTRA_SIZE, 0);
            Database database = new Database(this);
            int offlineSize = database.getWallpapersCount();
            int newWallpaperCount = size - offlineSize;

            if (newWallpaperCount > 0) {
                /*int accent = ColorHelper.getAttributeColor(this, R.attr.colorAccent);
                LinearLayout container = (LinearLayout) mBottomNavigationView.getMenu().findItem(R.id.navigation_view_wallpapers).getActionView();
                if (container != null) {
                    TextView counter = (TextView) container.findViewById(R.id.counter);
                    if (counter == null) return;

                    ViewCompat.setBackground(counter, DrawableHelper.getTintedDrawable(this,
                            R.drawable.ic_toolbar_circle, accent));
                    counter.setTextColor(ColorHelper.getTitleTextColor(accent));
                    counter.setText(String.valueOf(newWallpaperCount > 99 ? "99+" : newWallpaperCount));
                    container.setVisibility(View.VISIBLE);
                }*/
                if (mFragmentTag.equals(Extras.TAG_WALLPAPERS)) {
                    WallpapersFragment fragment = (WallpapersFragment)
                            mFragManager.findFragmentByTag(Extras.TAG_WALLPAPERS);
                    if (fragment != null) fragment.initPopupBubble(newWallpaperCount);
                }
            }
        }

        //LinearLayout container = (LinearLayout) mBottomNavigationView.getMenu().getItem(0).getActionView();
        //if (container != null) container.setVisibility(View.GONE);
    }

    @Override
    public void onInAppBillingInitialized(boolean success) {
        if (!success) mBillingProcessor = null;
    }

    @Override
    public void onInAppBillingSelected(InAppBilling product) {
        if (mBillingProcessor == null) return;
        mBillingProcessor.purchase(this, product.getProductId());
    }

    @Override
    public void onInAppBillingConsume(String productId) {
        if (mBillingProcessor == null) return;
        if (mBillingProcessor.consumePurchase(productId)) {
            new MaterialDialog.Builder(this)
                    .title(R.string.navigation_view_donate)
                    .content(R.string.donation_success)
                    .positiveText(R.string.close)
                    .show();
        }
    }

    @Override
    public void onSearchExpanded(boolean expand) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        if (expand) {
            int icon = ColorHelper.getAttributeColor(this, R.attr.toolbar_icon);
            toolbar.setNavigationIcon(DrawableHelper.getTintedDrawable(
                    this, R.drawable.ic_toolbar_back, icon));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        } else {
            SoftKeyboardHelper.closeKeyboard(this);

            mDrawerToggle.setDrawerArrowDrawable(new DrawerArrowDrawable(this));
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        mDrawerLayout.setDrawerLockMode(expand ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED :
                DrawerLayout.LOCK_MODE_UNLOCKED);
        supportInvalidateOptionsMenu();
        if (expand) {
            hideBottomNavBar();
        } else {
            showBottomNavBar();
        }
    }

    @Override
    public void onCategoryFragmentShow(Category c) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        int icon = ColorHelper.getAttributeColor(this, R.attr.toolbar_icon);
        toolbar.setNavigationIcon(DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_back, icon));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //hideBottomNavBar();

        mAppBar.setExpanded(true);
        mToolbarTitle.setText(c.getName());
    }

    @Override
    public void onNewWallpapersFragmentShow() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        int icon = ColorHelper.getAttributeColor(this, R.attr.toolbar_icon);
        toolbar.setNavigationIcon(DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_back, icon));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        hideBottomNavBar();

        mAppBar.setExpanded(true);
        mToolbarTitle.setText(getResources().getString(R.string.wallpaper_new_title));
    }

    private void initNavigationView(Toolbar toolbar) {
        resetNavigationView(getResources().getConfiguration().orientation);
        mDrawerToggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.txt_open, R.string.txt_close) {

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (mPosition >= 3 && mPosition <= 5) {
                    setFragment(getFragment(mPosition));
                }
            }
        };

        mDrawerLayout.setDrawerShadow(R.drawable.navigation_view_shadow, GravityCompat.START);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        ColorStateList colorStateList = ContextCompat.getColorStateList(this,
                Preferences.getPreferences(this).isDarkTheme() ?
                        R.color.navigation_view_item_highlight_dark :
                        R.color.navigation_view_item_highlight);
        mNavigationView.getMenu().getItem(mNavigationView.getMenu().size() - 2).setVisible(
                getResources().getBoolean(R.bool.enable_donation));
        mNavigationView.setItemTextColor(colorStateList);
        mNavigationView.setItemIconTintList(colorStateList);
        Drawable background = ContextCompat.getDrawable(this,
                Preferences.getPreferences(this).isDarkTheme() ?
                        R.drawable.navigation_view_item_background_dark :
                        R.drawable.navigation_view_item_background);
        mNavigationView.setItemBackground(background);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                setMenuChecked(false);
                if (id == R.id.navigation_view_settings) mPosition = 3;
                else if (id == R.id.navigation_view_about) mPosition = 4;
                else if (id == R.id.navigation_view_donate) mPosition = 5;

                item.setChecked(true);
                mToolbarTitle.setText(getToolbarTitle());
                mDrawerLayout.closeDrawers();
                return true;
            }
        });
    }

    private void initNavigationViewHeader() {
        String imageUrl = getResources().getString(R.string.navigation_view_header);
        String titleText = getResources().getString(R.string.navigation_view_header_title);
        View header = mNavigationView.getHeaderView(0);
        ImageView image = (ImageView) header.findViewById(R.id.header_image);
        LinearLayout container = (LinearLayout) header.findViewById(R.id.header_title_container);
        TextView title = (TextView) header.findViewById(R.id.header_title);
        TextView version = (TextView) header.findViewById(R.id.header_version);

        if (titleText.length() == 0) {
            container.setVisibility(View.GONE);
        } else {
            title.setText(titleText);
            try {
                String versionText = "v" + getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
                version.setText(versionText);
            } catch (Exception ignored) {
            }
        }

        if (ColorHelper.isValidColor(imageUrl)) {
            image.setBackgroundColor(Color.parseColor(imageUrl));
            return;
        }

        if (!URLUtil.isValidUrl(imageUrl)) {
            imageUrl = "drawable://" + DrawableHelper.getResourceId(this, imageUrl);
        }

        ImageLoader.getInstance().displayImage(imageUrl, new ImageViewAware(image),
                ImageConfig.getDefaultImageOptions(), new ImageSize(720, 720), null, null);
    }

    private void initInAppBilling() {
        if (!getResources().getBoolean(R.bool.enable_donation)) return;
        if (mBillingProcessor != null) return;

        if (BillingProcessor.isIabServiceAvailable(this)) {
            mBillingProcessor = new BillingProcessor(this,
                    mLicenseKey, new InAppBillingHelper(this));
        }
    }

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(WallpaperBoardReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mReceiver = new WallpaperBoardReceiver();
        registerReceiver(mReceiver, filter);
    }

    private void checkWallpapers() {
        int wallpapersCount = new Database(this).getWallpapersCount();

        if (Preferences.getPreferences(this).isConnectedToNetwork() && (wallpapersCount > 0)) {
            Intent intent = new Intent(this, WallpaperBoardService.class);
            startService(intent);
        }
    }

    private void resetNavigationView(int orientation) {
        int index = mNavigationView.getMenu().size() - 1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mNavigationView.getMenu().getItem(index).setVisible(true);
                mNavigationView.getMenu().getItem(index).setEnabled(false);
                return;
            }
        }
        mNavigationView.getMenu().getItem(index).setVisible(false);
    }

    private void setFragment(Fragment fragment) {
        if (fragment == null) return;
        clearBackStack();

        mAppBar.setExpanded(true);

        FragmentTransaction ft = mFragManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.container, fragment, mFragmentTag);
        try {
            ft.commit();
        } catch (Exception e) {
            ft.commitAllowingStateLoss();
        }
    }

    @Nullable
    private Fragment getFragment(int position) {
        if (position == 0) {
            mFragmentTag = Extras.TAG_WALLPAPERS;
            WallpapersFragment f = new WallpapersFragment();
            f.setCategoryMode(false);
            return f;
        } else if (position == 1) {
            mFragmentTag = Extras.TAG_CATEGORIES;
            WallpapersFragment f = new WallpapersFragment();
            f.setCategoryMode(true);
            return f;
        } else if (position == 2) {
            mFragmentTag = Extras.TAG_FAVORITES;
            return new FavoritesFragment();
        } else if (position == 3) {
            mFragmentTag = Extras.TAG_SETTINGS;
            return new SettingsFragment();
        } else if (position == 4) {
            mFragmentTag = Extras.TAG_ABOUT;
            return new AboutFragment();
        }
        return null;
    }

    private void clearBackStack() {
        if (mFragManager.getBackStackEntryCount() > 0) {
            mFragManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            onSearchExpanded(false);
            showBottomNavBar();
            mToolbarTitle.setText(getToolbarTitle());
        }
    }

    private int mapPositionToMenuId() {
        switch (mPosition) {
            case 0:
                return R.id.navigation_view_wallpapers;
            case 1:
                return R.id.navigation_view_categories;
            case 2:
                return R.id.navigation_view_favorites;
            case 3:
                return R.id.navigation_view_settings;
            case 4:
                return R.id.navigation_view_about;
        }
        return R.id.navigation_view_wallpapers;
    }

    private String getToolbarTitle() {
        switch (mPosition) {
            case 0:
                return getResources().getString(R.string.navigation_view_wallpapers);
            case 1:
                return getResources().getString(R.string.navigation_view_categories);
            case 2:
                return getResources().getString(R.string.navigation_view_favorites);
            case 3:
                return getResources().getString(R.string.navigation_view_settings);
            case 4:
                return getResources().getString(R.string.navigation_view_about);
        }
        return getResources().getString(R.string.navigation_view_wallpapers);
    }

    private void setMenuChecked(boolean value) {
        MenuItem item = mBottomNavigationView.getMenu().findItem(mapPositionToMenuId());
        if (item == null) {
            item = mNavigationView.getMenu().findItem(mapPositionToMenuId());
        }
        if (item != null) {
            item.setChecked(value);
        }
    }

    public void fileCategoryMenu(Menu menu) {
        List<Category> categories = new Database(this).getCategories();
        for (Category c : categories) {
            menu.removeItem(MENU_CATEGORY_START + c.getId());
            MenuItem item = menu.add(Menu.NONE, MENU_CATEGORY_START + c.getId(), Menu.NONE, c.getName());
            item.setCheckable(true);
            item.setChecked(c.isSelected());
        }
    }

    private void hideBottomNavBar() {
        mBottomNavigationView.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                mBottomNavigationView.setVisibility(View.GONE);
                mBottomNavigationView.setAlpha(1f);
            }
        });
    }

    private void showBottomNavBar() {
        mBottomNavigationView.setAlpha(0f);
        mBottomNavigationView.setVisibility(View.VISIBLE);
        mBottomNavigationView.animate().alpha(1f).setDuration(500);
    }
}

