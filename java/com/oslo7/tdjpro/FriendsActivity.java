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
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.firebase.auth.FirebaseAuth;
import com.oslo7.tdjpro.db.User;
import java.util.ArrayList;

public class FriendsActivity extends AppCompatActivity implements
        dbSingleton.DBInterface,
        dbSingleton.DBInterfaceFriends,
        AdapterView.OnItemClickListener {

    private static final String TAG = "FriendsActivity";
    private Keystore store;
    String mUID = null;
    User mUser;
    FirebaseAuth mAuth;

    ArrayAdapter<User> listAdapter = null;
    ArrayList<User> items = new ArrayList<User>();
    ListView listview = null;

    private View mProgressView;
    private View mView;

    boolean mNewUser = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        mView = findViewById(android.R.id.content);

        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setFinishOnTouchOutside(false);

        mAuth = FirebaseAuth.getInstance();

        store = Keystore.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        mNewUser = store.getBoolean("NEWUSER_FRIENDS", true);

        mProgressView = findViewById(R.id.progress);
        listview = findViewById(R.id.friends);

        ImageButton buttonAdd = findViewById(R.id.add);
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addFriend();
            }
        });

        getUser();

    }

    public void setupView() {
        mView.invalidate();
        getUser();
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser(User user) {
        dbSingleton.getInstance().updateUser(user, mUID, this, true);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch (retCode) {
                case constant.DB_GET_USER:
                    if(result==constant.DB_RESULT_OK) {
                        mUser = user;
                        getFriends();
                    } else
                        finishWithResult();
                    break;
                case constant.DB_UPD_USER:
                    setupView();
                    break;

        }
    }

    public void addFriend() {
        Intent intent = new Intent(this, NewFriendActivity.class);
        intent.putExtra("UID", mUID);
        startActivityForResult(intent, 20);
    }


    public void inviteUser() {
        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.invitation_title))
                .setMessage(getString(R.string.invitation_message))
                .setDeepLink(Uri.parse(getString(R.string.invitation_deep_link)))
                .setCustomImage(Uri.parse(getString(R.string.invitation_custom_image)))
                .setCallToActionText(getString(R.string.invitation_cta))
                .build();
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        switch(requestCode){
            case 10:
                if (resultCode == RESULT_OK) {
                    // Get the invitation IDs of all sent messages
                    String[] ids = AppInviteInvitation.getInvitationIds(resultCode, data);
                    for (String id : ids) {
                        Toast.makeText(getBaseContext(), getString(R.string.invitation_sent), Toast.LENGTH_LONG).show();
                    }
                } else if(!(resultCode == RESULT_CANCELED)) {
                    Toast.makeText(getBaseContext(), getString(R.string.invitation_not_sent), Toast.LENGTH_LONG).show();
                }
                break;
            case 20:
                break;
        }
        setupView();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finishWithResult();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void finishWithResult() {
        Intent intent = getIntent();
        setResult(RESULT_OK, intent);
        finish();
    }

    public void getFriends() {
        dbSingleton.getInstance().getFriends(mUser.getFriends(), this);
    }

    public void onFriendsCompleted(int result, int retCode, ArrayList<User> friends)  {

        items.clear();

        for (User friend : friends)
            items.add(friend);

        setAdapater();
    }


    public void setAdapater() {

        FriendUsers itemsAdapter =
                new FriendUsers(this, items);

        listview.setAdapter(itemsAdapter);
        listview.setOnItemClickListener(this);
        itemsAdapter.notifyDataSetChanged();

    }

    public void onItemClick(AdapterView parent, View v, int position, long id) {
        verifyDelete(items.get(position));
    }

    public void verifyDelete(final User u) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));


        builder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.delete_friend) + " " +
                u.userD.getName() + "?";
        String warning = getResources().getString(R.string.friend_warning);
        builder.setMessage(question + "\n" + warning);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                u.removeFriend(mUser.getUid());
                u.removeAssignmentsGivenBy(mUser.getUid());
                mUser.removeFriend(u.getUid());
                mUser.removeAssignmentsGivenBy(u.getUid());
                dialog.dismiss();
                writeUser(u);
                writeUser(mUser);
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }


    public class FriendUsers extends ArrayAdapter<User> {

        private final Activity context;
        private final ArrayList<User> itemname;


        public FriendUsers(Activity context, ArrayList<User> itemname) {
            super(context, R.layout.friendlist, itemname);
            this.context = context;
            this.itemname = itemname;
        }

        @NonNull
        public View getView(final int position, View view, @NonNull ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();
            final ViewGroup p = parent;
            final int pos = position;

            // this may be a little creative
            View rowView = inflater.inflate(R.layout.friendlist, null, true);

            TextView txtName = rowView.findViewById(R.id.name);
            txtName.setText(itemname.get(position).userD.getName());


            ImageButton delButton = rowView.findViewById(R.id.delete);
            delButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((ListView) p).performItemClick(v, pos, 0); // Let the event be handled in onItemClick()
                }
            });

            return rowView;

        }
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }



}
