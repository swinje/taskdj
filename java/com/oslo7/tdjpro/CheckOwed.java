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

import com.oslo7.tdjpro.db.Assignment;
import com.oslo7.tdjpro.db.User;

import java.util.ArrayList;

public class CheckOwed implements
        dbSingleton.DBInterface {

    private static final String TAG = "CheckOwned";
    private Keystore store;
    String mUID;
    private String mAssigner;
    User mUser;
    float balance = 0;
    private Object mCallback;

    public interface OwedInterface {
        void onOwedCompleted(float value);
    }

    public CheckOwed(Object callback, String mU, String mA) {
        mCallback = callback;
        mUID = mU;
        mAssigner = mA;
        getUser();
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        mUser = user;
        balance = getOwed(mAssigner);
        ((OwedInterface) mCallback).onOwedCompleted(balance);
    }

    public float getOwed(String aid) {

        if(mUser==null)
            return 0;

        float sumApproved = 0;

        ArrayList<Assignment> ITEMS = new ArrayList<>();
        ITEMS.addAll(mUser.getAssignments());

        for(Assignment a : ITEMS) {
            if(a.getStatus()==constant.STATUS_APPROVED)
                if(a.getAssignedBy().equals(aid))
                    sumApproved += a.getValue();
        }

        return sumApproved;
    }

}
