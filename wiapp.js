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
import React from "react";

import {
Platform,
Alert,
AppState
} from "react-native";

import AsyncStorage from "@react-native-community/async-storage"


import { Wiapi, getErrorMsg, utcnow, WiApiAuthListener } from './wiapi'
import { WiConfig} from "./wiconfig";
import { WiPushMgr} from "./wipushmgr";

import DeviceInfo from 'react-native-device-info';

import RNWisdk from "./wisdk";
import Permissions, {PERMISSIONS, RESULTS} from 'react-native-permissions';
import OpenSettings from 'react-native-open-settings';

import uuidv4 from "uuid/v4";
import moment from "moment";

const KEY_WIAPP_SETTINGS = "@TesWI:AppSettings";

/**
 * Endpoints
 */
const TES_PATH_REGISTER = "account";
const TES_PATH_LOGIN = "auth/login";
const TES_PATH_PROFILE = "account";
const TES_PATH_GEODEVICE = "geodevice"; // TODO: this should be plural

const PERMISSION_NONE = 0;
const PERMISSION_OK = 1;
const PERMISSION_PARTIAL = 2;

/**
 * Error codes returned by the server
 **/
const ERROR_NOT_FOUND = 1;
const ERROR_ADD = 2;
const ERROR_UPDATE = 4;

/**
 * 1 day in ms
 */
const ONE_DAY = 1000*60*60*24;

/**
 * For now just hardwire version
 */

const VERSION_NAME = "1.2";

/**
 * delegate class used to notify app of changes
 */

export class WiAppListener {
// ------------------------------------------------
    // delegate functions used to notify app of changes
    // ------------------------------------------------

    /**
     * Called when starup is complete and you have successfully been authorized. Will
     * at the end of start or if you are unauthorized, at the end of the authorize process
     * regardless of whehter you are authorized or not.
     *
     * @param isAuthorized - returns whether we are successfully authorized or not
     */
     onStartupComplete = (isAuthorized) => {};

    /**
     * sent when authorization is complete
     *
     * param responseObject  response object from call
     **/
    onAuthenticate = (resp) => {};

    /**
     * sent when authorization failed (either 403/401 or success=false)
     *
     * param error          Error set on error may have result set if the call returned success=False
     **/
    onAuthenticateFail = (resp) => {

        if (resp.status) {
            this._doMsg(`Authorization failed: HTTP Error ${resp.status}`);
        }
        else if (resp.msg) {
            this._doMsg(`Authenicate failed: ${resp.msg}`) ;
        }
        else {
            this._doMsg(`Authenicate failed: Unknown error ${resp}`);
        }
    };

    /**
     * Ask the user to change permission via the settings screen
     */
    askForPermission = (config, settings) => {
        Alert.alert(
            config.askForLocationPermTitle,
            config.askForLocationPermBody,
            [
                {
                    text: 'No thanks',
                    onPress: () => console.log('Permission denied'),
                    style: 'cancel',
                },
                {   text: 'Open Settings',
                    onPress: settings
                }
            ],
        )
    };

    /**
     * sent when a new access token is returned
     *
     * param token
     */
    onNewAccessToken = (token) => {};

    /**
     * sent when a new device token has been created
     *
     *  param devicetoken
     */
    onNewDeviceToken = (token) => {};


    /**
     * Called when remote notifications is registered or fails to register
     *
     * param token the new token
     */
    onRefreshPushToken = (token) => {};

    /**
     * called when noficiation is received in fg
     * @param notification
     */
    onNotification = (notification)=>{};


    /**
     * callewd when notification is displayed and app in bg or closed
     * @param action = the action that caused things
     * @param notification
     * @param appStarted = was the app started ?
     */
    onNotificationOpened = (action, notification, appStarted)=>{};

    /**
     * called when notifcation is displayed while app in bg and content available set to true
     * @param notification
     */
    onNotificationDisplayed = (notification)=>{};

    /**
     * Called when a wallet object is saved to the wallet
     *
     * param  requestCode the code of the request
     * param  resultCode the result code.
     * param  data extra stuff with the save to wallet
     * param  msg description of return code
     *
     */
    onSaveToWallet = (requestCode, resultCode, data, msg) => {};


    /**
     * Handlers for location/geo/boot/permission change
     */
    onLocationUpdateHandler = () => {};
    onGeoUpdateHandler = () => {};
    onBootHandler = () => {};
    onPermissionChangeHandler = () => {};

    /**
     * Error handler
     */
    onErrorHandler =(prefix, e) => {
        const {code, msg} = e;
        this._doMsg(prefix+" "+(code!==undefined && msg !==undefined)?code+" "+msg:e)
    };

    /**
    display a message either in an alert or in the console
     **/
    _doMsg = (msg) => {
        if (AppState.currentState  === 'active')
            alert(msg);
        else
            console.log(msg);
    }

};

