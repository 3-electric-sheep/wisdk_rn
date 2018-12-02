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


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import androidx.work.Data;
import androidx.work.WorkerParameters;

/**
 * Service launched when a location update occurs. this gets called from inside a broadcast receiver
 */
public class RNLocationUpdateService extends HeadlessJsJobService
{
    private static final String TAG = "RNLocationUpdateService";

    public RNLocationUpdateService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    @Nullable
    protected HeadlessJsTaskConfig getTaskConfig(Data extras)
    {

        WritableMap jsArgs = Arguments.createMap();
        if (extras == null) {
            jsArgs.putBoolean("success", false);
            jsArgs.putInt("code", TesJobDispatcher.ERROR_BUNDLE_DATA);
            jsArgs.putString("error", "Null Bundle data");
        }
        else {
            if (!TesJobDispatcher.isSuccess(extras)) {
                jsArgs.putBoolean("success", false);
                jsArgs.putInt("code", TesJobDispatcher.getErrorCode(extras));
                jsArgs.putString("error", TesJobDispatcher.getErrorMessage(extras));
            } else {
                JSONObject result = null;
                try {
                    result = TesJobDispatcher.getJsonData(extras);

                    JSONArray loclist = result.optJSONArray(TesJobDispatcher.TES_KEY_LOCATIONS);
                    JSONObject lastloc = result.optJSONObject(TesJobDispatcher.TES_KEY_LASTLOCATION);

                    WritableMap lastLocation = TesUtils.toLocMap(lastloc);

                    WritableArray locationList = Arguments.createArray();
                    if (loclist != null && loclist.length() > 0) {
                        for (int i = 0; i < loclist.length(); i++) {
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
                    jsArgs.putString("error", "Failed to decode JSON location results: " + e.getMessage());
                }
            }
        }

        Log.d(TAG, "Report location update event to JS: " + jsArgs);
        return new HeadlessJsTaskConfig(RNWisdkModule.LOC_UPDATE_TASK_NAME, jsArgs, 0, true);
    }

}
