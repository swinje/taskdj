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


import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Keystore {
    private static Keystore store;
    private SharedPreferences SP;
    private static String filename = "TaskDJKeys";

    private Keystore(Context context) {
        SP = context.getApplicationContext().getSharedPreferences(filename, 0);
    }

    public static Keystore getInstance(Context context) {
        if (store == null) {
            store = new Keystore(context);
        }
        return store;
    }

    public void put(String key, String value) {
        Editor editor;
        editor = SP.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String get(String key) {
        return SP.getString(key, null);
    }

    public boolean getBoolean(String key) {
        return SP.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defval) {
        return SP.getBoolean(key, defval);
    }

    public int getInt(String key) {
        return SP.getInt(key, -1);
    }

    public long getLong(String key) {
        return SP.getLong(key, -1);
    }

    public void putInt(String key, int num) {
        Editor editor;
        editor = SP.edit();

        editor.putInt(key, num);
        editor.apply();
    }

    public void putLong(String key, long num) {
        Editor editor;
        editor = SP.edit();

        editor.putLong(key, num);
        editor.apply();
    }

    public void putBoolean(String key, boolean value) {
        Editor editor;
        editor = SP.edit();

        editor.putBoolean(key, value);
        editor.apply();
    }


    public void clear() {
        Editor editor;
        editor = SP.edit();

        editor.clear();
        editor.apply();
    }

    public void remove() {
        Editor editor;
        editor = SP.edit();

        editor.remove(filename);
        editor.apply();
    }
}
