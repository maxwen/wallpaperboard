package com.maxwen.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bluelinelabs.logansquare.LoganSquare;
import com.futuremind.recyclerviewfastscroll.FastScroller;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.adapters.WallpapersAdapterUnified;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.items.Wallpaper;
import com.maxwen.wallpaper.board.items.WallpaperJson;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.Animator;
import com.maxwen.wallpaper.board.utils.Extras;
import com.maxwen.wallpaper.board.utils.ListUtils;
import com.maxwen.wallpaper.board.utils.LogUtil;
import com.maxwen.wallpaper.board.utils.listeners.SearchListener;
import com.rafakob.drawme.DrawMeButton;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

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

public class WallpapersFragment extends BaseFragment {

    @BindView(R.id.progress)
    ProgressBar mProgress;
    @BindView(R.id.fastscroll)
    FastScroller mFastScroller;
    //@BindView(R.id.reveal_bg)
    //View mRevealBg;

    private WallpapersAdapterUnified mAdapter;
    private AsyncTask<Void, Void, Boolean> mGetWallpapers;
    private boolean mCategoryMode;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallpapers_new, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mFastScroller.setRecyclerView(mRecyclerView);

        mProgress.getIndeterminateDrawable().setColorFilter(ColorHelper.getAttributeColor(
                getActivity(), R.attr.colorAccent), PorterDuff.Mode.SRC_IN);

