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

import {
    AsyncStorage,
    Platform
} from 'react-native';

/**
 * Created by pfrantz on 11/08/2017.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

/** Android constants */

/** Default. */
const NETWORK_TYPE_NONE = 0;
/** This job requires network connectivity. */
const NETWORK_TYPE_ANY = 1;
/** This job requires network connectivity that is unmetered. */
const NETWORK_TYPE_UNMETERED = 2;
/** This job requires network connectivity that is not roaming. */
const  NETWORK_TYPE_NOT_ROAMING = 3;
/** This job requires metered connectivity such as most cellular data networks. */
const NETWORK_TYPE_METERED = 4;


const PRIORITY_HIGH_ACCURACY = 100;
const PRIORITY_BALANCED_POWER_ACCURACY = 102;
const PRIORITY_LOW_POWER = 104;
const PRIORITY_NO_POWER = 105;

const ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
const ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";

const GEOFENCE_TRANSITION_ENTER = 1;
const GEOFENCE_TRANSITION_EXIT = 2;
const GEOFENCE_TRANSITION_DWELL = 4;
const NEVER_EXPIRE = -1;

const INITIAL_TRIGGER_ENTER = 1;
const INITIAL_TRIGGER_EXIT = 2;
const INITIAL_TRIGGER_DWELL = 4;

const ONE_MINUTE = 60000; // 1 minure in usec
const NAG_INTERVAL = 60*60*24*7; // 7 days

const ASK_LOCATION_PERM_TITLE = 'Can we access your location ?';
const ASK_LOCATION_PERM_BODY = 'We need access so you we can send you great offers in your area';

/** end Android constants **/

// TODO: PJF add ios constants and switch accordingly

export class WiConfig  {
    // IMPORTANT: don't change this without updating the ANDROID key of the same name
    // as its used to get the config from a job service.
    static get KEY_CONFIG_SETTINGS() { return "@WI:CurrentConfigSettings" }

    static get ENV_PROD() { return "prod" }
    static get ENV_TEST() { return "test" }

    static get PROD_SERVER() { return "https://api.3-electric-sheep.com" }
    static get PROD_PUSH_PROFILE() { return "<PROD_PROFILE>" }

    static get TEST_SERVER() { return "https://testapi.3-electric-sheep.com" }
    static get TEST_PUSH_PROFILE() { return "<TEST_PROFILE>" }

    static get WALLET_GOOGLE_OFFER_CLASS() { return "wi_offer_class" }
    static get WALLET_APPLE_OFFER_CLASS() { return "wi_offer_pass"}

    static get WALLET_PROFILE() { return "email" }
    static get MAIL_PROFILE() { return "email" }
    static get SMS_PROFILE() { return "phone" }
    static get PASSIVE_PROFILE() {return  "virtual"};

    static get MEM_CACHE_SIZE() { return 8388608 } //8 * 1024 * 1024;
    static get DISK_CACHE_SIZE() { return 20971520 } // 20 * 1024 * 1024;

    static get DEVICE_TYPE_GCM() { return "gcm" }
    static get DEVICE_TYPE_FCM() { return "gcm" } // just to allow backward compatibility

    static get DEVICE_TYPE_MAIL() { return "mail" }
    static get DEVICE_TYPE_SMS() { return "sms" }
    static get DEVICE_TYPE_GOGGLE_WALLET() { return "ap" }
    static get DEVICE_TYPE_MULTIPLE() { return "multiple" }
    static get DEVICE_TYPE_APN() {return"apn"};
    static get DEVICE_TYPE_APPLE_WALLET() {return "pkpass"};
    static get DEVICE_TYPE_PASSIVE() {return "passive"};

    // default job scheduler params
    static get JOB_DEFAULT_REQUIRE_CHARGING() { return false }
    static get JOB_DEFAULT_DELAY() { return 0 }
    static get JOB_DEFAULT_DEADLINE() { return 5 * 60 } /* 5 min */
    static get JOB_DEFAULT_NETWORK_TYPE() { return NETWORK_TYPE_ANY }
    static get JOB_DEFAULT_REQUIRE_IDLE() { return false  }

    // device push types
    static get deviceTypeNone() { return 0 }
    static get deviceTypeAPN() { return 1 }
    static get deviceTypeGCM() { return 2 }
    static get deviceTypeAppleWallet() { return 4 }
    static get deviceTypeWallet() { return 8 }
    static get deviceTypeMail() { return 16 }
    static get deviceTypeSms() { return 32 }
    static get deviceTypePassive() { return 64};


    // location constants

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    static get UPDATE_INTERVAL() { return ONE_MINUTE * 10 } // Every 10 min

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value, but they may be less frequent.
     */
    static get FASTEST_UPDATE_INTERVAL() { return ONE_MINUTE * 5 } // Every 5 min

    /**
    min delay between enter and exis
     */
    static get LOITERING_DELAY() { return ONE_MINUTE * 5 } // 5 min

    /**
     * The max time before batched results are delivered by location services. Results may be
     * delivered sooner than this interval.
     */
    static get MAX_WAIT_TIME() { return ONE_MINUTE * 20 } // Every 20 minutes.

