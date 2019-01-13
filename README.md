
# WiSDK
## About

This Wi SDK allows integration of native mobile apps with the Welcome Interruption (Wi) servers.  It allows for the collecting
location information from a mobile device to the Wi Servers and allow the receipt of notifications from Wi servers.

The SDK also provides various interfaces to the REST api suported by the Wi servers.

The SDK is available for IOS and Android and is available an:-

* Objective C library (IOS)
* Java library (Android)
* React Native library (IOS + Android)

This document specifically for the Android Java version of the library

## Requirements

Currently the WiSDK has the following requirements:-

* Android Version 4.4 or higher
* IOS version 9 or higher

It also requires, the following libraries as dependencies

* React Native version 0.57+
* react-native-firebase": 5.1.0+
* react-native-device-info": 0.24.3+
* react-native-open-settings 1.0.1+
* react-native-permissions 1.1.1+
* moment 2.23+
* uuid 3.2.1+

These should all be installed via npm or yarn and added to your application package.json file.

*NOTE: on Android the new androidx work library is used to schedule background tasks. This is still in beta but is the future of background tasks while remaining backwards compatibilty with old version of Android.*

## Intstallation

Instalation of the wiSDK is a three part process:-

1. Install platform requirmeents
2. Install all dependancies and follow their installing details
3. Install the wiSDK

### Step 1: Install any platform requirements

For IOS install cocoapods

    pod init
    pod install
    
For Android there are no special requirements, however its very worthwhile to ensure that your manifest has allowBackup set to false.  This will stop device tokens and other internal things being saved between uninstall / reinstalls which can lead to some hard to track errors.

	eg. <application android:allowBackup="false">

### Step 2: Install and configure dpendancies.

Add the following libraries to your package json file and ensure that they are setup as per their authors instructions as they all have a native code component to them -

    "react-native-device-info": "^0.24.3",
    "react-native-open-settings": "^1.0.1",
    "react-native-permissions": "^1.1.1",
    "react-native-firebase": "^5.1.0",


these libraries just need to be installed:-

    "uuid": "^3.2.1"
    "moment": "^2.23.0"
   
Install uuid and moment

	yarn add uuid
	yarn add moment

Install react-native-device-info

    yarn add react-native-device-info
    react-native link react-native-device-info

Install react-native-open-settings

    yarn add react-native-open-settings
    react-native link react-native-open-settings

Install react-native-permissions

    yarn add react-native-permissions
    react-native link react-native-permissions

Add appropriate permissions to the manifest and info.plist as described in doco. This is critical for IOS or your app will could be rejected by Apple.

Install react-native-firebase

    yarn add react-native-firebase
    react-native link react-native-firebase

Then follow the instructions to install the following components on Android and IOS:-

* firebase core 
* analytics 
* crash anlytics 
* cloud messaging
* notifications

This library is fairly complex to install and tends to cause the most issues to get a clean compile.  We endevour to keep this component dependancy fairly up to date as its critical to the correct processing of notification on both the Android and IOS platform.

As well as setting up the FCM library you also need to setup FCM with your application which is covered in the react-native-firebase setup.  You can see the following documentation for further informaton:-

https://firebase.google.com/docs/cloud-messaging/

NOTE: WiSDK shares many of the dependancies FCM . It is very important that all the play services dependancies are the same version and all the FCM dependancies match. It is safe to update the WiSDK dependances to match the version of FCM to use.

### Step 3: Install the WiSDK
When all the dependanicies have been installed and configured, you can install the wiSDK and automatically link it using the following:

	yarn add react-native-wisdk
	react-native link react-native-wisdk

If you like you can manually install the wiSDK but this is not recommended.

#### iOS
1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-wisdk` and add `RNWisdk.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNWisdk.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android
1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.welcomeinterruption.rnwisdk.RNWisdkPackage;` to the imports at the top of the file
  - Add `new RNWisdkPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-wisdk'
  	project(':react-native-wisdk').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-wisdk/android')
  	```
  	
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-wisdk')
  	```


