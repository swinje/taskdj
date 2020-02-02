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
import android.os.Bundle;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import com.oslo7.tdjpro.db.Friend;
import com.oslo7.tdjpro.db.User;

import java.nio.charset.StandardCharsets;

public class NewFriendActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    private static final String TAG = "NewFriend";

    User mUser;
    String mUID = null;
    boolean mReceive = true;
    boolean mGive = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_friend);

        setFinishOnTouchOutside(false);

        final Button buttonOK = findViewById(R.id.buttonOK);
        AutoCompleteTextView email = findViewById(R.id.email);
        email.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    buttonOK.performClick();
                    return true;
                }
                return false;
            }
        });

        buttonOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CheckBox receive = findViewById(R.id.checkbox_receive);
                mReceive = receive.isChecked();
                CheckBox give = findViewById(R.id.checkbox_give);
                mGive = give.isChecked();
                EditText email = findViewById(R.id.email);
                if(email.length()==0)
                    email.setError(getResources().getString(R.string.too_short));
                else
                    friendAdd(email.getText().toString().trim());
            }
        });

        Button buttonCancel = findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithResult(false);
            }
        });


        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        getUser();

    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser(User user) {
        dbSingleton.getInstance().updateUser(user, mUID,this, true);
    }

    public void checkUser(String email) {
        byte[] data = email.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        dbSingleton.getInstance().checkUser(base64, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch (retCode) {
            case constant.DB_GET_USER:
                if(result==constant.DB_RESULT_OK) {
                    mUser = user;
                    if(user.getFriendsMax())
                        showFriendsMax();
                } else
                    finishWithResult(false);
                break;
            case constant.DB_EXIST_USER:
                if(result==constant.DB_RESULT_OK) {
                    makeFriends(user);
                } else
                    showNoUser();
                break;
            case constant.DB_UPD_USER:
                finishWithResult(true);
                break;

        }
    }

    public void friendAdd(String u) {
        if(u==null)
            return;

        byte[] data = u.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);

        if(!base64.equals(mUser.userD.getMail()))
            checkUser(u);
        else
            Toast.makeText(getBaseContext(), getString(R.string.not_add_self), Toast.LENGTH_LONG).show();
    }

    public void makeFriends(User user) {
        mUser.addFriend(new Friend(user.getUid(), user.userD.getName(), mReceive));
        writeUser(mUser);
        user.addFriend(new Friend(mUser.getUid(), mUser.userD.getName(), mGive));
        writeUser(user);
    }

    private void showNoUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.no_user));
        builder.setMessage(getString(R.string.user_create));
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishWithResult(false);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showFriendsMax() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.limit_reached));
        builder.setMessage(getString(R.string.limit_friends));
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishWithResult(false);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void finishWithResult(boolean result) {
        Intent intent = getIntent();
        if(result)
            setResult(RESULT_OK, intent);
        else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

}
