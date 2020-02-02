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
import android.util.Log;

import androidx.annotation.NonNull;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.oslo7.tdjpro.db.DBLog;
import com.oslo7.tdjpro.db.Friend;
import com.oslo7.tdjpro.db.SenderMessage;
import com.oslo7.tdjpro.db.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by swinj on 28.09.2017.
 */


public class dbSingleton {
    private static final dbSingleton dbInstance = new dbSingleton();
    private FirebaseDatabase database = null;
    DatabaseReference dbRef = null;
    DatabaseReference userRef = null;
    DatabaseReference logRef = null;
    DatabaseReference messageRef = null;
    private Context context;
    private static final String TAG = "dbSingleton";

    public static dbSingleton getInstance() {
        return dbInstance;
    }

    private dbSingleton() {
    }


    public void init(Context context) {
        this.context = context.getApplicationContext();
        database = FirebaseDatabase.getInstance();
        /*if (BuildConfig.DEBUG) {
            dbRef = database.getReference("debug");
        } else*/
        dbRef = database.getReference();
        userRef = dbRef.child("users");
        logRef = dbRef.child("log");
        messageRef = dbRef.child("messages");
    }


    // ***************** MESSAGE *************************



    public void sendMessage(final String msg, final String dest) {


        if(msg==null || dest==null)
            return;

        messageRef.orderByKey().addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String key = null;
                        for (DataSnapshot msgSnapshot : dataSnapshot.getChildren()) {
                            key = msgSnapshot.getKey();
                            messageRef.child(key).removeValue();
                        }
                        reallySendMessage(msg, dest);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {

                    }
                });
    }

    public void sendUIDMessage(final String msg, final String uid) {

        if(msg==null || uid==null)
            return;

        userRef.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean user_found = false;

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            if(value.getUid().equals(uid)) {
                                user_found = true;
                                break;
                            }
                        }

                        if(user_found)
                            reallySendMessage(msg, value.userD.getTokens().get(0));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "error getting user: " + error);
                    }
                });
    }

    public void reallySendMessage(String m, String d) {
        messageRef.setValue(null);
        SenderMessage msg = new SenderMessage(m, d);
        messageRef.push();
        messageRef.setValue(msg);
    }


    // ***************** USERS *************************


    public interface DBInterface {
        void onRequestCompleted(int result, int retCode, User user);
    }

    public void addUser(User u, Object callback) {
        userExist(u, callback);
    }

    public void userExist(final User u, final Object callback) {

        userRef.orderByChild("uid").limitToLast(1).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean userFound = false;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            if (value.getUid().equalsIgnoreCase(u.getUid()))
                                userFound = true;
                            if(value.userD.getMail().equalsIgnoreCase(u.userD.getMail()))
                                userFound = true;
                        }

                        if (!userFound) {
                            makeDBUser(u, callback);
                        } else
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_EXISTS, constant.DB_ADD_USER, u);

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_ADD_USER, u);
                    }
                });
    }

    public void makeDBUser(User u, Object callback) {
        DatabaseReference newUserRef = userRef.push();
        newUserRef.setValue(u);
        logChange(u.getUid(), u.getUid(), constant.CHANGE_ADD, true);
        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_ADD_USER, u);
    }

    public void deleteUser(final User u, final Object callback) {

        if(u == null) {
            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_DEL_USER, u);
            return;
        }

        userRef.orderByChild("uid").equalTo(u.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String key;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            key = userSnapshot.getKey();
                            userRef.child(key).removeValue();
                        }
                        logChange(u.getUid(), u.getUid(), constant.CHANGE_DELETE, true);
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_DEL_USER, u);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Error delete user: " + error);
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_DEL_USER, u);
                    }
                });
    }

    public void updateUser(final User u, final String updater, final Object callback, final boolean logEvent) {
        userRef.orderByChild("uid").equalTo(u.getUid()).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String key = null;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            key = userSnapshot.getKey();
                        }

                        Map<String, Object> postUser;
                        postUser = u.toMap();
                        Map<String, Object> userUpdate = new HashMap<>();
                        userUpdate.put(key, postUser);
                        try {
                            userRef.updateChildren(userUpdate);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Crashlytics.log(userUpdate.toString());
                            Log.e(TAG, "update user children error: " + e);
                        }

                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_UPD_USER, u);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "update user error: " + error);
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_UPD_USER, u);

                    }
                });
    }


    public void getUser(final String uid, final Object callback) {

        // Why this would happen is interesting to understand
        // guessing firebase instance expires, and maybe need to refresh
        if(uid == null || userRef == null) {
            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_GET_USER, null);
            return;
        }

        userRef.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean user_found = false;

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            if(value.getUid().equals(uid)) {
                                user_found = true;
                                break;
                            }
                        }

                        if(!user_found)
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_NOT_FOUND, constant.DB_GET_USER, null);
                        else
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_GET_USER, value);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "error getting user: " + error);
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_GET_USER, null);
                    }
                });
    }

    public void mergeGet(final String uid, final Object callback) {

        // Why this would happen is interesting to understand
        // guessing firebase instance expires, and maybe need to refresh
        if(uid == null || userRef == null) {
            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_MERGE_USER, null);
            return;
        }

        userRef.orderByChild("uid").equalTo(uid).addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean user_found = false;

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            if(value.getUid().equals(uid)) {
                                user_found = true;
                                break;
                            }
                        }

                        if(!user_found)
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_NOT_FOUND, constant.DB_MERGE_USER, null);
                        else
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_MERGE_USER, value);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "error getting user: " + error);
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_MERGE_USER, null);
                    }
                });
    }

    public void checkUser(final String mail, final Object callback) {

        if(mail==null)
            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_EXIST_USER, null);

        userRef.orderByKey().addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean userFound = false;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            if (value.userD.getMail().equalsIgnoreCase(mail)) {
                                userFound = true;
                                break;
                            }
                        }

                        if (userFound) {
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_EXIST_USER, value);
                        } else
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_NOT_FOUND, constant.DB_EXIST_USER, null);

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_EXIST_USER, null);
                    }
                });
    }


    public void checkUserName(final String name, final Object callback) {

        if(name==null)
            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_EXIST_USER, null);


        userRef.orderByKey().addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value = null;
                        boolean userFound = false;
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);
                            try {
                                if (value.userD.getName().equalsIgnoreCase(name)) {
                                    userFound = true;
                                    break;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        if (userFound) {
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_OK, constant.DB_EXIST_USER, value);
                        } else
                            ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_NOT_FOUND, constant.DB_EXIST_USER, null);

                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        ((DBInterface) callback).onRequestCompleted(constant.DB_RESULT_ERROR, constant.DB_EXIST_USER, null);
                    }
                });
    }


    public interface DBInterfaceFriends {
        void onFriendsCompleted(int result, int retCode, ArrayList<User> friends);
    }

    public void getFriends(final ArrayList<Friend> friends, final Object callback) {

        userRef.orderByChild("uid").addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        User value;
                        ArrayList<User> friends_list = new ArrayList<>();

                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            value = userSnapshot.getValue(User.class);

                            if(friends.contains(new Friend(value.getUid(), value.userD.getName()))) {
                                friends_list.add(value);
                            }
                        }


                        // Sort the array list
                        class FriendComparator implements Comparator<Object> {

                            @Override
                            public int compare(Object o1, Object o2) {

                                User lhs = (User) o1, rhs = (User) o2;
                                return lhs.userD.getName().compareTo(rhs.userD.getName());
                            }
                        }

                        Collections.sort(friends_list, new FriendComparator());


                        ((DBInterfaceFriends) callback).onFriendsCompleted(constant.DB_RESULT_OK, constant.DB_GET_FRIENDS, friends_list);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "error getting friends: " + error);
                        ((DBInterfaceFriends) callback).onFriendsCompleted(constant.DB_RESULT_OK, constant.DB_GET_FRIENDS, null);
                    }
                });
    }

    // Changelog

    public void logChange(String uid, String aid, int changeType, boolean logEvent) {
        if(logEvent) {
            logRef.setValue(null);
            DBLog l = new DBLog(uid, aid, new Date().getTime());
            l.setChange(changeType);
            logRef.push();
            logRef.setValue(l);
        }
    }


    public interface LogInterface {
        void onLogChanged(DBLog l);
    }


    boolean initial = true;

    public void startLog(final Object callback) {

        Query query = logRef;

        query.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DBLog l = dataSnapshot.getValue(DBLog.class);
                if(l!=null)
                    if(!initial)
                        ((LogInterface) callback).onLogChanged(l);
                    else
                        initial=false;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, error.toString());
            }

        });

    }





}


