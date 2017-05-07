package com.maxwen.wallpaper.firebase;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.google.firebase.analytics.FirebaseAnalytics;

/**
 * Created by maxl on 5/5/17.
 */

public class FirebaseHelper {
    private static FirebaseHelper mInstance;
    private FirebaseAnalytics mFirebaseAnalytics;

    @NonNull
    public static FirebaseHelper getPreferences(@NonNull Context context) {
        if (mInstance == null) {
            mInstance = new FirebaseHelper(context);
        }
        return mInstance;
    }

    private FirebaseHelper(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public void addApplyWallpaperEvent(String url) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "apply_wall");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, url);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }
}
