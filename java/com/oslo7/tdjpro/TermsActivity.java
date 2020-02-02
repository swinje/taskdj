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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.appcompat.app.AppCompatActivity;

import com.oslo7.tdjpro.db.User;

public class TermsActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    private static final String TAG = "Terms";
    String mUID = null;
    User mUser = null;
    boolean mAccepted = false;
    private Keystore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        setFinishOnTouchOutside(false);

        // store for persistent variables
        store = Keystore.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        if(mUID==null)
            mUID = store.get("UID");

        Button buttonOK=  findViewById(R.id.ok);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAccepted = ((CheckBox) findViewById(R.id.accepted)).isChecked();
                if(mAccepted) {
                    store.putBoolean("terms", true);
                    finishWithResult(true);
                }
                else
                    CloseAccount();

            }
        });

        getUser();

    }

    public void CloseAccount() {
        // Call close activity
        Intent intent = new Intent(this, CloseAccountActivity.class);
        intent.putExtra("UID", mUID);
        startActivity(intent);
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch(retCode) {
            case constant.DB_GET_USER:
                mUser = user;
                if(mUser==null)
                    finishWithResult(false);
                setupView();
                break;
        }

    }

    public void setupView() {
        if(mUser!=null) {
            mAccepted = mUser.userD.getAcceptedTerms();
            CheckBox acc = findViewById(R.id.accepted);
            acc.setChecked(mAccepted);
        }
    }

    public void terms(View v)  {
        startWebView("TERMS");
    }

    public void privacy(View v)  {
        startWebView("PRIVACY");
    }

    public void startWebView(String typeWeb) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("URL", typeWeb);
        startActivity(intent);
    }

    private void finishWithResult(boolean ok) {
        Intent intent = new Intent();
        if(ok) {
            setResult(RESULT_OK, intent);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

}
