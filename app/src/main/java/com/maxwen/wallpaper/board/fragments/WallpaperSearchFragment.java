package com.maxwen.wallpaper.board.fragments;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
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
import com.maxwen.wallpaper.board.helpers.SoftKeyboardHelper;
import com.maxwen.wallpaper.board.helpers.ViewHelper;
import com.maxwen.wallpaper.board.items.Wallpaper;
import com.maxwen.wallpaper.board.utils.LogUtil;

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

public class WallpaperSearchFragment extends BaseFragment {

    @BindView(R.id.search_result)
    TextView mSearchResult;

    private SearchView mSearchView;
    private WallpapersAdapter mAdapter;
    private AsyncTask<Void, Void, Boolean> mGetWallpapers;

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
        getWallpapers();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
                        // TODO should search use filter?
                        //wallpapers = database.getFilteredWallpapers();
                        wallpapers = database.getWallpapers();
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
                    onSpanCountChanged();
                    if (mSearchView != null) mSearchView.requestFocus();
                    SoftKeyboardHelper.openKeyboard(getActivity());
                }
            }
        }.execute();
    }

    @Override
    protected void onSpanCountChanged() {
        mAdapter.setColumnsCount(mCurrentSpan);
    }
}
