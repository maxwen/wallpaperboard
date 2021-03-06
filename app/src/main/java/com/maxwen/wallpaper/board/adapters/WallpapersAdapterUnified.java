package com.maxwen.wallpaper.board.adapters;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.kogitune.activitytransition.ActivityTransitionLauncher;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.activities.WallpaperBoardActivity;
import com.maxwen.wallpaper.board.activities.WallpaperBoardPreviewActivity;
import com.maxwen.wallpaper.board.databases.Database;
import com.maxwen.wallpaper.board.fragments.FavoritesFragment;
import com.maxwen.wallpaper.board.fragments.WallpaperSearchFragment;
import com.maxwen.wallpaper.board.fragments.WallpapersFragment;
import com.maxwen.wallpaper.board.fragments.dialogs.WallpaperOptionsFragment;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.helpers.DrawableHelper;
import com.maxwen.wallpaper.board.helpers.WallpaperHelper;
import com.maxwen.wallpaper.board.items.Category;
import com.maxwen.wallpaper.board.items.Wallpaper;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.Extras;
import com.maxwen.wallpaper.board.utils.ImageConfig;
import com.maxwen.wallpaper.board.utils.listeners.WallpaperListener;
import com.maxwen.wallpaper.board.utils.views.HeaderView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

public class WallpapersAdapterUnified extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements SectionTitleProvider {

    private final Context mContext;
    private final DisplayImageOptions.Builder mOptions;
    private List<Object> mWallpapersAll;
    private List<Object> mWallpapers;
    private Set<Wallpaper> mNewWallpapers;
    private Set<String> mNewCategories;
    private Set<String> mCollapsedCategories;

    private int mLastSelectedPosition = -1;
    private final boolean mIsAutoGeneratedColor;
    private final boolean mIsFavoriteMode;
    private int mDefaultQuality;
    private boolean mCategoryMode;
    private String mCountString;
    private boolean mIsCollapseMode;
    private boolean mIsCategorySelectable;
    private int mColumnsCount;

    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_HEADER = 1;

    private static final float ROTATE_0_DEGREE = 0f;
    private static final float ROTATE_180_DEGREE = 180f;

