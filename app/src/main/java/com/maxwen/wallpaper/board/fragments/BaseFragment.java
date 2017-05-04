package com.maxwen.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.activities.WallpaperBoardActivity;
import com.maxwen.wallpaper.board.fragments.dialogs.FilterFragment;
import com.maxwen.wallpaper.board.helpers.ViewHelper;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.listeners.WallpaperListener;

import butterknife.BindView;
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

/*
 *
 * Copyright (c) 2017 Max Weninger
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

public abstract class BaseFragment extends Fragment implements WallpaperListener {

    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    protected ScaleGestureDetector mScaleGestureDetector;
    protected int mCurrentSpan;
    protected int mDefaultSpan;
    protected int mMaxSpan;
    protected int mMinSpan;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewHelper.resetViewBottomPadding(mRecyclerView, true);
        setHasOptionsMenu(true);

        mDefaultSpan = getActivity().getResources().getInteger(R.integer.wallpapers_column_count);
        mMaxSpan = getActivity().getResources().getInteger(R.integer.wallpapers_max_column_count);
        mMinSpan = getActivity().getResources().getInteger(R.integer.wallpapers_min_column_count);
        mCurrentSpan = Math.min(Preferences.getPreferences(getActivity()).getColumnSpanCount(mDefaultSpan), mMaxSpan);

        //mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setItemAnimator(new SlideInLeftAnimator());
        mRecyclerView.setLayoutManager(new WallpaperGridLayoutManager(getActivity(), mCurrentSpan));
        mRecyclerView.setHasFixedSize(false);

        //set scale gesture detector
        mScaleGestureDetector = new ScaleGestureDetector(getActivity(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                final float sf = detector.getScaleFactor();
                if (detector.getTimeDelta() > 200 && Math.abs(detector.getCurrentSpan() - detector.getPreviousSpan()) > 100) {
                    if (detector.getCurrentSpan() - detector.getPreviousSpan() < -1) {
                        int span = Math.min(mCurrentSpan + 1, mMaxSpan);
                        if (span != mCurrentSpan) {
                            mCurrentSpan = span;
                            ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                            Preferences.getPreferences(getActivity()).setColumnSpanCount(mCurrentSpan);
                        }
                        return true;
                    } else if (detector.getCurrentSpan() - detector.getPreviousSpan() > 1) {
                        int span = Math.max(mCurrentSpan - 1, mMinSpan);
                        if (span != mCurrentSpan) {
                            mCurrentSpan = span;
                            ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                            Preferences.getPreferences(getActivity()).setColumnSpanCount(mCurrentSpan);
                        }
                        ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mRecyclerView.setNestedScrollingEnabled(true);
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                mRecyclerView.setNestedScrollingEnabled(false);
                return true;
            }
        });
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mScaleGestureDetector.onTouchEvent(event);
                return mScaleGestureDetector.isInProgress();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDefaultSpan = getActivity().getResources().getInteger(R.integer.wallpapers_column_count);
        mMaxSpan = getActivity().getResources().getInteger(R.integer.wallpapers_max_column_count);
        mMinSpan = getActivity().getResources().getInteger(R.integer.wallpapers_min_column_count);
        mCurrentSpan = Math.min(Preferences.getPreferences(getActivity()).getColumnSpanCount(mDefaultSpan), mMaxSpan);
        ViewHelper.setSpanCountToColumns(getActivity(), mRecyclerView, mCurrentSpan);
        ViewHelper.resetViewBottomPadding(mRecyclerView, true);
        mRecyclerView.setNestedScrollingEnabled(true);
    }

    @Override
    public void onWallpaperSelected(int position) {
    }

    @Override
    public void onCategorySelected(int position, View v, Category c) {
    }

    @Override
    public boolean isSelectEnabled() {
        return !mScaleGestureDetector.isInProgress();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_wallpapers_base, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.navigation_view_settings) {
            ((WallpaperBoardActivity) getActivity()).showSettings();
            return true;
        }

        if (id == R.id.menu_filter) {
            FilterFragment.showFilterDialog(getActivity().getSupportFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
