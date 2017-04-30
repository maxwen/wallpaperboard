package com.maxwen.wallpaper.board.utils;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.helpers.ContextHelper;

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

public class Animator {

    public static void startAlphaAnimation(@Nullable View view, long duration, int visibility) {
        if (view == null) return;

        AlphaAnimation alphaAnimation = (visibility == View.VISIBLE)
                ? new AlphaAnimation(0f, 1f) : new AlphaAnimation(1f, 0f);

        alphaAnimation.setDuration(duration);
        alphaAnimation.setFillAfter(true);
        view.startAnimation(alphaAnimation);
    }

    public static void startSlideDownAnimation(@NonNull View view, int visibility) {
        Context context = ContextHelper.getBaseContext(view);
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_down_from_top);
        view.startAnimation(animation);
        view.setVisibility(visibility);
    }

    public static void showFab(@Nullable FloatingActionButton fab) {
        if (fab == null) return;

        if (ViewCompat.isLaidOut(fab)) {
            fab.show();
            return;
        }

        fab.animate().cancel();
        fab.setScaleX(0f);
        fab.setScaleY(0f);
        fab.setAlpha(0f);
        fab.setVisibility(View.VISIBLE);
        fab.animate().setDuration(200).scaleX(1).scaleY(1).alpha(1)
                .setInterpolator(new LinearOutSlowInInterpolator());
    }
}