    /**
     * Priority of location requests
     */
    static get PRIORITY_LOCATION_ACCURACY() { return PRIORITY_LOW_POWER }

    /**
     * Desired permission for locations
     */
    static get LOCATION_PERMISSION() { return ACCESS_FINE_LOCATION }

    // geofence initial radius
    static get GEOFENCE_RADIUS() { return 20 } // 20 meters

    constructor(providerKey, testProviderKey) {
        // system config
        this.environment = WiConfig.ENV_PROD;

        this.providerKey = providerKey;
        this.server = WiConfig.PROD_SERVER;
        this.pushProfile = WiConfig.PROD_PUSH_PROFILE;

        this.testProviderKey = testProviderKey;
        this.testServer = WiConfig.TEST_SERVER;
        this.testPushProfile = WiConfig.TEST_PUSH_PROFILE;

        this.walletProfile = WiConfig.WALLET_PROFILE;
        this.walletOfferClass =  (Platform.OS === "ios") ? WiConfig.WALLET_APPLE_OFFER_CLASS : WiConfig.WALLET_GOOGLE_OFFER_CLASS;
        this.walletType = (Platform.OS === "ios") ? WiConfig.DEVICE_TYPE_APPLE_WALLET : WiConfig.DEVICE_TYPE_GOGGLE_WALLET;

        this.memCacheSize = WiConfig.MEM_CACHE_SIZE;
        this.diskCacheSize = WiConfig.DISK_CACHE_SIZE;
        this.debug = false;

        // location config
        this.requireBackgroundLocation = true;

        //this.distacneFilter = this.LOC_DISTANCE_FILTER;
        // this.accuracy = this.LOC_DESIRED_ACCURACY;
        // this.staleLocationThreshold = this.LOC_STALE_LOCATION_THRESHOLD;
        this.logLocInfo = false;

        // auth config
        this.authAutoAuthenticate = true;
        this.authCredentials = {
            anonymous_user: true
        };

        //device config
        this.deviceTypes = WiConfig.deviceTypeGCM;  // both IOS and Android go through firebase in RN
        //this.deviceTypes = (Platform.OS === "ios") ? WiConfig.deviceTypeAPN : WiConfig.deviceTypeGCM;

        // Job details
        this.delay  = WiConfig.JOB_DEFAULT_DELAY;
        this.deadline = WiConfig.JOB_DEFAULT_DEADLINE;
        this.networkType = WiConfig.JOB_DEFAULT_NETWORK_TYPE;
        this.requireIdle = WiConfig.JOB_DEFAULT_REQUIRE_IDLE;
        this.requireCharging = WiConfig.JOB_DEFAULT_REQUIRE_CHARGING;

        this.locUpdateInterval = WiConfig.UPDATE_INTERVAL;
        this.locFastestUpdateInterval = WiConfig.FASTEST_UPDATE_INTERVAL;
        this.locMaxWaitTime = WiConfig.UPDATE_INTERVAL;
        this.locPriorityAccuracy = WiConfig.PRIORITY_LOCATION_ACCURACY;
        this.locPermission = WiConfig.LOCATION_PERMISSION;
        this.useGeoFences = true;

        this.geoRadius = this.GEOFENCE_RADIUS;
        this.geoTransitionType = GEOFENCE_TRANSITION_ENTER | GEOFENCE_TRANSITION_DWELL | GEOFENCE_TRANSITION_EXIT;
        this.geoExpiry = NEVER_EXPIRE;
        this.geoInitialTrigger = INITIAL_TRIGGER_ENTER;
        this.geoLoiteringDelay = this.LOITERING_DELAY;

        // permissions
        this.askForLocationPermission = true;
        this.askForNotificationPermission = true;
        this.locationPermissionNagInterval = NAG_INTERVAL;

        this.askForLocationPermTitle = ASK_LOCATION_PERM_TITLE;
        this.askForLocationPermBody = ASK_LOCATION_PERM_BODY;

        // notifications
        this.autoDisplayNotifications = true;
    }


    getEnvServer(){
        return (this.environment === WiConfig.ENV_PROD) ? this.server : this.testServer;
    }

    getEnvPushProfile(){
        return (this.environment === WiConfig.ENV_PROD) ? this.pushProfile : this.testPushProfile;
    }

    getEnvProvider() {
        return (this.environment === WiConfig.ENV_PROD) ? this.providerKey : this.testProviderKey;
    }

    saveConfig(){
        let currConfig = {};
        for (let key in this){
            if (this.hasOwnProperty(key)){
                let descr = Object.getOwnPropertyDescriptor(this, key);
                if (descr && descr.writable)
                    currConfig[key] = this[key];
            }
        }
        return AsyncStorage.setItem(WiConfig.KEY_CONFIG_SETTINGS, JSON.stringify(this));
    }

    getSavedConfig() {
       return AsyncStorage.getItem(WiConfig.KEY_CONFIG_SETTINGS).then((savedConfig)=>{
            if (savedConfig) {
                const jsonData = JSON.parse(savedConfig);
                for (let key in jsonData) {
                    if (jsonData.hasOwnProperty(key))
                        this[key] = jsonData[key];
                }
            }
            return this;
       });
    }
}

