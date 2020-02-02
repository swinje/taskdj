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
import android.content.Intent;
import android.os.Bundle;
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
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.oslo7.tdjpro.db.User;

import java.util.ArrayList;

public class ReassignActivity extends AppCompatActivity implements
        dbSingleton.DBInterface,
        dbSingleton.DBInterfaceFriends,
        AdapterView.OnItemClickListener {

    private static final String TAG = "ReassignActivity";
    User mUser;
    String mToUID;
    private View mView;
    private String mAssigner = null;
    private String mSelf = null;
    ArrayAdapter<User> listAdapter = null;
    ArrayList<User> items = new ArrayList<User>();
    ListView listview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reassign);
        mView = findViewById(android.R.id.content);

        Toolbar toolbar =  findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mAssigner = extras.getString("ASSIGNER");
            mSelf = extras.getString("UID");
        }
        listview = findViewById(R.id.friends);
        getUser();
    }

    public void setupView() {
        mView.invalidate();
        getUser();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finishWithResult(false);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mAssigner, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch (retCode) {
            case constant.DB_GET_USER:
                if(result==constant.DB_RESULT_OK) {
                    mUser = user;
                    getFriends();
                } else
                    finishWithResult(false);
                break;
            case constant.DB_UPD_USER:
                setupView();
                break;

        }
    }

    public void getFriends() {
        dbSingleton.getInstance().getFriends(mUser.getFriends(), this);
    }

    public void onFriendsCompleted(int result, int retCode, ArrayList<User> friends)  {

        items.clear();

        items.add(mUser);

        for (User friend : friends) {
            items.add(friend);
        }

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
        mToUID= items.get(position).getUid();

        if(mToUID.equals(mSelf)) {
            Toast.makeText(this, getResources().getString(R.string.not_move_self), Toast.LENGTH_LONG).show();
            mToUID = null;
            return;
        }


        finishWithResult(true);
    }

    public void finishWithResult(boolean retStatus) {
        Intent intent = getIntent();
        if(retStatus) {
            setResult(RESULT_OK, intent);
            intent.putExtra("TO_UID", mToUID);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
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

            // this may be a little creative
            View rowView = inflater.inflate(R.layout.friendlist, null, true);

            TextView txtName = rowView.findViewById(R.id.name);
            txtName.setText(itemname.get(position).userD.getName());


            ImageButton delButton = rowView.findViewById(R.id.delete);
            delButton.setVisibility(View.GONE);

            return rowView;

        }
    }


}
