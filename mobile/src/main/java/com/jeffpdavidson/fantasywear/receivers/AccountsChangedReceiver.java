package com.jeffpdavidson.fantasywear.receivers;

import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffpdavidson.fantasywear.services.CleanUnusedAccountsService;

/** Receiver to cleanup unused accounts whenever accounts have changed on the device. */
public class AccountsChangedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (AccountManager.LOGIN_ACCOUNTS_CHANGED_ACTION.equals(intent.getAction())) {
            CleanUnusedAccountsService.start(context);
        }
    }
}