export const WiAppAuthListener = class extends WiApiAuthListener {

    constructor(app){
        super();
        this.app = app;
    }
    /**
     sent what an authorization has failed
     */
    httpAuthFailure = (response) => {
        return this.app.httpAuthListener(response);
    };
};

export class Wiapp {
    static wiInstance = null;
    static wiInitDone = false;


    constructor(config, listener) {
        if (listener === undefined)
            listener = new WiAppListener();

        this.config = config;
        this.api = new Wiapi(config.getEnvServer(), new WiAppAuthListener(this));
        this.listener = listener;

        this.pushMgr = new WiPushMgr(this.config, this);

        this.locPermission = "undetermined";
        this.locType = null;

        // app specific state
        this.authUserName = null;
        this.authUserSettings = null;

        this.deviceToken = null;
        this.pushToken = null;
        this.providerToken = null;
        this.localeToken = null;
        this.timezoneToken = null;
        this.versionToken = null;

        // control the asking for permission
        this.lastPermissionNag = null;
        this.lastPermissionNagCount = 0;

        this.lastLoc = null;

        this.locCallback = null;
        this.geoCallback = null;
        this.permCallback = null;
        this.bootCallback = null;

        // are we monitoring
        this.isMonitoring = false;

        // are we autheticating
        this.autenticating = false;

        // dummy constants
        this.PERMISSION_NONE = PERMISSION_NONE;
        this.PERMISSION_OK = PERMISSION_OK;
        this.PERMISSION_PARTIAL =  PERMISSION_PARTIAL;

        this.initTokens();
    }

    /**
     * shared client is a singleton used to make all store related callses
     */

    static createManager(config, listeners) {
        if (Wiapp.wiInstance == null) {
            Wiapp.wiInstance = new Wiapp(config, listeners);
            Wiapp.wiInitDone = false;
        }
        return Wiapp.wiInstance;
    }

    static manager() {
        return Wiapp.wiInstance;
    }

    static setManager(app) {
        Wiapp.wiInstance = app;
    }

    static setInitDone() {
        Wiapp.wiInitDone = true;
    }

    static hasInit() {
        return Wiapp.wiInitDone;
    }

    /**
     * setup the test / prod environement.
     *
     * this should be called prior to calling start.
     *
     * @param testmode = true run test / false run prod
     */
    setTestEnvironment = async (testMode) => {
       this.config.environment = (testMode) ? WiConfig.ENV_TEST : WiConfig.ENV_PROD;
       this.api.setEndpoint(this.config.getEnvServer());
       await this.config.saveConfig();
    };

    /**
     * Start up sequence for WI.
     *
     * Note: while there are lots aync ops here we do them serially as there is not
     * much point to keep going if we can't authenticate with the server and start up
     * location services.
     *
     * @returns {Promise<void>}
     */
    start = async () => {

        try {
            this.api.setEndpoint(this.config.getEnvServer());

            if (Wiapp.hasInit()){
                // we self authenticate and pass on the result to any delegate implementing this routine.
                if (!this.api.isAuthorized() && this.config.authAutoAuthenticate) {
                    await this.authenticate(this.config.authCredentials, true);
                }

                this.getLastKnownLocation();

                // check for any notification
                await this.pushMgr.checkForNotification();

                return this.api.isAuthorized();
            }

            await this.initTokens();
            await this._setLocaleAndVersionInfo();

            await RNWisdk.configure(this.config);
            await this.config.saveConfig();

            // we self authenticate and pass on the result to any delegate implementing this routine.
            if (!this.api.isAuthorized() && this.config.authAutoAuthenticate) {
                await this.authenticate(this.config.authCredentials, true);
            }

            // get the permission for location.  Always/When in use is dependant on config
            // requireBackgroundProcessing flag.
            console.log("Before: xxx");
            const {locPermission, locType} = await this._checkAndRequestPermission();
            consle.log("After: uyy");
            if (locPermission === "authorized"){
                this.locPermission = locPermission;
                this.locType = locType;
                await this._ensureMonitoring();
            }
            else {
                // set our failed state against always as this is the base
                this.locPermission = locPermission;
                this.locType = 'always';
                this._checkForPermChange();
            }

            await this.pushMgr.start();

            Wiapp.setInitDone();

            // check for any notification
            await this.pushMgr.checkForNotification();

            if (this.listener.onStartupComplete)
                this.listener.onStartupComplete(this.isAuthorized());
        }
        catch(e){
            if (this.listener.onErrorHandler)
                this.listener.onErrorHandler('Startup Fail',e);
        }
        return this.api.isAuthorized();
    };

