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

#import "RNWisdk.h"


NSString * onGeofenceUpdate = @"onGeofenceUpdate";
NSString * onLocationUpdate = @"onLocationUpdate";
NSString * onPermissionChange =@"onPermissionChange";
@implementation RNWisdk
{
    BOOL _isMonitoring;
}

RCT_EXPORT_MODULE();

static BOOL _hasListeners = NO;

+ (id)allocWithZone:(NSZone *)zone {
    static RNWisdk *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [super allocWithZone:zone];
    });
    return sharedInstance;
}

#pragma mark - Lifecycle

-(void)dealloc
{
    self.config = nil;
    self.locMgr = nil;
}


- (NSArray<NSString *> *)supportedEvents
{
    return @[onGeofenceUpdate, onLocationUpdate, onPermissionChange];
}

- (NSDictionary *)constantsToExport
{
    return @{
            @"GeofenceUpdate": onGeofenceUpdate,
            @"LocationUpdate" : onLocationUpdate,
            @"PermissionChange" : onPermissionChange
    };
}


- (dispatch_queue_t)methodQueue
{
  return dispatch_get_main_queue();
}

+ (BOOL)requiresMainQueueSetup
{
    return YES;
}


#pragma mark - Public API

- (nullable instancetype) init
{
    _isMonitoring = NO;
    self = [super init];
    if (self){
        self.config = [[TESConfig alloc] init];
    }
    return self;
}

- (void)startObserving {
    [super startObserving];
    _hasListeners = YES;
}

- (void)stopObserving {
    [super stopObserving];
    _hasListeners = NO;
}

RCT_EXPORT_METHOD(configure:(NSDictionary *)data
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject )
{
    [self.config fromDictionary: data];
    resolve(nil);
}

RCT_EXPORT_METHOD(connect:(BOOL) requireBackgroundProcessing
            resolver:(RCTPromiseResolveBlock)resolve
            rejecter:(RCTPromiseRejectBlock)reject )
{
    self.locMgr = [[TESLocationMgr alloc] initWithConfig:self.config];
    if (self.locMgr == nil){
        reject(@"Error Unknown", @"Failed to initialise Location manager", nil);
    }
    self.locMgr.delegate = self;

    [self.locMgr startLocationManager:self.config.requireBackgroundLocation requestAuth:self.config.nativeRequestAuth];
    resolve(nil);

}

/**
 * gets last known location if any
 */
RCT_EXPORT_METHOD(getLastKnownLocation:(RCTPromiseResolveBlock) resolve
                                       rejecter:(RCTPromiseRejectBlock) reject)
{
    BOOL inBackground = [self.locMgr isInBackground];

    CLLocation * location = [self.locMgr location]; // gets the last loc direct from the location manager
    if (location != nil) {
        LocationInfo *locEntry = [[LocationInfo alloc] initWithLocation:location andBackgroundMode:inBackground];
        resolve([locEntry toDictionary]);
    }
    else {
        resolve(nil);
    }
}

/**
 * requests start of location updates.
 */
RCT_EXPORT_METHOD(requestLocationUpdates:(RCTPromiseResolveBlock)resolve
                                    rejecter:(RCTPromiseRejectBlock)reject)
{
    if ([self.locMgr ensureMonitoring])
        resolve(nil);
    else
        reject(@"RequestLocationUpdates",[self getError], nil);
}

/**
 * requests removal of location updates.
 */
RCT_EXPORT_METHOD(removeLocationUpdates:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    [self.locMgr ensureNotMonitoring];
    resolve(nil);
}

/**
 * Adds geofences. This method should be called after the user has granted the location
 * permission.
 */
RCT_EXPORT_METHOD(addGeofences: (NSArray *) geofencesToAdd
                  resolver: (RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject)
{
    for (NSDictionary * entry in geofencesToAdd) {
        double lat = [[entry valueForKey:@"latitude"] doubleValue];
        double lng = [[entry valueForKey:@"longitude"] doubleValue];
        NSNumber * radiusObj =  [entry valueForKey:@"radius"];
        double radius = (radiusObj != nil) ? [radiusObj doubleValue]: -1;
        NSString * ident =[entry valueForKey:@"identifier"];

        CLLocationCoordinate2D coord = CLLocationCoordinate2DMake(lat, lng);
        if (![self.locMgr addRegion:coord andRadius:radius andIdentifier:ident]) {
            reject(@"AddGeofenceFailed", [self getError], nil);
            break;
        }
    }
    resolve(nil);
}

/**
 * Removes geofences by id. This method should be called after the user has granted the location
 * permission.
 */
RCT_EXPORT_METHOD(removeGeofences:(NSArray<NSString *> *) ids
                      resolver: (RCTPromiseResolveBlock)resolve
                      rejecter:(RCTPromiseRejectBlock)reject)
{
    for(NSString * ident in ids) {
        if (![self.locMgr removeRegion:ident]) {
            reject(@"RemoveGeofenceFailed", [self getError], nil);
            break;
        }
    }
    resolve(nil);
}

/**
 * Removes all geofences. This method should be called after the user has granted the location
 * permission.
 */
RCT_EXPORT_METHOD(clearGeofences:(RCTPromiseResolveBlock)resolve
                      rejecter:(RCTPromiseRejectBlock)reject)
{
   [self.locMgr clearRegions];
    resolve(nil);
}

#pragma mark - TESLocationMgrDelegate methods

- (void)sendDeviceUpdate:(nonnull NSArray *)locations inBackground:(BOOL)background {

    if (!_hasListeners)
        return;

    NSMutableArray * newLocs = [[NSMutableArray alloc] initWithCapacity:[locations count]];
    for (LocationInfo * locInfo in locations) {
        [newLocs addObject:locInfo.toDictionary];
    }
    NSDictionary * result = @{
        @"success": @YES,
        @"locations": newLocs
    };

    [self sendEventWithName:onLocationUpdate body:result];

}

- (void)sendRegionUpdate:(nonnull CLRegion *)region withLocation:(nonnull LocationInfo *)locInfo inBackground:(BOOL)background {

    if (!_hasListeners)
        return;

    NSDictionary * result = @{
        @"success": @YES,
        @"location": [locInfo toDictionary]
    };

    [self sendEventWithName:onGeofenceUpdate body:result];

}

- (void)sendChangeAuthorizationStatus:(CLAuthorizationStatus)status {

    if (!_hasListeners)
        return;

    NSDictionary * result = @{
            @"status":@(status),
            @"status_name": [self.locMgr statusName:status]
    };
    [self sendEventWithName:onPermissionChange body:result];
}

- (void)sendError:(NSError *)error withMsg:(NSString *)msg inGeo:(BOOL)geoError {

    if (!_hasListeners)
        return;

    NSInteger code = -1;
    NSString * errorMsg = @"";
    if (error != nil){
        code = [error code];
        errorMsg = [error localizedDescription];

    }
    NSDictionary * result = @{
        @"success": @NO,
        @"code":@(code),
        @"error": [NSString stringWithFormat:@"%@ %@", msg, errorMsg]
    };

    NSString * type = (geoError) ? onGeofenceUpdate : onLocationUpdate;
    [self sendEventWithName:type body:result];

}

-(NSString *) getError {
    if (self.locMgr == nil)
        return @"Location manager has not been initialised";

    NSString *err = [self.locMgr getErrorMsg];
    [self.locMgr clearErrorMsg];
    return err;
}

@end

