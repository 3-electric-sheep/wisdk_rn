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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;

import java.util.List;
import java.util.UUID;

/**
 * Created by pfrantz on 1/2/18.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

public class TesGeofenceMgr {
    /**
     * Allows you to create and remove geofences using the GeofencingApi. Uses an IntentService
     * to monitor geofence transitions and creates notifications whenever a device enters or exits
     * a geofence.
     */

    private static final String TAG = "TesGeofenceMgr";

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;


    /**
     * Context to the main activity that all the intents will be bound too
     */
    private Context mCtx;

    /**
     * Config for the system
     */
    private final TesConfig config;

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;


    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    /**
     *  Constructor for class
     * @param ctx - the context (is the fragment)
     * @param config - the config object
     */

    public TesGeofenceMgr(@NonNull Context ctx, TesConfig config) {
        this.config = config;
        this.mCtx = ctx;
        this.mGeofencingClient = null;
        this.mGeofencePendingIntent = null;
    }

    public void connect() {
        // TODO: pjf - should I check if we have a client ?
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this.mCtx);
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    public void addGeofences(List<Geofence> geofencesToAdd,  OnCompleteListener<Void> listener) {
        mGeofencingClient.addGeofences(getGeofencingRequest(geofencesToAdd), getGeofencePendingIntent())
                .addOnCompleteListener(listener);
    }

    /**
     * Removes geofences by id. This method should be called after the user has granted the location
     * permission.
     */
    public void removeGeofences(List<String> ids, OnCompleteListener<Void> listener) {
        mGeofencingClient.removeGeofences(ids)
                .addOnCompleteListener(listener);
    }

    /**
     * Removes all geofences. This method should be called after the user has granted the location
     * permission.
     */
    public void clearGeofences( OnCompleteListener<Void> listener) {
        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).
                addOnCompleteListener(listener);
    }

    /**
     * Builds a geofence object that can be passed into addGeofences
     *
     * @param latitude lat of center
     * @param longitude long of center
     * @param radius in meters, < 0 implies config default
     * @param geoid id, if null, set to a random uuid
     * @return geoFence object
     */
    public Geofence buildGeofence(double latitude, double longitude, float radius, String geoid)
    {
        if (geoid == null)
            geoid = UUID.randomUUID().toString();

        if (radius < 0)
            radius = this.config.geoRadius;

        return new Geofence.Builder()
                .setRequestId(geoid)
                .setTransitionTypes(this.config.geoTransitionType)
                .setCircularRegion(latitude, longitude, radius)
                .setExpirationDuration(this.config.geoExpiry)
                .setLoiteringDelay(this.config.geoLoiteringDelay)
                .build();
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest(List<Geofence> geofencesToAdd) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(this.config.geoInitialTrigger);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(geofencesToAdd);

        // Return a GeofencingRequest.
        return builder.build();
    }


    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this.mCtx, RNGeoTransitionBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this.mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;

        // PJF - dont work in android 8 anymore
        //Intent intent = new Intent(this.mCtx, RNGeoTransitionService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        //return PendingIntent.getService(this.mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }


}