    /**
     * Start the app but only doing enough to kick the api so we can make network calls. This
     * is typically called when a pending intent starts
     *
     * @param config the config object
     * @return true for success / false otherwise
     */
     startApi = async () => {
         this.api.setEndpoint(this.config.getEnvServer());
         if (Wiapp.hasInit()){
            return true;
         }

         await this.initTokens();
         await this._setLocaleAndVersionInfo();

         // we self authenticate and pass on the result to any delegate implementing this routine.
         if (!this.api.isAuthorized() && this.config.authAutoAuthenticate) {
             await this.authenticate(this.config.authCredentials, true);
         }

         // if we are here we are in background and have been started via a broadcast
         // so connect to the location and geo mananger then setup all the callbacks
         RNWisdk.connect(true);
         this._setupCallbacks();

         Wiapp.setInitDone();
         return this.api.isAuthorized();


    };

     stop = () => {
         this.pushMgr.stop();
     };

    /**
     * returns the current permission as a promise that returns object with permission and type keys
     *
     * @return {Promise<{permission: *, type: string}>}
     */

     getLocationPermName = (locType) => {
         if (Platform.OS === 'ios'){
             locType = (this.config.requireBackgroundLocation) ? [PERMISSIONS.IOS.LOCATION_ALWAYS] : [PERMISSIONS.IOS.LOCATION_WHEN_IN_USE];
         }
         else if (Platform.OS === 'android'){
             locType = (this.config.requireBackgroundLocation) ? [PERMISSIONS.ANDROID.ACCESS_BACKGROUND_LOCATION, PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION] : [PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION];
         }
         return locType
     }

    _checkPermRes = (status) => {
        let res = RESULTS.UNAVAILABLE;
        for (var stat in status){
            res = status[stat];
            if (res !== RESULTS.GRANTED)
                break;
        }
        return res;
    }

     getCurrentLocationPermission = async () => {
         // options : 'authorized' | 'denied' | 'restricted' | 'undetermined'
         let locType = (this.config.requireBackgroundLocation) ? 'always' : 'whenInUse';
         const perm = this.getLocationPermName(locType);


         // sort out what permission we have
         let locPermission = await Permissions.checkMultiple(perm);

         // if we started with always and failed, we may have got wheninuse.
         // NOTE: this is IOS only - with android the always/wheninuse is ignored.
         if (this._checkPermRes(locPermission) !== RESULTS.GRANTED && this.config.requireBackgroundLocation) {
             locType = 'whenInUse'
             const perm = this.getLocationPermName(locType);
             locPermission = await Permissions.checkMultiple(perm);
         }

         if (this._checkPermRes(locPermission) !== RESULTS.GRANTED){
            locType = "always"
         }

         return {locType};

     };


    /**
     * Checks to see if we have the desired permission currently set
     * @return {*} 0 = no, 1 = yes, 2 = partial
     */
    haveDesiredPermission = (perm) => {
        if (perm.permission !==  RESULTS.GRANTED)
            return PERMISSION_NONE;

        const desiredType = (this.config.requireBackgroundLocation) ? 'always' : 'whenInUse';
        if (desiredType !== perm.type)
            return PERMISSION_PARTIAL;

        return PERMISSION_OK;
    };


    /**
     *  Sets the time of the last permission check and the number of times we have nagged the
     *  user about upping permission
     */
    setLastPermissionCheck = () => {
        if (this.lastPermissionNag === null) {
            this.lastPermissionNagCount = 1;
        }
        else {
            this.lastPermissionNagCount += 1;
        }
        this.lastPermissionNag = new Date().toISOString();
        this.saveTokens();
    };

    /**
     * Checks the time since the last permission check.
     */

    shouldCheckPermission = () => {
        if (this.lastPermissionNag === null)
            return true; // time to nag :-)

        if (this.lastPermissionNagCount >= this.config.locationPermissionNagMaxCount)
            return false; // give up on this person

        let dt =  moment(this.lastPermissionNag);
        let now =  moment();
        let diff = now.diff(dt, 'days');
        let nagPeriod = Math.pow(this.config.locationPermissionNagInterval, this.lastPermissionNagCount);
        if (diff > nagPeriod)
            return true;

        return false;
    };

    /**
     * Returns the current notification permission
     * @param types
     * @return {Promise<*>}
     */
    getCurrentNotificationPermission = async (types) => {
         if (types === undefined || types === null)
             types = ['alert'];

        return await Permissions.check('notification', { type: types});
     };

