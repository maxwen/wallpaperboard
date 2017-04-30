package com.maxwen.wallpaper.board.fragments.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Typeface;
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
import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarCallback;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.FileHelper;
import com.maxwen.wallpaper.board.helpers.PermissionHelper;
import com.maxwen.wallpaper.board.helpers.WallpaperHelper;
import com.maxwen.wallpaper.board.utils.Extras;
import com.maxwen.wallpaper.R;


import java.io.File;

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

public class WallpaperOptionsFragment extends DialogFragment implements View.OnClickListener {

    @BindView(R.id.apply)
    LinearLayout mApply;
    @BindView(R.id.save)
    LinearLayout mSave;
    @BindView(R.id.apply_icon)
    ImageView mApplyIcon;
    @BindView(R.id.save_icon)
    ImageView mSaveIcon;

    private String mName;
    private String mUrl;

    private static final String TAG = "com.maxwen.wallpaper.board.dialog.wallpaper.options";

    private static WallpaperOptionsFragment newInstance(String url, String name) {
        WallpaperOptionsFragment fragment = new WallpaperOptionsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Extras.EXTRA_URL, url);
        bundle.putString(Extras.EXTRA_NAME, name);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void showWallpaperOptionsDialog(FragmentManager fm, String url, String name) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        try {
            DialogFragment dialog = WallpaperOptionsFragment.newInstance(url, name);
            dialog.show(ft, TAG);
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity());
        builder.customView(R.layout.fragment_wallpaper_options, false);
        MaterialDialog dialog = builder.build();
        dialog.show();

        ButterKnife.bind(this, dialog);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mName = getArguments().getString(Extras.EXTRA_NAME);
        mUrl = getArguments().getString(Extras.EXTRA_URL);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mUrl = savedInstanceState.getString(Extras.EXTRA_URL);
            mName = savedInstanceState.getString(Extras.EXTRA_NAME);
        }

        int color = ColorHelper.getAttributeColor(getActivity(), android.R.attr.textColorPrimary);
        mApplyIcon.setImageDrawable(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_apply, color));
        mSaveIcon.setImageDrawable(DrawableHelper.getTintedDrawable(
                getActivity(), R.drawable.ic_toolbar_save, color));

        mApply.setOnClickListener(this);

        if (getActivity().getResources().getBoolean(R.bool.enable_wallpaper_download)) {
            mSave.setOnClickListener(this);
            return;
        }
        mSave.setVisibility(View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_URL, mUrl);
        outState.putString(Extras.EXTRA_NAME, mName);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        final int color = ColorHelper.getAttributeColor(getActivity(), R.attr.colorAccent);
        if (id == R.id.apply) {
            WallpaperHelper.applyWallpaper(getActivity(), null, color, mUrl, mName);
        } else if (id == R.id.save) {
            if (PermissionHelper.isPermissionStorageGranted(getActivity())) {
                File target = new File(WallpaperHelper.getDefaultWallpapersDirectory(getActivity()).toString()
                        + File.separator + mName + FileHelper.IMAGE_EXTENSION);

                if (target.exists()) {
                    final Context context = getActivity();
                    CafeBar.builder(getActivity())
                            .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(getActivity(), R.attr.card_background)))
                            .autoDismiss(false)
                            .fitSystemWindow(R.bool.view_fitsystemwindow)
                            .maxLines(4)
                            .content(String.format(getResources().getString(R.string.wallpaper_download_exist),
                                    ("\"" +mName + FileHelper.IMAGE_EXTENSION+ "\"")))
                            .icon(R.drawable.ic_toolbar_download)
                            .positiveText(R.string.wallpaper_download_exist_replace)
                            .positiveColor(color)
                            .positiveTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Font-Bold.ttf"))
                            .onPositive(new CafeBarCallback() {
                                @Override
                                public void OnClick(@NonNull CafeBar cafeBar) {
                                    if (context == null) {
                                        cafeBar.dismiss();
                                        return;
                                    }

                                    WallpaperHelper.downloadWallpaper(context, color, mUrl, mName);
                                    cafeBar.dismiss();
                                }
                            })
                            .negativeText(R.string.wallpaper_download_exist_new)
                            .negativeTypeface(Typeface.createFromAsset(getActivity().getAssets(), "fonts/Font-Bold.ttf"))
                            .onNegative(new CafeBarCallback() {
                                @Override
                                public void OnClick(@NonNull CafeBar cafeBar) {
                                    if (context == null) {
                                        cafeBar.dismiss();
                                        return;
                                    }

                                    WallpaperHelper.downloadWallpaper(context, color, mUrl, mName +"_"+ System.currentTimeMillis());
                                    cafeBar.dismiss();
                                }
                            })
                            .build().show();
                    dismiss();
                    return;
                }

                WallpaperHelper.downloadWallpaper(getActivity(), color, mUrl, mName);
                dismiss();
                return;
            }
            PermissionHelper.requestStoragePermission(getActivity());
        }
        dismiss();
    }
}
