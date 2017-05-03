/*
 *  Copyright (C) 2014 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.maxwen.wallpaper.board.fragments.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.maxwen.wallpaper.R;
import com.maxwen.wallpaper.board.helpers.ColorHelper;
import com.maxwen.wallpaper.board.utils.Extras;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DirectoryChooserDialog extends DialogFragment
    implements DialogInterface.OnClickListener {

    private String mSDCardDirectory;
    private String mCurrentDir;
    private List<File> mSubDirs;
    private ArrayAdapter<File> mListAdapter;
    private ListView mListView;
    private int mTextColor;
    private int mTextColorDisabled;
    private ChosenDirectoryListener mListener;
    private String mStartFolder;

    private static final String TAG = "com.maxwen.wallpaper.board.dialog.wallpaper.folder";

    public interface ChosenDirectoryListener {
        public void onChooseDirOk(Uri chosenDir);

        public void onChooseDirCancel();
    }

    public static DirectoryChooserDialog newInstance(ChosenDirectoryListener listener, String startFolder) {
        DirectoryChooserDialog fragment = new DirectoryChooserDialog();
        fragment.setChoosenListener(listener);
        Bundle bundle = new Bundle();
        bundle.putString(Extras.EXTRA_FOLDER, startFolder);
        fragment.setArguments(bundle);
        return fragment;
    }

    public static void showDirectoryChooserDialog(FragmentManager fm, ChosenDirectoryListener listener, String startFolder) {
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }

        try {
            android.support.v4.app.DialogFragment dialog = DirectoryChooserDialog.newInstance(listener, startFolder);
            dialog.show(ft, TAG);
        } catch (IllegalArgumentException | IllegalStateException ignored) {}
    }

    public DirectoryChooserDialog() {
        mSDCardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        try {
            mSDCardDirectory = new File(mSDCardDirectory).getCanonicalPath();
        } catch (IOException ioe) {
        }
    }

    public void setChoosenListener(ChosenDirectoryListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStartFolder = getArguments().getString(Extras.EXTRA_FOLDER);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            mStartFolder = getArguments().getString(Extras.EXTRA_FOLDER);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(Extras.EXTRA_FOLDER, mStartFolder);
        super.onSaveInstanceState(outState);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.folder_dialog_title)
                .setPositiveButton("", this)
                .setNegativeButton(android.R.string.cancel, this)
                .setView(createDialogView());

        return builder.create();
    }

    private List<File> getDirectories(String dir) {
        List<File> dirs = new ArrayList<File>();

        try {
            File dirFile = new File(dir);
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return dirs;
            }

            for (File file : dirFile.listFiles()) {
                if (!file.getName().startsWith(".")) {
                    dirs.add(file);
                }
            }
        } catch (Exception e) {
        }

        Collections.sort(dirs, new Comparator<File>() {
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile()) {
                    return -1;
                }
                if (o2.isDirectory() && o1.isFile()) {
                    return 1;
                }
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        if (!dir.equals(mSDCardDirectory)) {
            dirs.add(0, new File(".."));
        }
        return dirs;
    }

    private void updateDirectory() {
        mSubDirs.clear();
        mSubDirs.addAll(getDirectories(mCurrentDir));
        mListAdapter.notifyDataSetChanged();
        boolean enableOk = true; //!mCurrentDir.equals(mSDCardDirectory);
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setText(getResources().getString(android.R.string.ok));
        ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(enableOk ? View.VISIBLE : View.GONE);
    }

    private ArrayAdapter<File> createListAdapter(List<File> items) {
        return new ArrayAdapter<File>(getActivity(),
                R.layout.folder_item, R.id.folder_name, items) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View item = null;
                if (convertView == null){
                    final LayoutInflater inflater = (LayoutInflater) getActivity()
                            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    item = inflater.inflate(R.layout.folder_item, null);
                } else {
                    item = convertView;
                }
                TextView tv = (TextView) item.findViewById(R.id.folder_name);
                File f = mSubDirs.get(position);
                tv.setText(f.getName());
                if (f.isFile()) {
                    tv.setTextColor(mTextColorDisabled);
                } else {
                    tv.setTextColor(mTextColor);
                }
                return item;
            }
        };
    }

    private View createDialogView() {
        final LayoutInflater inflater = (LayoutInflater) getActivity()
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = inflater
                .inflate(R.layout.folder_dialog, null);

        mTextColor = ColorHelper.getAttributeColor(getActivity(), android.R.attr.textColorPrimary);
        mTextColorDisabled = ColorHelper.getAttributeColor(getActivity(), android.R.attr.listDivider);
        if (!TextUtils.isEmpty(mStartFolder)) {
            mCurrentDir = mStartFolder;
        } else {
            mCurrentDir = mSDCardDirectory;
        }
        mSubDirs = getDirectories(mCurrentDir);

        mListAdapter = createListAdapter(mSubDirs);
        mListView = (ListView) view.findViewById(R.id.folders);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                File f = mListAdapter.getItem(position);
                if (f.isFile()) {
                    return;
                }
                if (f.getName().equals("..")) {
                    if (!mCurrentDir.equals(mSDCardDirectory)) {
                        // Navigate back to an upper directory
                        mCurrentDir = new File(mCurrentDir).getParent();
                    }
                } else {
                    // Navigate into the sub-directory
                    mCurrentDir = mListAdapter.getItem(position).getAbsolutePath();
                }
                updateDirectory();
            }
        });

        return view;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Uri uri = Uri.fromFile(new File(mCurrentDir));
            mListener.onChooseDirOk(uri);
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            mListener.onChooseDirCancel();
        }
    }
}
