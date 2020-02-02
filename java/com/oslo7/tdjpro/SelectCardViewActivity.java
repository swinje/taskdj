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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.transition.Fade;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.ads.consent.ConsentInfoUpdateListener;
import com.google.ads.consent.ConsentInformation;
import com.google.ads.consent.ConsentStatus;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.oslo7.tdjpro.db.DBLog;
import com.oslo7.tdjpro.db.User;
import java.util.ArrayList;
import static android.view.View.GONE;


public class SelectCardViewActivity extends AppCompatActivity implements
        dbSingleton.DBInterface,
        dbSingleton.DBInterfaceFriends,
        dbSingleton.LogInterface,
        MySnack.SnackInterface {

    private static final String TAG = "SelectCardView";
    private Keystore store;
    ArrayList<User> users = new ArrayList<User>();
    private String mUID = null;
    private User mAssigner = null;
    private String mAssignerName = null;
    public boolean mNewUser = false;
    RecyclerView mRv;
    private View mProgressView;
    private boolean mGetUsersInProgress = false;
    boolean mNoConsent = false;


    private AdView mAdView;
    private boolean mAdFree = false;


    final static int GET_SETTINGS = 70;
    final static int DO_TERMS = 80;
    final static int SCREEN_NAME = 60;
    final static int ADS = 99;

    int versionCode = BuildConfig.VERSION_CODE;
    String versionName = BuildConfig.VERSION_NAME;

    int oldCurrency = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_card_view);

        // store for persistent variables
        store = Keystore.getInstance(this);

        //String locale = getResources().getConfiguration().locale.getDisplayName();
        //DBLog.v(TAG, "locale " + locale);

        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);

        mProgressView = findViewById(R.id.progress);

        mRv = findViewById(R.id.rv);
        mRv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        mRv.setLayoutManager(llm);


        mRv.addOnItemTouchListener(new MyTouchListener(this, mRv,
                new MyTouchListener.OnTouchActionListener() {

                    @Override
                    public void onLeftSwipe(View view, int position) {
                        runIT(position);
                    }

                    @Override
                    public void onRightSwipe(View view, int position) {
                        runIT(position);
                    }


                    @Override
                    public void onClick(View view, int position) {
                        runIT(position);
                    }

                    @Override
                    public void onLongPress(View view, int position) {

                    }


                }));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
            store.put("UID", mUID);
            mAssignerName = extras.getString("ASSIGNER_NAME");
        }

        // Initialize database
        dbSingleton.getInstance().init(this);

        dbSingleton.getInstance().startLog(this); // Listen for changes

        mNewUser = store.getBoolean("NEWUSER_SELECT", true);

        mAdFree = store.getBoolean("ADFREE", false);

        displayAd();

        fixOrientation(getResources().getConfiguration().orientation);

        if (hasNet())
            getUsers();
        else
            noNet();

    }

    public void displayAd() {
        mAdView = findViewById(R.id.adView);
        if (!mAdFree) {
            verifyConsent();
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);

            AdRequest request = new AdRequest.Builder().addTestDevice("D946162BAD6418844980CE7DB6A73355").build();
        }
        else {
            mAdView.setVisibility(GONE);
        }
    }

    public boolean hasNet() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    public void noNet() {
        Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }


    public void verifyConsent() {
        final ConsentInformation consentInformation = ConsentInformation.getInstance(this);
        String[] publisherIds = {"pub-6216000655891991"};
        consentInformation.requestConsentInfoUpdate(publisherIds, new ConsentInfoUpdateListener() {
            @Override
            public void onConsentInfoUpdated(ConsentStatus consentStatus) {
                // User's consent status successfully updated.
                checkConsent(consentInformation, consentStatus);
            }

            @Override
            public void onFailedToUpdateConsentInfo(String errorDescription) {
                // User's consent status failed to update.

            }
        });
    }

    public void checkConsent(ConsentInformation ci, ConsentStatus cs) {
        if (ConsentInformation.getInstance(this).isRequestLocationInEeaOrUnknown()) {
            // EU
            if (cs == ConsentStatus.UNKNOWN) {
                ci.setTagForUnderAgeOfConsent(false);
                getAdvertisingConsent();
            }
        }
    }

    public void getAdvertisingConsent() {
        Intent intent = new Intent(this, AdvertisingActivity.class);
        startActivityForResult(intent, ADS);
    }


    public void showVersion() {
        MySnack ms = new MySnack(findViewById(android.R.id.content));
        ms.MakeFadeSnack(getResources().getString(R.string.version) + " " + versionName);
    }

    public void onLogChanged(DBLog l) {

        if (mAssigner == null)
            return;

        // uid is user or friend update
        if (mAssigner.isFriend(l.getUid()) || mAssigner.getUid().equals(l.getUid())) {
            getUsers();
        }

    }

    public void onAds() {
        Intent intent = new Intent(this, AdvertisingActivity.class);
        intent.putExtra("BUY", true);
        startActivityForResult(intent, ADS);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixOrientation(newConfig.orientation);
    }

    private void fixOrientation(int o) {
        //if (o == Configuration.ORIENTATION_LANDSCAPE) {
            (findViewById(R.id.rlayout)).setBackgroundColor(getResources().getColor(R.color.medium_green));
        //} else {
        //    (findViewById(R.id.rlayout)).setBackgroundColor(getResources().getColor(R.color.white));
        //}
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_select, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if(mAdFree)
            menu.findItem(R.id.adfree).setVisible(false);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.refresh:
                getUsers();
                break;
            case R.id.settings:
                doSettings();
                return true;
            case R.id.adfree:
                onAds();
                return true;
            case R.id.logout:
                Logout();
                return true;
            case R.id.version:
                showVersion();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void doSettings() {

        PreferenceManager.setDefaultValues(getApplicationContext(), R.xml.pref_main, false);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
        editor.putBoolean("notifications", true);
        editor.putString("screenID", mAssigner.userD.getName());
        //editor.putString("chosen_currency", Integer.toString(mAssigner.userD.getCurrency()));
        editor.putString("chosen_currency", store.get("CURRENCY"));
        editor.commit();


        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, GET_SETTINGS);
    }


    public void Logout() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            getWindow().setExitTransition(new Fade());
        } else {
            Toast toast = Toast.makeText(this, getResources().getString(R.string.logged_off), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.TOP, 0, 100);
            toast.show();
        }
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void startWebView(String typeWeb) {
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("URL", typeWeb);
        startActivity(intent);
    }

    public void onFriends(View anchor) {
        Intent intent = new Intent(this, FriendsActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, 10);
    }


    public void onReload(View anchor) {
        getUsers();
    }

    public void CloseAccount() {
        // Call close activity
        Intent intent = new Intent(this, CloseAccountActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, 100);
    }

    public void runIT(int position) {

        User u = ((RVAdapter) mRv.getAdapter()).getItem(position);

        if(u !=null)
        {
            if (u.getUid().equals(mUID))
                runUser(u.getUid());
            else
                runManager(u.getUid());
        }
    }

    public void runManager(String uid) {
        if(uid==null)
            return;

        Intent intent = new Intent(this, FriendCardViewActivity.class);
        intent.putExtra("UID", uid);
        intent.putExtra("ASSIGNER", mUID);
        intent.putExtra("ASSIGNER_NAME", mAssignerName);
        startActivityForResult(intent, 20);
    }

    public void runUser(String uid) {

        if(uid==null)
            return;

        Intent intent = new Intent(this, MyCardViewActivity.class);
        intent.putExtra("UID", uid);
        startActivityForResult(intent, 30);
    }

    public void doTerms() {
        Intent intent = new Intent(this, TermsActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, DO_TERMS);
    }

    public void newFriend() {
        Intent intent = new Intent(this, FriendsActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, 40);
    }

    public void remindSettings(View anchor) {
        MySnack ms2 = new MySnack(findViewById(R.id.myCoordinatorLayout));
        ms2.MakeFadeSnack(getResources().getString(R.string.get_paid));
        getUsers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch(requestCode) {
            case GET_SETTINGS:
                if(mAssigner==null)
                    break;
                getPreferences();
                boolean p1 = store.getBoolean("ADFREE", false);
                if(p1) {
                    mAdFree = true;
                    store.putBoolean("ADFREE", true);
                    displayAd();
                    mAssigner.userD.setAdFree(true);
                }
                writeUser();
            case DO_TERMS:
                getUsers();
                break;
            case ADS:
                Bundle extras = data.getExtras();
                boolean p2 = false;
                if (extras != null)
                    p2 = extras.getBoolean("PURCHASED", false);
                if(p2) {
                    mAdFree = true;
                    store.putBoolean("ADFREE", true);
                    displayAd();
                    mAssigner.userD.setAdFree(true);
                    writeUser();
                } else
                    getUsers();
                break;
            case 100:
                if(resultCode==RESULT_OK)
                    Logout();
                break;
            default:
                getUsers();
        }

    }

    public void switchToAdFree() {
        mAssigner.userD.setAdFree(true);
        mAdFree = true;
        store.putBoolean("ADFREE", true);
        displayAd();
    }

    public void getPreferences() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean notPref = sharedPref.getBoolean("notifications", true);
        //String screenPref = sharedPref.getString("screenID", "");
        String currPref = sharedPref.getString("chosen_currency", "0");
        store.put("CURRENCY", currPref);
        Boolean terms = store.getBoolean("terms", false);
        int currID = Integer.parseInt(currPref);
        mAssigner.userD.setNotifications(notPref);
        //mAssigner.userD.setName(screenPref);
        mAssigner.userD.setCurrency(currID);
        mAssigner.userD.setAcceptedTerms(terms);
    }

    private void getUsers() {
        //mSwipe.setRefreshing(false);
        if(mGetUsersInProgress)
            return;
        mGetUsersInProgress = true;
        showProgress(true);
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        dbSingleton.getInstance().updateUser(mAssigner, mUID,this, true);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        if(result == constant.DB_RESULT_OK) {
            switch (retCode) {
                case constant.DB_GET_USER:
                    if (user == null)
                        finish();
                    users.clear();
                    users.add(user);
                    mAssigner = user;
                    checkCurrency();
                    mAssignerName = user.userD.getName();
                    dbSingleton.getInstance().getFriends(user.getManagedFriends(), this);
                    break;
                case constant.DB_UPD_USER:
                    checkCurrency();
                    getUsers();
                    break;
            }
        } else
            finish();
    }

    public void checkCurrency() {
        if(mAssigner==null)
            return;

        if(oldCurrency != mAssigner.userD.getCurrency()) {
            oldCurrency = mAssigner.userD.getCurrency();
            if (oldCurrency > 0)
                (findViewById(R.id.dollar)).setVisibility(GONE);
            else
                (findViewById(R.id.dollar)).setVisibility(View.VISIBLE);
        }
    }

    public void onFriendsCompleted(int result, int retCode, ArrayList<User> friend_list) {
        for (User f : friend_list) {
            users.add(f);
        }

        showListView();

    } public void showListView() {
        RVAdapter adapter = new RVAdapter(users);
        mRv.setAdapter(adapter);

        showProgress(false);
        mGetUsersInProgress = false;


        if(mNewUser) {
            store.putBoolean("NEWUSER_SELECT", false);
            mNewUser = false;
            //loadDefaults = true;
            doSettings();
        }
        else
            if(store.getBoolean("terms", false)==false)
                remindTerms();

    }

    public void remindTerms() {
        MySnack ms = new MySnack(findViewById(android.R.id.content));
        ms.MakeSnack(getResources().getString(R.string.accept_terms), this);
    }

    public void onSnackCompleted() {
        doSettings();
    }


    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.UserViewHolder> {

        Resources res;

         public class UserViewHolder extends RecyclerView.ViewHolder   {

            CardView cv;
            GridLayout gl1;
            GridLayout gl2;
            TextView name;
            TextView countUser1;
            TextView countUser2;
            ImageView people;
            TextView status;

            UserViewHolder(View itemView) {
                super(itemView);
                cv = itemView.findViewById(R.id.cv);
                name = itemView.findViewById(R.id.name);
                countUser1 = itemView.findViewById(R.id.countUser1);
                countUser2 = itemView.findViewById(R.id.countUser2);
                people = itemView.findViewById(R.id.people);
                gl1 = itemView.findViewById(R.id.GL1);
                gl2 = itemView.findViewById(R.id.GL2);
                status = itemView.findViewById(R.id.status);
            }

        }

        private ArrayList<User> users;

        RVAdapter(ArrayList<User> users){
            this.users = users;
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public User getItem(int position) {
            if(position < 0 || position > users.size()-1)
                return null;
            return users.get(position);
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.user_card, viewGroup, false);
            UserViewHolder uvh = new UserViewHolder(v);
            res = uvh.itemView.getContext().getResources();
            return uvh;
        }

        public void setStatus(UserViewHolder userViewHolder, String txt) {
            userViewHolder.status.setVisibility(View.VISIBLE);
            userViewHolder.status.setText(txt);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder userViewHolder, int i) {

            userViewHolder.status.setVisibility(GONE);

            if(users.get(i).getUid().equals(mUID)) {
                userViewHolder.gl2.setVisibility(GONE);
                userViewHolder.gl1.setVisibility(View.VISIBLE);
                userViewHolder.name.setText(getResources().getText(R.string.my_assignments));
                ArrayList<Integer> aO = users.get(i).myAssignmentOverview();
                int sumCount  = aO.get(0) + aO.get(1) + aO.get(2) + aO.get(4) + aO.get(5); // Not discarded
                userViewHolder.countUser1.setText("("+ sumCount +")");
                userViewHolder.people.setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
                if(aO.get(4) >0)
                    setStatus(userViewHolder, res.getString(R.string.confirm_payment));
            } else {

                userViewHolder.name.setText(users.get(i).userD.getName());

                ArrayList<Integer> aO = users.get(i).assignmentOverview(mAssigner.getUid());
                int sumCount  = aO.get(0) + aO.get(1) + aO.get(2) + aO.get(3) + aO.get(4) + aO.get(5);

                if(aO.get(2) > 0) {  // Has overdu
                    userViewHolder.gl2.setVisibility(View.VISIBLE);
                    userViewHolder.gl1.setVisibility(GONE);
                    userViewHolder.countUser2.setText("("+ sumCount +")");
                } else {
                    userViewHolder.gl2.setVisibility(GONE);
                    userViewHolder.gl1.setVisibility(View.VISIBLE);
                    userViewHolder.countUser1.setText("("+ sumCount +")");
                }

                if(aO.get(1) >0)
                    setStatus(userViewHolder, res.getString(R.string.need_approvals));
                if(aO.get(3) >0)
                    setStatus(userViewHolder, res.getString(R.string.have_discarded));

            }
        }


        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

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

