package com.maxwen.wallpaper.board.helpers;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.danimahardhika.cafebar.CafeBar;
import com.danimahardhika.cafebar.CafeBarCallback;
import com.danimahardhika.cafebar.CafeBarDuration;
import com.danimahardhika.cafebar.CafeBarTheme;
import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.fragments.dialogs.WallpaperApplyFragment;
import com.maxwen.wallpaper.board.preferences.Preferences;
import com.maxwen.wallpaper.board.utils.ImageConfig;
import com.maxwen.wallpaper.board.utils.LogUtil;
import com.maxwen.wallpaper.firebase.FirebaseHelper;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

public class WallpaperHelper {

    public static File getWallpapersDirectory(@NonNull Context context) {
        try {
            if (Preferences.getPreferences(context).getWallsDirectory() == null) {
                return new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES) +"/"+
                        context.getResources().getString(R.string.app_name));
            }
            return new File(Preferences.getPreferences(context).getWallsDirectory());
        } catch (Exception e) {
            return new File(context.getFilesDir().toString() +"/Pictures/"+
                    context.getResources().getString(R.string.app_name));
        }
    }

    private static String getWallpaperUri(@NonNull Context context, String url, String filename) {
        if (PermissionHelper.isPermissionStorageGranted(context)) {
            File directory = getWallpapersDirectory(context);
            if (new File(directory + File.separator + filename).exists()) {
                return "file://" + directory + File.separator + filename;
            }
        }
        return url;
    }

    public static void downloadWallpaper(@NonNull final Context context, @ColorInt final int color,
                                         final String link, final String name) {

        File cache = ImageLoader.getInstance().getDiskCache().get(link);
        if (cache != null) {
            File target = new File(getWallpapersDirectory(context).toString()
                    + File.separator + name + FileHelper.IMAGE_EXTENSION);

            if (FileHelper.copyFile(cache, target)) {
                wallpaperSaved(context, color, target);

                context.sendBroadcast(new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(
                        new File(target.toString()))));
                return;
            }
        }

        new AsyncTask<Void, Integer, Boolean>() {

            MaterialDialog dialog;
            HttpURLConnection connection;
            File output;
            File file;
            int fileLength;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                output = WallpaperHelper.getWallpapersDirectory(context);
                file = new File(output.toString() + File.separator + name + FileHelper.IMAGE_EXTENSION);

                MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
                builder.content(R.string.wallpaper_downloading)
                        .widgetColor(color)
                        .progress(true, 0)
                        .progressIndeterminateStyle(true);
                dialog = builder.build();
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        try {
                            if (connection != null) connection.disconnect();
                        } catch (Exception ignored) {
                        }
                        cancel(true);
                    }
                });
                dialog.show();
            }

            @Override
            protected Boolean doInBackground(Void... voids) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        if (!output.exists())
                            if (!output.mkdirs())
                                return false;

                        URL url = new URL(link);
                        connection = (HttpURLConnection) url.openConnection();
                        connection.setConnectTimeout(15000);

                        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            fileLength = connection.getContentLength();
                            InputStream stream = connection.getInputStream();
                            OutputStream output = new FileOutputStream(file.toString());

                            byte data[] = new byte[1024];
                            long total = 0;
                            int count;
                            while ((count = stream.read(data)) != -1) {
                                total += count;
                                if (fileLength > 0)
                                    publishProgress((int) (total * 100 / fileLength));
                                output.write(data, 0, count);
                            }

                            output.flush();
                            output.close();
                            stream.close();
                            return true;
                        }
                    } catch (Exception e) {
                        LogUtil.e(Log.getStackTraceString(e));
                        return false;
                    }
                }
                return false;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                int downloaded = fileLength / 1014;
                String size = String.valueOf(values[0] * fileLength/1024/100) + " KB" +
                        String.valueOf(fileLength == 0 ? "" : "/" + downloaded + " KB");
                String downloading = context.getResources().getString(
                        R.string.wallpaper_downloading);
                String text = downloading +"\n"+ size + "";
                dialog.setContent(text);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (((AppCompatActivity) context).isFinishing()) return;

                if (file != null) file.delete();

                Toast.makeText(context,
                        context.getResources().getString(R.string.wallpaper_download_cancelled),
                        Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                dialog.dismiss();
                if (aBoolean) {
                    context.sendBroadcast(new Intent(
                            Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(
                            new File(file.toString()))));

                    wallpaperSaved(context, color, file);
                } else {
                    Toast.makeText(context,
                            context.getResources().getString(R.string.wallpaper_download_failed),
                            Toast.LENGTH_LONG).show();
                }
            }

        }.execute();
    }

    private static void wallpaperSaved(@Nullable final Context context, @ColorInt int color, @NonNull final File file) {
        if (context == null) return;

        String downloaded = context.getResources().getString(
                R.string.wallpaper_downloaded);

        CafeBar.Builder builder = new CafeBar.Builder(context);
        builder.theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(context, R.attr.card_background)))
                .duration(CafeBarDuration.MEDIUM.getDuration())
                .maxLines(4)
                .content(downloaded + " " + file.toString())
                .icon(R.drawable.ic_toolbar_download)
                .neutralText(R.string.open)
                .neutralColor(color)
                .onNeutral(new CafeBarCallback() {
                    @Override
                    public void OnClick(@NonNull CafeBar cafeBar) {
                        Uri uri = FileHelper.getUriFromFile(context, context.getPackageName(), file);
                        if (uri == null) return;

                        context.startActivity(new Intent()
                                .setAction(Intent.ACTION_VIEW)
                                .setDataAndType(uri, "image/*")
                                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));

                        cafeBar.dismiss();
                    }
                });

        Window window = ((AppCompatActivity) context).getWindow();
        WindowManager.LayoutParams params = window.getAttributes();
        int flags = params.flags;

        if ((flags & WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) ==
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION) {
            builder.fitSystemWindow(true);
        } else {
            builder.fitSystemWindow(R.bool.view_fitsystemwindow);
        }

        CafeBar cafeBar = builder.build();
        cafeBar.show();
    }

    private static ImageSize getScaledSize(@NonNull Context context, String url) {
        Point point = ViewHelper.getRealScreenSize(context);
        int height = point.y;
        int width = point.x;

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            height = point.x;
            width = point.y;
        }

        int scaledWidth = 0;
        int scaledHeight = 0;
        File file = ImageLoader.getInstance().getDiskCache().get(url);
        if (file != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            double scale = (double) options.outHeight / (double) height;

            scaledWidth = Double.valueOf((double) options.outWidth / scale).intValue();
            scaledHeight = Double.valueOf((double) options.outHeight / scale).intValue();
        }

        if (scaledWidth == 0) scaledWidth = width;
        if (scaledHeight == 0) scaledHeight = height;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            int statusBarHeight = ViewHelper.getStatusBarHeight(context);
            double scale = (double) (scaledHeight - statusBarHeight) / (double) scaledHeight;

            scaledWidth = Double.valueOf((double) scaledWidth * scale).intValue();
            scaledHeight = Double.valueOf((double) scaledHeight * scale).intValue();
        }
        return new ImageSize(scaledWidth, scaledHeight);
    }

    @Nullable
    private static RectF getScaledRectF(@Nullable RectF rectF, float factor) {
        if (rectF == null) return null;

        RectF scaledRectF = new RectF(rectF);
        scaledRectF.top *= factor;
        scaledRectF.bottom *= factor;
        scaledRectF.left *= factor;
        scaledRectF.right *= factor;
        return scaledRectF;
    }

    public static void applyWallpaper(@NonNull final Context context, @Nullable final RectF rectF,
                                      @ColorInt final int color, final String url, final String name) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            WallpaperApplyFragment.showWallpaperApply(
                    ((AppCompatActivity) context).getSupportFragmentManager(), rectF, url, name);
        } else {
            doApplyWallpaper(context, rectF, color, url, name, -1);
        }
    }
    public static void doApplyWallpaper(@NonNull Context context, @Nullable RectF rectF,
                                      @ColorInt int color, String url, String name, int applyFlag) {
        final MaterialDialog.Builder builder = new MaterialDialog.Builder(context);

        FirebaseHelper.getPreferences(context).addApplyWallpaperEvent(url);

        builder.widgetColor(color)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .content(R.string.wallpaper_applying);
        final MaterialDialog dialog = builder.build();

        String imageUri = getWallpaperUri(context, url, name + FileHelper.IMAGE_EXTENSION);

        ImageSize imageSize = getScaledSize(context, url);
        loadBitmap(context, dialog, 1, imageUri, rectF, imageSize, applyFlag);
    }

    private static void loadBitmap(final Context context, final MaterialDialog dialog, final int call, final String imageUri,
                                   final RectF rectF, final ImageSize imageSize, final int applyFlag) {
        final AsyncTask<Bitmap, Void, Boolean> setWallpaper = getWallpaperAsync(
                context, dialog, rectF, applyFlag);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ImageLoader.getInstance().stop();
                setWallpaper.cancel(true);
            }
        });

        ImageLoader.getInstance().handleSlowNetwork(true);
        ImageLoader.getInstance().loadImage(imageUri, imageSize,
                ImageConfig.getWallpaperOptions(), new ImageLoadingListener() {

                    @Override
                    public void onLoadingStarted(String imageUri, View view) {
                        if (dialog.isShowing()) return;

                        dialog.setContent(R.string.wallpaper_loading);
                        dialog.show();
                    }

                    @Override
                    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                        if (failReason.getType() == FailReason.FailType.OUT_OF_MEMORY) {
                            if (call <= 5) {
                                double scaleFactor = 1 - (0.1 * call);
                                int scaledWidth = Double.valueOf(imageSize.getWidth() * scaleFactor).intValue();
                                int scaledHeight = Double.valueOf(imageSize.getHeight() * scaleFactor).intValue();

                                RectF scaledRecF = getScaledRectF(rectF, (float) scaleFactor);
                                loadBitmap(context, dialog, (call + 1), imageUri, scaledRecF, new ImageSize(scaledWidth, scaledHeight), applyFlag);
                                return;
                            }
                        }

                        dialog.dismiss();
                        String message = context.getResources().getString(R.string.wallpaper_apply_failed);
                        message = message +": "+ failReason.getType().toString();
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                        dialog.setContent(R.string.wallpaper_applying);
                        setWallpaper.execute(loadedImage);
                    }

                    @Override
                    public void onLoadingCancelled(String imageUri, View view) {
                        dialog.dismiss();
                        Toast.makeText(context, R.string.wallpaper_apply_failed,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private static AsyncTask<Bitmap, Void, Boolean> getWallpaperAsync(@NonNull final Context context, final MaterialDialog dialog,
                                                                      final RectF rectF, final int applyFlag) {
        return new AsyncTask<Bitmap, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Bitmap... bitmaps) {
                while (!isCancelled()) {
                    try {
                        Thread.sleep(1);
                        WallpaperManager manager = WallpaperManager.getInstance(context);
                        if (bitmaps[0] != null) {
                            Bitmap bitmap = bitmaps[0];

                            if (!Preferences.getPreferences(context).isScrollWallpaper() && rectF != null) {
                                Point point = ViewHelper.getRealScreenSize(context);

                                int targetWidth = Double.valueOf(
                                        ((double) bitmaps[0].getHeight() / (double) point.y)
                                                * (double) point.x).intValue();

                                bitmap = Bitmap.createBitmap(
                                        targetWidth,
                                        bitmaps[0].getHeight(),
                                        bitmaps[0].getConfig());
                                Paint paint = new Paint();
                                paint.setFilterBitmap(true);
                                paint.setAntiAlias(true);
                                paint.setDither(true);

                                Canvas canvas = new Canvas(bitmap);
                                canvas.drawBitmap(bitmaps[0], null, rectF, paint);
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                manager.setBitmap(bitmap, null, true, applyFlag);
                            } else {
                                manager.setBitmap(bitmap);
                            }
                            return true;
                        }
                        return false;
                    } catch (Exception | OutOfMemoryError e) {
                        LogUtil.e(Log.getStackTraceString(e));
                        return false;
                    }
                }
                return false;
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                Toast.makeText(context, R.string.wallpaper_apply_cancelled,
                        Toast.LENGTH_LONG).show();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                super.onPostExecute(aBoolean);
                dialog.dismiss();
                if (aBoolean) {
                    CafeBar.builder(context)
                            .theme(new CafeBarTheme.Custom(ColorHelper.getAttributeColor(
                                    context, R.attr.card_background)))
                            .fitSystemWindow(R.bool.view_fitsystemwindow)
                            .content(R.string.wallpaper_applied)
                            .build().show();
                } else {
                    Toast.makeText(context, R.string.wallpaper_apply_failed,
                            Toast.LENGTH_LONG).show();
                }
            }
        };
    }
}
