package com.dm.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;

import com.dm.wallpaper.board.R;
import com.dm.wallpaper.board.R2;
import com.dm.wallpaper.board.adapters.WallpapersAdapter;
import com.dm.wallpaper.board.databases.Database;
import com.dm.wallpaper.board.helpers.PreferencesHelper;
import com.dm.wallpaper.board.helpers.ViewHelper;
import com.dm.wallpaper.board.items.Wallpaper;
import com.dm.wallpaper.board.utils.LogUtil;
import com.dm.wallpaper.board.utils.listeners.WallpaperListener;

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

public class FavoritesFragment extends Fragment implements WallpaperListener {

    @BindView(R2.id.recyclerview)
    RecyclerView mRecyclerView;
    @BindView(R2.id.swipe)
    SwipeRefreshLayout mSwipe;

    private AsyncTask<Void, Void, Boolean> mGetWallpapers;
    private List<Wallpaper> mWallpapers;
    private WallpapersAdapter mAdapter;
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
        mWallpapers = new ArrayList<Wallpaper>();
        mAdapter = new WallpapersAdapter(getActivity(), mWallpapers, true, false);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewHelper.resetViewBottomPadding(mRecyclerView, true);
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
                if (detector.getCurrentSpan() > 200 && detector.getTimeDelta() > 200) {
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mScaleInProgress = false;
        mDefaultSpan = getActivity().getResources().getInteger(R.integer.wallpapers_column_count);
        mMaxSpan = getActivity().getResources().getInteger(R.integer.wallpapers_max_column_count);
        PreferencesHelper p = new PreferencesHelper(getActivity());
        mCurrentSpan = Math.min(p.getColumnSpanCount(mDefaultSpan), mMaxSpan);
        ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
        ViewHelper.resetViewBottomPadding(mRecyclerView, true);
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
        if (mRecyclerView == null) return;
        if (position < 0 || position > mRecyclerView.getAdapter().getItemCount()) return;

        mRecyclerView.scrollToPosition(position);
    }

    public void filterWallpapers() {
        if (mAdapter == null) return;
        getWallpapers();
    }

    private void getWallpapers() {
        mGetWallpapers = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mWallpapers.clear();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        Database database = new Database(getActivity());
                        mWallpapers.addAll(database.getFavoriteWallpapers());
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
                mAdapter.notifyDataSetChanged();
                mGetWallpapers = null;
            }
        }.execute();
    }

    @Override
    public boolean isSelectEnabled() {
        return !mScaleInProgress;
    }
}
