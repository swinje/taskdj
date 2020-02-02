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

package com.oslo7.tdjpro.db;

import java.util.HashMap;
import java.util.Map;

public class Friend {

        private static final String TAG = "Friend";
        String uid;
        String name;
        float balance;
        boolean manager;

        public Friend() { }

        public Friend(String uid, String name) {
            this.uid = uid;
            this.name = name;
            this.balance = 0;
            this.manager = false;
        }

        public Friend(String uid, String name, boolean manager) {
            this.uid = uid;
            this.name = name;
            this.balance = 0;
            this.manager = manager;
        }

        public String getUid() {
            return uid;
        }
        public void setUid(String uid) {
            this.uid = uid;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public float getBalance() {
            return balance;
        }
        public void setManager(boolean manager) {
        this.manager = manager;
    }
        public boolean getManager() {
        return manager;
    }
        public void setBalance(float balance) { this.balance = balance; }
        public void addBalance(float value) { this.balance += value;}
        public void subtractBalance(float value) { this.balance -= value;}

        public String toString() {
            return this.uid + "," + this.name + "," + this.balance;
        }

        @Override
        public boolean equals(Object object) {
            boolean result = false;
            if (object == null || object.getClass() != getClass()) {
                result = false;
            } else {
                Friend f = (Friend) object;
                if (this.uid.equals(f.getUid())) {
                    result = true;
                }
            }
            return result;
        }


    public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();

            result.put("uid", uid);
            result.put("name", name);
            result.put("balance", balance);
            result.put("manager", manager);

            return result;
        }
}