    _checkAndRequestPermission = async (askForFullPermission) => {
        // options : 'authorized' | 'denied' | 'restricted' | 'undetermined'
        let partial = false;
        let locType = (this.config.requireBackgroundLocation) ? 'always' : 'whenInUse';
        let perm = this.getLocationPermName(locType);

        // sort out what permission we have
        let locPermission = await Permissions.checkMultiple(perm);
        this.locPermission = locPermission;
        this.locType = locType;

        // if we started with always and failed, we may have got wheninuse.
        // NOTE: this is IOS only - with android the always/wheninuse is ignored.
        if (this._checkPermRes(locPermission) === RESULTS.DENIED && this.config.requireBackgroundLocation) {
            locType ='whenInUse';
            perm =  this.getLocationPermName(locType);
            locPermission = await Permissions.checkMultiple(perm);
            partial = (this._checkPermRes(locPermission) === RESULTS.GRANTED);
        }

        if (this.config.askForLocationPermission) {
            if (this._checkPermRes(locPermission) !== RESULTS.BLOCKED) {
                // haven't asked yet - so heres our big chance. Note: the request permission
                // does not return the correct permission if we ask for always due to a bug
                // in the code.  No matter just call check after this routine returns
                await Permissions.request(perm,
                    {
                        title: this.config.askForLocationPermTitle,
                        message: this.config.askForLocationPermBody
                    }
                );
                locPermission = await Permissions.checkMultiple(perm);

            } else if (this._checkPermRes(locPermission) === RESULT.DENIED || (partial && this.config.askForFullPermission)) {
                // we have already been refused
                this._alertForLocationPermission(locType, locPermission)
            } else if (this._checkPermRes(locPermission )=== RESULTS.LIMITED) {
                // either not supported or been told to never ask again
            }
        }

        if (this._checkPermRes(locPermission) === RESULTS.GRANTED){
            this.locPermission = this._checkPermRes(locPermission);
            this.locType = locType;
        }
        else {
            // set our failed state against always as this is the base
            this.locPermission = this._checkPermRes(locPermission);
            this.locType = locType;
        }

        return {locPermission: this._checkPermRes(locPermission), locType};
    };

    _alertForLocationPermission = (locType, permission) =>
    {
        if (this.shouldCheckPermission()) {
            const settings = (Platform.OS === 'ios') ? Permissions.openSettings : OpenSettings.openSettings;
            if (this.listener) {
                this.listener.askForPermission(this.config, settings);
            }
        }
    };

    isAuthorized = () => { return this.api.isAuthorized(); };

    authenticate =  (params, retry) => {

        if (this.autenticating){
            return;
        }

        let haveUser = false;
        if (params === null){
            params = {};
            if (this.authUserName != null && this.api.accessAuthType === Wiapi.TES_AUTH_TYPE_ANONYMOUS){
                params.user_name = this.authUserName;
            }
            else {
                params.anonymous_user = true;
            }
        }
        else {
            params = {...params};
        }

        if (params.user_name){
            haveUser = true;
        }

        if (!params.provider_id){
            params.provider_id = this.config.getEnvProvider();
        }

        const path = (haveUser) ? TES_PATH_LOGIN : TES_PATH_REGISTER;
        const {onAuthenticate, onAuthenticateFail, onNewAccessToken} = this.listener;

        this.autenticating =true;
        return this.api.call("POST", path, params, false, true).then((resp)=> {
            if (resp.success) {
                let data = resp.data;
                let token_id = data.token;
                let auth_type = data.auth_type;
                let user_name = data.user_name;

                if (token_id || this.api.accessToken !== token_id) {
                    this.api.accessToken = token_id;
                    this.api.authType = auth_type;
                    this.authUserName = user_name;
                    onNewAccessToken(token_id);
                    onAuthenticate(resp);
                    return this.saveTokens();
                }
            } else {
                onAuthenticateFail(resp);
                return this.clearAuth();
            }
        }).then(()=> {
            if (!this.deviceToken && this.isAuthorized()) {
                // NOTE: lastloc could be null here but thats ok
                return this.sendDeviceUpdate(this.lastLoc, false, this._ensureMonitoring);
            }
        }).then(()=>{
            if (this.isAuthorized() && this.deviceToken)
                this._ensureMonitoring();

            this.autenticating = false;
        }).catch((e)=>{
            if (retry === true){
                let res = this._checkForApiAuthFailure(e);
                if (res !== null)
                    return res; // a new promise
            }

            onAuthenticateFail({success:false, msg:e+""});
            this.clearAuth();
            this.autenticating = false;
        })
    };

    httpAuthListener = (resp) => {
        console.log("Auth failure encountered: "+resp);

        if (this.api.isAuthorized())
            this.api.clearAuth();

        // we self authenticate and pass on the result to any delegate implementing this routine.
        if (!this.api.isAuthorized() && this.config.authAutoAuthenticate) {
            return this.authenticate(this.config.authCredentials, false);
        }
    };

    _checkForApiAuthFailure = (e) => {
        const resp = e.response;
        if (resp && (resp.status === 401 || resp.status === 403)) {
            if (this.api.isAuthorized())
                this.api.clearAuth();


            // we self authenticate and pass on the result to any delegate implementing this routine.
            if (!this.api.isAuthorized() && this.config.authAutoAuthenticate) {
                return this.authenticate(this.config.authCredentials, false);
            }
        }
        return null; // haven't dealt with it.
    };

