package com.jeffpdavidson.fantasywear.fragments;

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;

import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/** Dialog displaying details for a FantasyWear account. */
public class AccountInfoDialogFragment extends DialogFragment implements OnClickListener {
    @VisibleForTesting
    public static final String TAG = "AccountInfoDialog";

    private static final String ARG_ACCOUNT = "account";
    private static final String ARG_LEAGUE_NAMES = "league_names";

    private Account mAccount;

    public static void show(FragmentManager fm, Account account, ArrayList<String> leagueNames) {
        AccountInfoDialogFragment fragment = new AccountInfoDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putParcelable(ARG_ACCOUNT, account);
        arguments.putStringArrayList(ARG_LEAGUE_NAMES, leagueNames);
        fragment.setArguments(arguments);
        fragment.show(fm, TAG);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        mAccount = arguments.getParcelable(ARG_ACCOUNT);
        List<String> leagueNames = arguments.getStringArrayList(ARG_LEAGUE_NAMES);
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.account_details)
                .setMessage(getString(R.string.account_details_message,
                        TextUtils.join(getString(R.string.league_name_separator), leagueNames)))
                .setNegativeButton(R.string.remove, this)
                .setNeutralButton(R.string.close, null)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ConfirmRemoveAccountDialogFragment.show(getFragmentManager(), mAccount);
    }
}
