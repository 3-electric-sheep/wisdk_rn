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

#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

#import "LocationInfo.h"
#import "TESConfig.h"

#define METERS_PER_MILE 1609.344
#define METERS_PER_KM  1000

#define EARTH_RADIUS 6378137 // in meters

#define MAP_MAX_SIZE 2000*1000 // 2000km max size

//----------------------------------------------
// TESWIAppDelegate protocol
//----------------------------------------------
@protocol TESLocationMgrDelegate <NSObject>

- (void)sendDeviceUpdate:(nonnull NSArray *)locInfo inBackground: (BOOL) background;
- (void) sendRegionUpdate: (nonnull CLRegion *) region withLocation:(nonnull LocationInfo *) locInfo inBackground: (BOOL) background;
- (void)sendChangeAuthorizationStatus:(CLAuthorizationStatus)status;

- (void)sendError:(nullable NSError *)error withMsg:(nullable NSString *)msg inGeo:(BOOL)geoError;

@end

@interface TESLocationMgr : NSObject <CLLocationManagerDelegate> {

}

@property(nonatomic, strong, nullable) TESConfig *config;

@property (strong, nonatomic, nullable) CLLocationManager *locationManager;

@property (nonatomic) CLAuthorizationStatus authStatus;
@property (nonatomic) BOOL requireAlwaysAuth;

@property (nonatomic) BOOL backgroundMonitoring;
@property (nonatomic) BOOL foregroundMonitoring;
@property (nonatomic) BOOL visitMonitoring;
@property (nonatomic) BOOL locationFixOnly;  // if set to YES then don't send server location updates

@property (readonly, nonatomic, nullable) CLLocation * location;
@property (readonly, nonatomic) BOOL isUsingMetric;
@property (readonly, nonatomic) BOOL isLocationStale;

@property (strong, nonatomic, nullable) LocationInfo * lastLocation;


@property (strong, nonatomic, nullable) id <TESLocationMgrDelegate> delegate;

@property (strong, nonatomic, nullable) NSString * errorMsg;

@property (strong, nonatomic, nullable) id<NSObject> observer_fg;
@property (strong, nonatomic, nullable) id<NSObject> observer_bg;

- (nonnull instancetype)initWithConfig: (nullable TESConfig * ) config;
- (void) startLocationManager: (BOOL) requireAlwaysAuth requestAuth: (BOOL) makeRequest;

- (BOOL) locationServicesEnabled;
- (BOOL) significantLocationChangeMonitoringAvailable;

- (BOOL)startForegroundMonitoring: (BOOL) force;
- (BOOL)startBackgroundMonitoring: (BOOL) force;
- (BOOL)startVisitMonitoring: (BOOL) force;

-(void) stopForegroundMonitoring;
-(void) stopBackgroungMonitoring;
-(void) stopVisitMonitoring;

- (BOOL) addRegion: (CLLocationCoordinate2D) coord andRadius: (double) radius  andIdentifier: (NSString *_Nullable) identifier;
- (BOOL) removeRegion:(nonnull NSString *)identifier;
- (void) clearRegions;

-(BOOL)ensureMonitoring;
- (void)ensureNotMonitoring;

- (BOOL) isInBackground;

- (double) distanceInMeters: (double) distanceInLocaleUnits;
- (CLLocationDistance) distanceFromCurrentLocation: (CLLocationCoordinate2D) pos;
- (CLLocationDistance) distanceFromLocation: (nonnull CLLocation *) loc forPosition: (CLLocationCoordinate2D) pos;

- (void)sendLocationUpdate:(nonnull NSArray *)locations;
- (void)sendVisitUpdate:(nonnull CLVisit *)visit;

- (void)sendGeoRegionUpdate:(nonnull CLRegion *)region andLocation:(CLLocation *_Nullable)location andState:(CLRegionState)state;

- (nullable NSString *)statusName: (CLAuthorizationStatus) status;

- (nullable NSString *) getErrorMsg;
- (void)clearErrorMsg;


@end
