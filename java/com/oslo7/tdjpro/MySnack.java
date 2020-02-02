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

import android.view.View;

import com.google.android.material.snackbar.Snackbar;


public class MySnack {

    Object callback;

    public interface SnackInterface {
        void onSnackCompleted();
    }

    View v;

    public MySnack(View v) {
        this.v = v;
    }

    public void MakeSnack(String txt) {

        Snackbar mySnackbar = Snackbar.make(v, txt,
                Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction(R.string.ok, new SnackListener());
        mySnackbar.show();
    }

    public void MakeFadeSnack(String txt) {

        Snackbar mySnackbar = Snackbar.make(v, txt,
                Snackbar.LENGTH_SHORT);
        mySnackbar.show();
    }

    public void MakeSnack(String txt, Object cb) {
        callback = cb;

        Snackbar mySnackbar = Snackbar.make(v, txt,
                Snackbar.LENGTH_INDEFINITE);
        mySnackbar.setAction(R.string.ok, new SnackListener());
        mySnackbar.show();
    }


    class SnackListener implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            if(callback != null)
                ((SnackInterface) callback).onSnackCompleted();
        }
    }

}
