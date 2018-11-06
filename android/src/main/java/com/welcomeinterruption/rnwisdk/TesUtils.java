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

import android.app.ActivityManager;
import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by pfrantz on 3/2/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

public class TesUtils {
    private static final String TAG = "TESUtils";


    /**
     * -------------------
     * date util functions
     * -------------------
     **/

    static public Date dateFromString(@Nullable String dateString) throws ParseException {
        if (dateString == null || dateString.length() < 1)
            return null;

        return TesISO8601DateParser.parse(dateString);
    }

    static public String stringFromDate(@Nullable Date date)
    {
        if (date == null)
            return null;

        return TesISO8601DateParser.toString(date);
    }


    /**
     * JSON helpers
     */

    public static JSONObject mergeJSONObjects(JSONObject json1, JSONObject json2) throws JSONException {
        final JSONObject mergedJSON = new JSONObject();

        Iterator<String> i = json1.keys();
        while (i.hasNext()){
            String key = i.next();
            mergedJSON.put(key, json1.get(key));
        }

        i = json2.keys();
        while (i.hasNext()){
            String key = i.next();
            mergedJSON.put(key, json2.get(key));
        }

        return mergedJSON;
    }

    /**
     * Finds the index of the object who key has the given value
     * @param key the key of the object entry
     * @param value the value of the key to look for
     * @param stringArray the jsonarray to search
     * @return -1 = not found else the index
     */
    public static int findJsonArrayIndex(String key, String value, JSONArray stringArray){
        int index = -1;
        for (int i=0; i<stringArray.length(); i++){
            JSONObject obj = null;
            try {
                obj = stringArray.getJSONObject(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get list: %s", e.toString()));
            }

            String oid = null;
            try {
                oid = obj.getString(key);
                if (oid.equals(value)){
                    index = i;
                    break;
                }
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get key list entry: %s", e.toString()));

            }
        }
        return index;
    }

    public static int findJsonArrayIndex(String value, JSONArray stringArray){
        int index = -1;
        for (int i=0; i<stringArray.length(); i++){
            String obj = null;
            try {
                obj = stringArray.getString(i);
            } catch (JSONException e) {
                Log.i(TAG, String.format("Unable to get list: %s", e.toString()));
            }

            if (obj != null && obj.equals(value)){
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Converts a list of locations to a JSON array
     * @param locations location list
     * @param inBackground whether the list came while the app was not running in foreground.
     * @return location list as a JSON array
     * @throws JSONException
     */
    public static JSONArray locationsToJson(List<Location> locations, boolean inBackground) throws JSONException
    {
        JSONArray json = new JSONArray();
        for (Location loc: locations){
            json.put(new TesLocationInfo(loc, inBackground).toDictionary());
        }
        return json;
    }

    /** converts a json TesLocation to a facebook writable map
     *
     * @param json - json that represents a TesLocationInfo
     * @return a writealbe map
     * @throws JSONException
     */
    public static WritableMap toLocMap(JSONObject json) throws JSONException{
        TesLocationInfo loc = new TesLocationInfo(json);
        WritableMap locEntry = Arguments.createMap();

        locEntry.putDouble("longitude", loc.longitude);
        locEntry.putDouble("latitude", loc.latitude);
        locEntry.putDouble("accuracy", loc.accuracy);
        locEntry.putDouble("speed", loc.speed);
        locEntry.putDouble("course", loc.course);
        locEntry.putDouble("altitude", loc.altitude);
        locEntry.putString("fix_timestamp", TesUtils.stringFromDate(loc.fix_timestamp));
        locEntry.putBoolean("in_background", loc.inBackground);
        locEntry.putString("arrival", null);
        locEntry.putString("departure", null);
        return locEntry;
    }

    static public  boolean isAppOnForeground(Context context) {
        /**
         We need to check if app is in foreground otherwise the app will crash.
         http://stackoverflow.com/questions/8489993/check-android-application-is-in-foreground-or-not
         **/
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses =
                activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance ==
                    ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName.equals(packageName)) {
                return true;

            }
        }
        return false;
    }
}
