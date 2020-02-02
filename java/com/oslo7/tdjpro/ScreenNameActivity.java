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
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.oslo7.tdjpro.db.User;

public class ScreenNameActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    private static final String TAG = "ScreenName";
    String mUID = null;
    User mUser = null;
    String mOldName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_name);

        setFinishOnTouchOutside(false);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        Button buttonOK=  findViewById(R.id.ok);
        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText screenName = findViewById(R.id.screen_name);
                if (screenName.getText().toString().trim().length() < 6 &&
                        screenName.getText().toString().trim().length() != 0) {
                    screenName.setError(getResources().getString(R.string.too_short));
                    screenName.requestFocus();
                } else
                    checkUserName();
            }
        });

        getUser();

    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        dbSingleton.getInstance().updateUser(mUser, mUID,this, false);
    }

    public void checkUserName() {
        EditText screenName = findViewById(R.id.screen_name);
        String newName = screenName.getText().toString().trim();
        if(newName.length() > 0)
            mUser.userD.setName(newName);
        dbSingleton.getInstance().checkUserName(newName, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch(retCode) {
            case constant.DB_GET_USER:
                mUser = user;
                mOldName = mUser.userD.getName();
                setupView();
                break;
            case constant.DB_UPD_USER:
                finishWithResult(true);
                break;
            case constant.DB_EXIST_USER:
                if(result==constant.DB_RESULT_OK) {
                    EditText screenName = findViewById(R.id.screen_name);
                    screenName.setError(getString(R.string.name_in_use));
                }
                else
                    writeUser();
                break;
        }

    }

    public void setupView() {
        EditText screenName = findViewById(R.id.screen_name);
        screenName.setHint(mUser.userD.getName());
    }

    private void finishWithResult(boolean ok) {
        Intent intent = new Intent();
        if(ok)
            setResult(RESULT_OK, intent);
        else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

}
