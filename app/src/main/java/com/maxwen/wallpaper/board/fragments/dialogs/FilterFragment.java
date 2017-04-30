package com.maxwen.wallpaper.board.fragments.dialogs;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.maxwen.wallpaper.R;

import com.maxwen.wallpaper.board.adapters.FilterAdapter;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.fragments.WallpapersFragment;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.utils.Extras;

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

public class FilterFragment extends DialogFragment {

    @BindView(R.id.listview)
    ListView listView;

    private static final String TAG = "com.maxwen.wallpaper.board.dialog.filter";

    private static FilterFragment newInstance() {
        FilterFragment fragment = new FilterFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void showFilterDialog(FragmentManager fm) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        try {
            DialogFragment dialog = FilterFragment.newInstance();
            dialog.show(ft, TAG);
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.title(R.string.wallpaper_filter);
        builder.customView(R.layout.fragment_filter, false);
        MaterialDialog dialog = builder.build();
        dialog.show();

        ButterKnife.bind(this, dialog);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        List<Category> categories = new Database(getActivity()).getCategories();
        listView.setAdapter(new FilterAdapter(getActivity(), categories));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (getActivity() == null) return;

        FragmentManager fm = getActivity().getSupportFragmentManager();
        if (fm == null) return;

        WallpapersFragment fragment = (WallpapersFragment) fm.findFragmentByTag(Extras.TAG_WALLPAPERS);
        if (fragment != null) {
            fragment.filterWallpapers();
        }
        super.onDismiss(dialog);
    }
}
