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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.oslo7.tdjpro.db.User;

import java.util.ArrayList;

public class SettlementHistoryActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    private static final String TAG = "SettlementHistory";
    private Keystore store;
    String mUID = null;
    User mUser;
    FirebaseAuth mAuth;
    ArrayList<String> items = new ArrayList<String>();
    ListView listview = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settlement_history);

        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setFinishOnTouchOutside(false);

        mAuth = FirebaseAuth.getInstance();

        store = Keystore.getInstance(this);

        listview = findViewById(R.id.settlements);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        getUser();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.clear:
                clearLog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        dbSingleton.getInstance().updateUser(mUser, mUID,this, false);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch (retCode) {
            case constant.DB_GET_USER:
                if(result==constant.DB_RESULT_OK) {
                    mUser = user;
                    getHistory();
                }
                break;
            case constant.DB_UPD_USER:
                getUser();
                break;
        }
    }

    public void clearLog() {
        mUser.userD.deleteSettlements();
        writeUser();
    }


    public void getHistory() {
        items.clear();
        items = mUser.userD.getSettlements();
        setAdapter();
    }

    public void setAdapter() {

        HistoryItems itemsAdapter =
                new HistoryItems(this, items);

        listview.setAdapter(itemsAdapter);
        itemsAdapter.notifyDataSetChanged();

    }

    public class HistoryItems extends ArrayAdapter<String> {

        private final Activity context;
        private final ArrayList<String> itemname;


        public HistoryItems(Activity context, ArrayList<String> itemname) {
            super(context, R.layout.historylist, itemname);
            this.context = context;
            this.itemname = itemname;
        }

        @NonNull
        public View getView(final int position, View view, @NonNull ViewGroup parent) {
            LayoutInflater inflater = context.getLayoutInflater();

            // this may be a little creative
            View rowView = inflater.inflate(R.layout.historylist, null, true);

            TextView txtName = rowView.findViewById(R.id.item);
            txtName.setText(itemname.get(position));


            return rowView;

        }
    }





}
