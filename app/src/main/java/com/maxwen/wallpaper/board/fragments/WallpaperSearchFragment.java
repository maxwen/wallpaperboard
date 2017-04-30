package com.maxwen.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;
import android.widget.TextView;

import com.maxwen.wallpaper.R;

import com.maxwen.wallpaper.board.adapters.WallpapersAdapter;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.PreferencesHelper;
import com.maxwen.wallpaper.board.helpers.SoftKeyboardHelper;
import com.maxwen.wallpaper.board.helpers.ViewHelper;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.items.Wallpaper;
import com.maxwen.wallpaper.board.utils.LogUtil;
import com.maxwen.wallpaper.board.utils.listeners.WallpaperListener;

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

public class WallpaperSearchFragment extends Fragment implements WallpaperListener {

    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipe)
    SwipeRefreshLayout mSwipe;
    @BindView(R.id.search_result)
    TextView mSearchResult;

    private SearchView mSearchView;
    private WallpapersAdapter mAdapter;
    private AsyncTask<Void, Void, Boolean> mGetWallpapers;
    private ScaleGestureDetector mScaleGestureDetector;
    private int mCurrentSpan;
    private int mDefaultSpan;
    private boolean mScaleInProgress;
    private int mMaxSpan;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallpapers, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        //ViewCompat.setNestedScrollingEnabled(mRecyclerView, false);
        ViewHelper.resetViewBottomPadding(mRecyclerView, false);
        mSwipe.setEnabled(false);
        mDefaultSpan = getActivity().getResources().getInteger(R.integer.wallpapers_column_count);
        mMaxSpan = getActivity().getResources().getInteger(R.integer.wallpapers_max_column_count);
        PreferencesHelper p = new PreferencesHelper(getActivity());
        mCurrentSpan = Math.min(p.getColumnSpanCount(mDefaultSpan), mMaxSpan);

        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), mCurrentSpan));
        mRecyclerView.setHasFixedSize(false);

        //set scale gesture detector
        mScaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (detector.getTimeDelta() > 200 && Math.abs(detector.getCurrentSpan() - detector.getPreviousSpan()) > 100) {
                    if (detector.getCurrentSpan() - detector.getPreviousSpan() < -1) {
                        int span = Math.min(mCurrentSpan + 1, mDefaultSpan + 1);
                        if (span != mCurrentSpan) {
                            mCurrentSpan = span;
                            ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                            PreferencesHelper p = new PreferencesHelper(getActivity());
                            p.setColumnSpanCount(mCurrentSpan);
                        }
                        return true;
                    } else if (detector.getCurrentSpan() - detector.getPreviousSpan() > 1) {
                        int span = Math.max(mCurrentSpan - 1, mDefaultSpan - 1);
                        if (span != mCurrentSpan) {
                            mCurrentSpan = span;
                            ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                            PreferencesHelper p = new PreferencesHelper(getActivity());
                            p.setColumnSpanCount(mCurrentSpan);
                        }
                        ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                        return true;
                    }
                }
                return false;
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mScaleInProgress = false;
                mRecyclerView.setNestedScrollingEnabled(true);
            }
            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mScaleInProgress = true;
                mRecyclerView.setNestedScrollingEnabled(false);
                return true;
            }
        });
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleGestureDetector.onTouchEvent(event);
                return false;
            }
        });
        getWallpapers();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_wallpaper_search, menu);
        MenuItem search = menu.findItem(R.id.menu_search);
        int color = ColorHelper.getAttributeColor(getActivity(), R.attr.toolbar_icon);
        search.setIcon(DrawableHelper.getTintedDrawable(getActivity(),
                R.drawable.ic_toolbar_search, color));

        mSearchView = (SearchView) MenuItemCompat.getActionView(search);
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_ACTION_SEARCH);
        mSearchView.setQueryHint(getActivity().getResources().getString(R.string.menu_search));
        mSearchView.setMaxWidth(Integer.MAX_VALUE);

        MenuItemCompat.expandActionView(search);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.clearFocus();

        ViewHelper.changeSearchViewTextColor(mSearchView, color,
                ColorHelper.setColorAlpha(color, 0.6f));
        View view = mSearchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        if (view != null) view.setBackgroundColor(Color.TRANSPARENT);

        ImageView closeIcon = (ImageView) mSearchView.findViewById(
                android.support.v7.appcompat.R.id.search_close_btn);
        if (closeIcon != null) closeIcon.setImageResource(R.drawable.ic_toolbar_close);

        ImageView searchIcon = (ImageView) mSearchView.findViewById(
                android.support.v7.appcompat.R.id.search_mag_icon);
        ViewHelper.removeSearchViewSearchIcon(searchIcon);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String string) {
                filterSearch(string);
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String string) {
                mSearchView.clearFocus();
                return true;
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScaleInProgress = false;
        mDefaultSpan = getActivity().getResources().getInteger(R.integer.wallpapers_column_count);
        mMaxSpan = getActivity().getResources().getInteger(R.integer.wallpapers_max_column_count);
        PreferencesHelper p = new PreferencesHelper(getActivity());
        mCurrentSpan = Math.min(p.getColumnSpanCount(mDefaultSpan), mMaxSpan);
        ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
        ViewHelper.resetViewBottomPadding(mRecyclerView, false);
        // force reload aspect ratio for images
        mRecyclerView.setAdapter(mAdapter);
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
    }

    private void filterSearch(String query) {
        try {
            mAdapter.search(query);
            if (mAdapter.getItemCount()==0) {
                String text = String.format(getActivity().getResources().getString(
                        R.string.search_result_empty), query);
                mSearchResult.setText(text);
                mSearchResult.setVisibility(View.VISIBLE);
            }
            else mSearchResult.setVisibility(View.GONE);
        } catch (Exception e) {
            LogUtil.e(Log.getStackTraceString(e));
        }
    }

    private void getWallpapers() {
        mGetWallpapers = new AsyncTask<Void, Void, Boolean>() {

            List<Wallpaper> wallpapers;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                wallpapers = new ArrayList<>();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        Database database = new Database(getActivity());
                        wallpapers = database.getFilteredWallpapers();
                        return true;
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
                if (aBoolean) {
                    mAdapter = new WallpapersAdapter(getActivity(), wallpapers, false, true);
                    mRecyclerView.setAdapter(mAdapter);
                    if (mSearchView != null) mSearchView.requestFocus();
                    SoftKeyboardHelper.openKeyboard(getActivity());
                }
            }
        }.execute();
    }

    @Override
    public boolean isSelectEnabled() {
        return !mScaleInProgress;
    }
}
