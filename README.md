
# react-native-wisdk

## Dependencies

Init cocoapods

    pod init
    pod install

require manual installation:-

    "react-native-device-info": "^0.24.3",
    "react-native-open-settings": "^1.0.1",
    "react-native-permissions": "^1.1.1",
    "react-native-firebase": "^5.1.0",


will just be used:-

    "uuid": "^3.2.1"

Steps to enusre dependanices work:

Install react-native-device-info

    yarn add react-native-device-info
    react-native link react-native-device-info

Install react-native-open-settings

    yarn add react-native-open-settings
    react-native link react-native-open-settings

Install react-native-permissions

    yarn add react-native-permissions
    react-native link react-native-permissions


Add appropriate permissions to the manifest and info.plist as described in doco

Install react-native-firebase

    yarn add react-native-firebase
    react-native link react-native-firebase

    follow the instructions to install firebase core, analytics, crash anlytics, cloud messaging, notifications



## Getting started

`$ npm install react-native-wisdk --save`

### Mostly automatic installation

`$ react-native link react-native-wisdk`

### Manual installation


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


## Usage
```javascript
import RNWisdk from 'react-native-wisdk';

// TODO: What to do with the module?
RNWisdk;
```

Extras:

in manifest need    <application android:allowBackup="false">