    sendDeviceUpdate =  (loc, background, callback,  forceNew) => {
        const {onErrorHandler, onNewDeviceToken} = this.listener;
        const deviceInfo = this.addHardwareDetails(this._fillDeviceFromLocation(loc, background));

        if (callback === undefined)
            callback = null;

        if (forceNew === undefined)
            forceNew = false;

        let apicall = null;
        if (this.deviceToken === null || this.deviceToken === undefined || forceNew) {
            apicall = this.api.call("POST", TES_PATH_GEODEVICE, deviceInfo, true, true);
        } else {
            apicall = this.api.call("PUT", `${TES_PATH_GEODEVICE}/${this.deviceToken}`, deviceInfo, true, true);
        }

        return apicall.then((result) => {
            if (result.success){
                const device_id = result.device_id;
                if (device_id && device_id !== this.deviceToken) {
                    onNewDeviceToken(device_id);
                    this.deviceToken = device_id;
                    this.saveTokens();
                }
                if (callback != null)
                    callback();
            }
            else {
                if (result.code && result.code === ERROR_NOT_FOUND) {
                    // looks like the device id has been nuked on the server. Just try again with a new device id
                    console.log("Device token invalid - Trying again with no device token");
                    return this.sendDeviceUpdate(loc, background, callback, true);
                }
                else {
                    onErrorHandler('Send device fail',result.msg);
                }
            }
        }).catch((e) => {
             let res = this._checkForApiAuthFailure(e);
             if (res !== null)
                return res; // a new promise

            onErrorHandler('Send device fail', e);
        });
    };

    addHardwareDetails = (info) => {
        info.platform = Platform.OS;
        info.application = DeviceInfo.getBundleId();
        info.platform_version = DeviceInfo.getReadableVersion();
        info.platform_info = {
            manufacturer: DeviceInfo.getManufacturer(),
            model: DeviceInfo.getModel(),
            brand: (Platform.OS === "android") ? DeviceInfo.getBrand() : DeviceInfo.getDeviceId(),
            sdk: (Platform.OS === "android") ? DeviceInfo.getAPILevel(): "",
            release: DeviceInfo.getSystemVersion()+"",
            build_type:  (__DEV__)? "debug":"release",
            location_permission: (Platform.OS === "android") ? this.locPermission: this._apple_perm(),
            location_type: this.locType
        };

        return info;
    };

    _apple_perm = ()=>{
        if (this.locPermission !== "authorized")
            return this.locPermission;

        if (this.locType === 'always'){
            return "Always Allowed";
        }
        else if (this.locType === "whenInUse"){
            return "When In Use Allowed";
        }
        else {
            return this.locType;
        }
    };

    /**
     * gets the last known location,
     */
    getLastKnownLocation = () => {
        if (!this.isMonitoring){
            return;
        }
        const {onErrorHandler} = this.listener;
        RNWisdk.getLastKnownLocation().then((loc) => {
            this._onLocationUpdate({success: true, locations: [loc]});
        }).catch((e) => {
            onErrorHandler('Get Last location Fail', e);
        });
    };
    /**
     * start location monitioring for the app.
     * Make this syncrohonous
     * @returns {Promise<void>}
     * @private
     */
    _ensureMonitoring = () => {
        if (this.locPermission !== "authorized"){
            return;
        }

        if (this.isMonitoring){
            return;
        }

        const {onErrorHandler} = this.listener;
        try {

            const allowBackgroundLocation = (this.locType === "always");

            // we need to get permission info and if we aint got enay
            RNWisdk.connect(allowBackgroundLocation);
            this._setupCallbacks();

            RNWisdk.requestLocationUpdates().then(()=>{
                this.isMonitoring = true;
                RNWisdk.getLastKnownLocation().then((loc) => {
                    this._onLocationUpdate({success:true, locations:[loc]});
                }).catch((e) => {
                    onErrorHandler('Get Last location Fail',e);
                });
            }).catch((e)=>{
                onErrorHandler('Ensure monitoring failed',e);
            });
        }
        catch(e){
            onErrorHandler('Ensure monitoring failed',e);
        }
    };

    _disableMonitoring = ()=>{
        if (!this.isMonitoring){
            return;
        }

        this._clearCallbacks(true);

        RNWisdk.removeLocationUpdates();
        RNWisdk.clearGeofences();

        this.isMonitoring = false;
    };

    _setupCallbacks = () => {
        this._clearCallbacks(false);
        this.geoCallback = RNWisdk.onGeofenceUpdate(this._onGeofenceUpdate);
        this.locCallback = RNWisdk.onLocationUpade(this._onLocationUpdate);
        this.permCallback = RNWisdk.onPermissionChange(this._onPermissionChange);
        this.bootCallback = RNWisdk.onBoot(this._onBoot);
    };

    _checkForPermChange = () => {
        // connect to the loc manager so we can get perm change notifications
        RNWisdk.connect(false);

        if (this.permCallback) this.permCallback();
        this.permCallback = RNWisdk.onPermissionChange(this._onPermissionChange);
    };

    _clearCallbacks = (leavePermChg) => {
        if (this.geoCallback) { this.geoCallback(); this.geoCallback=null;}
        if (this.locCallback) {this.locCallback(); this.locCallback=null;}
        if (this.bootCallback) {this.bootCallback(); this.bootCallback=null;}
        if (!leavePermChg)
            if (this.permCallback) {this.permCallback(); this.permCallback=null;}
    };

