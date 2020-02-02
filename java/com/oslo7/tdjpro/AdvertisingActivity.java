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

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.Purchase;
import com.google.ads.consent.ConsentForm;
import com.google.ads.consent.ConsentFormListener;
import com.google.ads.consent.ConsentStatus;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.oslo7.tdjpro.Billing.BillingManager;
import com.oslo7.tdjpro.Billing.BillingProvider;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static com.oslo7.tdjpro.Billing.BillingManager.BILLING_MANAGER_NOT_INITIALIZED;

public class AdvertisingActivity extends AppCompatActivity implements
        BillingProvider,
        MySnack.SnackInterface {

    private static final String TAG = "Advertising";
    private ConsentForm form;
    boolean mNoConsent = false;

    private BillingManager mBillingManager;
    private final UpdateListener mUpdateListener = new UpdateListener();
    boolean mBillingManagerInSetup = true;
    private boolean mAdFree = false;

    private Keystore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advertising);

        // store for persistent variables
        store = Keystore.getInstance(this);

        Log.d(TAG, "Initiating Billingmanager");
        mBillingManager = new BillingManager(this, mUpdateListener);

        Bundle extras = getIntent().getExtras();
        boolean buy = false;
        if (extras != null) {
            buy = extras.getBoolean("BUY");
        }

        if(buy)
            showRemoveAds();
        else
            getAdvertisingConsent();

    }

    public void getAdvertisingConsent () {

        URL privacyUrl = null;

        try {
            privacyUrl = new URL(getResources().getString(R.string.url_privacy));
        } catch (MalformedURLException e) {
            e.printStackTrace();
            // Handle error.
        }
        form = new ConsentForm.Builder(this, privacyUrl)
                .withListener(new ConsentFormListener() {
                    @Override
                    public void onConsentFormLoaded() {
                        // Consent form loaded successfully.
                        showForm();
                    }

                    @Override
                    public void onConsentFormOpened() {
                        // Consent form was displayed.
                    }

                    @Override
                    public void onConsentFormClosed(
                            ConsentStatus consentStatus, Boolean userPrefersAdFree) {
                        // Consent form was closed.
                        manageConsent(consentStatus, userPrefersAdFree);
                    }

                    @Override
                    public void onConsentFormError(String errorDescription) {
                        // Consent form error.
                        Log.e(TAG, "consent form error " + errorDescription);
                    }

                })
                .withPersonalizedAdsOption()
                .withNonPersonalizedAdsOption()
                .withAdFreeOption()
                .build();

        form.load();

    }

    public void showForm() {
        form.show();
    }

    public void manageConsent(ConsentStatus consentStatus, Boolean userPrefersAdFree) {
        if (userPrefersAdFree) {
            mNoConsent = true;
            showRemoveAds();
            return;
        }
        if(consentStatus== ConsentStatus.NON_PERSONALIZED) {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");

            AdRequest request = new AdRequest.Builder()
                    .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                    .build();
        }

        finishWithResult(false);
    }

    public void showRemoveAds() {
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        builder.setTitle(getResources().getString(R.string.upgrade))
                .setMessage(getResources().getString(R.string.press_to_purchase))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        purchase();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishWithResult(false);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // BILLING

    public void purchase() {
        if (mBillingManager != null
                && mBillingManager.getBillingClientResponseCode()
                > BILLING_MANAGER_NOT_INITIALIZED) {
            mBillingManager.initiatePurchaseFlow("adfree", null, BillingClient.SkuType.INAPP);
        }
    }

    @Override
    public BillingManager getBillingManager() {
        return mBillingManager;
    }

    public void getPurchases() {
        mBillingManager.queryPurchases();
    }

    @Override
    public boolean isAdFreePurchased() {
        return mAdFree;
    }

    void onBillingManagerSetupFinished() {
        Log.d(TAG, "Billing manager Setup Finished");
        mBillingManagerInSetup = false;
        getPurchases();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying helper.");
        if (mBillingManager != null) {
            mBillingManager.destroy();
        }
        super.onDestroy();
    }

    public void showFailedPurchase() {
        (findViewById(R.id.question)).setVisibility(View.VISIBLE);
        MySnack ms = new MySnack(findViewById(R.id.myCoordinatorLayout));
        ms.MakeSnack(getResources().getString(R.string.purchase_failed), this);
    }
    public void onSnackCompleted() {
        finishWithResult(false);
    }

    private class UpdateListener implements BillingManager.BillingUpdatesListener {
        @Override
        public void onBillingClientSetupFinished() {
            onBillingManagerSetupFinished();
        }

        @Override
        public void onConsumeFinished(String token, @BillingClient.BillingResponse int result) {
            Log.d(TAG, "Consumption finished. Purchase token: " + token + ", result: " + result);

            if (result == BillingClient.BillingResponse.OK) {
                // Successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
                // update database
                store.putBoolean("ADFREE", true);
                finishWithResult(true);

            } else {
                Log.e(TAG, R.string.purchase_failed + " " + result);
                showFailedPurchase();
            }

            Log.d(TAG, "End consumption flow.");

        }


        public void onConsumeFailed(int result) {
            finishWithResult(false);
        }

        @Override
        public void onPurchasesUpdated(List<Purchase> purchaseList) {

            Log.d(TAG, "onPurchasesUpdate");

            for (Purchase purchase : purchaseList) {
                switch (purchase.getSku()) {
                    case "adfree":
                        mAdFree = true;
                        Log.d(TAG, "AdFree");
                        break;
                    default:
                        Log.d(TAG, "some other " + purchase.getSku());
                }
            }
        }
    }

    public void finishWithResult(boolean didPurchase) {
        Intent intent = getIntent();
        intent.putExtra("PURCHASED", didPurchase);
        setResult(RESULT_OK, intent);
        finish();
    }


}
