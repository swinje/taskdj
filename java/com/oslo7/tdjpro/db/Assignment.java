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


import android.graphics.Color;
import android.text.format.DateUtils;
import android.util.Log;

import com.oslo7.tdjpro.constant;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;




public class Assignment implements Serializable {

    private String name;
    private String detail;
    private float value;
    private int status;
    private Long due;
    private String assignedBy;
    private String assignerName;
    private Long created;
    private Long lastStatus;
    private int color;
    private String feedback;
    private boolean settled;

    private static final String TAG = "Assignment";

    public Assignment() { }

    public Assignment(Assignment n) {
        if(n==null)
            return;

        this.name = n.name;
        this.detail = n.detail;
        this.value = n.value;
        this.status = n.status;
        this.due = n.due;
        this.assignedBy = n.assignedBy;
        this.assignerName = n.assignerName;
        this.created = n.created;
        this.lastStatus = n.lastStatus;
        this.color = Color.BLACK;
        this.feedback = null;
        this.settled = false;
    }

    public Assignment(String name, String detail, float value, Long due, String assignedBy, String assignerName, int color) {
        this.name = name;
        this.detail = detail;
        this.value = value;
        this.status = constant.STATUS_TO_DO;
        this.due = due;
        this.assignedBy = assignedBy;
        this.assignerName = assignerName;
        this.created = new Date().getTime();
        this.lastStatus = this.created;
        this.color = color;
        this.settled = false;
    }


    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDetail() {
        return detail;
    }
    public void setDetail(String detail) { this.detail = detail; }
    public float getValue() {
        return value;
    }
    public void setValue(float value) { this.value = value; }
    public int getStatus() { return status; }
    public void setStatus(int status) {
        lastStatus = new Date().getTime();
        this.status = status;
    }
    public Long getDue() {
        return due;
    }
    public void setDue(Long due) { this.due = due; }
    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }
    public String getAssignerName() { return assignerName; }
    public void setAssignerName(String assignerName) { this.assignerName = assignerName; }
    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }
    public Long getLastStatus() { return lastStatus; }
    public void setLastStatus(Long lastStatus) { this.lastStatus= lastStatus; }
    public int getColor() { return color;}
    public void setColor(int color) { this.color = color;}
    public String getFeedback() { return feedback;}
    public void setFeedback(String feedback) { this.feedback = feedback;}
    public boolean getSettled() { return settled; }
    public void setSettled() { this.settled = true; }

    public String toString() {
        return this.name + "," + this.value + "," + this.due +"," + status;
    }

    public boolean new_assignment() {
        Date d = new Date(due);
        // Hides overdue assignments and those not TO DO
        return (!d.before(new Date()) || DateUtils.isToday(d.getTime())) && status == constant.STATUS_TO_DO;
    }

    public boolean new_or_overdue_assignment() {
        Date d = new Date(due);
        // Hides overdue assignments and those not TO DO
        return status == constant.STATUS_TO_DO;
    }

    public boolean overdue_assignment() {
        Date d = new Date(due);
        return d.before(new Date()) && !DateUtils.isToday(d.getTime());
    }

    public boolean completeAssignment() {
        Date d = new Date(due);
        // Hides overdue assignments and those not TO DO
        return status != constant.STATUS_COMPLETE;
    }

    public boolean overdueAndDiscardedAssignment() {
        Date d = new Date(due);

        if(status==constant.STATUS_COMPLETE)
            return false;

        if(status==constant.STATUS_DISCARDED)
            return true;

        return d.before(new Date()) && !DateUtils.isToday(d.getTime());

    }


    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("name", name);
        result.put("detail", detail);
        result.put("value", value);
        result.put("status", status);
        result.put("due", due);
        result.put("assignedBy", assignedBy);
        result.put("assignerName", assignerName);
        result.put("created", created);
        result.put("lastStatus", lastStatus);
        result.put("color", color);
        result.put("feedback", feedback);
        result.put("settled", settled);

        return result;
    }

}
