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

import {AppRegistry, NativeModules, PermissionsAndroid} from 'react-native';
import {Wiapp} from './wiapp';

let RNWisdk = NativeModules.RNWisdk;

const GEO_TRANSITION_TASK_NAME = "wisdk-geo-transition";
const LOC_UPDATE_TASK_NAME = "wisdk-location-update";
const BOOT_TASK_NAME = "wisdk-boot-tassk";

let geofence_callbacks = [];
let locupdate_callbacks = [];
let boot_callbacks = [];

// ensure we have atleast the callbacks and networking setup
const setup = (pr, fullStartup=false) => {
    let wi = Wiapp.manager();
    if (wi === null){
        // no wi instance - create one from saved config
        const wiBackgroundListener = class extends WiAppListener {
            onAuthenticateFail = (resp) => {console.log('BG Athenticate fail: '+JSON.stringify(resp))};
            onLocationUpdateHandler=(loc)=>{const l = JSON.stringify(loc);console.log(`BG Loc update: ${l}`)};
            onGeoUpdateHandler=(loc)=>{const l = JSON.stringify(loc);console.log(`BG Geo update: ${l}`)};
            onBootHandler=()=>{console.log("BG Boot handler called")};
            onErrorHandler=(prefix, e) => {console.log(`BG error: ${prefix}: ${(e.message) ? e.message : e}`)};
            onRefreshPushToken = (token) => { console.log(`BG FCM token: ${token}`)}
        };
        let config = new WiConfig();
        return config.getSavedConfig().then(()=> {
            wi = Wiapp.createManager(config, new wiBackgroundListener());
            const startmeup = (fullStartup) ? wi.start: wi.startApi ;
            return startmeup();
        }).then(()=>{
            return pr;
        });

    }
    else {
        // already have a we instance with config and listener
        return wi.config.getSavedConfig().then(()=> {
            const startmeup = (fullStartup) ? wi.start: wi.startApi ;
            startmeup().then(()=>{
                return pr;
            })
        });
    }

};

// register the various headless tasks
AppRegistry.registerHeadlessTask(GEO_TRANSITION_TASK_NAME, () => {
    return (geofenceEvent) => {
        try {
            return setup(new Promise((resolve, reject) => {
                //console.log('#### in geofence task: '+JSON.stringify(geofenceEvent));
                try {
                    geofence_callbacks.forEach(callback => {
                        callback(geofenceEvent);
                    });

                    resolve();
                } catch (e) {
                    reject(e);
                }
                //console.log('#### out geofence task');
            }));
        }
        catch(e){
            console.log("Exception in geo event:"+e);
        }
    }
});

AppRegistry.registerHeadlessTask(LOC_UPDATE_TASK_NAME, () => {
    return (locEvent) => {
        try {
            return setup(new Promise((resolve, reject) => {
                //console.log('#### in location task: '+JSON.stringify(locEvent));
                try {
                    locupdate_callbacks.forEach(callback => {
                        callback(locEvent);
                    });
                    resolve();
                } catch (e) {
                    reject(e);
                }
                //console.log('#### out location task');
            }));
        }
        catch(e){
            console.log("Exception in loc update:"+e);
        }
    }
});

AppRegistry.registerHeadlessTask(BOOT_TASK_NAME, () => {
    return () => {
        try {
            return setup(new Promise((resolve, reject) => {
                try {
                    boot_callbacks.forEach(callback => {
                        callback();
                    });
                    resolve();
                } catch (e) {
                    reject(e);
                }
            }), true);
        }
        catch(e){
            console.log("Exception in bootL"+e);
        }
    }
});


let rnwisdkInterface = {
    configure: (cfg) => {
         return RNWisdk.configure(JSON.stringify(cfg))
    },

    connect: RNWisdk.connect,

    getLastKnownLocation: RNWisdk.getLastKnownLocation,

    requestLocationUpdates: RNWisdk.requestLocationUpdates,
    removeLocationUpdates: RNWisdk.removeLocationUpdates,

    addGeofences: RNWisdk.addGeofences,
    clearGeofences: RNWisdk.clearGeofences,
    removeGeofences: RNWisdk.removeGeofences,

    onGeofenceUpdate: (callback) => {
        geofence_callbacks.push(callback);
        return function off() {
            const idx = geofence_callbacks.indexOf(callback);
            if(idx >= 0) {
                geofence_callbacks.splice(idx, 1);
            }
        };
    },

    onLocationUpade: (callback) => {
        locupdate_callbacks.push(callback);
        return function off() {
            const idx = locupdate_callbacks.indexOf(callback);
            if(idx >= 0) {
                locupdate_callbacks.splice(idx, 1);
            }
        };
    },

    onBoot: (callback) => {
        boot_callbacks.push(callback);
        return function off() {
            const idx = boot_callbacks.indexOf(callback);
            if(idx >= 0) {
                boot_callbacks.splice(idx, 1);
            }
        };
    },

    onPermissionChange: (callback) => {
      // no op on android
    },

    ERROR_UNEXPECTED_ERROR : -1,
    ERROR_NULL_LOCATION_RETURNED : -2
};

console.log("Android bridge called");
module.exports = rnwisdkInterface;
