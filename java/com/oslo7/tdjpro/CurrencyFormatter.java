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

import java.util.Formatter;


public class CurrencyFormatter {

    String[] currencies;

    public CurrencyFormatter(Context context) {
        currencies = context.getResources().getStringArray(R.array.currency_list);
    }

    public String formatCurrency(float value, int currID) {

        if (currencies == null)
            return null;

        StringBuilder sbuf = new StringBuilder();
        Formatter fmt = new Formatter(sbuf);

        System.out.print(sbuf.toString());

        if (currID == 0)
            return "";

        if (currID == 1)
            if (value != 0)
                return fmt.format("%d", (int) value).toString();
            else
                return "";

        return currencies[currID] + " " + fmt.format("%.2f", value).toString();

    }

    public String getCurrency(int currID) {
        if (currID == 0 || currID == 1)
            return "";
        else
            return currencies[currID];
    }
}
