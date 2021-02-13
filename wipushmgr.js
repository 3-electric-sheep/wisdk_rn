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

import firebase from '@react-native-firebase/app';
import "@react-native-firebase/analytics";
import "@react-native-firebase/app";
import "@react-native-firebase/crashlytics";
import "@react-native-firebase/messaging";

import {Alert, Platform} from "react-native";

import Permissions from 'react-native-permissions';
import OpenSettings from 'react-native-open-settings';

// NOTE: this needs to match the string in the Android Manifsest.xml file.
const WI_NOTIFICATION_CHANNEL = "wi_notification_channel_1";
const WI_NOTIFICATION_CHANNEL_NAME = "WI Notification Channel";

export class WiPushMgrListener
{
    onRefreshToken = (token)=>{};
    onNotification = (notification)=>{}; // FG
    onNotificationDisplayed = (notification)=>{}; // BG on IOS if content=true
    onNotificationOpened = (action, notification, appStarted)=>{}; // BG and Closed

};

/**
 * Remote notifications behave as follows:

            App in foreground	            App in background	                                                App closed
 Android	onNotification triggered	    onNotificationOpened triggered if the notification is tapped	    getInitialNotification is populated if the notification is tapped and opens the app

 iOS	    onNotification triggered	    onNotificationDisplayed triggered if content_available set to true  getInitialNotification is populated if the notification is tapped and opens the app
                                            onNotificationOpened triggered if the notification is tapped

 Notes	    No visible notification         The notification is presented to the user by the                    The notification is presented to the user by the Mobile Device's OS
            it is up to you to display      Mobile Device's OS
            notifications manually
 */

export class WiPushMgr {

    constructor(config, listener) {
        this.config = config;
        this.deviceToken = null;
        this.listener = listener;
        this.onTokenRefreshListener = null;

        this.onForegroundMessageListener = null;
        this.onBackgroundMessageListener = null;
    };

    start(){
        return this._checkPermissions().then(()=>{
            this.onTokenRefreshListener = firebase.messaging().onTokenRefresh(this._setupToken);

            this.onForegroundMessageListener = firebase.messaging().onMessage(async remoteMessage => {
                console.log('message arrived in foreground!', JSON.stringify(remoteMessage));
                this._onNotification(remoteMessage)
            });

            this.onBackgroundMessageListener = firebase.messaging().setBackgroundMessageHandler(async remoteMessage => {
                console.log('Message handled in the background!', JSON.stringify(remoteMessage));
                this._onNotificationOpened(notificationOpen, false)
                if (remoteMessage.notification && remoteMessage.contentAvailable){
                    this._onNotificationDisplayed(remoteMessage)
                }

            });

            //if (Platform.OS === 'android') {
            //    const channel = new firebase.notifications.Android.Channel(WI_NOTIFICATION_CHANNEL, WI_NOTIFICATION_CHANNEL_NAME, firebase.notifications.Android.Importance.Max)
            //        .setDescription(WI_NOTIFICATION_CHANNEL_NAME);
            //    firebase.notifications().android.createChannel(channel);
            //}

            return firebase.messaging().getToken()
                .then(fcmToken => {
                    this._setupToken(fcmToken);
                })
        })
    }

    stop() {
        if (this.onTokenRefreshListener) this.onTokenRefreshListener();

        if (this.onForegroundMessageListener) this.onForegroundMessageListener();
        if (this.onBackgroundMessageListener) this.onBackgroundMessageListener();
    }

    async checkForNotification() {
        // Assume a message-notification contains a "type" property in the data payload of the screen to open

        // setup listener for background opener
        firebase.messaging().onNotificationOpenedApp(remoteMessage => {
            console.log(
                'Notification caused app to open from background state:',
                JSON.stringify(remoteMessage)
            );
            //
        });

        // Check whether an initial notification is available
        return firebase.messaging().getInitialNotification().then(remoteMessage => {
            if (remoteMessage) {
                console.log(
                    'Notification caused app to open from quit state:',
                    JSON.stringify(remoteMessage),
                );
                this._onNotificationOpened(remoteMessage, true);

            }
        });
    }

    displayNotification = (notification)=>{
        //if (Platform.OS === "android") {
        //    if (notification.data.media_thumbnail_url) {
        //        notification.android.setChannelId(WI_NOTIFICATION_CHANNEL)
        //            .android.setSmallIcon('ic_launcher')
        //            .android.setBigPicture(notification.data.media_thumbnail_url);
        //    }
        //    else {
        //        notification.android.setChannelId(WI_NOTIFICATION_CHANNEL)
        //            .android.setSmallIcon('ic_launcher');
        //    }
        //}
        //
        //firebase.notifications().displayNotification(notification);
        Alert.alert(
            notification.title,
            notification.body,
            [
                {   text: 'OK',
                    onPress: () => {}
                }
            ],
        )
    };

    _setupToken = (fcmToken)=>{
        this.deviceToken = fcmToken;
        if (this.listener)
            this.listener.onRefreshToken(fcmToken)

    };

    _onNotificationDisplayed = (notification) => {
        // Process your notification as required
        // ANDROID: Remote notifications do not contain the channel ID. You will have to specify this manually if you'd like to re-display the notification.
        if (this.listener)
            this.listener.onNotificationDisplayed(notification);
    };

    _onNotification = (notification) => {
        // Process your notification as required
        if (this.config.autoDisplayNotifications)
            this.displayNotification(notification);

        if (this.listener)
            this.listener.onNotification(notification)
    };

    _onNotificationOpened =  (notificationOpen, appStarted=false) => {
        const action = notificationOpen.action; // Get the action triggered by the notification being opened
        const notification = notificationOpen.notification; /// Get information about the notification that was opened
        if (this.listener)
            this.listener.onNotificationOpened(action, notification, appStarted);


    };

    _checkPermissions = ()=>{
        return firebase.messaging().hasPermission()
            .then(enabled => {
                if (enabled) {
                    // user has permissions
                    return Promise.resolve();
                } else {
                    // user doesn't have permission
                    return firebase.messaging().requestPermission();
                }
            })
            .catch((e)=>{
                this._alertForNotificationPermission(e);
            });
    };

    _alertForNotificationPermission = (e) =>
    {
        if (!this.config.askForNotificationPermission)
            return;

        const settings = (Platform.OS === 'ios')  ? Permissions.openSettings : OpenSettings.openSettings;

        Alert.alert(
            'Can we send you notifications?',
            'We can let you know of great offers in your area via a notification',
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

};
