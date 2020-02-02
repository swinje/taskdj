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

import java.util.Calendar;
import java.util.Date;

public class DateFormatter {

    String[] days;

    public DateFormatter(Context context) {
        days = context.getResources().getStringArray(R.array.days);
    }

    public String formatDate(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
        int myDayOfWeek = dayOfWeek - 2;
        if (myDayOfWeek < 0) {
            myDayOfWeek += 7;
        }
        return  days[myDayOfWeek];
    }

}
