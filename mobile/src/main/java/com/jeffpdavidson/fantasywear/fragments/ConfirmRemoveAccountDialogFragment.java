package com.jeffpdavidson.fantasywear.fragments;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.jeffpdavidson.fantasywear.R;

/** Dialog to confirm removing an account. */
public class ConfirmRemoveAccountDialogFragment extends DialogFragment implements OnClickListener {
    private static final String TAG = "ConfirmRemoveAcctDlg";

    private static final String ARG_ACCOUNT = "account";
    private Account mAccount;

    public static void show(FragmentManager fm, Account account) {
        ConfirmRemoveAccountDialogFragment fragment = new ConfirmRemoveAccountDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ACCOUNT, account);
        fragment.setArguments(arguments);
        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mAccount = getArguments().getParcelable(ARG_ACCOUNT);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.remove_account)
                .setMessage(R.string.remove_account_message)
                .setPositiveButton(R.string.remove, this)
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        AccountManager am = AccountManager.get(getActivity());
        am.removeAccount(mAccount, null, null);
    }
}
