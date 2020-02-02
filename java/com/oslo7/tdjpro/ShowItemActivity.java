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

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.oslo7.tdjpro.db.Assignment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.view.View.GONE;

public class ShowItemActivity extends AppCompatActivity implements
        DatePickerDialog.OnDateSetListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = "ShowItemActivity";
    private Keystore store;
    private String mUID = null;
    private Assignment mOldAssignment = null;
    private Assignment mAssignment = null;
    private String mAssigner;
    private boolean mDirty = false;
    private boolean mAsgDeleted = false;
    private boolean mSubtractBalance = false;
    private boolean mApproved = false;
    private boolean mReset = false;
    private int mCurrency = 0;
    boolean mNewUser = false;
    boolean keyboardVisible = false;
    boolean EThasFocus1 = false;
    boolean EThasFocus2 = false;
    boolean EThasFocus3 = false;
    SwipeRefreshLayout mSwipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_item);

        final View contentView = findViewById(android.R.id.content);

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
            mAssigner = extras.getString("ASSIGNER");
        }

        mNewUser = store.getBoolean("NEWUSER_SHOWITEM", true);

       setupView();

    }

    public void onTaskClick(View v) {
        EThasFocus1 = true;
    }

    public void onDescriptionClick(View v) { EThasFocus2 = true; }

    public void onFeedbackClick(View v) { EThasFocus3 = true; }

    //Performing action onItemSelected and onNothing selected
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position,long id) {
        mAssignment.setColor(position);
        ImageView taskIcon = findViewById(R.id.taskicon);
        taskIcon.setColorFilter(constant.colors[position]);
        mDirty = true;
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }

    public void runBack() {
        if(!keyboardVisible) {
            checkTextChanged();
            finishWithResult();
        } else {
            // Check if no view has focus:
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                findViewById(R.id.nofocus).requestFocus();
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                runBack();
                return true;
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
            intent.putExtra("Subtract", mSubtractBalance);
            intent.putExtra("Approved", mApproved);
            intent.putExtra("Reset", mReset);
        } else
            setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void buttonOK(View v) {
        checkTextChanged();
        finishWithResult();
    }

    public void checkTextChanged() {

        if(mAssignment==null)
            return;

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

        EditText feedback = findViewById(R.id.feedback);
        String newFeedback = feedback.getText().toString();
        if( mAssignment.getFeedback() == null) {
            mAssignment.setFeedback(newFeedback);
            mDirty = true;
        } else
        if(!mAssignment.getFeedback().equals(newFeedback)) {
            mAssignment.setFeedback(newFeedback);
            mDirty = true;
        }


        finishWithResult();
    }

    public void buttonCancel(View v) {
        finishWithResult();
    }

    public void buttonRevert(View v) {
        mAssignment.setStatus(constant.STATUS_TO_DO);
        mSubtractBalance = true;
        mDirty = true;
        setupView();
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

    public void buttonApprove(View v) {
        mAssignment.setStatus(constant.STATUS_APPROVED);
        mApproved = true;
        mDirty = true;
        checkTextChanged();
        finishWithResult();
    }

    public void askComment() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.reminder_title));

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT );
        input.setMaxLines(1);
        input.setHint(getResources().getString(R.string.optional));
        input.setHintTextColor(getResources().getColor(R.color.grey));
        builder.setView(input);

        builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String a = input.getText().toString();
                sendM(a.substring(0, Math.min(30, a.length())));
            }
        });
        builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void sendM(String txt) {
        SendMessage sm = new SendMessage();
        sm.sendUser(mUID, mAssignment.getAssignerName(), getResources().getString(R.string.reminder) + " " + txt);
        showReminderSent();
        setupView();
    }

    public void buttonRemind(View v) {
        askComment();
    }

    public void showReminderSent() {
        MySnack ms = new MySnack(findViewById(android.R.id.content));
        ms.MakeSnack(getResources().getString(R.string.reminder_sent), new MySnack.SnackInterface() {
            @Override
            public void onSnackCompleted() {

            }
        });
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
        if(mAssignment.getStatus()!=constant.STATUS_TO_DO) {
            mAssignment.setStatus(constant.STATUS_TO_DO);
            mReset = true;
        }
        mDirty = true;
        checkTextChanged();
        finishWithResult();
    }

    public void setupView() {

        if(mAssignment==null)
            return;

        ImageView taskIcon = findViewById(R.id.taskicon);
        taskIcon.setColorFilter(Color.BLUE);

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

        EditText feedback = findViewById(R.id.feedback);
        if(mAssignment.getFeedback() != null) {
            feedback.setVisibility(View.VISIBLE);
            (findViewById(R.id.feedback_info)).setVisibility(View.VISIBLE);
            feedback.setText(mAssignment.getFeedback());
            feedback.setSelection(feedback.getText().length());
            feedback.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    EThasFocus3 = hasFocus;
                }
            });
        } else {
            feedback.setVisibility(GONE);
            (findViewById(R.id.feedback_info)).setVisibility(GONE);
        }

        final Toolbar bt = findViewById(R.id.toolbar_bottom);
        if(EThasFocus1 || EThasFocus2 || EThasFocus3) {
            keyboardVisible = true;
            bt.setVisibility(GONE);
        } else {
            keyboardVisible = false;
            bt.setVisibility(View.VISIBLE);
        }

        float val = mAssignment.getValue();
        TextView txtValue = findViewById(R.id.value);
        CurrencyFormatter fmt = new CurrencyFormatter(this);
        String result = fmt.formatCurrency(val, mCurrency);
        txtValue.setText(result);


        TextView due = findViewById(R.id.due);

        Date d = new Date(mAssignment.getDue());
        DateFormatter df = new DateFormatter(this);
        String reportDate = df.formatDate(d) + new SimpleDateFormat(" (dd/MM)").format(d);
        due.setText(reportDate);

        if (DateUtils.isToday(d.getTime())) {
            due.setTextColor(getResources().getColor(R.color.green));
            reportDate = getResources().getString(R.string.today);
        }

        TextView status = findViewById(R.id.status);

        (findViewById(R.id.task_name)).setFocusable(true);
        (findViewById(R.id.task_description)).setFocusable(true);
        (findViewById(R.id.feedback)).setFocusable(false);

        toggleDeleteButton(false);
        toggleResetButton(false);
        toggleApproveButton(false);
        toggleDateButton(false);

        switch(mAssignment.getStatus()) {
            case constant.STATUS_COMPLETE:
                toggleResetButton(true);
                toggleApproveButton(true);
                status.setText(getResources().getString(R.string.waiting_approval));
                break;
            case constant.STATUS_DISCARDED:
                toggleDateButton(true);
                toggleDeleteButton(true);
                status.setText(getResources().getString(R.string.discarded));
                break;
            case constant.STATUS_APPROVED:
                toggleDeleteButton(true);
                status.setText(getResources().getString(R.string.approved));
                (findViewById(R.id.task_name)).setFocusable(false);
                (findViewById(R.id.task_description)).setFocusable(false);
                break;
            case constant.STATUS_PAY_RECEIVED:
                toggleDeleteButton(true);
                status.setText(getResources().getString(R.string.payment_received));
                (findViewById(R.id.task_name)).setFocusable(false);
                (findViewById(R.id.task_description)).setFocusable(false);
                break;
            default:
                toggleDeleteButton(true);
                toggleDateButton(true);
                toggleRemindButton(true);
                // Check if overdue
                Date now = new Date();
                if (d.before(now) && !DateUtils.isToday(d.getTime())) {
                    status.setText(getResources().getString(R.string.past_due));
                } else
                    status.setText(getResources().getString(R.string.todo));
                break;
        }

    }

    public void toggleDeleteButton(boolean state) {
        ImageView button = findViewById(R.id.button_delete);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(GONE);
    }

    public void toggleResetButton(boolean state) {
        ImageView button = findViewById(R.id.button_reset);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(GONE);
    }

    public void toggleDateButton(boolean state) {
        ImageView button =  findViewById(R.id.button_date);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(GONE);
    }

    public void toggleApproveButton(boolean state) {
        ImageView button =  findViewById(R.id.button_approve);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(GONE);
    }

    public void toggleRemindButton(boolean state) {
        ImageView button =  findViewById(R.id.button_remind);
        if (state)
            button.setVisibility(View.VISIBLE);
        else
            button.setVisibility(GONE);
    }

    public void deleteAssignment(View v) {
        verifyDelete(mAssignment);
    }

    public void verifyDelete(final Assignment a) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));

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



}
