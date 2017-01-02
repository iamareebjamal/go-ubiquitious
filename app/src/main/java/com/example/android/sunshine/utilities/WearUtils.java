package com.example.android.sunshine.utilities;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.sunshine.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class WearUtils {

    private static final String TAG = "WearUtils";

    private static final String[] WEATHER_WEAR_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
    };

    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;

    private static final String
            WEATHER_ID = "id",
            MIN_TEMP   = "min",
            MAX_TEMP   = "max";

    public static void notifyWearDevice(Context context) {
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();

        mGoogleApiClient.blockingConnect();

        if(!mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Not Connected");
            return;
        }

        Uri todaysWeatherUri = WeatherContract.WeatherEntry
                .buildWeatherUriWithDate(SunshineDateUtils.normalizeDate(System.currentTimeMillis()));

        Cursor todayWeatherCursor = context.getContentResolver().query(
                todaysWeatherUri,
                WEATHER_WEAR_PROJECTION,
                null,
                null,
                null);

        if (todayWeatherCursor!=null && todayWeatherCursor.moveToFirst()) {

            int weatherId = todayWeatherCursor.getInt(INDEX_WEATHER_ID);
            double high = todayWeatherCursor.getDouble(INDEX_MAX_TEMP);
            double low = todayWeatherCursor.getDouble(INDEX_MIN_TEMP);

            String tempHigh = SunshineWeatherUtils.formatTemperature(context, high);
            String tempLow = SunshineWeatherUtils.formatTemperature(context, low);

            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/weather");
            putDataMapReq.getDataMap().putInt(WEATHER_ID, weatherId);
            putDataMapReq.getDataMap().putString(MAX_TEMP, tempHigh);
            putDataMapReq.getDataMap().putString(MIN_TEMP, tempLow);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
            pendingResult.setResultCallback(new ResultCallbacks<DataApi.DataItemResult>() {
                @Override
                public void onSuccess(@NonNull DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "Data stored!");
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    Log.d(TAG, "Data store failed!");
                }
            });

        }

        if(todayWeatherCursor!=null) todayWeatherCursor.close();
    }

}
