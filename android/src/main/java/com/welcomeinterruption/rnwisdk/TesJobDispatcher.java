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

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.LocationResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;


/**
 * Created by pfrantz on 2/6/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */
public class TesJobDispatcher {
    public static String TES_JOB_KEY_PARAMS = "TesJobKeyParamns";

    public static String TES_JOBPARAM_SUCCESS = "success";
    public static String TES_JOBPARAM_MSG = "msg";
    public static String TES_JOBPARAM_CODE = "code";
    public static String TES_JOBPARAM_DATA = "data";

    public static String TES_KEY_LOCATIONS = "locations";
    public static String TES_KEY_LASTLOCATION = "lastLocation";
    public static String TES_KEY_TRIGGERING_GEOFENCES = "triggeringGeofences";
    public static String TES_KEY_TRIGGERING_LOCATION = "triggeringLocation";
    public static String TES_KEY_GEOFENCE_TRANSITION = "geofenceTransition";

    public static int ERROR_JSON_ENCODE_DECODE = -3;
    public static int ERROR_BUNDLE_DATA = -2;


    private static final String TAG = "TesJobDispatcher";

    private static int mJobId = 0;

    private static synchronized int nextJobId() {
        return ++mJobId;
    }


    public TesJobDispatcher() {
    }

    /**
     * Set an error in the bundle to pass to the backend
     * @param msg - message to set
     * @param code - error code
     * @return bundle
     */
    public static Data setError(String msg, int code){
        Data.Builder b = new Data.Builder();
        b.putInt(TES_JOBPARAM_SUCCESS, 0);
        b.putString(TES_JOBPARAM_MSG, msg);
        b.putInt(TES_JOBPARAM_CODE, code);
        return b.build();
    }

    /**
     * sets data field to a Location result and returns a new bundle with the field set internally
     *
     * NOTE: we have to json things as the firebase job scheduler only supports basic data types like strings, numbers, etc..
     *
     * @param lr - location result
     * @return bunder
     */
    public static Data setData(LocationResult lr) {
        Data.Builder b = new Data.Builder();
        try {
            List<Location> locations = lr.getLocations();

            JSONArray locArray = TesUtils.locationsToJson(locations, true);
            JSONObject lastLoc = new TesLocationInfo(lr.getLastLocation(), true).toDictionary();
            JSONObject params = new JSONObject();
            params.put(TES_KEY_LOCATIONS, locArray);
            params.put(TES_KEY_LASTLOCATION, lastLoc);

            String json = params.toString();

            b.putInt(TES_JOBPARAM_SUCCESS, 1);
            b.putString(TES_JOBPARAM_DATA, json);
        }
        catch (JSONException e){
            setError("Faled to encode location to JSON "+e.getMessage(), -3);
        }
        return b.build();
    }

    /**
     * sets data field to a Geofence event result and returns a new bundle with the field set internally
     *
     * NOTE: we have to json things as the firebase job scheduler only supports basic data types like strings, numbers, etc..
     *
     * @param ge - geofence envent
     * @return bunder
     */
    public static Data setData(GeofencingEvent ge) {
        Data.Builder b = new Data.Builder();
        try {

            JSONObject params = new JSONObject();
            JSONArray regionIdentifiers = new JSONArray();
            for (Geofence triggered : ge.getTriggeringGeofences()) {
                regionIdentifiers.put(triggered.getRequestId());
            }
            JSONObject loc = new TesLocationInfo(ge.getTriggeringLocation(), true).toDictionary();

            params.put(TES_KEY_TRIGGERING_GEOFENCES, regionIdentifiers);
            params.put(TES_KEY_TRIGGERING_LOCATION, loc);
            params.put(TES_KEY_GEOFENCE_TRANSITION, ge.getGeofenceTransition());

            String json = params.toString();

            b.putInt(TES_JOBPARAM_SUCCESS, 1);
            b.putString(TES_JOBPARAM_DATA, json);
        }
        catch (JSONException e){
            setError("Faled to encode geoevtn to JSON "+e.getMessage(), -3);
        }
        return b.build();
    }

    /**
     * returns a json object from bundled up data
     * @param b - bundle containting jsonified geoevent or location result
     * @return JSON obbject
     */
    public static JSONObject getJsonData(Data b) throws JSONException {
        String json = b.getString(TES_JOBPARAM_DATA);
        return new JSONObject(json);
    }


    /**
     * bundle check to see what success is set to
     * @param b - bunle to check
     * @return success - true
     */
    public static boolean isSuccess(Data b){
        return (b.getInt(TES_JOBPARAM_SUCCESS, 0) == 1);
    }

    public static int getErrorCode(Data b){
        return b.getInt(TES_JOBPARAM_CODE, 0);
    }

    public static String getErrorMessage(Data b){
        return b.getString(TES_JOBPARAM_MSG);
    }

    /**
     * Schedule a job
     * @param cls - class of job
     * @param extras - bundle to pass to the job
     */
    public void scheduleJob(@NonNull Class<? extends Worker> cls, final @Nullable Data extras){

        Constraints myConstraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest.Builder builder =
                new OneTimeWorkRequest.Builder(cls).setConstraints(myConstraints);

        if (extras!=null) {
            builder.setInputData(extras);
        }

        OneTimeWorkRequest myJob = builder.build();
        WorkManager.getInstance().enqueue(myJob);
    }


}
