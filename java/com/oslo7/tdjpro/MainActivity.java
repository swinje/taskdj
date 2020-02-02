/*
 * Copyright (C) 2019 Oslo7
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

package com.oslo7.tdjpro;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.ads.MobileAds;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.google.firebase.messaging.FirebaseMessaging;
import com.oslo7.tdjpro.Billing.StringXOR;
import io.fabric.sdk.android.Fabric;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private static final String BB = BuildConfig.BB;

    private static final byte[] SALT = BuildConfig.salt;



    private LicenseCheckerCallback mLicenseCheckerCallback;
    private LicenseChecker mChecker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fabric.with(this, new Crashlytics());

        MobileAds.initialize(this, BuildConfig.mobileAds);

        // Initialize database
        dbSingleton.getInstance().init(this);

        // messaging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId = getString(R.string.default_notification_channel_id);
            String channelName = getString(R.string.default_notification_channel_name);
            NotificationManager notificationManager =
                    getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel(channelId,
                    channelName, NotificationManager.IMPORTANCE_LOW));
        }

        // In theory we should now get news
        FirebaseMessaging.getInstance().subscribeToTopic(getString(R.string.default_notification_channel_name));


        boolean connected = false;
        boolean wifiState = false;
        boolean mobileState = false;

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);

                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
                    mobileState = true;
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
                    wifiState = true;
            }
        } catch (Exception e) {
        }


        if(connectivityManager.getActiveNetworkInfo().isConnected())
            connected = wifiState || mobileState;

        if (!connected) {
            Toast.makeText(MainActivity.this, getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        StringXOR stor = new StringXOR();
        String bb = stor.decode(BB, BuildConfig.decoder);


        mLicenseCheckerCallback = new MyLicenseCheckerCallback();

        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                bb);
        doCheck();

    }

    private void startSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    public void showMessage(String message) {
        MySnack ms = new MySnack(findViewById(android.R.id.content));
        ms.MakeSnack(message, new MySnack.SnackInterface() {
            @Override
            public void onSnackCompleted() {
                finish();
            }
        });
    }

    private void doCheck() {
        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    private class MyLicenseCheckerCallback implements LicenseCheckerCallback {


        public void allow(int policyReason) {
            if (isFinishing()) {
                return;
            }
            startSignIn();
        }

        public void dontAllow(int policyReason) {
            if (isFinishing()) {
                return;
            }
            if(!BuildConfig.DEBUG)
                showMessage(getResources().getString(R.string.no_license));
            else
                startSignIn();
        }

        public void applicationError(int errorCode) {
            if (isFinishing()) {
                return;
            }
            showMessage(getResources().getString(R.string.unknown_error) + " " + errorCode);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mChecker!=null)
            mChecker.onDestroy();
    }




}