## Getting Started

Welcome interruption is a platform for real time digital reach. By using the WiSDK you can turn any mobile application into an
effective mobile marketting tool that can target customers based on their real time location.

To get started we need to explain some terminoloy. In Welcome Interruption there are a number of key entities:-

 - Provider - A provider is the main entity which controls which offers gets sent to where, when and who. This is usually the owner of
 the users of the system (ie. the company that owns the app the Wi Sdk will be added too)
 - Place of interest (POI) - A defined geographic area that the provider sets up.
 - Event - A real time offer created by a Provider that is targeted to one or more POI's that they previously created
 - Users - A customer of the provider. Users can be anonymous and have a link to an external system or they can be full users (ie. email, name, etc)
 - Device - Links a phone and user to a provider. Contains reference to a user, current Lat/Lng of a phone as well as preferred
 notification mechanisms.
 - Campaign - the ability to target users/devices using external attributes as well as geo information.


Typically to setup a Client in Welcome Interruption we do the following:-

1. add a provider to Welcome interruption and configure it with a campaign schema, external system intergration details and push
certificates and API keys. This will require us to have your clients APNS push cerificates and any API key for Googles FCM serivce.
2. Once the provider is setup you will be provided with a provider key and certificate keys which **MUST** be specified as
part of the WiSDK configuration.
3. Start Integrating the WiSDK


## WiSDK Quickstart integration

Typically integration is done as follows:-

1. Configure SDK
2. Create listener (optional)
3. Start Wi up
4. Use the API to list offers, update profiles, etc (optional)
5. Permissions and capabilities

Wi works silently in background sending location updates to our servers as users go about their daily business. You can even close the app and
everything just keeps working

A minimal integration is just creating and configuring a wiApp singleton in your app start screen then calling the start method in your startup screen componentDidMount screen.

###Step 1: In your App.js file or entry point

```javascript

const PROD_PROVIDER_KEY = "xxxxxxxxxxxxxxxxxxxxxxxx"; // <- provided by us
const TEST_PROVIDER_KEY = "5b53e675ec8d831eb30242d3"; // <- provided by us

// Create a WiApp singleton object
const createWiApp = (prodProviderKey, testProviderKey) => {
    const config = new WiConfig(prodProviderKey, testProviderKey);
    config.deviceTypes = WiConfig.deviceTypeGCM ;
    config.askForLocationPermTitle = 'Allow "MyApp" to access your location?';
    
    return WiApp.createManager(config);
};

export const wi =  createWiApp(PROD_PROVIDER_KEY, TEST_PROVIDER_KEY);
```

**NOTE: it is essential to create the wi singleton as soon as possible in app startup since Android will call your app.js file it the app is started in background due to some event such as a notification, location update, reboot etc..**

###Step 2: In your startup screen start wi up

```javascript
import { wi } from "../App";

class StartupScreen extends React.Component {

    componentDidMount() {
	 wi.start().then((isauthorized) => {
        console.log(`We are done: ${isauthorized}`);
    })
    .catch((e) => {
        alert(`Failed to start up Wi location services: ${e}`);
    })
    .finally(() => {
         // do whatever 
    });

} 
```
**NOTE: by default the start routine sets up location monitoring, registers with FCM and authenticates with the wiServers. This process is asynchronous**

##Prod/Test environments
The wiSDK can run against test servers or production servers. This is simply a matter of setting the environment variable as part of the WiConfig object setup or calling the convience function setTestEnvironment

