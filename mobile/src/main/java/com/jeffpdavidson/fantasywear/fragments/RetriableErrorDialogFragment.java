package com.jeffpdavidson.fantasywear.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.jeffpdavidson.fantasywear.R;

/**
 * Dialog to show an error message when a retriable error occurs.
 *
 * The parent activity must implement {@link Listener} which provides callback when the user decides
 * to retry the action or give up for good.
 */
public class RetriableErrorDialogFragment extends DialogFragment
        implements DialogInterface.OnClickListener {
    private static final String TAG = "RetriableErrorDialog";

    private static final String ARG_MESSAGE = "message";

    public static interface Listener {
        void onRetry();
        void onCancel();
    }

    public static void showIfNeeded(FragmentManager fm, String message) {
        if (fm.findFragmentByTag(TAG) == null) {
            RetriableErrorDialogFragment.newInstance(message).show(fm, TAG);
        }
    }

    private static RetriableErrorDialogFragment newInstance(String message) {
        RetriableErrorDialogFragment fragment = new RetriableErrorDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    private Listener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Listener)) {
            throw new IllegalStateException(activity.getClass().getSimpleName()
                    + " must implement RetriableErrorDialogFragment.Listener.");
        }
        mListener = (Listener) activity;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(getArguments().getString(ARG_MESSAGE))
                .setPositiveButton(R.string.retry, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            mListener.onRetry();
        } else {  // BUTTON_NEGATIVE
            mListener.onCancel();
        }
    }
}
