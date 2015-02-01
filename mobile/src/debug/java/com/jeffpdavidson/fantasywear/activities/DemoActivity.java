package com.jeffpdavidson.fantasywear.activities;

import android.accounts.Account;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.api.auth.AccountAuthenticator;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.api.model.Team;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.protocol.LeagueData;
import com.jeffpdavidson.fantasywear.sync.SyncAdapter;

/** Activity to send demo data to wear for debugging purposes. */
public class DemoActivity extends ActionBarActivity implements OnClickListener, ErrorListener {
    private static final String TAG = "DemoActivity";

    private static final Account DEMO_ACCOUNT =
            new Account("demo_account", AccountAuthenticator.ACCOUNT_TYPE_YAHOO);
    private static final String DEMO_LEAGUE_KEY = "demo_league";

    private GoogleApiClient mGoogleApiClient;
    private EditText mOppName;
    private EditText mScore;
    private EditText mOppScore;
    private EditText mLogoUrl;
    private EditText mOppLogoUrl;
    private int mLogoSize;
    private Bitmap mLogo;
    private Bitmap mOppLogo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        mOppName = (EditText) findViewById(R.id.opp_name);
        mScore = (EditText) findViewById(R.id.score);
        mOppScore = (EditText) findViewById(R.id.opp_score);
        mLogoUrl = (EditText) findViewById(R.id.logo_url);
        mOppLogoUrl = (EditText) findViewById(R.id.opp_logo_url);

        mLogoSize = getResources().getDimensionPixelSize(R.dimen.logo_size);

        findViewById(R.id.push).setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        Volley.getInstance(this).getRequestQueue().cancelAll(TAG);
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onClick(View view) {
        ImageRequest logoReq = new ImageRequest(mLogoUrl.getText().toString(),
                new Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        mLogo = bitmap;
                        onLogoFetchComplete();
                    }
                }, mLogoSize, mLogoSize, Bitmap.Config.ARGB_8888, this);
        logoReq.setTag(TAG);

        ImageRequest oppLogoReq = new ImageRequest(mOppLogoUrl.getText().toString(),
                new Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        mOppLogo = bitmap;
                        onLogoFetchComplete();
                    }
                }, mLogoSize, mLogoSize, Bitmap.Config.ARGB_8888, this);
        oppLogoReq.setTag(TAG);

        Volley.getInstance(this).getRequestQueue().add(logoReq);
        Volley.getInstance(this).getRequestQueue().add(oppLogoReq);
    }

    private void onLogoFetchComplete() {
        if (mLogo == null || mOppLogo == null) {
            return;
        }
        League league = new League.Builder()
                .account_name(DEMO_ACCOUNT.name)
                .league_key(DEMO_LEAGUE_KEY)
                .build();
        Matchup matchup = new Matchup.Builder()
                .my_team(new Team.Builder()
                        .score(mScore.getText().toString())
                        .build())
                .opponent_team(new Team.Builder()
                        .name(mOppName.getText().toString())
                        .score(mOppScore.getText().toString())
                        .build())
                .build();
        PutDataRequest request = LeagueData.getUpdateRequest(DEMO_ACCOUNT, league, matchup,
                SyncAdapter.getLogoAssetForBitmap(mLogo, mLogoSize),
                SyncAdapter.getLogoAssetForBitmap(mOppLogo, mLogoSize), true);
        Wearable.DataApi
                .putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataItemResult>() {
                    @Override
                    public void onResult(DataItemResult dataItemResult) {
                        Toast.makeText(DemoActivity.this, "Demo league pushed", Toast.LENGTH_SHORT)
                                .show();
                    }
                });
        mLogo = null;
        mOppLogo = null;
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        FWLog.e(volleyError, "Error fetching logo");
        Toast.makeText(this, "Error fetching logo", Toast.LENGTH_SHORT).show();
    }
}
