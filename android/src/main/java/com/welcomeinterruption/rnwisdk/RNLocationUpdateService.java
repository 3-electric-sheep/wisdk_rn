//  Copyright © 2012-2018 3 Electric Sheep Pty Ltd. All rights reserved.
//
//  The Welcome Interruption Software Development Kit (SDK) is licensed to you subject to the terms
//  of the License Agreement. The License Agreement forms a legally binding contract between you and
//  3 Electric Sheep Pty Ltd in relation to your use of the Welcome Interruption SDK.
//  You may not use this file except in compliance with the License Agreement.
//
//  A copy of the License Agreement can be found in the LICENSE file in the root directory of this
//  source tree.
//
//  Unless required by applicable law or agreed to in writing, software distributed under the License
//  Agreement is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
//  express or implied. See the License Agreement for the specific language governing permissions
//  and limitations under the License Agreement.

/**
 * Created by pfrantz on 3/2/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */


package com.welcomeinterruption.rnwisdk;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationResult;

import com.firebase.jobdispatcher.JobParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;

/**
 * Service launched when a location update occurs. this gets called from inside a broadcast receiver
 */
public class RNLocationUpdateService extends HeadlessJsJobService
{
    private static final String TAG = "RNLocationUpdateService";

    @Override
    @Nullable
    protected HeadlessJsTaskConfig getTaskConfig(JobParameters params)
    {
        if (params.getExtras() == null) {
            return null;
        }


        Bundle extras = params.getExtras();

        WritableMap jsArgs = Arguments.createMap();
        if (!TesJobDispatcher.isSuccess(extras)) {
            jsArgs.putBoolean("success", false);
            jsArgs.putInt("code", TesJobDispatcher.getErrorCode(extras));
            jsArgs.putString("error", TesJobDispatcher.getErrorMessage(extras));
        }
        else {
            JSONObject result = null;
            try {
                result = TesJobDispatcher.getJsonData(extras);

                JSONArray loclist = result.optJSONArray(TesJobDispatcher.TES_KEY_LOCATIONS);
                JSONObject lastloc = result.optJSONObject(TesJobDispatcher.TES_KEY_LASTLOCATION);

                WritableMap lastLocation = TesUtils.toLocMap(lastloc);

                WritableArray locationList = Arguments.createArray();
                if (loclist != null && loclist.length()>0) {
                    for (int i=0; i<loclist.length(); i++) {
                        JSONObject loc = loclist.getJSONObject(i);
                        WritableMap locEntry = TesUtils.toLocMap(loc);
                        locationList.pushMap(locEntry);
                    }
                }
                jsArgs.putBoolean("success", true);
                jsArgs.putArray("locations", locationList);
                jsArgs.putMap("lastLocation", lastLocation);
            } catch (JSONException e) {
                jsArgs.putBoolean("success", false);
                jsArgs.putInt("code", TesJobDispatcher.ERROR_JSON_ENCODE_DECODE);
                jsArgs.putString("error", "Failed to decode JSON location results: "+e.getMessage());
            }
        }

        Log.d(TAG, "Report location update event to JS: " + jsArgs);
        return new HeadlessJsTaskConfig(RNWisdkModule.LOC_UPDATE_TASK_NAME, jsArgs, 0, true);
    }

}
