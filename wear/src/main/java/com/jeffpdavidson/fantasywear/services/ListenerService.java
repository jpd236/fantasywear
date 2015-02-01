package com.jeffpdavidson.fantasywear.services;

import android.app.Notification;
import android.app.Notification.WearableExtender;
import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Shader;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi.GetFdForAssetResult;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.protocol.LeagueData;
import com.jeffpdavidson.fantasywear.protocol.Paths;
import com.jeffpdavidson.fantasywear.util.Constants;
import com.jeffpdavidson.fantasywear.util.MessageApiUtil;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Wearable listener service to show notifications for score updates. */
public class ListenerService extends WearableListenerService {
    private static final int NOTIFICATION_ID = 1;

    private GoogleApiClient mGoogleApiClient;
    private NotificationManager mNotificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        FWLog.d("Running ListenerService.onDataChanged");
        List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();

        if (!mGoogleApiClient.isConnected()) {
            ConnectionResult result = mGoogleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!result.isSuccess()) {
                FWLog.e("Failed to connect to GoogleApiClient");
                return;
            }
        }

        for (DataEvent event : events) {
            String tag;
            if ((tag = LeagueData.getTagIfMatches(event.getDataItem().getUri())) != null) {
                switch (event.getType()) {
                    case DataEvent.TYPE_CHANGED:
                        FWLog.d("Got league change event, tag = %s", tag);
                        // Generate the background image from the team logos.
                        DataMap dataMap =
                                DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                        if (LeagueData.isManualSync(dataMap)) {
                            // Send an ACK back to connected devices for manual syncs.
                            try {
                                MessageApiUtil.sendMessage(mGoogleApiClient, Paths.ACK,
                                        event.getDataItem().getUri().toString().getBytes("UTF-8"));
                            } catch (UnsupportedEncodingException e) {
                                throw new IllegalStateException(
                                        "UTF-8 must be a supported encoding", e);
                            }
                        }

                        Asset logo = LeagueData.getLogo(dataMap);
                        Asset oppLogo = LeagueData.getOpponentLogo(dataMap);
                        Bitmap background = getBitmapForLogos(logo, oppLogo);
                        if (background == null) {
                            return;
                        }

                        // Build a notification containing the score info.
                        Matchup matchup = LeagueData.getMatchup(dataMap);
                        String score = matchup.my_team.score;
                        String opponentScore = matchup.opponent_team.score;
                        String opponentName = matchup.opponent_team.name;
                        Notification.Builder notificationBuilder = new Notification.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle(
                                        getString(R.string.score_title, score, opponentScore));
                        float scoreValue = Float.parseFloat(score);
                        float opponentScoreValue = Float.parseFloat(opponentScore);
                        if (scoreValue == opponentScoreValue) {
                            notificationBuilder.setContentText(
                                    getString(R.string.score_tied_with, opponentName));
                        } else if (scoreValue > opponentScoreValue) {
                            notificationBuilder.setContentText(
                                    getString(R.string.score_beating, opponentName));
                        } else {
                            notificationBuilder.setContentText(
                                    getString(R.string.score_losing_to, opponentName));
                        }
                        notificationBuilder.extend(new WearableExtender()
                                .setHintHideIcon(true)
                                .setBackground(background));

                        // Update the notification for this league on the device.
                        mNotificationManager.notify(tag, NOTIFICATION_ID,
                                notificationBuilder.build());
                        break;
                    case DataEvent.TYPE_DELETED:
                        FWLog.d("Got league deleted event, tag = %s", tag);
                        mNotificationManager.cancel(tag, NOTIFICATION_ID);
                        break;
                    default:
                        FWLog.e("Unknown data event type: %s", event.getType());
                }
            } else {
                FWLog.e("Unrecognized URI: %s", event.getDataItem().getUri());
            }
        }
    }

    @Nullable
    private Bitmap getBitmapForLogos(Asset logoAsset, Asset oppLogoAsset) {
        PendingResult<GetFdForAssetResult> logoPendingResult =
                Wearable.DataApi.getFdForAsset(mGoogleApiClient, logoAsset);
        PendingResult<GetFdForAssetResult> oppLogoPendingResult =
                Wearable.DataApi.getFdForAsset(mGoogleApiClient, oppLogoAsset);
        GetFdForAssetResult logoResult =
                logoPendingResult.await(Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        GetFdForAssetResult oppLogoResult =
                oppLogoPendingResult.await(
                        Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!logoResult.getStatus().isSuccess() || !oppLogoResult.getStatus().isSuccess()) {
            FWLog.e("Failed to get FD for bitmap assets, logo status = %s, opponent status = %s",
                    logoResult.getStatus(), oppLogoResult.getStatus());
            return null;
        }

        int backgroundWidth = getResources().getDimensionPixelSize(R.dimen.background_width);
        int backgroundHeight = getResources().getDimensionPixelSize(R.dimen.background_height);
        int logoPaddingHorizontal =
                getResources().getDimensionPixelSize(R.dimen.logo_padding_horizontal);
        int logoPaddingTop = getResources().getDimensionPixelSize(R.dimen.logo_padding_top);

        // Prepare the canvas and fill with the background color.
        Bitmap composedBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight,
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(composedBitmap);
        canvas.drawColor(getResources().getColor(R.color.background));

        int logoBackground = getResources().getColor(R.color.logo_background);
        LinearGradient whiteShader = new LinearGradient(
                0, 0, 1, 1, logoBackground, logoBackground, Shader.TileMode.REPEAT);

        // Draw the two logos onto the canvas.
        Bitmap logoBitmap = BitmapFactory.decodeStream(logoResult.getInputStream());
        drawLogo(canvas, whiteShader, logoBitmap, logoPaddingHorizontal, logoPaddingTop);
        Bitmap oppLogoBitmap = BitmapFactory.decodeStream(oppLogoResult.getInputStream());
        drawLogo(canvas, whiteShader, oppLogoBitmap,
                backgroundWidth - logoPaddingHorizontal - oppLogoBitmap.getHeight(),
                logoPaddingTop);

        return composedBitmap;
    }

    private static void drawLogo(Canvas canvas, LinearGradient whiteShader, Bitmap logoBitmap,
            int x, int y) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        BitmapShader logoShader =
                new BitmapShader(logoBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix matrix = new Matrix();
        matrix.preTranslate(x, y);
        logoShader.setLocalMatrix(matrix);
        paint.setShader(new ComposeShader(whiteShader, logoShader, PorterDuff.Mode.SRC_OVER));
        RectF rect = new RectF(x, y, x + logoBitmap.getWidth(), y + logoBitmap.getHeight());
        canvas.drawRoundRect(rect, logoBitmap.getWidth() / 2, logoBitmap.getHeight() / 2, paint);
    }
}
