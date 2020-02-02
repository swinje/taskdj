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

import android.graphics.Color;

public class constant {

    public static String UID = "UID";

    public static final int DB_RESULT_OK = 1;
    public static final int DB_RESULT_ERROR = 0;
    public static final int DB_RESULT_NOT_FOUND = 2;
    public static final int DB_RESULT_EXISTS = 3;

    public static final int DB_ADD_USER = 1;
    public static final int DB_DEL_USER = 2;
    public static final int DB_UPD_USER = 3;
    public static final int DB_GET_USER = 4;
    public static final int DB_GET_FRIENDS = 5;
    public static final int DB_EXIST_USER = 6;
    public static final int DB_MERGE_USER = 7;

    public static final int STATUS_TO_DO = 1;
    public static final int STATUS_COMPLETE = 2;
    public static final int STATUS_DISCARDED = 3;
    public static final int STATUS_APPROVED = 4;
    public static final int STATUS_PAY_RECEIVED = 5;

    public static final int CHANGE_ADD = 1;
    public static final int CHANGE_UPDATE = 2;
    public static final int CHANGE_DELETE = 3;

    public static int[] colors={Color.DKGRAY, Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW};


}
