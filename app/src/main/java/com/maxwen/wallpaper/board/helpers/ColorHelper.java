package com.maxwen.wallpaper.board.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import com.maxwen.wallpaper.R;

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

public class ColorHelper {

    public static void setTransparentStatusBar(@NonNull Context context, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((Activity) context).getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            ((Activity) context).getWindow().setStatusBarColor(color);
        }
    }

    public static void setStatusBarColor(@NonNull Context context, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((Activity) context).getWindow().setStatusBarColor(ColorHelper.getDarkerColor(
                    color, 0.85f));
        }
    }

    public static void setNavigationBarColor(@NonNull Context context, @ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((Activity) context).getWindow().setNavigationBarColor(color);
        }
    }

    public static int getAttributeColor(@NonNull Context context, @AttrRes int attr) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(attr, typedValue, true);
        return typedValue.data;
    }

    public static int getTitleTextColor(@ColorInt int color) {
        double darkness = 1-(0.299* Color.red(color) + 0.587*Color.green(color) + 0.114*Color.blue(color))/255;
        return (darkness < 0.35) ? getDarkerColor(color, 0.25f) : Color.WHITE;
    }

    public static int getDarkerColor(@ColorInt int color, float transparency) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= transparency;
        return Color.HSVToColor(hsv);
    }

    public static int setColorAlpha(@ColorInt int color, float alpha) {
        int alpha2 = Math.round(Color.alpha(color) * alpha);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha2, red, green, blue);
    }

    public static ColorStateList getColorStateList(@ColorInt int color) {
        int[][] states = new int[][] {
                new int[] {android.R.attr.state_pressed},
                new int[] {}
        };
        int[] colors = new int[] {
                ColorHelper.getDarkerColor(color, 0.9f),
                color
        };
        return new ColorStateList(states, colors);
    }

    private static boolean isLightToolbar(@NonNull Context context) {
        int color = getAttributeColor(context, R.attr.colorPrimary);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return red >= 192 && green >= 192 && blue >= 192;
    }

    public static void setStatusBarIconColor(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = ((AppCompatActivity) context).getWindow().getDecorView();
            if (view != null) {
                if (isLightToolbar(context)){
                    view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                    return;
                }

                view.setSystemUiVisibility(0);
            }
        }
    }

    public static boolean isValidColor(String string) {
        try {
            Color.parseColor(string);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
