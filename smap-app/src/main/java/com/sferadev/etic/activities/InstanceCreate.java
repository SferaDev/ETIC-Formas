/*
 * Copyright (C) 2011 Cloudtec Pty Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sferadev.etic.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.sferadev.etic.utilities.ManageForm;
import com.sferadev.etic.utilities.ManageFormResponse;

/**
 * Creates an instance with initial data
 * This instance represents a task
 * Parameters:
 * 1) form name
 * 2) form url
 * 3) Initial instance data url
 */
public class InstanceCreate extends Activity {

    ManageFormResponse mfResponse = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        /*
         * 1. Get the parameters (task id / form name, form URL, initial instance data, update type) 
         * 2. Get the form path from the forms database using the taskId as the form name
         *      b) If this does not exist download the form using the form URL
         *         The requested taskId is discarded and does not have to match the id of the down loaded form
         * 3. Get the instance path
         * 4. Write the instance initial data to the sdcard
         * 5. Create the new instance entry in the content provider
         */
        String taskId = null;
        String formURL = null;
        String initialDataURL = null;


        // Get the input parameters
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            taskId = extras.getString("taskId");
            formURL = extras.getString("formURL");
            initialDataURL = extras.getString("initialDataURL");
        }

        if (taskId != null) {
            Log.i("InstanceCreate taskid", taskId);
        }    // Debug
        if (formURL != null) {
            Log.i("InstanceCreate formURL", formURL);
        }    // Debug
        if (initialDataURL != null) {
            Log.i("InstanceCreate instanceData", initialDataURL);
        }    // Debug

        ManageForm mf = new ManageForm();
        mfResponse = mf.insertForm(taskId, formURL, initialDataURL, "1");

        finish();
    }


    /*
     * (non-Javadoc)
     * @see android.app.Activity#finish()
     * Return data to calling application
     */
    @Override
    public void finish() {
        Intent data = new Intent();

        if (mfResponse != null) {
            if (mfResponse.isError) {

                data.putExtra("status", "error");
                data.putExtra("message", mfResponse.statusMsg);

            } else {

                String instanceUri = mfResponse.mUri.toString();
                data.putExtra("status", "success");
                data.putExtra("instanceUri", instanceUri);

            }
            setResult(RESULT_OK, data);
        }
        super.finish();
    }


}