    _fillDeviceFromLocation = (loc, background) => {
        let locinfo = null;
        if (loc === null){
            let now = utcnow();
            locinfo = {
                longitude:0.0,
                latitude:0.0,
                accuracy: -1.0,
                speed: -1.0,
                course: -1.0,
                altitude: 0,
                fix_timestamp: now,
                arrival: null,
                departure: null,
                in_background: background
            }
        }
        else {
            if (loc.coords) {
                locinfo = {
                    longitude: loc.coords.longitude,
                    latitude: loc.coords.latitude,
                    accuracy: loc.coords.accuracy,
                    speed: loc.coords.speed,
                    course: loc.coords.course,
                    altitude: loc.coords.altitude,
                    fix_timestamp: (loc.timestamp) ? new Date(loc.timestamp).toISOString() : utcnow(),
                    arrival: null,
                    departure: null,
                    in_background: background
                }
            }
            else {
                locinfo = {
                    longitude: loc.longitude,
                    latitude: loc.latitude,
                    accuracy: loc.accuracy,
                    speed: loc.speed,
                    course: loc.course,
                    altitude: loc.altitude,
                    fix_timestamp: (loc.timestamp) ? new Date(loc.timestamp).toISOString() : utcnow(),
                    arrival:  null,
                    departure: null,
                    in_background: background
                };
            }
        }

        let device = {
            current: locinfo
        };

        let pushTargets = [];
        if ((this.config.deviceTypes & WiConfig.deviceTypeGCM) === WiConfig.deviceTypeGCM && this.pushToken !== null){
            // setup the push token if its new or we are a new device.
            pushTargets.push({
                push_info: this.pushToken,
                push_type: WiConfig.DEVICE_TYPE_GCM,
                push_profile: this.config.getEnvPushProfile()
            });
        }

        if ((this.config.deviceTypes & WiConfig.deviceTypeAPN) === WiConfig.deviceTypeAPN && this.pushToken !== null){
            // setup the push token if its new or we are a new device.
            pushTargets.push({
                push_info: this.pushToken,
                push_type: WiConfig.DEVICE_TYPE_APN,
                push_profile: this.config.getEnvPushProfile()
            });
        }

        if (((this.config.deviceTypes & WiConfig.deviceTypeWallet) === WiConfig.deviceTypeWallet )
            || ((this.config.deviceTypes & WiConfig.deviceTypeAppleWallet) === WiConfig.deviceTypeAppleWallet)){
            pushTargets.push({
                push_info: this.config.walletProfile,
                push_type: this.config.walletType,
                push_profile: this.config.walletOfferClass
            });
        }

        if ((this.config.deviceTypes & WiConfig.deviceTypeMail) === WiConfig.deviceTypeMail){
            pushTargets.push({
                push_info: WiConfig.MAIL_PROFILE,
                push_type: WiConfig.DEVICE_TYPE_MAIL,
                push_profile: ""
            });
        }

        if ((this.config.deviceTypes & WiConfig.deviceTypeSms) === WiConfig.deviceTypeSms){
            pushTargets.push({
                push_info: WiConfig.SMS_PROFILE,
                push_type: WiConfig.DEVICE_TYPE_SMS,
                push_profile: ""
            });
        }
        if ((this.config.deviceTypes & WiConfig.deviceTypePassive) === WiConfig.deviceTypePassive){
            pushTargets.push({
                push_info: WiConfig.PASSIVE_PROFILE,
                push_type: WiConfig.DEVICE_TYPE_PASSIVE,
                push_profile: ""
            });
        }

        if ( pushTargets.length === 1) {
            // setup the push token if its new or we are a new device.
            device.push_info = pushTargets[0].push_info;
            device.push_type = pushTargets[0].push_type;
            device.push_profile = pushTargets[0].push_profile;
        }
        else if ( pushTargets.length > 1){
            device.push_type = WiConfig.DEVICE_TYPE_MULTIPLE;
            device.push_info = "";
            device.push_profile = "";
            device.push_targets = pushTargets;
        }

        if (this.localeToken != null){
            device.locale = this.localeToken;
        }
        if (this.timezoneToken != null){
            device.timezone_offset = this.timezoneToken;
        }
        if (this.versionToken != null ){
            device.version = this.versionToken;
        }

        return device;
    };

    _setLocaleAndVersionInfo = () => {
        const locale = DeviceInfo.getDeviceLocale();

        const version =  VERSION_NAME;
        const now = new Date();
        const tzOffset = now.getTimezoneOffset();

        this.localeToken  = locale;
        this.timezoneToken = tzOffset;
        this.versionToken = version;

         return this.saveTokens().catch((e)=>{
            this.listener.onErrorHandler("Saving token failed", e);
        });
    };


