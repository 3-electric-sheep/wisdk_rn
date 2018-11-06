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

package com.welcomeinterruption.rnwisdk;

/**
 * Created by pfrantz on 1/2/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;


import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.google.android.gms.location.Geofence;
import com.firebase.jobdispatcher.JobParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;


/**
 * Service launched when a region transition occurs
 */
public class RNGeoTransitionService extends HeadlessJsJobService
{
    private static final String TAG = "RNGeoTransitionService";

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

                WritableArray regionIdentifiers = Arguments.createArray();
                JSONArray triggeringGeofences = result.optJSONArray(TesJobDispatcher.TES_KEY_TRIGGERING_GEOFENCES);
                if (triggeringGeofences != null) {
                    for (int i=0; i<triggeringGeofences.length(); i++) {
                        String region = triggeringGeofences.getString(i);
                        regionIdentifiers.pushString(region);
                    }
                }
                int geofenceTransition = result.optInt(TesJobDispatcher.TES_KEY_GEOFENCE_TRANSITION);

                JSONObject triggeringLoc = result.optJSONObject(TesJobDispatcher.TES_KEY_TRIGGERING_LOCATION);
                if (triggeringLoc != null) {
                    WritableMap loc = TesUtils.toLocMap(triggeringLoc);
                    loc.putInt("did_enter", (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) ? 1 : 0);
                    loc.putInt("did_exit", (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) ? 1 : 0);
                    loc.putInt("did_dwell", (geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) ? 1 : 0);
                    loc.putArray("region_identifier", regionIdentifiers);

                    jsArgs.putBoolean("success", true);
                    jsArgs.putMap("location", loc);

                }

            } catch (JSONException e) {
                jsArgs.putBoolean("success", false);
                jsArgs.putInt("code", TesJobDispatcher.ERROR_JSON_ENCODE_DECODE);
                jsArgs.putString("error", "Failed to decode JSON geofence event results: "+e.getMessage());
            }
        }
        return new HeadlessJsTaskConfig(RNWisdkModule.GEO_TRANSITION_TASK_NAME, jsArgs, 0, true);
    }
}
