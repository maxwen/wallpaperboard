package com.maxwen.wallpaper.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.maxwen.wallpaper.board.activities.WallpaperBoardSplashActivity;

public class SplashActivity extends WallpaperBoardSplashActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initSplashActivity(savedInstanceState, MainActivity.class);
    }
}
