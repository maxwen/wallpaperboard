package com.maxwen.wallpaper.board.fragments.dialogs;

import android.app.Dialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.WallpaperHelper;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.Extras;

import butterknife.BindView;
import butterknife.ButterKnife;

/*
 * Wallpaper Board
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

public class WallpaperApplyFragment extends DialogFragment implements View.OnClickListener {

    @BindView(R.id.apply_all)
    LinearLayout mApplyAll;
    @BindView(R.id.apply_home)
    LinearLayout mApplyHome;
    @BindView(R.id.apply_lock)
    LinearLayout mApplyLock;
    @BindView(R.id.apply_all_icon)
    ImageView mApplyAllIcon;
    @BindView(R.id.apply_home_icon)
    ImageView mApplyHomeIcon;
    @BindView(R.id.apply_lock_icon)
    ImageView mApplyLockIcon;

    private static final String TAG = "com.maxwen.wallpaper.board.dialog.wallpaper.apply";
    private String mName;
    private String mUrl;
    private int mColor;
    private RectF mRect;

    @NonNull
    private static WallpaperApplyFragment newInstance(final RectF rectF, final String url, final String name) {
        WallpaperApplyFragment fragment = new WallpaperApplyFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Extras.EXTRA_URL, url);
        bundle.putString(Extras.EXTRA_NAME, name);
        bundle.putParcelable(Extras.EXTRA_RECT, rectF);

        fragment.setArguments(bundle);
        return fragment;
    }

    public static void showWallpaperApply(FragmentManager fm, final RectF rectF,
                                          final String url, final String name) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        try {
            DialogFragment dialog = WallpaperApplyFragment.newInstance(rectF, url, name);
            dialog.show(ft, TAG);
        } catch (IllegalStateException | IllegalArgumentException ignored) {}
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mName = getArguments().getString(Extras.EXTRA_NAME);
        mUrl = getArguments().getString(Extras.EXTRA_URL);
        mRect = getArguments().getParcelable(Extras.EXTRA_RECT);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.customView(R.layout.fragment_wallpaper_apply, false);
        builder.negativeText(android.R.string.cancel);
        MaterialDialog dialog = builder.build();
        dialog.show();

        ButterKnife.bind(this, dialog);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(Extras.EXTRA_URL);
            mName = savedInstanceState.getString(Extras.EXTRA_NAME);
            mRect = savedInstanceState.getParcelable(Extras.EXTRA_RECT);
        }
        int color = ColorHelper.getAttributeColor(getActivity(), android.R.attr.textColorPrimary);
        mColor = ColorHelper.getAttributeColor(getActivity(), R.attr.colorAccent);

        mApplyAllIcon.setImageDrawable(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_apply, color));
        mApplyHomeIcon.setImageDrawable(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_home, color));
        mApplyLockIcon.setImageDrawable(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_lockscreen, color));
        mApplyAll.setOnClickListener(this);
        mApplyHome.setOnClickListener(this);
        mApplyLock.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_URL, mUrl);
        outState.putString(Extras.EXTRA_NAME, mName);
        outState.putParcelable(Extras.EXTRA_RECT, mRect);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (Preferences.getPreferences(getActivity()).isScrollWallpaper()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        super.onDismiss(dialog);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        int applyFlag = -1;
        if (id == R.id.apply_all) {
            applyFlag = WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM;
        } else if (id == R.id.apply_home) {
            applyFlag = WallpaperManager.FLAG_SYSTEM;
        } else if (id == R.id.apply_lock) {
            applyFlag = WallpaperManager.FLAG_LOCK;
        }

        if (applyFlag != -1) {
            WallpaperHelper.doApplyWallpaper(getContext(), mRect, mColor, mUrl, mName, applyFlag);
        }
        dismiss();
    }
}
