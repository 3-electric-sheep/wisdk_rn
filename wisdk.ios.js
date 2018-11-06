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

import {NativeModules, NativeEventEmitter} from 'react-native';

let RNWisdk = NativeModules.RNWisdk;

const RNWisdkEmitter = new NativeEventEmitter(RNWisdk);

const onGeofenceUpdate = "onGeofenceUpdate";
const onLocationUpdate = "onLocationUpdate";
const onPermissionChange = "onPermissionChange";

let rnwisdkInterface = {
    configure: RNWisdk.configure,

    connect: RNWisdk.connect,

    getLastKnownLocation: RNWisdk.getLastKnownLocation,
    requestLocationUpdates: RNWisdk.requestLocationUpdates,
    removeLocationUpdates: RNWisdk.removeLocationUpdates,

    addGeofences: RNWisdk.addGeofences,
    clearGeofences: RNWisdk.clearGeofences,
    removeGeofences: RNWisdk.removeGeofences,

    onGeofenceUpdate: (callback) => {
        const subscription = RNWisdkEmitter.addListener(RNWisdk.GeofenceUpdate, callback);
        return function off() {
            subscription.remove()
        };
    },

    onLocationUpade: (callback) => {
        const subscription = RNWisdkEmitter.addListener(RNWisdk.LocationUpdate, callback);
        return function off() {
            subscription.remove()
        };
    },

    onPermissionChange: (callback) => {
        const subscription = RNWisdkEmitter.addListener(RNWisdk.PermissionChange, callback);
        return function off() {
            subscription.remove()
        };
    },

    onBoot: (callback) => {
        // no op on IOS
    },

    ERROR_UNEXPECTED_ERROR : -1,
    ERROR_NULL_LOCATION_RETURNED : -2
};

console.log("IOS bridge called");
module.exports = rnwisdkInterface;
