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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONException;

import java.util.ArrayList;

/**
 * Receiver for geofence transition changes.
 * <p>
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a JobIntentService
 * that will handle the intent in the background.
 */
public class RNGeoTransitionBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "RNGeofenceReceiver";

    public static final String GEOFENCE_TRANSITION = "GeofenceTransition";
    public static final String TRIGGERING_LOCATION = "TriggeringLocation";
    public static final String TRIGGERING_GEOFENCES = "TriggeringGeofences";

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by Location Services. This Intent is provided to Location
     *                Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle b;

        if (intent.getExtras() != null) {
            GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
            if (geofencingEvent.hasError()){
                b = TesJobDispatcher.setError("Invalid Geoevent received",  geofencingEvent.getErrorCode());
            }
            else {
                b = TesJobDispatcher.setData(geofencingEvent);
            }

            TesJobDispatcher jm = new TesJobDispatcher(context);
            TesConfig cfg = new TesConfig();
            try {
                cfg = TesConfig.getSavedConfig(context);
            } catch (JSONException e) {
                Log.e(TAG, "Failed to get saved configs - using defaults. invalid JSON found");
                e.printStackTrace();
            }

            jm.scheduleJob(RNGeoTransitionService.class,
                    cfg.delay,
                    cfg.deadline,
                    cfg.networkType,
                    cfg.requireIdle,
                    cfg.requireCharging, b);
        }
    }
}
