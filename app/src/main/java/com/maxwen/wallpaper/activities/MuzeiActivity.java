package com.maxwen.wallpaper.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.maxwen.wallpaper.board.activities.WallpaperBoardMuzeiActivity;
import com.maxwen.wallpaper.services.MuzeiService;

public class MuzeiActivity extends WallpaperBoardMuzeiActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        initMuzeiActivity(savedInstanceState, MuzeiService.class);
    }
}
