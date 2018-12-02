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

import android.Manifest;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;

import org.json.JSONException;
import org.json.JSONObject;

import androidx.work.NetworkType;

/**
 * Created by pfrantz on 11/08/2017.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

public class TesConfig implements Parcelable, Cloneable {
    // TODO : pjf ensure we change these for the android environment
    public static final String ENV_PROD = "prod";
    public static final String ENV_TEST = "test";

    public static final String PROD_SERVER = "https://api.3-electric-sheep.com";
    public static final String PROD_PUSH_PROFILE = "<PROD_PROFILE>";

    public static final String TEST_SERVER = "https://testapi.3-electric-sheep.com";
    public static final String TEST_PUSH_PROFILE = "<TEST_PROFILE>";

    public static final String WALLET_OFFER_CLASS = "wi_offer_class";
    public static final String WALLET_PROFILE = "email";
    public static final String MAIL_PROFILE = "email";
    public static final String SMS_PROFILE = "phone";

    public static final long MEM_CACHE_SIZE = 8388608; //8 * 1024 * 1024;
    public static final long DISK_CACHE_SIZE = 20971520; // 20 * 1024 * 1024;

    public static final String DEVICE_TYPE_GCM = "gcm";
    public static final String DEVICE_TYPE_MAIL = "mail";
    public static final String DEVICE_TYPE_SMS = "sms";
    public static final String DEVICE_TYPE_WALLET = "ap";
    public static final String DEVICE_TYPE_MULTIPLE = "multiple";

    // default job scheduler params
    private static final boolean JOB_DEFAULT_REQUIRE_CHARGING = false;
    private static final int JOB_DEFAULT_DELAY = 0;
    private static final int JOB_DEFAULT_DEADLINE = 5 * 60; /* 5 min */
    private static final int JOB_DEFAULT_NETWORK_TYPE = NetworkType.CONNECTED.ordinal();
    private static final boolean JOB_DEFAULT_REQUIRE_IDLE = false ;

    // device push types
    public static final int deviceTypeNone = 0;
    public static final int deviceTypeGCM = 1;
    public static final int deviceTypeWallet = 2;
    public static final int deviceTypeMail = 4;
    public static final int deviceTypeSms  = 8;

    // location constants
    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL = 60000 * 5; // Every 5 min

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    private static final long FASTEST_UPDATE_INTERVAL = 30000; // Every 30 seconds

    private static final int LOITERING_DELAY = 1000 * 60 * 5; // 5 min

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    private static final long MAX_WAIT_TIME = UPDATE_INTERVAL * 3; // Every 15 minutes.

    /**
     * Priority of location requests
     */
    public static final int PRIORITY_LOCATION_ACCURACY = LocationRequest.PRIORITY_HIGH_ACCURACY;

    /**
     * Desired permission for locations
     */
    public static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;

    // key for config
    // IMPORTANT: do not change this withought changing the same key in the TesConfig.js file
    private static final String KEY_CONFIG_SETTINGS = "@WI:CurrentConfigSettings";

    // geofence initial radius
    private static int GEOFENCE_RADIUS = 50; // 50 meters

    /**
    Configuration options for the WiApp object
    */

    public String environment;
    public String providerKey;

    public long memCacheSize;
    public long diskCacheSize;

    public String server;
    public String pushProfile;

    public String testServer;
    public String testPushProfile;

    public String walletOfferClass;

    public boolean requireBackgroundLocation;

    public long distacneFilter; // in meters
    public long accuracy;

    public long staleLocationThreshold; // in seconds

    public boolean logLocInfo; // whether to log debugging info

    /**
     * do automatic authentication if set. uses auth credentials to fill the register/login call
     * if credentials has the field  anonymous_user set ot YES  or
     * left as null, then an anonymous register/login is made
     */
    public boolean authAutoAuthenticate;
    public JSONObject authCredentials;

    /**
     * Location config constants
     */

    public long locUpdateInterval = UPDATE_INTERVAL;
    public long locFastestUpdateInterval = FASTEST_UPDATE_INTERVAL;
    public long locMaxWaitTime = UPDATE_INTERVAL;
    public int locPriorityAccuracy = PRIORITY_LOCATION_ACCURACY;
    public String locPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    public boolean useGeoFences = true;

    public float geoRadius = GEOFENCE_RADIUS;
    public int   geoTransitionType = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT;
    public long  geoExpiry = Geofence.NEVER_EXPIRE;
    public int   geoInitialTrigger = GeofencingRequest.INITIAL_TRIGGER_ENTER;
    public int  geoLoiteringDelay= LOITERING_DELAY;
    /**
     * list of device types wanted by a user apn, pkpass, mail, sms,
     */
    public int deviceTypes;


    public boolean debug;

    /** default job characteristics */
    public int delay;
    public int deadline;
    public int networkType;
    public boolean requireIdle;
    public boolean requireCharging;


    public TesConfig(String providerKey) {
         // system config
        this.providerKey = providerKey;
        this.environment = ENV_PROD;
        this.server = PROD_SERVER;
        this.pushProfile = PROD_PUSH_PROFILE;
        this.testServer = TEST_SERVER;
        this.testPushProfile = TEST_PUSH_PROFILE;
        this.walletOfferClass = WALLET_OFFER_CLASS;
        this.memCacheSize = MEM_CACHE_SIZE;
        this.diskCacheSize = DISK_CACHE_SIZE;
        this.debug = false;

        // location config
        this.requireBackgroundLocation = true;

        //this.distacneFilter = LOC_DISTANCE_FILTER;
       // this.accuracy = LOC_DESIRED_ACCURACY;
       // this.staleLocationThreshold = LOC_STALE_LOCATION_THRESHOLD;
        this.logLocInfo = false;

        // auth config
        this.authAutoAuthenticate = false;
        this.authCredentials = null;

        //device config
        this.deviceTypes = deviceTypeGCM;

        // Job details
        this.delay  = JOB_DEFAULT_DELAY;
        this.deadline = JOB_DEFAULT_DEADLINE;
        this.networkType =  JOB_DEFAULT_NETWORK_TYPE;
        this.requireIdle = JOB_DEFAULT_REQUIRE_IDLE;
        this.requireCharging = JOB_DEFAULT_REQUIRE_CHARGING;

        this.locUpdateInterval = UPDATE_INTERVAL;
        this.locFastestUpdateInterval = FASTEST_UPDATE_INTERVAL;
        this.locMaxWaitTime = UPDATE_INTERVAL;
        this.locPriorityAccuracy = PRIORITY_LOCATION_ACCURACY;
        this.locPermission = Manifest.permission.ACCESS_FINE_LOCATION;
        this.useGeoFences = true;

        this.geoRadius = GEOFENCE_RADIUS;
        this.geoTransitionType = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT;
        this.geoExpiry = Geofence.NEVER_EXPIRE;
        this.geoInitialTrigger = GeofencingRequest.INITIAL_TRIGGER_ENTER;
        this.geoLoiteringDelay = LOITERING_DELAY;


    }

    public TesConfig(){
        this("");
    }

    protected TesConfig(Parcel in) {
        environment = in.readString();
        providerKey = in.readString();
        memCacheSize = in.readLong();
        diskCacheSize = in.readLong();
        server = in.readString();
        pushProfile = in.readString();
        testServer = in.readString();
        testPushProfile = in.readString();
        walletOfferClass = in.readString();
        requireBackgroundLocation = in.readByte() != 0;
        distacneFilter = in.readLong();
        accuracy = in.readLong();
        staleLocationThreshold = in.readLong();
        logLocInfo = in.readByte() != 0;
        authAutoAuthenticate = in.readByte() != 0;
        deviceTypes = in.readInt();
        debug = in.readByte() != 0;
        delay  = in.readInt();
        deadline = in.readInt();;
        networkType =  in.readInt();;
        requireIdle = in.readByte() != 0;
        requireCharging = in.readByte() != 0;;

        locUpdateInterval = in.readLong();
        locFastestUpdateInterval = in.readLong();
        locMaxWaitTime = in.readLong();
        locPriorityAccuracy = in.readInt();
        locPermission = in.readString();
        useGeoFences = in.readByte() != 0;

        geoRadius = in.readFloat();
        geoTransitionType = in.readInt();
        geoExpiry = in.readLong();
        geoInitialTrigger = in.readInt();
        geoLoiteringDelay = in.readInt();
    }

    public static final Creator<TesConfig> CREATOR = new Creator<TesConfig>() {
        @Override
        public TesConfig createFromParcel(Parcel in) {
            return new TesConfig(in);
        }

        @Override
        public TesConfig[] newArray(int size) {
            return new TesConfig[size];
        }
    };

    public String getEnvServer(){

       return (this.environment.equals(ENV_PROD)) ? this.server : this.testServer;
    }

    public String getEnvPushProfile(){

        return (this.environment.equals(ENV_PROD)) ? this.pushProfile : this.testPushProfile;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(environment);
        dest.writeString(providerKey);
        dest.writeLong(memCacheSize);
        dest.writeLong(diskCacheSize);
        dest.writeString(server);
        dest.writeString(pushProfile);
        dest.writeString(testServer);
        dest.writeString(testPushProfile);
        dest.writeString(walletOfferClass);
        dest.writeByte((byte) (requireBackgroundLocation ? 1 : 0));
        dest.writeLong(distacneFilter);
        dest.writeLong(accuracy);
        dest.writeLong(staleLocationThreshold);
        dest.writeByte((byte) (logLocInfo ? 1 : 0));
        dest.writeByte((byte) (authAutoAuthenticate ? 1 : 0));
        dest.writeInt(deviceTypes);
        dest.writeByte((byte) (debug ? 1 : 0));
        dest.writeInt(delay);
        dest.writeInt(deadline);
        dest.writeInt(networkType);
        dest.writeByte((byte) (requireIdle ? 1 : 0));
        dest.writeByte((byte) (requireCharging ? 1 : 0));

        dest.writeLong(locUpdateInterval);
        dest.writeLong(locFastestUpdateInterval);
        dest.writeLong(locMaxWaitTime);
        dest.writeInt(locPriorityAccuracy);
        dest.writeString(locPermission);
        dest.writeByte((byte)(useGeoFences ? 1 : 0));

        dest.writeFloat(geoRadius);
        dest.writeInt(geoTransitionType);
        dest.writeLong(geoExpiry);
        dest.writeInt(geoInitialTrigger);
        dest.writeInt(geoLoiteringDelay);
    }

    @Override
    public String toString() {
        return "TesConfig{" +
                "environment='" + environment + '\'' +
                ", providerKey='" + providerKey + '\'' +
                ", memCacheSize=" + memCacheSize +
                ", diskCacheSize=" + diskCacheSize +
                ", server='" + server + '\'' +
                ", pushProfile='" + pushProfile + '\'' +
                ", testServer='" + testServer + '\'' +
                ", testPushProfile='" + testPushProfile + '\'' +
                ", walletOfferClass='" + walletOfferClass + '\'' +
                ", requireBackgroundLocation=" + requireBackgroundLocation +
                ", distacneFilter=" + distacneFilter +
                ", accuracy=" + accuracy +
                ", staleLocationThreshold=" + staleLocationThreshold +
                ", logLocInfo=" + logLocInfo +
                ", authAutoAuthenticate=" + authAutoAuthenticate +
                ", authCredentials=" + authCredentials +
                ", deviceTypes=" + deviceTypes +
                ", debug=" + debug +
                ", delay=" + this.delay+
                ", deadline=" + this.deadline+
                ", networkType=" + this.networkType+
                ", requireIdle=" + this.requireIdle+
                ", requireCharging=" +  this.requireCharging+
                ", locUpdateInterval=" +  this.locUpdateInterval+
                ", locFastestUpdateInterval=" +  this.locFastestUpdateInterval+
                ", locMaxWaitTime=" +  this.locMaxWaitTime+
                ", locPriorityAccuracy=" +  this.locPriorityAccuracy+
                ", locPermission=" +  this.locPermission+
                ", useGeoFences=" +  this.useGeoFences+
                ", geoRadius=" +  this.geoRadius +
                ", geoTransitionType=" +  this.geoTransitionType +
                ", geoExpiry=" +  this.geoExpiry +
                ", geoInitialTrigger=" + this.geoInitialTrigger +
                ", geoLoiteringDelay=" +  this.geoLoiteringDelay +
        '}';
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("environment", this.environment);
        json.put("providerKey", this.providerKey);
        json.put("memCacheSize", this.memCacheSize);
        json.put("diskCacheSize", this.diskCacheSize);
        json.put("server", this.server);
        json.put("pushProfile", this.pushProfile);
        json.put("testServer", this.testServer);
        json.put("testPushProfile", this.testPushProfile);
        json.put("walletOfferClass", this.walletOfferClass);
        json.put("requireBackgroundLocation", this.requireBackgroundLocation);
        json.put("distacneFilter", this.distacneFilter);
        json.put("accuracy", this.accuracy);
        json.put("staleLocationThreshold", this.staleLocationThreshold);
        json.put("logLocInfo", this.logLocInfo);
        json.put("authAutoAuthenticate", this.authAutoAuthenticate);
        json.put("authCredentials", this.authCredentials);
        json.put("deviceTypes", this.deviceTypes);
        json.put("debug", this.debug);
        json.put("delay", this.delay);
        json.put("deadline", this.deadline);
        json.put("networkType", this.networkType);
        json.put("requireIdle", this.requireIdle);
        json.put("requireCharging", this.requireCharging);
        json.put("locUpdateInterval=",  this.locUpdateInterval);
        json.put("locFastestUpdateInterval",  this.locFastestUpdateInterval);
        json.put("locMaxWaitTime", this.locMaxWaitTime);
        json.put("locPriorityAccuracy", this.locPriorityAccuracy);
        json.put("locPermission", this.locPermission);
        json.put("useGeoFences", this.useGeoFences);
        json.put("geoRadius", this.geoRadius);
        json.put("geoTransitionType", this.geoTransitionType);
        json.put("geoExpiry", this.geoExpiry);
        json.put("geoInitialTrigger", this.geoInitialTrigger);
        json.put("geoLoiteringDelay",  this.geoLoiteringDelay);

        return json;
    }

    public void fromJSON(String jsonString) throws JSONException {
        JSONObject json = new JSONObject(jsonString);
        this.environment = json.optString("environment", this.environment);
        this.providerKey = json.optString("providerKey", this.providerKey);
        this.memCacheSize = json.optLong("memCacheSize", this.memCacheSize);
        this.diskCacheSize = json.optLong("diskCacheSize", this.diskCacheSize);
        this.server = json.optString("server",this. server);
        this.pushProfile = json.optString("pushProfile", this.pushProfile);
        this.testServer = json.optString("testServer", this.testServer);
        this.testPushProfile = json.optString("testPushProfile", this.testPushProfile);
        this.walletOfferClass = json.optString("walletOfferClass", this.walletOfferClass);
        this.requireBackgroundLocation = json.optBoolean("requireBackgroundLocation", this.requireBackgroundLocation);
        this.distacneFilter = json.optLong("distacneFilter", this.distacneFilter);
        this.accuracy = json.optLong("accuracy", this.accuracy);
        this.staleLocationThreshold = json.optLong("staleLocationThreshold", this.staleLocationThreshold);
        this.logLocInfo = json.optBoolean("logLocInfo", this.logLocInfo);
        this.authAutoAuthenticate = json.optBoolean("authAutoAuthenticate", this.authAutoAuthenticate);

        JSONObject auth = json.optJSONObject("authCredentials");
        if (auth != null)
        this.authCredentials = auth;

        this.deviceTypes = json.optInt("deviceTypes", this.deviceTypes);
        this.debug = json.optBoolean("debug", this.debug);

        this.delay  = json.optInt("delay", this.delay);
        this.deadline = json.optInt("deadline", this.deadline);;
        this.networkType =  json.optInt("networkType", this.networkType);;
        this.requireIdle = json.optBoolean("requireIdle", this.requireIdle);
        this.requireCharging = json.optBoolean("requireCharging", this.requireCharging);

        this.locUpdateInterval = json.optLong("locUpdateInterval=",  this.locUpdateInterval);
        this.locFastestUpdateInterval = json.optLong("locFastestUpdateInterval",  this.locFastestUpdateInterval);
        this.locMaxWaitTime = json.optLong("locMaxWaitTime", this.locMaxWaitTime);
        this.locPriorityAccuracy = json.optInt("locPriorityAccuracy", this.locPriorityAccuracy);
        this.locPermission = json.optString("locPermission", this.locPermission);
        this.useGeoFences = json.optBoolean("useGeoFences", this.useGeoFences);

        this.geoRadius = (float) json.optDouble("geoRadius", this.geoRadius);
        this.geoTransitionType = json.optInt("geoTransitionType", this.geoTransitionType);
        this.geoExpiry = json.optLong("geoExpiry", this.geoExpiry);
        this.geoInitialTrigger = json.optInt("geoInitialTrigger", this.geoInitialTrigger);
        this.geoLoiteringDelay = json.optInt("geoLoiteringDelay",  this.geoLoiteringDelay);


    }

    public void saveConfig(Context context) throws JSONException {
        String cfg = this.toJSON().toString();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(KEY_CONFIG_SETTINGS, cfg)
                .apply();
    }

    static public TesConfig getSavedConfig(Context context) throws JSONException {
        String cfgStr = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_CONFIG_SETTINGS, null);
        TesConfig cfg = new TesConfig();
        cfg.fromJSON(cfgStr);
        return cfg;
    }
}

