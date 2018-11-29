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

#ifndef TESConfig_h
#define TESConfig_h

@interface TESConfig : NSObject

#define ENV_PROD @"prod"
#define ENV_TEST @"test"

#define PROD_SERVER @"https://api.3-electric-sheep.com"
#define PROD_PUSH_PROFILE @"<PROD_PROFILE>"

#define TEST_SERVER @"https://testapi.3-electric-sheep.com"
#define TEST_PUSH_PROFILE @"<TEST_PROFILE>"

#define WALLET_OFFER_CLASS @"wi_offer_pass"
#define WALLET_PROFILE @"email"

#define MEM_CACHE_SIZE 8388608  //8 * 1024 * 1024;
#define DISK_CACHE_SIZE 20971520 // 20 * 1024 * 1024;

#define GEOFENCE_RADIUS  20; // 20 mtrs

#define DEVICE_TYPE_APN  @"apn"
#define DEVICE_TYPE_MAIL  @"mail"
#define DEVICE_TYPE_SMS  @"sms"
#define DEVICE_TYPE_WALLET  @"pkpass"
#define DEVICE_TYPE_MULTIPLE @"multiple"


typedef NS_OPTIONS(NSUInteger, DevicePushTargets
){
    deviceTypeNone = 0,
    deviceTypeAPN = 1 << 0,
    deviceTypeWallet = 1 << 1,
    deviceTypeMail = 1 << 2,
    deviceTypeSms = 1 << 3
};

/**
 Configuration options for the WiApp object
 */

@property (strong, nonatomic, nullable) NSString * environment;
@property (strong, nonatomic, nullable) NSString * providerKey;

@property (nonatomic) NSUInteger memCacheSize;
@property (nonatomic) NSUInteger diskCacheSize;

@property (nonatomic, strong, nullable) NSString * server;
@property (nonatomic, strong, nullable) NSString * pushProfile;

@property (nonatomic, strong, nullable) NSString * testServer;
@property (nonatomic, strong, nullable) NSString * testPushProfile;

@property (nonatomic, strong, nullable) NSString * walletOfferClass;

@property (nonatomic) BOOL requireBackgroundLocation;

@property (nonatomic) BOOL useSignficantLocation;
@property (nonatomic) BOOL useVisitMonitoring;
@property (nonatomic) BOOL useForegroundMonitoring;

@property (nonatomic) CLLocationDistance distacneFilter; // in meters
@property (nonatomic) CLLocationAccuracy accuracy;
@property (nonatomic) CLActivityType activityType;

@property (nonatomic) NSInteger staleLocationThreshold; // in seconds

@property (nonatomic) BOOL logLocInfo; // whether to log debugging info
@property (nonatomic) BOOL nativeRequestAuth;

/**
 * do automatic authentication if set. uses auth credentials to fill the register/login call
 * if credentials has the field  anonymous_user set ot YES  or
 * left as null, then an anonymous register/login is made
 */
@property (nonatomic) BOOL authAutoAuthenticate;
@property (nonatomic, strong, nullable) NSDictionary * authCredentials;

/**
 * list of device types wanted by a user apn, pkpass, mail, sms,
 */
@property (nonatomic) DevicePushTargets deviceTypes;


@property (nonatomic) BOOL debug;

@property (nonatomic, strong, nullable) NSURLSessionConfiguration * sessionConfig;

@property (nonatomic, strong, readonly, nonnull) NSString * envServer;
@property (nonatomic, strong, readonly, nullable) NSString * envPushProfile;

@property (nonatomic) BOOL useGeoFences;
@property (nonatomic) double geoRadius;

- (nullable instancetype) initWithProviderKey: (nullable NSString *) providerKey NS_DESIGNATED_INITIALIZER;
- (void) fromDictionary: (nonnull NSDictionary *) dict;
@end
#endif /* TESConfig_h */
