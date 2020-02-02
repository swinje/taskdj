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

import com.oslo7.tdjpro.constant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class User implements Serializable {

    private static final String TAG = "User";
    private String uid;
    public UserDetail userD;
    private ArrayList<Friend> friends = new ArrayList<>();  // Array List of UID of friends
    private ArrayList<Assignment> assignments = new ArrayList<>(); // Array List of Assignments

    public User() {
        this.userD = new UserDetail();
    }

    public User(String uid, String mail, String name) {
        this.uid = uid;
        this.userD = new UserDetail(mail, name);
    }

    public UserDetail getUserD() {
        return userD;
    }

    public void setUserD(UserDetail uD) {
        this.userD = uD;
    }

    public String getUid() {
        return this.uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }


    public ArrayList<Friend> getFriends() {
        return friends;
    }

    public boolean getFriendsMax() { return (friends.size() >= userD.getLimitFriends()); }

    public boolean getAssignmentsMax() { return (assignments.size() >= userD.getLimitAssignments()); }

    public boolean isFriend(String uid) {
        for (Friend f : friends) {
            if (f.getUid().equals(uid))
                return true;
        }
        return false;
    }

    public ArrayList<Friend> getManagedFriends() {
        ArrayList<Friend> managedFriends = new ArrayList<>();

        for (Friend f : friends) {
            if (f.getManager())
                managedFriends.add(f);
        }

        return managedFriends;
    }

    public void setFriends(ArrayList<Friend> friends) {
        this.friends = new ArrayList<>(friends);
    }

    public void removeFriend(String friendID) {
        ArrayList<Friend> newFriends = new ArrayList<>();

        if (friends == null)
            return;

        for (Friend f : friends) {
            if (!f.getUid().equals(friendID))
                newFriends.add(f);
        }

        this.friends = new ArrayList<>(newFriends);
    }


    public void addFriend(Friend f) {
       this.friends.add(f);
    }

    public void addBalanceToFriend(String fid, float value) {
        if (friends == null)
            return;

        for (Friend cf : friends) {
            if (cf.getUid().equals(fid))
                cf.addBalance(value);
        }
    }

    public void subtractBalanceFromFriend(String fid, float value) {
        if (friends == null)
            return;

        for (Friend cf : friends) {
            if (cf.getUid().equals(fid))
                cf.subtractBalance(value);
        }
    }

    public void zeroFriendBalance(String fid) {
        if (friends == null)
            return;

        for (Friend cf : friends) {
            if (cf.getUid().equals(fid)) {
                cf.setBalance(0);
            }
        }
    }

    public float getFriendBalance(String fid)  {
        if (friends == null)
            return -1;

        for (Friend cf : friends) {
            if (cf.getUid().equals(fid))
                return cf.getBalance();
        }
        return -1;
    }


    public ArrayList<Assignment> getAssignments() {
        return assignments;
    }

    public ArrayList<Assignment> getNewAssignments(String assigner) {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
            if(a.getAssignedBy().equals(assigner))
                if(a.new_assignment())
                    newAsg.add(a);
        }
        return newAsg;
    }

    public ArrayList<Assignment> getAssignments(String assigner) {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
            if(a.getAssignedBy().equals(assigner))
                newAsg.add(a);
        }
        return newAsg;
    }

    public ArrayList<Assignment> getAssignmentsNotDiscarded() {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
            if(a.getStatus() != constant.STATUS_DISCARDED)
                newAsg.add(a);
        }
        return newAsg;
    }

    public ArrayList<Assignment> getCompletedAssignments(String assigner) {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
            if(a.getAssignedBy().equals(assigner))
                if(a.getStatus()== constant.STATUS_COMPLETE)
                    newAsg.add(a);
        }
        return newAsg;
    }

    public ArrayList<Assignment> getCompletedAssignments() {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
                if(a.getStatus()== constant.STATUS_COMPLETE)
                    newAsg.add(a);
        }
        return newAsg;
    }

    public ArrayList<Assignment> getOverdueAndDiscardedAssignments(String assigner) {
        ArrayList<Assignment> newAsg = new ArrayList<>();
        for (Assignment a : assignments) {
            if(a.getAssignedBy().equals(assigner))
                if (a.overdueAndDiscardedAssignment())
                    newAsg.add(a);
        }
        return newAsg;
    }


    public Assignment getAssignment(int pos) { return assignments.get(pos);}

    public Assignment getAssignment(int pos, int sel) {
        ArrayList<Assignment> newAsg = new ArrayList<>();

        for (Assignment a : assignments) {
            switch (sel) {
                case constant.STATUS_TO_DO:
                    if (a.getStatus() == constant.STATUS_TO_DO)
                        if(!a.overdue_assignment())
                            newAsg.add(a);
                    break;
                case constant.STATUS_COMPLETE:
                    if (a.getStatus() == constant.STATUS_COMPLETE)
                        newAsg.add(a);
                    break;
                default:
                    if (a.getStatus() == constant.STATUS_DISCARDED || a.overdue_assignment())
                    newAsg.add(a);
                    break;
            }
        }

        return newAsg.get(pos);
    }


    public void setAssignments(ArrayList<Assignment> assignments) {
        this.assignments = new ArrayList<>(assignments);
    }

    public void updateAssignment(String name, Long created, int beforeStatus, int afterStatus, String feedback, Long lastStatus, int color ) {
        int ind = searchAssignment(name, created);
        if(ind<0)
            return;
        if(beforeStatus!=afterStatus) // User has changed status go with user
            assignments.get(ind).setStatus(afterStatus);
        assignments.get(ind).setFeedback(feedback);
        assignments.get(ind).setColor(color);
        assignments.get(ind).setLastStatus((new Date().getTime()));
    }

    public void addAssignment(Assignment assignment) {
        this.assignments.add(assignment);
    }

    public void removeAssignment(Assignment asg) {
        ArrayList<Assignment> newAsg = new ArrayList<>();

        if (asg == null)
            return;

        for (Assignment a : assignments) {
            if (!a.getName().equals(asg.getName()))
                newAsg.add(a);
            else if (!a.getCreated().equals(asg.getCreated()))
                newAsg.add(a);

        }

        this.assignments = new ArrayList<>(newAsg);
    }

    public void replaceAssignment(Assignment oldAsg, Assignment newAsg) {
        ArrayList<Assignment> updAsg = new ArrayList<>();

        if (oldAsg == null)
            return;

        for (Assignment a : assignments) {
            if (a.getName().equals(oldAsg.getName()) && a.getCreated().equals(oldAsg.getCreated()))
                updAsg.add(newAsg);
            else
                updAsg.add(a);
        }

        this.assignments = new ArrayList<>(updAsg);
    }

    public int searchAssignment(String name, Long created) {
        int ind = 0;
        for (Assignment a : assignments) {
            if (a.getName().equals(name) && a.getCreated().equals(created))
                return ind;
            ind++;
        }
        return -1;
    }

    public Assignment fetchAssignment(String name, Long created) {
        for (Assignment a : assignments) {
            if (a.getName().equals(name) && a.getCreated().equals(created))
                return a;
        }
        return null;
    }

    public int countNewAssignments() {
        int cnt = 0;

        for (Assignment a : assignments) {
            if(a.new_assignment())
                cnt++;
        }

        return cnt;
    }

    public ArrayList<Integer>  myAssignmentOverview() {
        int overdue  = 0;
        int complete = 0;
        int new_assignments = 0;
        int discarded = 0;
        int approved = 0;
        int pay_received = 0;

        for (Assignment a : assignments) {
            switch (a.getStatus()) {
                case constant.STATUS_TO_DO:
                    if (!a.overdue_assignment())
                        new_assignments++;
                    else
                        overdue++;
                    break;
                case constant.STATUS_COMPLETE:
                    complete++;
                    break;
                case constant.STATUS_DISCARDED:
                    discarded++;
                    break;
                case constant.STATUS_APPROVED:
                    approved++;
                    break;
                case constant.STATUS_PAY_RECEIVED:
                    pay_received++;
                    break;
            }
        }

        ArrayList<Integer> retArr = new ArrayList<>();
        retArr.add(new_assignments);
        retArr.add(complete);
        retArr.add(overdue);
        retArr.add(discarded);
        retArr.add(approved);
        retArr.add(pay_received);
        return retArr;
    }

    public ArrayList<Integer> assignmentOverview(String assigner) {
        int overdue  = 0;
        int complete = 0;
        int new_assignments = 0;
        int discarded = 0;
        int approved = 0;
        int pay_received = 0;

        for (Assignment a : assignments) {
            if(a.getAssignedBy().equals(assigner)) {
                switch (a.getStatus()) {
                    case constant.STATUS_TO_DO:
                        if (!a.overdue_assignment())
                            new_assignments++;
                        else
                            overdue++;
                        break;
                    case constant.STATUS_COMPLETE:
                        complete++;
                        break;
                    case constant.STATUS_DISCARDED:
                        discarded++;
                        break;
                    case constant.STATUS_APPROVED:
                        approved++;
                        break;
                    case constant.STATUS_PAY_RECEIVED:
                        pay_received++;
                        break;
                }
            }
        }

        ArrayList<Integer> retArr = new ArrayList<>();
        retArr.add(new_assignments);
        retArr.add(complete);
        retArr.add(overdue);
        retArr.add(discarded);
        retArr.add(approved);
        retArr.add(pay_received);
        return retArr;

    }

    public void removeComplete() {
        ArrayList<Assignment> newAsg = new ArrayList<>();

        for (Assignment a : assignments) {
            if (!(a.getStatus()==constant.STATUS_COMPLETE))
                newAsg.add(a);
        }
        this.assignments = new ArrayList<>(newAsg);
    }

    public void removeAssignmentsGivenBy(String uid) {
        ArrayList<Assignment> newAsg = new ArrayList<>();

        for (Assignment a : assignments) {
            if (!a.getAssignedBy().equals(uid))
                newAsg.add(a);
        }
        this.assignments = new ArrayList<>(newAsg);
    }


    public String toString() {
        return "User: " + uid + "," + userD.toString() + "," + friends  + "," + assignments;
    }


    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();

        result.put("uid", uid);
        result.put("userD", userD);
        result.put("friends", friends);
        result.put("assignments", assignments);

        return result;
    }


}
