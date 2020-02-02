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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.iid.FirebaseInstanceId;
import com.oslo7.tdjpro.db.User;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static android.view.View.GONE;

public class SignInActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {


    private static final String TAG = "SignIn";
    private static final int RC_SIGN_IN = 100;
    Button signInButton;
    FirebaseAuth mAuth;
    private Keystore store;
    View mView = null;
    boolean mNewUser = false;
    boolean mLoggedIn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mView = findViewById(android.R.id.content);

        // store for persistent variables
        store = Keystore.getInstance(this);

        mAuth = FirebaseAuth.getInstance();

        if (mAuth.getCurrentUser()!= null && mAuth.getCurrentUser().getUid() !=null) {
            mView.setVisibility(GONE);
            mLoggedIn = true;
            getUser();
        }

        if(!mLoggedIn) {

            signInButton = findViewById(R.id.signInButton);
            signInButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mView.setVisibility(GONE);
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
            });

            if(store.getBoolean("VERIFY"))
                (findViewById(R.id.resendCode)).setVisibility(View.VISIBLE);

        }

    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Logout();
    }

    public void Logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            // Successfully signed in
            if (resultCode == RESULT_OK) {
                final FirebaseUser currentUser = mAuth.getCurrentUser();

                if(null != currentUser) {
                    boolean verified_mail = true;
                    boolean mail_signed_in = false;

                    for (UserInfo user: FirebaseAuth.getInstance().getCurrentUser().getProviderData()) {
                        if (user.getProviderId().equals("password")) {
                            mail_signed_in = true;
                        }
                    }

                    if (mail_signed_in) {
                        if (!currentUser.isEmailVerified()) {
                            sendVerificationEmail();
                            verified_mail = false;
                        }
                    }
                    if(verified_mail) {
                        store.putBoolean("VERIFY", false);
                        getUser();
                    } else
                        return;
                }
            } else {
                mView.setVisibility(View.VISIBLE);
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    showSnackbar(R.string.sign_in_cancelled);
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection);
                    return;
                }

                //showSnackbar(R.string.unknown_error);
                Log.e(TAG, "Sign-in error: ", response.getError());
                finish();
            }
        }
    }

    public void resendVerification(View v) {
        MySnack ms = new MySnack(findViewById(R.id.myCoordinatorLayout));
        ms.MakeSnack(getResources().getString(R.string.mail_sent), new MySnack.SnackInterface() {
            @Override
            public void onSnackCompleted() {
            }
        });
    }

    private void sendVerificationEmail()
    {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        user.sendEmailVerification()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            store.putBoolean("VERIFY", true);
                            Toast.makeText(getApplicationContext(),
                                    getResources().getString(R.string.verify_mail),
                                    Toast.LENGTH_LONG).show();
                            Logout();
                        }
                        else
                        {
                            // email not sent
                            Logout();
                        }
                    }
                });
    }




    public void showSnackbar(int msgid) {

        String msg = getResources().getString(msgid);

        Snackbar snackbar = Snackbar
                .make(getWindow().getDecorView().getRootView(), msg, Snackbar.LENGTH_LONG);

        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
        snackbar.show();
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mAuth.getCurrentUser().getUid(), this);
    }

    public void writeUser(User u) {
        dbSingleton.getInstance().updateUser(u, u.getUid(),this, false);
    }

    public void addRecord() {
        String mail = mAuth.getCurrentUser().getEmail();
        byte[] data = mail.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        User u = new User(mAuth.getCurrentUser().getUid(), base64,  mAuth.getCurrentUser().getDisplayName().trim());
        u.userD.addToken(FirebaseInstanceId.getInstance().getToken());
        dbSingleton.getInstance().addUser(u, this);
    }


    public void onRequestCompleted(int result, int retCode, User u)  {

        if(result== constant.DB_RESULT_OK) {
            switch (retCode) {
                case constant.DB_ADD_USER:
                    mNewUser = true;
                    runTaskDJ(u);
                    break;
                case constant.DB_GET_USER:
                    u.userD.addToken(FirebaseInstanceId.getInstance().getToken());
                    store.putBoolean("ADFREE", u.userD.getAdFree());
                    writeUser(u);
                    break;
                case constant.DB_UPD_USER:
                    runTaskDJ(u);
                    break;
            }
            return;
        }

        // Must be logging into new device
        // Is OK since user is identified by Firebase (I hope)
        if(result==constant.DB_RESULT_EXISTS) {
            if(retCode==constant.DB_ADD_USER) {
                mNewUser = false;
                runTaskDJ(u);
            }
        }

        if(result==constant.DB_RESULT_NOT_FOUND) {
            if(mLoggedIn)
                removeFBuser();  // Error since user is supposed to be there
            else
                addRecord(); // New user

            return;
        }

        if(result==constant.DB_RESULT_ERROR) {
            // What to do now??
            Log.e(TAG, "Error in Firebase");
        }

    }

    public void runTaskDJ(User u) {

        store.putBoolean("NEWUSER_SELECT", mNewUser);

        Intent intent = new Intent(this, SelectCardViewActivity.class);
        intent.putExtra("UID", u.getUid());
        intent.putExtra("ASSIGNER_NAME", u.userD.getName());
        startActivity(intent);
        finish();

    }

    public void removeFBuser() {

        mLoggedIn = false;

        final FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final FirebaseUser user = mAuth.getCurrentUser();

        user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    finish();
                } else {
                    Log.e(TAG, "Delete Firebase account failed because user is not signed in" );
                    showSnackbar(R.string.user_failure);
                }
            }
        });

    }


}
