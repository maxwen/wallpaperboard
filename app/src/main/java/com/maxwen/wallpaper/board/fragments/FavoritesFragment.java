package com.maxwen.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.adapters.WallpapersAdapter;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.items.Wallpaper;
import com.maxwen.wallpaper.board.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

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

public class FavoritesFragment extends BaseFragment {

    private AsyncTask<Void, Void, Boolean> mGetWallpapers;
    private List<Wallpaper> mWallpapers;
    private WallpapersAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wallpapers, container, false);
        ButterKnife.bind(this, view);
        mWallpapers = new ArrayList<>();
        mAdapter = new WallpapersAdapter(getActivity(), mWallpapers, true, false);
        mRecyclerView.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        onSpanCountChanged();
        getWallpapers();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
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
    protected void onSpanCountChanged() {
        mAdapter.setColumnsCount(mCurrentSpan);
    }
}
