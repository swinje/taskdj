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

import androidx.appcompat.app.AppCompatActivity;

import com.oslo7.tdjpro.db.Assignment;
import com.oslo7.tdjpro.db.User;

public class adderActivity extends AppCompatActivity implements
        dbSingleton.DBInterface {

    String mUID;
    User mUser;
    String mAssigner;
    Assignment mAssignment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
            mAssigner = extras.getString("ASSIGNER");
            mAssignment = (Assignment) extras.getSerializable("Assignment");
        }
        getUser();
    }


    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        dbSingleton.getInstance().updateUser(mUser, mAssigner, this, true);
    }


    public void onRequestCompleted(int result, int retCode, User user) {
        if(retCode== constant.DB_GET_USER) {
            mUser = user;
            mUser.addAssignment(mAssignment);
            writeUser();

        }
        if(retCode==constant.DB_UPD_USER)
            finishWithResult(true);
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
