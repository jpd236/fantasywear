package com.jeffpdavidson.fantasywear.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.storage.Preferences;

import java.util.Arrays;

/** Dialog for setting the user's sync interval preference. */
public class SyncIntervalDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {
    @VisibleForTesting
    public static final String TAG = "SyncIntervalDialog";

    private static final String SAVED_SELECTED_INDEX = "selected_index";

    private int[] mSyncIntervalsMinutes;
    private int mSelectedIndex;

    public static void show(FragmentManager fm) {
        new SyncIntervalDialogFragment().show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mSyncIntervalsMinutes = getResources().getIntArray(R.array.sync_interval_minutes);

        if (savedInstanceState != null) {
            mSelectedIndex = savedInstanceState.getInt(SAVED_SELECTED_INDEX);
        } else {
            mSelectedIndex = Math.max(0, Arrays.binarySearch(mSyncIntervalsMinutes,
                    Preferences.getSyncIntervalSec(getActivity()) / 60));
        }

        String[] choices = new String[mSyncIntervalsMinutes.length];
        for (int i = 0; i < mSyncIntervalsMinutes.length; i++) {
            choices[i] = getResources().getQuantityString(R.plurals.minutes,
                    mSyncIntervalsMinutes[i], mSyncIntervalsMinutes[i]);
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.sync_interval)
                .setSingleChoiceItems(choices, mSelectedIndex, this)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.save, this)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_SELECTED_INDEX, mSelectedIndex);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (mSelectedIndex >= 0) {
                Preferences.setSyncIntervalSec(getActivity(),
                        mSyncIntervalsMinutes[mSelectedIndex] * 60);
            }
        } else {
            mSelectedIndex = which;
        }
    }
}
