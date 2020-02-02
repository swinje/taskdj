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

import android.util.Base64;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class UserDetail implements Serializable {

    private String name;
    private String mail;
    private int currency;
    private boolean searchable;
    private boolean acceptedTerms;
    private boolean notifications;
    private int assignmentsCompleted;
    private float totalEarnings;
    private boolean adFree;
    private int limitFriends = 20;
    private int limitAssignments = 20;
    private ArrayList<String> tokens = new ArrayList<>(); // Array List of device tokens
    private ArrayList<String> settlements = new ArrayList<>();


    public UserDetail() {
        this.mail = null;
        this.name = null;
        this.searchable = true;
        this.assignmentsCompleted = 0;
        this.totalEarnings = 0;
        this.adFree = false;
        this.limitAssignments = 20;
        this.limitFriends = 20;
    }

    public UserDetail(String mail, String name) {
        this.mail = mail;
        this.name = name;
        this.searchable = true;
        this.assignmentsCompleted = 0;
        this.totalEarnings = 0;
        this.adFree = false;
        this.limitAssignments = 20;
        this.limitFriends = 20;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getName() { return this.name; }

    public String getMail() {
        return this.mail;
    }
    public String getDecodeMail() {
        byte[] data = Base64.decode(this.mail, Base64.DEFAULT);
        return Arrays.toString(data);
    }
    public void setMail(String mail) {
        this.mail = mail;
    }
    public float getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(float totalEarnings) { this.totalEarnings = totalEarnings;}
    public void addTotalEarnings(float earning) { this.totalEarnings += +earning; }
    public void setSearchable(boolean searchable) { this.searchable = searchable;}
    public boolean getSearchable() { return searchable; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }
    public boolean getAcceptedTerms() {return acceptedTerms; }
    public void setNotifications(boolean notifications) { this.notifications = notifications;}
    public boolean getNotifications() { return notifications; }
    public int getCurrency() { return this.currency; }
    public void setCurrency(int currency) { this.currency = currency; }
    public int getAssignmentsCompleted() { return this.assignmentsCompleted; }
    public void setAssignmentsCompleted(int assignmentsCompleted) { this.assignmentsCompleted = assignmentsCompleted;}
    public void addAssignmentsCompleted() { this.assignmentsCompleted++; }
    public void subAssigmentsCompleted() { if(this.assignmentsCompleted>0) this.assignmentsCompleted--; }
    public boolean getAdFree() { return this.adFree; }
    public void setAdFree(boolean state) { this.adFree = state; }
    public int getLimitFriends() { return this.limitFriends;}
    public void setLimitFriends(int l) { this.limitFriends = l; }
    public int getLimitAssignments() { return this.limitAssignments; }
    public void setLimitAssignments(int l) { this.limitAssignments = l; }


    public ArrayList<String> getTokens() {
        return tokens;
    }
    public void setTokens(ArrayList<String> tokens) {
        this.tokens= new ArrayList<>(tokens);
    }

    public void addToken(String token) {

        /* Multiple tokens ignore that for now
        if (token == null)
            return;

        boolean token_found = false;

        for (String t : tokens) {
            if (t.equals(token))
                token_found = true;
        }

        if(!token_found)
            this.tokens.add(token);
            */

        this.tokens.clear();
        this.tokens.add(token);
    }

    public void removeToken(String token) {
        ArrayList<String> newTokens = new ArrayList<>();

        if (tokens == null)
            return;

        for (String t : tokens) {
            if (!t.equals(token))
                newTokens.add(t);
        }

        this.tokens = new ArrayList<>(newTokens);
    }


    public ArrayList<String> getSettlements() {
        return settlements;
    }

    public void addSettlement(String desc) {

        LinkedList<String> lSettlements = new LinkedList<>(settlements);

        if(lSettlements.size() > 5)
            lSettlements.removeLast();

        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = format1.format(c.getTime());

        lSettlements.addFirst(formatted + ": " + desc);

        settlements = new ArrayList<>(lSettlements);
    }

    public void deleteSettlements() {
        settlements = new ArrayList<>();
    }

    public String toString() {
        return "UserDetail: <<" + name + "," + mail + "," + currency + "," + searchable +  "," +
                tokens +"," + acceptedTerms + "," + notifications + "," + settlements + "," +
                assignmentsCompleted + "," + totalEarnings + " ADFREE:" + adFree + ">>";
    }


    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("name", name);
        result.put("mail", mail);
        result.put("currency", currency);
        result.put("searchable", searchable);
        result.put("tokens", tokens);
        result.put("acceptedTerms", acceptedTerms);
        result.put("notifications", notifications);
        result.put("settlements", settlements);
        result.put("assignmentsCompleted", assignmentsCompleted);
        result.put("totalEarnings", totalEarnings);
        result.put("adfree", adFree);
        result.put("limitFriends", limitFriends);
        result.put("limitAssignments", limitAssignments);

        return result;
    }

}