    initTokens =  () => {
        return AsyncStorage.getItem(KEY_WIAPP_SETTINGS).then((tokens)=>{
            const _nwu = (x) => (x === undefined) ? null : x;
            console.log(tokens);
            if (!tokens)
                return;

            const jsonData = JSON.parse(tokens);
            //setup the special tokens we keep track of
            this.api.accessToken =           _nwu(jsonData.accessToken);
            this.api.accessAuthType =        _nwu(jsonData.accessAuthType);
            this.api.accessSystemDefaults =  _nwu(jsonData.accessSystemDefaults);

            this.authUserName =     _nwu(jsonData.authUserName);
            this.authUserSettings = _nwu(jsonData.authUserSettings);

            this.deviceToken  =  _nwu(jsonData.deviceToken);
            this.pushToken  =    _nwu(jsonData.pushToken);
            this.localeToken =   _nwu(jsonData.localeToken);
            this.timezoneToken = _nwu(jsonData.timezoneToken);
            this.versionToken =  _nwu(jsonData.versionToken);
            this.lastPermissionNag = _nwu(jsonData.lastPermissionNag);
            this.lastPermissionNagCount = _nwu(jsonData.lastPermissionNagCount);
        }).catch((e)=>{
            this.listener.onErrorHandler("init token failed", e);
        });

    };

    saveTokens =  () => {
        const tokens = {
            accessToken : this.api.accessToken,
            accessAuthType: this.api.accessAuthType,
            accessSystemDefaults: this.api.accessSystemDefaults,

            authUserName: this.authUserName,
            authUserSettings: this.authUserSettings,

            deviceToken : this.deviceToken,
            pushToken : this.pushToken,
            localeToken : this.localeToken,
            timezoneToken : this.timezoneToken,
            versionToken : this.versionToken,
            lastPermissionNag: this.lastPermissionNag,
            lastPermissionNagCount: this.lastPermissionNagCount,
        };
        return AsyncStorage.setItem(KEY_WIAPP_SETTINGS, JSON.stringify(tokens));
    };

    resetTokens = () => {
        return AsyncStorage.removeItem(KEY_WIAPP_SETTINGS);
    };

    setPushToken = (pushToken) => {
        this.pushToken = pushToken;
        this.saveTokens();
    };

    setDeviceToken = (deviceToken) => {
        this.deviceToken = deviceToken;
        this.saveTokens();
    };

    clearAuth = () => {
        this.api.accessToken = null;
        this.api.accessAuthType = null;

        this.authUserName = null;
        this.authUserSettings = null;
        this.deviceToken  = null;

        return this.saveTokens();
    };

    /**
     * Called on geofence enter/exit/dwell event
     * @param geo the geo info
     * @private
     */
    _onGeofenceUpdate = (geo) => {
        //console.log("Geo update:" + JSON.stringify(geo));
        const {onErrorHandler, onGeoUpdateHandler} = this.listener;
        if (!geo.success){
            onErrorHandler("Geo update failure", geo);
            return;
        }
        if (geo && geo.location) {
            const lastloc = geo.location;
            if (lastloc.did_exit){
                this.sendDeviceUpdate(lastloc, lastloc.in_background );

                onGeoUpdateHandler(lastloc);

                RNWisdk.clearGeofences();
                RNWisdk.addGeofences([
                    {latitude: lastloc.latitude, longitude: lastloc.longitude, radius: this.config.geoRadius, identifier: "gf_"+uuidv4()}
                ]).catch((e) => {
                    onErrorHandler('Geofence create fail',e);
                });
            }

            this.lastLoc = lastloc;
        }
    };

    /**
     * Called on a location update from the device
     * @param loc location information
     * @private
     */
    _onLocationUpdate = (loc) => {
        //console.log("Loc update:" + JSON.stringify(loc));
        const {onErrorHandler, onLocationUpdateHandler} = this.listener;
        if (!loc.success){
            onErrorHandler("Geo update failure", loc);
            return;
        }

        if (loc && loc.locations && loc.locations.length > 0) {
            let lastloc = null;
            loc.locations.map((l) => {
                if (l !== null && l !== undefined){
                    if (lastloc === null || lastloc.longitude !== l.longitude || lastloc.latitude !== l.latitude){
                        lastloc = l;
                        this.sendDeviceUpdate(lastloc, lastloc.in_background );

                        onLocationUpdateHandler(lastloc)

                    }
                }

            });

            if (lastloc !== null) {
                RNWisdk.clearGeofences();
                RNWisdk.addGeofences([
                    {
                        latitude: lastloc.latitude,
                        longitude: lastloc.longitude,
                        radius: this.config.geoRadius,
                        identifier: "gf_" + uuidv4()
                    }
                ]).catch((e) => {
                    onErrorHandler('Geofence create fail', e);
                });

                this.lastLoc = lastloc;
            }
        }
    };