**IMPORTANT** You need to set the environment prior to calling start as this determines the following :-

 * endpoint
 * provider
 * push profile (used to select the correct FCM project id or APN certificate

By default the environment is set to production.

You can set the environment as follows:

```javascript
  wi.setTestEnvironment(true); // for test mode
  wi.setTestEnvironment(false); // for prod mode
```

or

```javascript
	config = new WiConfig(prodProviderKey, testProviderKey);
	config.renvironment = (!__DEV__) ? "prod": "test";
```

### configure

Configuration is done completely through the WiConfig object.  It is used to bind a provider with an app and describe how the WiSDK should interact with the device and Wi Servers.

Typically a config object is created at app startup and then passed to the WiApp object start method. The config object can set the sensitivty of geo regions monitored, how users and devices are created and the type of notification mechanism that should be used by the sdk

By default the config object has good defaults and usually the only thing needed to be set is the type of device type notification to set (ie. the deviceTypes field) and any custom messages for permission setup


```javascript
    const config = new WiConfig(prodProviderKey, testProviderKey);
    config.deviceTypes = WiConfig.deviceTypeGCM ;
    config.askForLocationPermTitle = 'Allow "MyApp" to access your location?';
```

### device profile
By default an anonymous user is created on the wi servers and is  asociated with your device token. if you want to get / update the user details you can use the getAccountProfile and updateAccountProfile api calls to get / set the following fields

* email =  email for the device
* first_name = users first name
* last_name = users last name

if interfacing to an external system has been enabled to your provider, you can also enter

* external_id - any string value (should be unique)
* program_attr - a dictionary of name value pairs - only available if a program is setup for a provider

This allows you to associate the phone device with a real member.

```javsscript

    wi.updateAccountProfile({
        external_id: profile.ID,  // a key to an external system
        first_name: profile.GivenNames,
        last_name: profile.Surname,
        email: profile.Email,
        
        // Note attribues are specific to each provider
        attributes: {
            MemberNo: profile.MemberNo,
            Mobile: profile.Mobile,
            RegistrationDate: profile.RegistrationDate,
            Gender: profile.Gender,

        },
    })
    .then((res) => {
         console.log(`update wi meta data: ${JSON.stringify(res)}`);
    })
    .catch((res) => {
         console.log(`Failed to update meta data${JSON.stringify(res)}`);
    });
```
      
To manipulate profiles calls have the follwoing prototype:-

```javascript
   getAccountProfile();
   updateAccountProfile(params);
```  

Both of these calls return a promise. On success or failure a JSON result dictionary is returned.  

*NOTE: these calls will fail unless you have successfully authenticated with the system*

### Advanced setup
By default wi asks for permission where necessary and displays dialogs and so on itself. You can stop it from doing this and ask for all this infomration yourself. 

If you want to ask for location permission yourself, you just need to set the noPermissionDialog config option as following:-

```javascript
     WiConfig config = new WiConfig(PROD_PROVIDER_KEY, TEST_PROVIDER_KEY);
     config.noPermissionDialog = true;                                       
```

You can also check for permissions and decide to do something based on the results.

```javascript

    wi.getCurrentLocationPermission().then((perms)=>{
        if (wi.haveDesiredPermission(perms) !== wi.PERMISSION_OK {
            // ask for permission
        }
        else {
           // do normal app startup
           wi.start();
        }
    }).catch((e)=>{
         alert("Failed to determine location permission: "+e);
			// you can start the app up here if you like but wi will be disabled
    });

```

### Listeners

The WiSDK supports a interface or listener class that can be used to get information about what the WiSDK is doing behind the scenes.
Implmenting this interface is optional but may be useful depending on the app you are integrating with.

this protocol is defined in TESWiApp as follows:-

```javascript

export class WiAppListener {

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
     * param error  Error set on error may have result set if the call returned success=False
     **/
    onAuthenticateFail = (resp) => {};

    /**
     * Ask the user to change permission via the settings screen
     */
    askForPermission = (config, settings) => {};

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
    onErrorHandler =(prefix, e) => {};

};

```

If defined it is typically passed into the create manager call right at app initialisation.

```javascript
const PROD_PROVIDER_KEY = "xxxxxxxxxxxxxxxxxxxxxxxx"; // <- provided by us
const TEST_PROVIDER_KEY = "5b53e675ec8d831eb30242d3"; // <- provided by us

// create a listener class and overwrite the methods of interest

const AppWiListener =  class extends WiAppListener {
    onStartupComplete =(isAuthorized) => {
        console.log('startup complete: '+isAuthorized);
    };

    onLocationUpdateHandler=(loc)=>{
        console.log('loc: '+JSON.stringify(loc));
    };

    onGeoUpdateHandler=(loc)=>{
        console.log('geo: '+JSON.stringify(loc));
    };

    onNotificationDisplayed = (notification)=>{
        console.log("--> Notification Displayed: "+JSON.stringify(notification.data));
    };

    onNotificationOpened = (action, notification, appStarted)=>{
        console.log("--> Notification Opened: "+action+" "+JSON.stringify(notification.data)+" started:"+appStarted)
    };
}

// Create a WiApp singleton object
const createWiApp = (prodProviderKey, testProviderKey) => {
    const config = new WiConfig(prodProviderKey, testProviderKey);
    config.deviceTypes = WiConfig.deviceTypeGCM ;
    config.askForLocationPermTitle = 'Allow "MyApp" to access your location?';
    
    return WiApp.createManager(config, new AppWiListener());
};

```

### Push notification format
From the wi dashboard, events and campaigns are created. These eventually make it to the app in the form of push notifications when a device enter in an active radius of an events geofence. 

The event can create notifications that are customisable per user via :-

* template substition (for most string fields)
* plugin integrations (requires server development and is used to inteface directly to back end systems)

#### Templates
Template substitution allows for special field inserts to be added to most text fields. At runtime the field inserts
are substituted with the device details (as setup in config or by calling the updateAccountProfile api call). The following
field inserts are supported:-

* user_name
* email
* first_name
* last_name 
* full_name
* external_id
* All program defined field as per provider program

The following items in an event are templatable:-

 * title
 * detail 
 * extract 
 * media_external_url 
 * media_thumbnail_url
 * notification_channel  
 * offer_code 
 
From the wi Dashboard, to specify a insert in one of these field simply wrap the field insert around {} 
 
 eg.  To add first name to the media url you would write something like this:- 
  
> ‘https://x.y.z/videoxxx?name={first_name}  
 
if first_name for the device was set to Phillip then it would resolve to:-
 
> https://x.y.z/videoxxx?name=Phillip) 
 

#### Plugins
Plugin integration are outside of the scope of this document but allow much flexibilty in modifying an events detail for 
individual devices.  

A plugin can be used to 

* add integration spectific items to an event record
* custom an event for each device so its notification is unique 
* communicate with a back end system at event creation and device notification
* allow for backend systems to call back to wi.

#### push format
The payload for a push notification on android is is divided into a notification section and data section.

An Android notification example follows:-

```json
{
  "to": "fb734-JQwgs….mxle",
  "notification": {
    "title": "a test notification",                 // <-- title field
    "body": "a test message",                       // <-- body field
    "android_channel_id": "WiNotifyChannelPush",    // <-- event notification channel field 
  },
  "android": {
    "priority": "high"
  },
  "data": {                                 // <-- event deails that triggered the notification
    "event_id": "5b9a0f05f26f9f7104cd08a5",
    "title": "a test notification",
    "detail": "a test message",
    "further_info": "",
    "type": "deal"
    "event_category": "General",
    "starts": "2018-09-13T07:17:00+0000",  
    "expires": "2018-09-13T08:17:00+0000",
    "broadcast_band": "auto",
    "poi_id": "5b7287cff26f9f3e3b5b82ee",
    "poi_lat": -37.356721,
    "poi_lng": 144.52809100000002,
    "provider_id": "5b44a3fff26f9fcf1f04b0ac",         // <--- provider id as configured
    "media_external_url": "https://www.youtube.com/watch?v=xLCn88bfW1o",
    "media_thumbnail_url": "http://i3.ytimg.com/vi/xLCn88bfW1o/maxresdefault.jpg",
    "enactable": false,
    "event_group_id": "b0357a7a-5128-4463-8b4e-a45771183b84",
    "event_history_id": "5b9a0f05f26f9f7104cd08a4",
    "notification_channel": "WiNotifyChannelPush",
  }
}
```
An IOS notification example follows:-

	{
		"aps": {
			"alert": "A notification\r\nThis is a test notification",
			"sound": "default",
			"mutable-content": 1,
			"category": "WiCategoryPush",
		},
		
  		"data": {
			"event_id": "5b9a0b36f26f9f7104cd089c",
			"type": "deal",
			"title": "A notification",
			"detail": "This is a test notification",
			"event_category": "General",
			"further_info": "",
			"starts": "2018-09-13T07:00:00+0000",
			"expires": "2018-09-13T08:00:00+0000",
			"broadcast_band": "auto",
			"poi_id": "5b5ea71bf26f9fffdf92058e",
			"poi_lng": 144.52037359999997,
			"poi_at": -37.36093909999999,
			"enactable": false,
			"provider-id": "5b5ea71bf26f9fffdf92058d",
			"media_external_url": "https://www.youtube.com/watch?v=xLCn88bfW1o",
			"media_thumbnail_url": "http://i3.ytimg.com/vi/xLCn88bfW1o/maxresdefault.jpg",
			"notification-channel": "WiCategoryPush",
			"event_histor_Id": "5b9a0b36f26f9f7104cd089b",
			"event_group_id": "bf059e8d-06cd-400d-ac99-cfddfc0aa88c"
		}
	}

### Using thie API

The remainder of the WiSDK wraps the Wi Rest based API. This API can be used to

* view live/historical events
* events that have been taken up by this user
* setting up inclusions/exclusions for event notification
* searching for events


## API documentation


### User management
```javascript
   registerUser(params);
   loginUser(params;

```

### Account profile calls
```javascript
   getAccountProfile() ;
   updateAccountProfile(params);
   updateAccountProfilePassword(password, oldPassword);
   updateAccountSettings(params)
```

### Event details
```javascript
   listLiveEvents(params);
   listSearchEvents(params, latitude, longitude);
   listAcknowledgedLiveEvents(paramsr);
   listFollowedLiveEvents(params);
   listAlertedEvents(params);
```
#### List Alerted Events
List all alerted live events for this device. A list of events are returned in the data field of the result JSON.

```
	listAlertedEvents(params)
```

Parameter Name | Description
-----------| -----------
params | Optional filters to apply to the list live events call
listener | the code block to call on successful completion

Valid key / value filters for this call are:-

Field | Value
------|------
notification_type |type of notification since a device can have multiple push targets. Valid values are:-<br/><ul><li>'apn' - apple push notification</li><li>'gcm' - google cloud message<li>'mail' - email</li><li>'sms' - sms</li><li>'test' - test</li><li>'pkpass' - apple wallet</li><li>'ap' - google wallet</li><li>'passive' - a virtual push (for people that don't want to set notification on)</li><br>
pending| Whether the alert is pending true/false (default true)
relative_start| the relative start date to get events from. This can be a number suffixed by d (days) h (hours) m (minutes) s (sections) - eg. 20d - give me the events for the last 20 days
start|start record for results (default = 0)
limit|number of records to return (default = -1 - all)
sort_field|sort on field (default = alerted)
sort_desc|sort decending (default = True)


### Updating event status
Normally the wiSDK deals with event opens internally, but if you do custom notification handling bypassing the standard mechansim you can call these function

```javadcript
   updateEventAck(eventId, ack);
   updateEventEnacted(eventId, enacted);
```

## Example

Coming soon...

It is a bare bones project that will send location information from the device to the Wi Servers.  It also
has a demo provider key  which can be used to send offers from wi to the device.

## Author

Welcome Interruption and the WiSDK are developed by the 3-electric-sheep pty ltd.

for support please contact:-

pfrantz, pfrantz@3-elecric-sheep.com

## License

WiSDK is available under the Welcome Interruption SDK License. See the LICENSE file for more info.

