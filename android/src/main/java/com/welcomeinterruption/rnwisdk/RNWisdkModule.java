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

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


import org.json.JSONException;


public class RNWisdkModule extends ReactContextBaseJavaModule {

    public static final String GEO_TRANSITION_TASK_NAME = "wisdk-geo-transition";
    public static final String LOC_UPDATE_TASK_NAME = "wisdk-location-update";
    public static final String BOOT_TASK_NAME = "wisdk-boot-tassk";

    public static final int ERROR_UNKNOWN = -1;
    public static final int ERROR_EMPTY_LOC = 1;

    private TesConfig config;
    private TesGeofenceMgr geofenceMgr;
    private TesLocationMgr locMgr;

    private final ReactApplicationContext reactContext;

    public RNWisdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.config = new TesConfig();
        this.geofenceMgr = new TesGeofenceMgr(this.reactContext, this.config);
        this.locMgr = new TesLocationMgr(this.reactContext, this.config);
    }

    @Override
    public String getName() {
        return "RNWisdk";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("GEO_TRANSITION_TASK_NAME", GEO_TRANSITION_TASK_NAME);
        constants.put("LOC_UPDATE_TASK_NAME", LOC_UPDATE_TASK_NAME);
        constants.put("BOOT_TASK_NAME", BOOT_TASK_NAME);
        return constants;
    }

    @ReactMethod
    public void configure(String json, Promise promise) {
        try {
            this.config.fromJSON(json);
            this.config.saveConfig(reactContext);
            promise.resolve(null);
        } catch (JSONException e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void connect(boolean requireBackgroundProcessing) {
        this.geofenceMgr.connect();
        this.locMgr.connect(requireBackgroundProcessing);
    }

    /**
     * gets last known location if any
     */
    @ReactMethod
    public void getLastKnownLocation(@Nullable final Promise promise) {
        this.locMgr.getLastKnownLocation(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful() && task.getResult() != null) {
                    Location loc = task.getResult();

                    WritableMap locEntry = Arguments.createMap();

                    locEntry.putDouble("longitude", loc.getLongitude());
                    locEntry.putDouble("latitude", loc.getLatitude());
                    locEntry.putDouble("accuracy", loc.getAccuracy());
                    locEntry.putDouble("speed", loc.getSpeed());
                    locEntry.putDouble("course", loc.getBearing());
                    locEntry.putDouble("altitude", loc.getAltitude());
                    locEntry.putString("fix_timestamp", TesUtils.stringFromDate(new Date(loc.getTime())));
                    locEntry.putBoolean("in_background", true);
                    locEntry.putString("arrival", null);
                    locEntry.putString("departure", null);
                    promise.resolve(locEntry);

                } else {
                    if (task.getResult() == null){
                        promise.reject(Integer.toString(ERROR_EMPTY_LOC), "Empty Location result");
                    }
                    else {
                        Exception exc = task.getException();
                        if (exc instanceof ApiException){
                            ApiException apiexc = (ApiException) exc;
                            promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                        }
                        else {
                            promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                        }
                    }
                }
            }
        });
    }

    /**
     * requests start of location updates.
     */
    @ReactMethod
    public void requestLocationUpdates(@Nullable final Promise promise) {
        this.locMgr.requestLocationUpdates(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                } else {
                    Exception exc = task.getException();
                    if (exc instanceof ApiException){
                        ApiException apiexc = (ApiException) exc;
                        promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                    }
                    else {
                        promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                    }
                }
            }
        });
    }

    /**
     * requests removal of location updates.
     */
    @ReactMethod
    public void removeLocationUpdates(@Nullable final Promise promise) {
        this.locMgr.removeLocationUpdates(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                } else {
                    Exception exc = task.getException();
                    if (exc instanceof ApiException){
                        ApiException apiexc = (ApiException) exc;
                        promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                    }
                    else {
                        promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                    }
                }
            }
        });
    }

    /**
     * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @ReactMethod
    public void addGeofences(@NonNull ReadableArray geofencesToAdd, @Nullable final Promise promise) {
        List<Geofence> geofenceList = new ArrayList<>();
        for (int i = 0; i < geofencesToAdd.size(); i++) {
            ReadableMap obj = geofencesToAdd.getMap(i);
            double lat = obj.getDouble("latitude");
            double lng = obj.getDouble("longitude");
            String ident = obj.getString("identifier");
            float radius = (float) (obj.hasKey("radius") ? obj.getInt("radius"): -1);
            geofenceList.add(this.geofenceMgr.buildGeofence(lat,lng,radius,ident));

        }

        this.geofenceMgr.addGeofences(geofenceList, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                } else {
                    Exception exc = task.getException();
                    if (exc instanceof ApiException){
                        ApiException apiexc = (ApiException) exc;
                        promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                    }
                    else {
                        promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                    }
                }
            }
        });
    }

    /**
     * Removes geofences by id. This method should be called after the user has granted the location
     * permission.
     */
    @ReactMethod
    public void removeGeofences(@NonNull ReadableArray ids, @Nullable final Promise promise) {
        List<String> idList = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            idList.add(ids.getString(i));
        }
        this.geofenceMgr.removeGeofences(idList, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                } else {
                    Exception exc = task.getException();
                    if (exc instanceof ApiException){
                        ApiException apiexc = (ApiException) exc;
                        promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                    }
                    else {
                        promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                    }
                }
            }
        });
    }

    /**
     * Removes all geofences. This method should be called after the user has granted the location
     * permission.
     */
    @ReactMethod
    public void clearGeofences(@Nullable final Promise promise) {
        this.geofenceMgr.clearGeofences(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    promise.resolve(null);
                } else {
                    Exception exc = task.getException();
                    if (exc instanceof ApiException){
                        ApiException apiexc = (ApiException) exc;
                        promise.reject(Integer.toString(apiexc.getStatusCode()), apiexc);
                    }
                    else {
                        promise.reject(Integer.toString(ERROR_UNKNOWN), exc);
                    }
                }
            }
        });
    }

}
