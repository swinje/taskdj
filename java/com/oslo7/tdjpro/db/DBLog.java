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

public class DBLog {

    String uid;
    String aid;
    Long t;
    int changeType;


    public DBLog() { }

    public DBLog(String uid, String aid, Long t) {
        this.uid = uid;
        this.aid = aid;
        this.t = t;
        this.changeType = 0;
    }

    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }
    public String getAid() {
        return aid;
    }
    public void setAid(String aid) {
        this.aid = aid;
    }
    public Long getT() {
        return t;
    }
    public void setT(Long t) { this.t = t; }
    public int getChange() {
        return changeType;
    }
    public void setChange(int changeType) {
        this.changeType = changeType;
    }


    public String toString() {
        return this.uid + "," + this.aid + "," + this.t + "," + this.changeType;
    }

    public Map<String, Object> toMap() {
            HashMap<String, Object> result = new HashMap<>();

            result.put("uid", uid);
            result.put("aid", aid);
            result.put("t", t);
            result.put("changeType", changeType);

            return result;

    }

}