    public WallpapersAdapterUnified(@NonNull Context context, @NonNull List<Object> wallpapers,
                                    boolean isFavoriteMode, boolean isCategoryMode, boolean isCollapseMode,
                                    boolean isCategorySelectable) {
        mContext = context;
        mIsFavoriteMode = isFavoriteMode;
        mCategoryMode = isCategoryMode;
        mIsCollapseMode = isCollapseMode;
        mIsCategorySelectable = isCategorySelectable;

        mWallpapersAll = wallpapers;
        if (mIsCollapseMode) {
            mWallpapers = new ArrayList<>();
            mCollapsedCategories = Preferences.getPreferences(context).getCollapsedCategories();
            filterCollapsedCategories();
        } else {
            mWallpapers = mWallpapersAll;
        }
        Database database = new Database(mContext);
        mNewCategories = new HashSet<>();
        mNewWallpapers = database.getWallpapersNewer(Preferences.getPreferences(mContext).getLastUpdate(), mNewCategories);

        mIsAutoGeneratedColor = mContext.getResources().getBoolean(
                R.bool.card_wallpaper_auto_generated_color);
        mCountString = mContext.getResources().getString(R.string.category_count);
        mDefaultQuality = context.getResources().getInteger(R.integer.wallpaper_grid_preview_quality);
        int color = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorSecondary);
        Drawable loading = DrawableHelper.getDefaultImage(
                mContext, R.drawable.ic_default_image_loading, color,
                mContext.getResources().getDimensionPixelSize(R.dimen.default_image_padding));
        Drawable failed = DrawableHelper.getDefaultImage(
                mContext, R.drawable.ic_default_image_failed, color,
                mContext.getResources().getDimensionPixelSize(R.dimen.default_image_padding));
        mOptions = ImageConfig.getRawDefaultImageOptions();
        mOptions.resetViewBeforeLoading(true);
        mOptions.cacheInMemory(true);
        mOptions.cacheOnDisk(true);
        mOptions.showImageForEmptyUri(failed);
        mOptions.showImageOnFail(failed);
        mOptions.showImageOnLoading(loading);
        mOptions.displayer(new FadeInBitmapDisplayer(700));
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.fragment_wallpapers_item_grid_new, parent, false);
            return new ImageHolder(view);
        }
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(mContext).inflate(
                    R.layout.fragment_wallpapers_category, parent, false);
            return new HeaderHolder(view);
        }
        return null;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = mWallpapers.get(position);
        if (o instanceof Category) {
            return TYPE_HEADER;
        }
        return TYPE_IMAGE;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        if (h instanceof ImageHolder) {
            final ImageHolder holder = (ImageHolder) h;
            Wallpaper w = ((Wallpaper) mWallpapers.get(position));
            holder.name.setText(w.getName());
            holder.author.setText(w.getAuthor());
            holder.newWallpaper.setVisibility(mNewWallpapers.contains(w) ? View.VISIBLE : View.GONE);

            setFavorite(holder.favorite, ColorHelper.getAttributeColor(
                    mContext, android.R.attr.textColorPrimary), position, false);

            String url = w.getThumbUrl();

            ImageLoader.getInstance().displayImage(url, new ImageViewAware(holder.image),
                    mOptions.build(), ImageConfig.getThumbnailSize(mContext), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            super.onLoadingStarted(imageUri, view);
                            if (mIsAutoGeneratedColor) {
                                int vibrant = ColorHelper.getAttributeColor(
                                        mContext, R.attr.card_background);
                                holder.imageInfo.setBackgroundColor(vibrant);
                                int primary = ColorHelper.getAttributeColor(
                                        mContext, android.R.attr.textColorPrimary);
                                holder.name.setTextColor(primary);
                                holder.author.setTextColor(primary);
                            } else {
                                int color = mContext.getResources().getColor(R.color.image_info_text);
                                holder.imageInfo.setBackgroundColor(mContext.getResources().getColor(R.color.image_info_bg));
                                holder.name.setTextColor(color);
                                holder.author.setTextColor(color);
                            }
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            super.onLoadingComplete(imageUri, view, loadedImage);
                            if (mIsAutoGeneratedColor) {
                                if (loadedImage != null) {
                                    Palette.from(loadedImage).generate(new Palette.PaletteAsyncListener() {
                                        @Override
                                        public void onGenerated(Palette palette) {
                                            int vibrant = ColorHelper.getAttributeColor(
                                                    mContext, R.attr.card_background);
                                            int color = palette.getVibrantColor(vibrant);
                                            if (color == vibrant)
                                                color = palette.getMutedColor(vibrant);
                                            color = Color.argb(0x60, Color.red(color), Color.green(color), Color.blue(color));
                                            holder.imageInfo.setBackgroundColor(color);
                                            int text = ColorHelper.getTitleTextColor(color);
                                            holder.name.setTextColor(text);
                                            holder.author.setTextColor(text);
                                            setFavorite(holder.favorite, text, holder.getAdapterPosition(), false);
                                        }
                                    });
                                }
                            } else {
                                int color = mContext.getResources().getColor(R.color.image_info_text);
                                holder.imageInfo.setBackgroundColor(mContext.getResources().getColor(R.color.image_info_bg));
                                holder.name.setTextColor(color);
                                holder.author.setTextColor(color);
                                setFavorite(holder.favorite, color, holder.getAdapterPosition(), false);
                            }
                        }
                    }, null);

        } else if (h instanceof HeaderHolder) {
            HeaderHolder holder = (HeaderHolder) h;
            holder.mCatageory = (Category) mWallpapers.get(position);
            holder.category.setText(holder.mCatageory.getName());
            holder.count.setText(holder.mCatageory.getNumWallpapers() + " " + mCountString);
            holder.container.setClickable(mIsCategorySelectable);
            if (mIsCollapseMode) {
                holder.collapse.setRotation(!mCollapsedCategories.contains(holder.mCatageory.getName()) ? 180 : 0);
            }
            holder.newWallpaper.setVisibility(mNewCategories.contains(holder.mCatageory.getName()) ? View.VISIBLE : View.GONE);

            String url = holder.mCatageory.getThumbUrl();
            ImageLoader.getInstance().displayImage(url, new ImageViewAware(holder.image),
                    mOptions.build(), ImageConfig.getThumbnailSize(mContext), new SimpleImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            super.onLoadingStarted(imageUri, view);
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            super.onLoadingComplete(imageUri, view, loadedImage);
                        }
                    }, null);

            int color = mContext.getResources().getColor(R.color.image_info_text);
            holder.imageInfo.setBackgroundColor(mContext.getResources().getColor(R.color.image_info_bg));
            holder.category.setTextColor(color);
            holder.count.setTextColor(color);
            holder.collapse.setImageDrawable(DrawableHelper.getTintedDrawable(mContext,
                    R.drawable.ic_expand, color));
        }
    }

    @Override
    public int getItemCount() {
        return mWallpapers.size();
    }

    public void filter() {
        Database database = new Database(mContext);
        if (mCategoryMode) {
            mWallpapersAll = database.getFilteredCategoriesUnified();
        } else {
            mWallpapersAll = database.getFilteredWallpapersUnified();
        }
        if (mIsCollapseMode) {
            filterCollapsedCategories();
        } else {
            mWallpapers = mWallpapersAll;
        }
        notifyDataSetChanged();
    }

    private void filterCollapsedCategories() {
        mWallpapers.clear();
        for (Object o : mWallpapersAll) {
            if (o instanceof Wallpaper) {
                Wallpaper w = (Wallpaper) o;
                if (mCollapsedCategories.contains(w.getCategory())) {
                    continue;
                }
            }
            mWallpapers.add(o);
        }
    }

    private int[] hideCollapsedCategory(String category) {
        int changePos[] = new int[]{-1, -1};
        mWallpapers.clear();
        int i = 0;
        for (Object o : mWallpapersAll) {
            if (o instanceof Category) {
                Category c = (Category) o;
                if (c.getName().equals(category)) {
                    if (changePos[0] == -1) {
                        changePos[0] = i + 1;
                        changePos[1] = c.getNumWallpapers();
                    }
                }
            } else if (o instanceof Wallpaper) {
                Wallpaper w = (Wallpaper) o;
                if (mCollapsedCategories.contains(w.getCategory())) {
                    continue;
                }
            }
            mWallpapers.add(o);
            i++;
        }
        return changePos;
    }

    private int[] showCollapsedCategory(String category) {
        int changePos[] = new int[]{-1, -1};
        mWallpapers.clear();
        int i = 0;
        for (Object o : mWallpapersAll) {
            if (o instanceof Category) {
                Category c = (Category) o;
                if (c.getName().equals(category)) {
                    if (changePos[0] == -1) {
                        changePos[0] = i + 1;
                        changePos[1] = c.getNumWallpapers();
                    }
                }
            } else if (o instanceof Wallpaper) {
                Wallpaper w = (Wallpaper) o;
                if (mCollapsedCategories.contains(w.getCategory())) {
                    continue;
                }
            }
            mWallpapers.add(o);
            i++;
        }
        return changePos;
    }

    public void updateCollapsedCategories(String category) {
        boolean hide = !mCollapsedCategories.contains(category);
        if (hide) {
            mCollapsedCategories.add(category);
        } else {
            mCollapsedCategories.remove(category);
        }
        Preferences.getPreferences(mContext).setCollapsedCategories(mCollapsedCategories);
        if (hide) {
            int[] changePos = hideCollapsedCategory(category);
            if (changePos[0] != -1 && changePos[1] != -1) {
                notifyItemRangeRemoved(changePos[0], changePos[1]);
                return;
            }
        } else {
            int[] changePos = showCollapsedCategory(category);
            if (changePos[0] != -1 && changePos[1] != -1) {
                notifyItemRangeInserted(changePos[0], changePos[1]);
                return;
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public String getSectionTitle(int position) {
        Object o = mWallpapers.get(position);
        if (o instanceof Category) {
            return ((Category) o).getName();
        }
        if (o instanceof Wallpaper) {
            return ((Wallpaper) o).getCategory();
        }
        return "";
    }

    public void setColumnsCount(int columnsCount) {
        mColumnsCount = columnsCount;
    }

    class ImageHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        @BindView(R.id.card)
        CardView card;
        @BindView(R.id.container)
        FrameLayout container;
        @BindView(R.id.image)
        HeaderView image;
        @BindView(R.id.name)
        TextView name;
        @BindView(R.id.author)
        TextView author;
        @BindView(R.id.favorite)
        ImageView favorite;
        @BindView(R.id.image_info)
        View imageInfo;
        @BindView(R.id.new_wallpaper)
        ImageView newWallpaper;

        ImageHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            container.setOnClickListener(this);
            container.setOnLongClickListener(this);
            favorite.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();
            int position = getAdapterPosition();
            if (id == R.id.container) {
                try {
                    final Intent intent = new Intent(mContext, WallpaperBoardPreviewActivity.class);
                    intent.putExtra(Extras.EXTRA_URL, ((Wallpaper) mWallpapers.get(position)).getUrl());
                    intent.putExtra(Extras.EXTRA_AUTHOR, ((Wallpaper) mWallpapers.get(position)).getAuthor());
                    intent.putExtra(Extras.EXTRA_NAME, ((Wallpaper) mWallpapers.get(position)).getName());

                    ActivityTransitionLauncher.with((AppCompatActivity) mContext)
                            .from(image, Extras.EXTRA_IMAGE)
                            .image(((BitmapDrawable) image.getDrawable()).getBitmap())
                            .launch(intent);
                } catch (Exception e) {
                }

                WallpaperListener listener = getWallpaperListener();
                if (listener != null) {
                    listener.onWallpaperSelected(position);
                }
            } else if (id == R.id.favorite) {
                if (position < 0 || position > mWallpapers.size()) return;

                if (mIsFavoriteMode) {
                    Database database = new Database(mContext);
                    database.favoriteWallpaper(((Wallpaper) mWallpapers.get(position)).getId(),
                            !((Wallpaper) mWallpapers.get(position)).isFavorite());
                    mWallpapers.remove(position);
                    notifyItemRemoved(position);
                    return;
                }

                ((Wallpaper) mWallpapers.get(position)).setFavorite(!((Wallpaper) mWallpapers.get(position)).isFavorite());
                setFavorite(favorite, name.getCurrentTextColor(), position, true);

                CafeBar.builder(mContext)
                        .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(
                                mContext, R.attr.card_background)))
                        .fitSystemWindow(R.bool.view_fitsystemwindow)
                        .content(String.format(
                                mContext.getResources().getString(((Wallpaper) mWallpapers.get(position)).isFavorite() ?
                                        R.string.wallpaper_favorite_added : R.string.wallpaper_favorite_removed),
                                ((Wallpaper) mWallpapers.get(position)).getName()))
                        .icon(((Wallpaper) mWallpapers.get(position)).isFavorite() ?
                                R.drawable.ic_toolbar_love : R.drawable.ic_toolbar_unlove)
                        .build().show();
            }
        }

        @Override
        public boolean onLongClick(View view) {
            int id = view.getId();
            int position = getAdapterPosition();
            if (id == R.id.container) {
                WallpaperListener listener = getWallpaperListener();
                if (listener != null) {
                    if (!listener.isSelectEnabled()) {
                        return false;
                    }
                }
                if (position < 0 || position > mWallpapers.size()) {
                    mLastSelectedPosition = -1;
                    return false;
                }

                mLastSelectedPosition = position;
                WallpaperOptionsFragment.showWallpaperOptionsDialog(
                        ((AppCompatActivity) mContext).getSupportFragmentManager(),
                        ((Wallpaper) mWallpapers.get(position)).getUrl(),
                        ((Wallpaper) mWallpapers.get(position)).getName());
                return true;
            }
            return false;
        }
    }

    class HeaderHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        @BindView(R.id.card)
        CardView card;
        @BindView(R.id.container)
        FrameLayout container;
        @BindView(R.id.category)
        TextView category;
        @BindView(R.id.image)
        ImageView image;
        @BindView(R.id.count)
        TextView count;
        @BindView(R.id.collapse)
        ImageView collapse;
        Category mCatageory;
        @BindView(R.id.new_wallpaper)
        ImageView newWallpaper;
        @BindView(R.id.image_info)
        View imageInfo;

        HeaderHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            container.setOnClickListener(this);
            if (!mIsCollapseMode) {
                collapse.setVisibility(View.GONE);
            } else {
                collapse.setOnClickListener(this);
            }
            newWallpaper.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int id = view.getId();

            if (id == R.id.container) {
                handleClick(view);
            }
            if (id == R.id.collapse) {
                updateCollapsedCategories(mCatageory.getName());
                boolean hide = mCollapsedCategories.contains(mCatageory.getName());
                if (hide) {
                    hideCategory();
                } else {
                    showCategory();
                }
            }
            if (id == R.id.new_wallpaper) {
                ((WallpaperBoardActivity) mContext).showNew();
            }
        }

        @Override
        public boolean onLongClick(View view) {
            return false;
        }

        private void handleClick(View view) {
            int position = getAdapterPosition();
            Category c = (Category) mWallpapers.get(position);

            WallpaperListener listener = getWallpaperListener();
            if (listener != null) {
                listener.onCategorySelected(position, card, c);
            }
        }

        private void hideCategory() {
            Animator rotateAnimator = ObjectAnimator.ofFloat(collapse, View.ROTATION, ROTATE_180_DEGREE, ROTATE_0_DEGREE);
            rotateAnimator.setDuration(500);
            rotateAnimator.start();
        }

        private void showCategory() {
            Animator rotateAnimator = ObjectAnimator.ofFloat(collapse, View.ROTATION, ROTATE_0_DEGREE, ROTATE_180_DEGREE);
            rotateAnimator.setDuration(500);
            rotateAnimator.start();
        }
    }

    private void setFavorite(@NonNull ImageView imageView, @ColorInt int color, int position, boolean write) {
        if (position < 0 || position > mWallpapers.size()) return;

        boolean isFavorite = ((Wallpaper) mWallpapers.get(position)).isFavorite();
        if (isFavorite)
            color = ContextCompat.getColor(mContext, R.color.favoriteColor);
        imageView.setImageDrawable(DrawableHelper.getTintedDrawable(mContext,
                isFavorite ? R.drawable.ic_toolbar_love : R.drawable.ic_toolbar_unlove, color));
        if (write) {
            Database database = new Database(mContext);
            database.favoriteWallpaper(((Wallpaper) mWallpapers.get(position)).getId(), isFavorite);
        }
    }

    private WallpaperListener getWallpaperListener() {
        FragmentManager fm = ((AppCompatActivity) mContext).getSupportFragmentManager();
        if (fm != null) {
            Fragment fragment = fm.findFragmentById(R.id.container);
            if (fragment != null) {
                if (fragment instanceof WallpapersFragment ||
                        fragment instanceof FavoritesFragment ||
                        fragment instanceof WallpaperSearchFragment) {
                    return (WallpaperListener) fragment;
                }
            }
        }
        return null;
    }

    public void downloadLastSelectedWallpaper() {
        if (mLastSelectedPosition < 0 || mLastSelectedPosition > mWallpapers.size()) return;

        WallpaperHelper.downloadWallpaper(mContext,
                ColorHelper.getAttributeColor(mContext, R.attr.colorAccent),
                ((Wallpaper) mWallpapers.get(mLastSelectedPosition)).getUrl(),
                ((Wallpaper) mWallpapers.get(mLastSelectedPosition)).getName());
    }
}
