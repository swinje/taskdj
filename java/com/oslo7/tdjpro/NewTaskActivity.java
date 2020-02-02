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
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;

import com.oslo7.tdjpro.db.Assignment;
import com.oslo7.tdjpro.db.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;


public class NewTaskActivity extends AppCompatActivity implements
        dbSingleton.DBInterface,
        DatePickerDialog.OnDateSetListener,
        View.OnClickListener {

    private static final String TAG = "NewTaskActivity";
    private Keystore store;
    User mUser;
    String mUID;
    String mAssigner, mAssignerName;
    Assignment mAssignment;
    private String[] currencies;
    InputMethodManager imm = null;

    String editName = null;
    String editDescrip = null;
    float editValue = 0;

    private PopupMenu popupMenu;
    boolean mNewHistory = true;

    View mView = null;
    private View mProgressView;
    boolean mNewUser = false;

    ArrayList<String> history;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_task);
        mView = findViewById(android.R.id.content);
        mProgressView = findViewById(R.id.progress);


        setFinishOnTouchOutside(false);

        // store for persistent variables
        store = Keystore.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
            mAssigner = extras.getString("ASSIGNER");
            mAssignerName = extras.getString("ASSIGNER_NAME");
        }

        mNewUser = store.getBoolean("NEWUSER_NEWTASK", true);

        currencies = getResources().getStringArray(R.array.currency_list);

        final Context context = this;

        final Button button = findViewById(R.id.buttonOK);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                EditText EN = findViewById(R.id.editTaskName);
                editName = EN.getText().toString();

                if (editName.trim().length() < 1) {
                    EN.setError(getResources().getString(R.string.need_value));
                    EN.requestFocus();
                    return;
                }

                EditText ED = findViewById(R.id.editTaskDescrip);
                editDescrip = ED.getText().toString();


                editValue = 0;
                String val = ((EditText) findViewById(R.id.editValue)).getText().toString();

                if(val!=null) {
                    try {
                        editValue = Float.parseFloat(val);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Number format error setting to zero");
                        editValue = 0;
                    }
                }

                // Add assignment
                datePick();
            }
        });

        Button buttonCancel = findViewById(R.id.buttonCancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishWithResult(false);
            }
        });

        loadHistory();

        getUser();

    }



    @Override
    public void onClick(View anchor) {

        popupMenu = new PopupMenu(this, anchor);
        popupMenu.setOnDismissListener(new OnDismissListener());
        popupMenu.setOnMenuItemClickListener(new OnMenuItemClickListener());
        int i= 0;

        ArrayList<String> uH = history;
        if(uH != null) {
            for (String h : uH)
                popupMenu.getMenu().add(0, i++, 0, h);
            popupMenu.show();
            if (i == 0)
                Toast.makeText(getBaseContext(), getString(R.string.no_recent), Toast.LENGTH_LONG).show();
        } else
            Toast.makeText(getBaseContext(), getString(R.string.no_recent), Toast.LENGTH_LONG).show();
    }

    private class OnDismissListener implements PopupMenu.OnDismissListener {
        @Override
        public void onDismiss(PopupMenu menu) { }
    }

    private class OnMenuItemClickListener implements
            PopupMenu.OnMenuItemClickListener {

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            EditText editDesc = findViewById(R.id.editTaskName);
            editDesc.setText(history.get(item.getItemId()));
            EditText editValue = findViewById(R.id.editValue);
            editValue.requestFocus();
            mNewHistory = false;
            return true;
        }
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, null);
        View v = this.findViewById(android.R.id.content);
        finishWithResult(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.home:
                finishWithResult(false);
        }
        return super.onOptionsItemSelected(item);
    }

    public void getUser() {
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        if(mUID.equals(mAssigner))
            dbSingleton.getInstance().updateUser(mUser, mUID, this, false);
        else
            dbSingleton.getInstance().updateUser(mUser, mAssigner, this, true);
    }

    public void onRequestCompleted(int result, int retCode, User user) {
        if(retCode== constant.DB_GET_USER) {
            mUser = user;

            if(user.getAssignmentsMax())
                showAssignmentMax();

            TextView editValue = findViewById(R.id.textValue);
            String outStr = getResources().getString(R.string.value);

            if ( (mUser.userD.getCurrency() == 0) || (mUser.getUid().equals(mAssigner))) {
                toggleValue(false);
                outStr += ": ";
            } else {
                toggleValue(true);
                outStr += " (" + currencies[mUser.userD.getCurrency()] + "): ";
            }
            editValue.setText(outStr);
        }
        if(retCode==constant.DB_UPD_USER)
            finishWithResult(true);
    }

    public void datePick() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.datepicker, this, year, month, day);
        datePickerDialog.getDatePicker().setMinDate(cal.getTimeInMillis());
        showProgress(true);
        datePickerDialog.show();
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        addRecord(cal.getTimeInMillis());

    }

    public void addRecord(long due) {

        if(mUser==null)
            return;

        editName = editName.replaceAll("\\s*$", "").replaceAll("^\\s*", "");
        //editDescrip = editDescrip.replaceAll("\\s*$", "").replaceAll("^\\s*", "");
        mAssignment = new Assignment(editName, editDescrip, editValue, due, mAssigner, mAssignerName, 2); // null needs to be edit due
        mUser.addAssignment(mAssignment);
        if(mNewHistory)
            addHistory(mAssignment.getName());
        writeUser();

    }

    private void finishWithResult(boolean ok) {
        Intent intent = new Intent();
        if(ok) {
            setResult(RESULT_OK, intent);
            intent.putExtra("Assignment", mAssignment);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
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

    public void toggleValue(boolean turnOn) {
        if(turnOn) {
            findViewById(R.id.textValue).setVisibility(View.VISIBLE);
            findViewById(R.id.editValue).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.textValue).setVisibility(View.GONE);
            findViewById(R.id.editValue).setVisibility(View.GONE);
        }
    }

    public void addHistory(String t) {
        if (null == history) {
            history= new ArrayList<String>();
            history.add(t);
        } else {
            LinkedList<String> lHistory = new LinkedList<String>(history);
            if(lHistory.size() > 5)
                lHistory.removeLast();
            lHistory.addFirst(t);
            history = new ArrayList<String>(lHistory);
        }

        // save the task list to preference
        SharedPreferences prefs = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        try {
            editor.putString("HISTORY", ObjectSerializer.serialize(history));
        } catch (IOException e) {
            e.printStackTrace();
        }
        editor.commit();
    }

    private void showAssignmentMax() {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));
        builder.setTitle(getString(R.string.limit_reached));
        builder.setMessage(getString(R.string.limit_assignments));
        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finishWithResult(false);
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void loadHistory() {
        if (null == history) {
            history = new ArrayList<String>();
        }

        // load tasks from preference
        SharedPreferences prefs = getSharedPreferences("PREFERENCES", Context.MODE_PRIVATE);

        try {
            history = (ArrayList<String>) ObjectSerializer.deserialize(prefs.getString("HISTORY",
                    ObjectSerializer.serialize(new ArrayList<String>())));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }



}