        ((GridLayoutManager) mRecyclerView.getLayoutManager()).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case WallpapersAdapterUnified.TYPE_HEADER:
                        return mCurrentSpan;
                    case WallpapersAdapterUnified.TYPE_IMAGE:
                        return 1;
                }
                return 1;
            }
        });
        getWallpapers(false, false);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // force reload aspect ratio for images
        mRecyclerView.setAdapter(mAdapter);
        mFastScroller.setRecyclerView(mRecyclerView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_wallpapers, menu);
        MenuItem search = menu.findItem(R.id.menu_search);

        MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                if (fm == null) return false;

                setHasOptionsMenu(false);
                SearchListener listener = (SearchListener) getActivity();
                listener.onSearchExpanded(true);

                FragmentTransaction ft = fm.beginTransaction()
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .replace(R.id.container, new WallpaperSearchFragment(),
                                Extras.TAG_WALLPAPER_SEARCH)
                        .addToBackStack(null);
                try {
                    ft.commit();
                } catch (Exception e) {
                    ft.commitAllowingStateLoss();
                }
                return false;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                return true;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /*if (id == R.id.menu_filter) {
            FilterFragment.showFilterDialog(getActivity().getSupportFragmentManager(), false);
        }*/
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        if (mGetWallpapers != null) mGetWallpapers.cancel(true);
        super.onDestroy();
    }

    @Override
    public void onWallpaperSelected(int position) {
        if (mAdapter == null) return;
        if (position < 0 || position > mAdapter.getItemCount()) return;

        mRecyclerView.scrollToPosition(position);
    }

    @Override
    public void onCategorySelected(int position, View v, Category c) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        if (fm == null) return;

        CategoryFragment cf = new CategoryFragment();
        cf.setCategory(c);
        FragmentTransaction ft = fm.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.container, cf, Extras.TAG_CATEGORY)
                .addToBackStack(null);
        try {
            ft.commit();
        } catch (Exception e) {
            ft.commitAllowingStateLoss();
        }

        /*int[] coords = new int[2];
        v.getLocationOnScreen(coords);
        final Point viewPoint = new Point(coords[0] + v.getWidth() / 2, coords[1]);

        int width = getContext().getResources().getDisplayMetrics().widthPixels;
        int height = getContext().getResources().getDisplayMetrics().heightPixels;
        int cx = viewPoint.x;
        int cy = viewPoint.y;
        float finalRadius = (float) Math.sqrt(width * width + height * height);
        mRevealBg.setVisibility(View.VISIBLE);
        android.animation.Animator anim = ViewAnimationUtils.createCircularReveal(mRevealBg, cx, cy, 0, finalRadius);
        anim.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                CategoryFragment cf = new CategoryFragment();
                cf.setCategory(c);
                cf.setTouchPoint(viewPoint);
                FragmentTransaction ft = fm.beginTransaction()
                        .replace(R.id.container, cf, Extras.TAG_CATEGORY)
                        //.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        .addToBackStack(null);
                try {
                    ft.commit();
                } catch (Exception e) {
                    ft.commitAllowingStateLoss();
                }
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                mRevealBg.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
            }

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {
            }
        });
        anim.setDuration(1500);
        anim.setInterpolator(new LinearInterpolator());
        anim.start();*/
    }

    public void initPopupBubble(int newWallpaperCount) {
        int color = ContextCompat.getColor(getActivity(), R.color.popupBubbleText);
        DrawMeButton popupBubble = (DrawMeButton) getActivity().findViewById(R.id.popup_bubble);
        popupBubble.setCompoundDrawablesWithIntrinsicBounds(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_arrow_up, color), null, null, null);
        popupBubble.setText(String.valueOf(newWallpaperCount > 99 ? "99+" : newWallpaperCount)
                + "  " + getResources().getString(R.string.wallpaper_new_added));
        popupBubble.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animator.startAlphaAnimation(getActivity().findViewById(R.id.popup_bubble), 200, View.GONE);
                getWallpapers(true, true);
            }
        });
        Animator.startSlideDownAnimation(popupBubble, View.VISIBLE);
    }

    public void filterWallpapers() {
        if (mAdapter == null) return;
        mAdapter.filter();
    }

    public void downloadWallpaper() {
        if (mAdapter == null) return;
        mAdapter.downloadLastSelectedWallpaper();
    }

    private void getWallpapers(final boolean refreshing, final boolean showNew) {
        final String wallpaperUrl = getActivity().getResources().getString(R.string.wallpaper_json);
        mGetWallpapers = new AsyncTask<Void, Void, Boolean>() {

            List<Object> wallpapersUnified;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mProgress.setVisibility(View.VISIBLE);
                wallpapersUnified = new ArrayList<>();

                DrawMeButton popupBubble = (DrawMeButton) getActivity().findViewById(R.id.popup_bubble);
                if (popupBubble.getVisibility() == View.VISIBLE)
                    popupBubble.setVisibility(View.GONE);
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        Database database = new Database(getActivity());
                        if (!refreshing && database.getWallpapersCount() > 0) {
                            if (mCategoryMode) {
                                wallpapersUnified = database.getFilteredCategoriesUnified();
                            } else {
                                wallpapersUnified = database.getFilteredWallpapersUnified();
                            }
                            return true;
                        }

                        URL url = new URL(wallpaperUrl);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(15000);

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            InputStream stream = connection.getInputStream();
                            WallpaperJson wallpapersJson = LoganSquare.parse(stream, WallpaperJson.class);

                            if (wallpapersJson == null) return false;
                            if (refreshing) {
                                Preferences.getPreferences(getActivity()).setLastUpdate(
                                        System.currentTimeMillis());
                                if (database.getWallpapersCount() > 0) {
                                    List<Wallpaper> wallpapers = database.getWallpapers();
                                    List<Category> categories = database.getCategories();
                                    {
                                        List<Wallpaper> newWallpapers = new ArrayList<>();
                                        for (WallpaperJson wallpaper : wallpapersJson.getWallpapers) {
                                            newWallpapers.add(new Wallpaper(
                                                    0,
                                                    wallpaper.name,
                                                    wallpaper.author,
                                                    wallpaper.thumbUrl,
                                                    wallpaper.url,
                                                    wallpaper.category,
                                                    false,
                                                    0));
                                        }

                                        List<Wallpaper> intersection = (List<Wallpaper>)
                                                ListUtils.intersect(newWallpapers, wallpapers);
                                        List<Wallpaper> deleted = (List<Wallpaper>)
                                                ListUtils.difference(intersection, wallpapers);
                                        List<Wallpaper> newlyAdded = (List<Wallpaper>)
                                                ListUtils.difference(intersection, newWallpapers);

                                        database.deleteWallpapers(deleted);
                                        database.addWallpapers(newlyAdded);
                                    }

                                    {
                                        List<Category> newCategories = new ArrayList<>();
                                        for (WallpaperJson category : wallpapersJson.getCategories) {
                                            newCategories.add(new Category(
                                                    0,
                                                    category.name,
                                                    category.thumbUrl,
                                                    false));
                                        }
                                        List<Category> intersection = (List<Category>)
                                                ListUtils.intersect(newCategories, categories);
                                        List<Category> deleted = (List<Category>)
                                                ListUtils.difference(intersection, categories);
                                        List<Category> newlyAdded = (List<Category>)
                                                ListUtils.difference(intersection, newCategories);
                                        database.addCategories(newlyAdded);
                                        database.deleteCategories(deleted);
                                    }

                                    if (mCategoryMode) {
                                        wallpapersUnified = database.getFilteredCategoriesUnified();
                                    } else {
                                        wallpapersUnified = database.getFilteredWallpapersUnified();
                                    }
                                    return true;
                                }
                            }

                            if (database.getWallpapersCount() > 0)
                                database.deleteWallpapers();

                            database.addCategories(wallpapersJson);
                            database.addWallpapers(wallpapersJson);

                            if (mCategoryMode) {
                                wallpapersUnified = database.getFilteredCategoriesUnified();
                            } else {
                                wallpapersUnified = database.getFilteredWallpapersUnified();
                            }
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        LogUtil.e(Log.getStackTraceString(e));
                        return false;
                    }
                }
                return false;
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                mProgress.setVisibility(View.GONE);
                if (aBoolean) {
                    setHasOptionsMenu(true);
                    mAdapter = new WallpapersAdapterUnified(getActivity(), wallpapersUnified, false, mCategoryMode, !mCategoryMode, true);
                    mRecyclerView.setAdapter(mAdapter);
                    mFastScroller.setRecyclerView(mRecyclerView);

                    if (showNew) {
                        showNewWallpapersFragment();
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.connection_failed, Toast.LENGTH_LONG).show();
                }
                mGetWallpapers = null;
            }
        }.execute();
    }

    public void setCategoryMode(boolean value) {
        mCategoryMode = value;
    }

    private void showNewWallpapersFragment() {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        if (fm == null) return;
        FragmentTransaction ft = fm.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.container, new NewWallpaperFragment(),
                        Extras.TAG_NEW_WALLPAPERS)
                .addToBackStack(null);
        try {
            ft.commit();
        } catch (Exception e) {
            ft.commitAllowingStateLoss();
        }
    }
}
