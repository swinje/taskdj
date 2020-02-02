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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.oslo7.tdjpro.db.User;

import java.util.Arrays;

import static android.view.View.GONE;


public class CloseAccountActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    private static final String TAG = "CloseAccount";
    String mUID = null;
    User mUser;
    private Keystore store;
    private Context context;
    private static final int RC_SIGN_IN = 100;
    FirebaseAuth mAuth;
    Button buttonOK;
    Button buttonCancel;
    private View mProgressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_close_account);
        context = this;

        mProgressView = findViewById(R.id.progress);


        mAuth = FirebaseAuth.getInstance();

        buttonOK = findViewById(R.id.closeOK);
        buttonCancel = findViewById(R.id.closeCancel);


        store = Keystore.getInstance(this);

        mUID = store.get("UID");

        Bundle b = getIntent().getExtras();
        if (b != null)
            mUID = b.getString("UID");

        if(mUID == null)
            finish();

        getUser();
    }

    public void closeCancel(View v) {
        finishWithResult(false);
    }

    public void closeOK(View v) {
        signInAgain();
    }

    void signInAgain() {
        buttonOK.setVisibility(GONE);
        buttonCancel.setVisibility(GONE);
        findViewById(R.id.again).setVisibility(GONE);
        showProgress(true);
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG /* credentials */, true /* hints */)
                        .setTheme(R.style.AppTheme_NoActionBar)
                        .setAvailableProviders(Arrays.asList(
                                new AuthUI.IdpConfig.EmailBuilder().build(),
                                new AuthUI.IdpConfig.GoogleBuilder().build()))
                        .setTosUrl(BuildConfig.terms)
                        .setPrivacyPolicyUrl(BuildConfig.privacy)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                dbSingleton.getInstance().deleteUser(mUser, this);
            }
            else
                finishWithResult(false);
        }
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {
        switch (retCode) {
            case constant.DB_GET_USER:
                mUser = user;
                break;
            case constant.DB_DEL_USER:
                removeFBuser();
                break;
        }
    }

    public void removeFBuser() {

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();

        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    store.clear();
                    showMessage();
                } else {
                    Log.e(TAG, "Failed to delete FB User");
                    showLoginAgain();
                }
            }
        });
    }



    private void deleteAccount() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        final FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    store.clear();
                    showMessage();
                } else {
                    Log.e(TAG, "Delete Firebase account failed!");
                    showLoginAgain();
                }
            }
        });
    }

    private void showMessage() {
        if(!((Activity) context).isFinishing()) {
            showProgress(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            builder.setTitle(getResources().getString(R.string.app_name));
            builder.setMessage(getResources().getString(R.string.goodbye));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(context, MainActivity.class);
                    setResult(RESULT_OK, intent);
                    startActivity(intent);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }

    private void showLoginAgain() {
        if(!((Activity) context).isFinishing()) {
            showProgress(false);
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
            builder.setTitle(getResources().getString(R.string.app_name));
            builder.setMessage(getResources().getString(R.string.login_again));
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(context, MainActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    startActivity(intent);
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }


    private void finishWithResult(boolean ok) {
        showProgress(false);
        Intent intent = new Intent();
        if(ok) {
            setResult(RESULT_OK, intent);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : GONE);
        }
    }



}
