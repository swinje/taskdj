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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.oslo7.tdjpro.db.Assignment;
import com.oslo7.tdjpro.db.DBLog;
import com.oslo7.tdjpro.db.User;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class MyCardViewActivity extends AppCompatActivity implements
        dbSingleton.DBInterface,
        dbSingleton.LogInterface,
        CheckEarned.EarnedInterface {

    private static final String TAG = "MyCardViewActivity";
    public ArrayList<Assignment> ITEMS;
    RecyclerView mRv;
    User mUser = null;
    private Keystore store;
    private String mUID = null;
    private View mProgressView;
    boolean mNewUser = false;
    //SwipeRefreshLayout mSwipe;
    Assignment assignmentToMove = null;
    boolean getNoAssignments = false;
    Assignment saveAssignment;

    GestureDetectorCompat detector;

    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_card_view);

        Toolbar toolbar = findViewById(R.id.toolbar_top);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressView = findViewById(R.id.progress);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addAssignment();
            }
        });

        store = Keystore.getInstance(this);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mUID = extras.getString("UID");
        }

        mNewUser = store.getBoolean("NEWUSER_MYCARD", true);


        mRv = findViewById(R.id.rv);
        mRv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        mRv.setLayoutManager(llm);

        mRv.addOnItemTouchListener(new MyTouchListener(this, mRv,
                new MyTouchListener.OnTouchActionListener() {

                    @Override
                    public void onLeftSwipe(View view, int position) {
                        finishWithResult();
                    }

                    @Override
                    public void onRightSwipe(View view, int position) {
                        finishWithResult();
                    }


                    @Override
                    public void onClick(View view, int position) {
                        if(position>=0) {
                            Assignment a = ((RVAdapter) mRv.getAdapter()).getItem(position);
                            if(a!=null)
                                showMyItem(a);
                        }
                    }

                    @Override
                    public void onLongPress(View view, int position) {
                        Assignment a = ((RVAdapter) mRv.getAdapter()).getItem(position);
                        saveAssignment = a;
                        Reassign(a, position);
                    }


                }));

        if (!store.getBoolean("ADFREE", false)) {
            mAdView = findViewById(R.id.adView);
            mAdView.setVisibility(View.VISIBLE);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
            AdRequest request = new AdRequest.Builder().addTestDevice(BuildConfig.testdevice).build();
        }


        // Initialize database
        dbSingleton.getInstance().init(this);

        dbSingleton.getInstance().startLog(this); // Listen for changes

        fixOrientation(getResources().getConfiguration().orientation);

        if(hasNet())
            getUser();
        else
            noNet();

    }

    public boolean hasNet() {
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            if (Build.VERSION.SDK_INT < 23) {
                final NetworkInfo ni = cm.getActiveNetworkInfo();

                if (ni != null) {
                    return (ni.isConnected() && (ni.getType() == ConnectivityManager.TYPE_WIFI || ni.getType() == ConnectivityManager.TYPE_MOBILE));
                }
            } else {
                final Network n = cm.getActiveNetwork();

                if (n != null) {
                    final NetworkCapabilities nc = cm.getNetworkCapabilities(n);

                    return (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                }
            }
        }

        return false;
    }

    public void noNet() {
        Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_LONG).show();
        finish();
    }


    public void onLogChanged(DBLog l) {
        if (mUser == null)
            return;
        if (mUser.getUid().equals(l.getUid())) {
            getUser();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getUser();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mycard, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                finishWithResult();
                return true;
            case R.id.refresh:
                getUser();
                break;
            case R.id.settlements:
                runSettlementHistory();
                return true;
            case R.id.move:
                Toast.makeText(this, getResources().getString(R.string.moveinfo), Toast.LENGTH_LONG).show();
                break;
            case R.id.balance:
                CheckEarned co = new CheckEarned(this, mUID);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        fixOrientation(newConfig.orientation);
    }

    private void fixOrientation(int o) {
        //if (o==Configuration.ORIENTATION_LANDSCAPE) {
            (findViewById(R.id.rlayout)).setBackgroundColor(getResources().getColor(R.color.medium_green));
        //} else {
        //    (findViewById(R.id.rlayout)).setBackgroundColor(getResources().getColor(R.color.white));
        //}
    }


    public void onEarnedCompleted(float balance) {
        CurrencyFormatter fmt = new CurrencyFormatter(this);
        String balance_formatted = fmt.formatCurrency(balance, mUser.userD.getCurrency());
        String total_formatted = fmt.formatCurrency(mUser.userD.getTotalEarnings(), mUser.userD.getCurrency());
        String total_assignments = Integer.toString(mUser.userD.getAssignmentsCompleted());

        if(balance_formatted.equals(""))
            balance_formatted = "0";
        if(total_formatted.equals(""))
            total_formatted = "0";
        if(total_assignments.equals(""))
            total_assignments = "0";

        String infoTxt = getResources().getString(R.string.unpaid_balance) + " " + balance_formatted + "\n" +
                getResources().getString(R.string.total_earnings) + " " + total_formatted + "\n" +
                getResources().getString(R.string.total_assignments) + " " + total_assignments;
        Toast.makeText(this, infoTxt, Toast.LENGTH_LONG).show();

    }

    public void showMyItem(Assignment a) {
        Intent intent = new Intent(this, ShowMyItemActivity.class);
        intent.putExtra("UID", mUID);
        intent.putExtra("Assignment", a);
        intent.putExtra("CURRENCY", mUser.userD.getCurrency());
        startActivityForResult(intent, 20);
    }

    void runSettlementHistory() {
        // This activity returns to the activity not the fragment
        Intent i = new Intent(this, SettlementHistoryActivity.class);
        i.putExtra("UID", mUID);
        startActivityForResult(i, 80);
    }

    public void onReload(View anchor) {
        getUser();
    }

    private void finishWithResult() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }

    public void showStatus(boolean visible) {

        /*
        ImageView imgView = (ImageView) findViewById(R.id.imgSmiley);
        if (visible) {
            TextView t = (TextView) findViewById(R.id.textStatus);
            t.setVisibility(View.VISIBLE);
            t.setText(getResources().getString(R.string.no_assignments));
            imgView.setVisibility(View.VISIBLE);
        } else {
            TextView t = (TextView) findViewById(R.id.textStatus);
            t.setVisibility(View.GONE);
            imgView.setVisibility(View.GONE);
        }
        */

    }

    public void getUser() {
        //mSwipe.setRefreshing(false);
        showProgress(true);
        dbSingleton.getInstance().getUser(mUID, this);
    }

    public void writeUser() {
        showProgress(true);
        dbSingleton.getInstance().updateUser(mUser, mUID, this, false);
    }

    public void onRequestCompleted(int result, int retCode, User user) {

        showProgress(false);

        switch (retCode) {
            case constant.DB_GET_USER:
                mUser = user;
                getSupportActionBar().setTitle(R.string.my_assignments);
                getAssignments();
                break;
            case constant.DB_UPD_USER:
                getUser();
        }

    }

    // Sort the array list
    class DueComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {

            Assignment lhs = (Assignment) o1, rhs = (Assignment) o2;
            if (lhs.getDue() < rhs.getDue())
                return -1;
            if (lhs.getDue() > rhs.getDue())
                return 1;
            if (lhs.getDue() == rhs.getDue())
                return 0;
            return 0;
        }
    }


    public void getAssignments() {

        if (mUser == null)
            return;

        ITEMS = new ArrayList<>();
        ITEMS.addAll(mUser.getAssignmentsNotDiscarded());

        if (ITEMS != null)
            Collections.sort(ITEMS, new DueComparator());

        RVAdapter adapter = new RVAdapter(this, ITEMS);
        mRv.setAdapter(adapter);

        if (ITEMS.size() == 0) {
            showStatus(true);
            if (!getNoAssignments) {
                getNoAssignments = true;
            }
        } else
            showStatus(false);

    }

    public void verifyDone(final Assignment a) {

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom));

        builder.setTitle(R.string.confirm);
        String question = getResources().getString(R.string.finished_task) + " " +
                a.getName() + "?";
        builder.setMessage(question);

        builder.setNeutralButton(R.string.discard, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                deleteAssignment(a);
                dialog.dismiss();
            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                setDone(a);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void setDone(Assignment a) {
        // If user owns the assignment, delete it
        if (mUser.getUid().equals(a.getAssignedBy()))
            mUser.removeAssignment(a);
        else {
            a.setStatus(constant.STATUS_COMPLETE);
            mUser.addBalanceToFriend(a.getAssignedBy(), a.getValue());
        }
        writeUser();
    }

    public class RVAdapter extends RecyclerView.Adapter<RVAdapter.AssignmentViewHolder> {

        public class AssignmentViewHolder extends RecyclerView.ViewHolder {

            CardView cv;
            TextView name;
            TextView value;
            TextView due;
            TextView asgby;
            ImageView assignmentImage;

            AssignmentViewHolder(View itemView) {
                super(itemView);
                cv = itemView.findViewById(R.id.cv);
                name = itemView.findViewById(R.id.name);
                value = itemView.findViewById(R.id.value);
                due = itemView.findViewById(R.id.due);
                asgby = itemView.findViewById(R.id.asgby);
                assignmentImage = itemView.findViewById(R.id.assignment_image);
            }

        }

        private ArrayList<Assignment> assignments;

        private Context context;

        RVAdapter(Context context, ArrayList<Assignment> assignments) {
            this.context = context;
            this.assignments = assignments;
        }

        @Override
        public int getItemCount() {
            return assignments.size();
        }

        public Assignment getItem(int position) {
            if(position < 0 || position > assignments.size()-1)
                return null;
            return assignments.get(position);
        }

        @NonNull
        @Override
        public AssignmentViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.assignment_card, viewGroup, false);
            AssignmentViewHolder uvh = new AssignmentViewHolder(v);
            return uvh;
        }

        @Override
        public void onBindViewHolder(@NonNull AssignmentViewHolder avh, int i) {

            boolean today = false;

            boolean isSelf = assignments.get(i).getAssignedBy().equals(mUser.getUid());

            Date d = new Date(assignments.get(i).getDue());

            DateFormatter df = new DateFormatter(context);
            String reportDate = df.formatDate(d);

            if (DateUtils.isToday(d.getTime())) {
                avh.due.setTextColor(getResources().getColor(R.color.green));
                reportDate = getResources().getString(R.string.today);
                today = true;
            }

            long diff = d.getTime() - (new Date()).getTime();
            long diffDays = diff / (24 * 60 * 60 * 1000);

            if (diffDays >= 6 || diffDays < 0) {
                reportDate += " " + new SimpleDateFormat("(dd/MM)").format(d);
            }

            if (diffDays < 0 && assignments.get(i).getStatus() == constant.STATUS_TO_DO)
                avh.due.setTextColor(getResources().getColor(R.color.colorAccent));

            avh.due.setText(reportDate);

            avh.name.setText(assignments.get(i).getName());

            if (isSelf) {
                avh.asgby.setText(null);
                avh.cv.setCardBackgroundColor(getResources().getColor(R.color.light_green));
            } else {

                float val = assignments.get(i).getValue();

                CurrencyFormatter fmt = new CurrencyFormatter(context);
                String result = fmt.formatCurrency(val, mUser.userD.getCurrency());
                avh.value.setText(result);

                avh.asgby.setText(assignments.get(i).getAssignerName());
            }

            switch (assignments.get(i).getStatus()) {
                case constant.STATUS_COMPLETE:
                    avh.cv.setBackgroundColor(getResources().getColor(R.color.purple));
                    avh.due.setText(getResources().getString(R.string.complete));
                    break;
                case constant.STATUS_APPROVED:
                    avh.cv.setCardBackgroundColor(getResources().getColor(R.color.yellow));
                    if (!isSelf && assignments.get(i).getValue() > 0)
                        avh.due.setText(getResources().getText(R.string.approved_confirm));
                    else
                        avh.due.setText(R.string.approved);
                    break;
                case constant.STATUS_PAY_RECEIVED:
                    avh.cv.setCardBackgroundColor(getResources().getColor(R.color.light_grey));
                    if (!isSelf)
                        avh.due.setText(getResources().getText(R.string.payment_received));
                    break;
            }

            avh.assignmentImage.setColorFilter(constant.colors[assignments.get(i).getColor()]);
        }

        @Override
        public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }

    }

    public void addAssignment() {

        if (mUser == null)
            return;

        // start an intent and also pass in the child id
        Intent intent = new Intent(this, NewTaskActivity.class);
        intent.putExtra("UID", mUID);
        intent.putExtra("ASSIGNER", mUID);
        intent.putExtra("ASSIGNER_NAME", mUser.userD.getName());
        startActivityForResult(intent, 10);
    }

    void Reassign(Assignment asg, int pos) {

        assignmentToMove = asg;
        if (asg.getStatus() == constant.STATUS_TO_DO) {
            Intent i = new Intent(this, ReassignActivity.class);
            i.putExtra("ASSIGNER", asg.getAssignedBy());
            i.putExtra("UID", mUID);
            startActivityForResult(i, 97);
        } else
            Toast.makeText(this, getResources().getString(R.string.only_todo_move), Toast.LENGTH_LONG).show();

    }

    void addAssignmentToUser(String user, Assignment asg) {
        Intent i = new Intent(this, adderActivity.class);
        i.putExtra("UID", user);
        i.putExtra("ASSIGNER", asg.getAssignedBy());
        i.putExtra("Assignment", asg);
        startActivityForResult(i, 98);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK) {
                    Assignment a = (Assignment) data.getExtras().getSerializable("Assignment");
                }
                break;
            case 20:
                if (resultCode == RESULT_OK) {
                    Assignment a = (Assignment) data.getExtras().getSerializable("Assignment");
                    Assignment o = (Assignment) data.getExtras().getSerializable("OldAssignment");
                    boolean pay_received = data.getExtras().getBoolean("Accepted");
                    boolean deleted = data.getExtras().getBoolean("Deleted");
                    boolean completed = data.getExtras().getBoolean("Completed");
                    boolean discarded = data.getExtras().getBoolean("Discarded");

                    mUser.replaceAssignment(o, a);

                    if (pay_received) {
                        float value = a.getValue();
                        CurrencyFormatter fmt = new CurrencyFormatter(this);
                        String value_formatted = fmt.formatCurrency(value, mUser.userD.getCurrency());
                        String infoStr = getResources().getString(R.string.accepted_from) + " " + a.getAssignerName() + " " +
                                getResources().getString(R.string.of_word) + " " +
                                value_formatted + " " + getResources().getString(R.string.for_word) + " " + a.getName();
                        mUser.userD.addSettlement(infoStr);
                        mUser.userD.addTotalEarnings(a.getValue());
                        SendMessage sm = new SendMessage();
                        sm.sendUser(a.getAssignedBy(), mUser.userD.getName(), getResources().getString(R.string.payment_received));
                    }

                    if (deleted) {
                        mUser.removeAssignment(a);
                        if (!mUID.equals(a.getAssignedBy()))
                            dbSingleton.getInstance().logChange(mUID, a.getAssignedBy(), constant.CHANGE_DELETE, true);
                    }

                    if (completed) {
                        dbSingleton.getInstance().logChange(mUser.getUid(), a.getAssignedBy(), constant.CHANGE_UPDATE, true);
                        dbSingleton.getInstance().sendUIDMessage(mUser.userD.getName() + " " + getResources().getString(R.string.completed), a.getAssignedBy());
                        mUser.userD.addAssignmentsCompleted();
                        SendMessage sm = new SendMessage();
                        sm.sendUser(a.getAssignedBy(), mUser.userD.getName(), getResources().getString(R.string.completed));
                    }

                    if (discarded) {
                        dbSingleton.getInstance().logChange(mUser.getUid(), a.getAssignedBy(), constant.CHANGE_UPDATE, true);
                        SendMessage sm = new SendMessage();
                        sm.sendUser(a.getAssignedBy(), mUser.userD.getName(), getResources().getString(R.string.discarded));
                    }

                    writeUser();
                }
                getUser();
                break;
            case 80:
                getUser();
                break;
            case 97:
                if (resultCode == RESULT_OK) {
                    String toUID = data.getExtras().getString("TO_UID");
                    addAssignmentToUser(toUID, assignmentToMove);
                }
                break;
            case 98:
                if (resultCode == RESULT_OK) {
                    mUser.removeAssignment(assignmentToMove);
                    writeUser();
                }
                break;
        }
    }

    public void deleteAssignment(Assignment a) {
        // If the user owns the assignment, it can be removed o.w. set to discard
        if (mUser.getUid().equals(a.getAssignedBy()))
            mUser.removeAssignment(a);
        else
            a.setStatus(constant.STATUS_DISCARDED);
        writeUser();
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

}