    /**
     * Android only - catch the boot broadcast message
     * @private
     */
    _onBoot = () => {
        //console.log("System booted");
        if (this.listener.onBootHandler)
            this.listener.onBootHandler()

    };

    /**
     * IOS only - catch user changes in permission
     * @param status - the change id
     * @param statusName - the change name
     * @private
     */
    _onPermissionChange = (res) => {
        const perm = {
            0:'undetermined',
            1:'restricted',
            2:'denied',
            3:'authorized',
            4:'authorized'
        };

        const permType = {
            3: 'always',
            4:'whenInUse'
        };

        const {status, status_name} = res;
        console.log("Permission change "+status+" "+status_name);

        const locPermission = perm[status];
        const locType = (locPermission === "authorized") ? permType[status] : null;
        this.locPermission = locPermission;
        this.locType = locType;

        if (this.locPermission === "authorized") {
            this._ensureMonitoring();
        }
        else {
            this._disableMonitoring();
        }

        if (this.listener.onPermissionChangeHandler())
            this.listener.onPermissionChangeHandler({status, statusName})

    };

    /****************************
     *  push notification support
     ***************************/

    onRefreshToken = (token)=>{
        if (token != null){
            // next location update should sort this
            // TODO: deal with no location but notication on support
            this.setPushToken(token);
        }
        if (this.listener!=null)
            this.listener.onRefreshPushToken(token);
    };

    onNotification = (notification)=>{
        console.log(`onNotification ${notification.data}`);
        if (this.listener!=null){
            this.listener.onNotification(notification.data);
        }
        this.ackEvent(notification);
    };


    onNotificationOpened = (action, notification, appStarted)=>{
        if (this.listener!=null){
            this.listener.onNotificationOpened(action, notification, appStarted);
        }
        this.ackEvent(notification);
    };

    // wither the onNoificationOpened or onNotification has already been called.

    onNotificationDisplayed = (notification)=>{
        if (this.listener!=null){
            this.listener.onNotificationDisplayed(notification);
        }
    };

    ackEvent = (notification) => {
        const event_id = notification.data.event_id || notification.data['event-id'];
        if (event_id)
            return this.updateEventAck(event_id, true);

    };

    /***********************
     * Event processing
     **********************/
    imageUrl = (id) => {
        return this.api.callurl(`/images/${id}`, true)
    };

    /**
     List all live events for a device token

     @param params Parameters to the list live events call
     **/

    listLiveEvents = (params) => {
        const devToken = this.deviceToken;
        const path = `geodevice/${devToken}/live-events`;
        return this.api.callapi("GET", path, params, true, true);
    };

    updateEventAck = (eventId, ack) => {
        const params = {"ack": ack};
        const path = `events/${eventId}/ack`;
        return this.api.callapi("PUT" , path, params, true, true);
    };


    updateEventEnacted = (eventId, ack) => {
        const params = {"enact": ack};
        const path = `events/${eventId}/enact`;
        return this.api.callapi("PUT" , path, params, true, true);
    };

    /**
     List all search events for a location and distance

     @param params Parameters to the list search events call -
     distance = distance to search
     units = metric/imperial
     num = number of results to return , 0 for all
     @param latitude = lat of center point
     @param longitude = long of center point
     **/

    listSearchEvents = (params, latitude, longitude) => {
        return this.api.callapi("GET", `geopos/${longitude},${latitude}/live-events`, params, true, true);
    };

    /**
     List all acknowledged live events

     @param params Parameters to the list live events call
     **/

    listAcknowledgedLiveEvents = (params) => {
        return this.api.callapi("GET", "events/acknowledged", params, true, true);
    };

    /**
     List all followed live events

     @param params Parameters to the list live events call
     **/
    listFollowedLiveEvents = (params) => {
        return this.api.callapi("GET", "events/following", params, true, true);
    };

    /**
     List all followed live events

     @param params Parameters to the list live events call
     **/
    listAlertedEvents= (params) => {
        const devToken = this.deviceToken;
        const path = `/geodevice/${devToken}/alerted-events`;
        return this.api.callapi("GET", path , params, true, true);
    };


    // TODO: add all the api calls that the others have.

    /*-----------------------------
     * profile options
     -----------------------------*/
    /**
     account profile for user
     must be registered or logged in
     **/

    getAccountProfile = () => {
        return this.api.callapi("GET", TES_PATH_PROFILE, null, true);
    };

    /**
     update account profile for user

     must be registered or logged in

     @param listener the code block to call on  completion
     **/

    updateAccountProfile = (params) => {
        return this.api.callapi("PUT", TES_PATH_PROFILE, params, true);
    };


    /**
     update account profile password for user

     only if user is not anonymous

     @param password - the new password
     @param oldPassword - the old password
     @param listener the code block to call on  completion
     **/

    updateAccountProfilePassword = (password, oldPassword, ) => {
        const params = {
            password: password,
            old_password: oldPassword
        };
        return this.api.callapi("PUT", TES_PATH_PROFILE, params, true);

    }



};

