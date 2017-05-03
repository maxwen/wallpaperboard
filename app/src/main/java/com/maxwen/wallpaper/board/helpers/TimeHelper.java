package com.maxwen.wallpaper.board.helpers;

import android.support.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Locale;

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

public class TimeHelper {

    /*public static String getDateTime() {
        SimpleDateFormat dateFormat = getDefaultDateTimeFormat();
        Date date = new Date();
        return dateFormat.format(date);
    }*/

    @NonNull
    private static SimpleDateFormat getDefaultDateTimeFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
    }

    public static int convertMinuteToMilli(int minute) {
        return minute * 60 * 1000;
    }

    public static int convertMilliToMinute(int milli) {
        return milli / 60 / 1000;
    }
}
