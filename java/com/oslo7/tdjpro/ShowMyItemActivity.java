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

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.oslo7.tdjpro.db.Assignment;
import com.oslo7.tdjpro.db.DBLog;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ShowMyItemActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener,
        dbSingleton.LogInterface,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "ShowMyItemActivity";
    private Keystore store;
    private String mUID = null;
    private int mCurrency = 0;
    private Assignment mAssignment = null;
    private Assignment mOldAssignment = null;
    private boolean mAsgDeleted = false;
    private boolean mAsgCompleted = false;
    private boolean mAsgAccepted = false;
    private boolean mAsgDiscarded = false;
    private boolean mDirty = false;
    boolean mNewUser = false;
    boolean keyboardVisible = false;
    boolean initial = true;
    boolean EThasFocus1 = false;
    boolean EThasFocus2 = false;
    Assignment mCalendarAssignment = null;
    SwipeRefreshLayout mSwipe;
    View contentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_my_item);

        contentView = findViewById(android.R.id.content);


        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.title_activity_show_item));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        store = Keystore.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
            mAssignment = (Assignment) extras.getSerializable("Assignment");
            mOldAssignment = new Assignment(mAssignment);
            mCurrency = extras.getInt("CURRENCY");
        }

        mNewUser = store.getBoolean("NEWUSER_SHOWMYITEM", true);

        if (mCurrency == 0) {
            (findViewById(R.id.value)).setVisibility(View.GONE);
        }


        Spinner spin = findViewById(R.id.colorSpinner);
        spin.setOnItemSelectedListener(this);
        CustomSpinnerAdapter customAdapter=new CustomSpinnerAdapter(getApplicationContext(),constant.colors);
        spin.setAdapter(customAdapter);
        spin.setSelection(mAssignment.getColor());

        dbSingleton.getInstance().startLog(this); // Listen for changes

        setupView(mAssignment.getAssignedBy().equals(mUID));

    }

    public void onTaskClick(View v) {
        EThasFocus1 = true;
    }

    public void onDescriptionClick(View v) { EThasFocus2 = true; }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        int p = mAssignment.getColor();
        if (p != position) {
            mAssignment.setColor(position);
            mDirty = true;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {

    }

    public void onLogChanged(DBLog l) {
        if(initial) {
            initial = false;
            return;
        }

    }

    public void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            findViewById(R.id.nofocus).requestFocus();
        }
    }

    public void runBack() {
        if (!keyboardVisible) {
            checkTextChanged();
            finishWithResult();
        } else {
            hideKeyboard();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_showmyitem, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                runBack();
                return true;
            case R.id.calendar:
                addToCalendar(mAssignment);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            runBack();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }


    // This need to trigger refresh of fragments!
    public void finishWithResult() {
        Intent intent = getIntent();
        if (mDirty) {
            setResult(RESULT_OK, intent);
            intent.putExtra("Assignment", mAssignment);
            intent.putExtra("OldAssignment", mOldAssignment);
            intent.putExtra("Deleted", mAsgDeleted);
            intent.putExtra("Completed", mAsgCompleted);
            intent.putExtra("Accepted", mAsgAccepted);
            intent.putExtra("Discarded", mAsgDiscarded);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void buttonOK(View v) {
        checkTextChanged();
    }

    public void checkTextChanged() {
        EditText name = findViewById(R.id.task_name);
        String newText = name.getText().toString();
        if(!mAssignment.getName().equals(newText)) {
            mAssignment.setName(newText);
            mDirty = true;
        }

        EditText desc = findViewById(R.id.task_description);
        String newDesc = desc.getText().toString();
        if( mAssignment.getDetail() == null) {
            mAssignment.setDetail(newDesc);
            mDirty = true;
        } else
            if(!mAssignment.getDetail().equals(newDesc)) {
                mAssignment.setDetail(newDesc);
                mDirty = true;
            }

    }

    public void buttonCancel(View v) {
        finishWithResult();
    }

    public void buttonDone(View v) {

        if(keyboardVisible) {
            hideKeyboard();
            return;
        }
        checkTextChanged();
        if(mAssignment.getAssignedBy().equals(mUID))
            verifyCompleteNoFeedback(mAssignment);
        else
            verifyComplete(mAssignment);
    }

    public void buttonChangeDate(View v) {

        if (mAssignment != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(mAssignment.getDue()));
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH);
            int year = cal.get(Calendar.YEAR);
            DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.datepicker, this, year, month, day);
            datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis() - 1000);

            datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                        return;
                    }
                }
            });
            datePickerDialog.show();
        }

    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        month++;
        String d = year + "/" + month + "/" + day;
        updateDate(d);
    }

    public void buttonPaid(View v) {
        mAssignment.setStatus(constant.STATUS_PAY_RECEIVED);
        mAsgAccepted = true;
        mDirty=true;
        verifyDelete(mAssignment, true);
    }

    private void updateDate(String d) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date convertedDate = new Date();
        try {
            convertedDate = dateFormat.parse(d);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mAssignment.setDue(convertedDate.getTime());
        mDirty = true;
        checkTextChanged();
        finishWithResult();
    }

    public void setupView(boolean isSelf) {

        EditText name = findViewById(R.id.task_name);
        name.setText(mAssignment.getName());
        name.setSelection(name.getText().length());
        name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                EThasFocus1 = hasFocus;
            }
        });

        EditText description = findViewById(R.id.task_description);
        description.setText(mAssignment.getDetail());
        description.setSelection(description.getText().length());
        description.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                EThasFocus2 = hasFocus;
            }
        });


        keyboardVisible = EThasFocus1 || EThasFocus2;

        final Toolbar bt = findViewById(R.id.toolbar_bottom);
        if(keyboardVisible)
            bt.setVisibility(View.GONE);
        else
            bt.setVisibility(View.VISIBLE);

        float val = mAssignment.getValue();
        TextView txtValue = findViewById(R.id.value);
        CurrencyFormatter fmt = new CurrencyFormatter(this);
        String result = fmt.formatCurrency(val, mCurrency);
        if(!isSelf)
            txtValue.setText(result);
        else
            txtValue.setText("");


        TextView due = findViewById(R.id.due);

        Date d = new Date(mAssignment.getDue());
        DateFormatter df = new DateFormatter(this);
        String reportDate = df.formatDate(d);

        if (DateUtils.isToday(d.getTime())) {
            due.setTextColor(getResources().getColor(R.color.green));
            reportDate = getResources().getString(R.string.today);
        }

        long diff = d.getTime() - (new Date()).getTime();
        long diffDays = diff / (24 * 60 * 60 * 1000);

        if (diffDays >= 6 || diffDays < 0) {
            reportDate += " " + new SimpleDateFormat("(dd/MM)").format(d);
        }

        if(diffDays < 0)
            due.setTextColor(getResources().getColor(R.color.colorAccent));

        due.setText(reportDate);

        TextView status = findViewById(R.id.status);

        toggleDateButton(false);
        toggleDeleteButton(false);
        toggleDoneButton(false);
        togglePaidButton(false);

        if(mAssignment.getStatus()==constant.STATUS_APPROVED && mAssignment.getValue()==0)
            mAssignment.setStatus(constant.STATUS_PAY_RECEIVED);

        switch(mAssignment.getStatus()) {
            case constant.STATUS_COMPLETE:
                if(isSelf)
                    toggleDeleteButton(true);
                else {
                    status.setText(getResources().getString(R.string.waiting_approval));
                    disableFocus();
                    toggleDeleteButton(true);
                }
                break;
            case constant.STATUS_DISCARDED:
                status.setText(getResources().getString(R.string.discarded));
                toggleDeleteButton(true);
                disableFocus();
                break;
            case constant.STATUS_APPROVED:
                if(!isSelf)
                    status.setText(getResources().getString(R.string.confirm_payment));
                togglePaidButton(true);
                toggleDeleteButton(isSelf);
                disableFocus();
                break;
            case constant.STATUS_PAY_RECEIVED:
                if(!isSelf && mAssignment.getValue()>0)
                    status.setText(getResources().getString(R.string.payment_received));
                else
                    status.setText(getResources().getString(R.string.approved));
                toggleDeleteButton(true);
                disableFocus();
                break;
            case constant.STATUS_TO_DO:
                if(isSelf) {
                    enableFocus();
                    toggleDateButton(true);
                    toggleDeleteButton(true);
                } else {
                    (findViewById(R.id.task_name)).setFocusable(false);
                    (findViewById(R.id.task_description)).setFocusable(false);
                    toggleDeleteButton(true);
                }
                toggleDoneButton(true);
                // Check if overdue
                Date now = new Date();
                if (d.before(now) && !DateUtils.isToday(d.getTime())) {
                    status.setText(getResources().getString(R.string.past_due));
                } else
                    status.setText("");
                break;
            default:
                break;
        }

    }

    public void enableFocus() {
        (findViewById(R.id.task_name)).setFocusable(true);
        (findViewById(R.id.task_description)).setFocusable(true);
    }

    public void disableFocus() {
        (findViewById(R.id.task_name)).setFocusable(false);
        (findViewById(R.id.task_description)).setFocusable(false);
    }

    public void buttonDelete(View v) {

        if(mUID.equals(mAssignment.getAssignedBy())) {
            if (mAssignment.getStatus() == constant.STATUS_PAY_RECEIVED)
                verifyDelete(mAssignment, true);
            else
                verifyDelete(mAssignment, false);
        } else {
            if (mAssignment.getStatus() == constant.STATUS_PAY_RECEIVED)
                verifyDelete(mAssignment, false);
            else
                discardRecord(mAssignment);
        }
    }

    public void verifyDelete(final Assignment a, boolean payment) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));

        if(payment)
            builder.setTitle(getResources().getString(R.string.payment_received));
        else
            builder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.delete_assignment) + " " +
                a.getName() + "?";
        builder.setMessage(question);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteRecord(a);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void deleteRecord(Assignment a) {
        mAsgDeleted = true;
        mDirty=true;
        finishWithResult();
    }

    public void verifyDiscard(final Assignment a) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));

        builder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.discard_assignment) + " " +
                a.getName() + "?";
        builder.setMessage(question);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                discardRecord(a);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void discardRecord(Assignment a) {
        checkTextChanged();
        a.setStatus(constant.STATUS_DISCARDED);
        mAsgDiscarded = true;
        mDirty=true;
        finishWithResult();
    }



    public void verifyComplete(final Assignment a) {

        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View promptView = layoutInflater.inflate(R.layout.feedback, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptView);

        alertDialogBuilder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.complete_assignment) + " " +
                a.getName() + "?";
        alertDialogBuilder.setMessage(question);


        final EditText editText = promptView.findViewById(R.id.feedback);

        alertDialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                String feedback = editText.getText().toString();
                a.setFeedback(feedback);
                completeRecord(a);
                dialog.dismiss();
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();

    }

    public void verifyCompleteNoFeedback(final Assignment a) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));

        builder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.complete_assignment) + " " +
                a.getName() + "?";
        builder.setMessage(question);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                mAsgDeleted = true;
                mDirty = true;
                finishWithResult();
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }


    public void completeRecord(Assignment a) {
        a.setStatus(constant.STATUS_COMPLETE);
        mAsgCompleted = true;
        mDirty=true;
        finishWithResult();
    }


    public void toggleDeleteButton(boolean state) {
        ImageView button = findViewById(R.id.button_delete);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.GONE);
    }

    public void toggleDateButton(boolean state) {
        ImageView button = findViewById(R.id.button_date);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.GONE);
    }

    public void toggleDoneButton(boolean state) {
        ImageView button = findViewById(R.id.button_done);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.GONE);
    }

    public void togglePaidButton(boolean state) {
        ImageView button = findViewById(R.id.button_paid);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(View.GONE);
    }


    public void addToCalendar(Assignment a) {


        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(a.getDue());
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        Date d = calendar.getTime();

        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("beginTime", d.getTime());
        intent.putExtra("endTime", d.getTime()+60*60*1000);
        intent.putExtra("title", a.getName());
        startActivity(intent);
    }

    private void requestForSpecificPermission(Assignment a) {
        mCalendarAssignment = a;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_CALENDAR}, 101);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    addToCalendar(mCalendarAssignment);
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }




}
