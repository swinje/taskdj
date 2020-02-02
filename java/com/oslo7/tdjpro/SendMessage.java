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

import com.oslo7.tdjpro.db.User;

public class SendMessage implements  dbSingleton.DBInterface {

    private String mMsg;
    private String mName;

    public SendMessage() { }

    public void sendUser(String uid, String name, String msg) {
        if(uid==null || msg==null || name==null)
            return;
        mMsg = msg;
        mName = name;
        dbSingleton.getInstance().getUser(uid, this);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        switch(retCode) {
            case constant.DB_GET_USER:
                dbSingleton.getInstance().sendMessage(mName + ": " + mMsg, user.userD.getTokens().get(0));
                // Should loop through tokens here
                break;
        }

    }

}
