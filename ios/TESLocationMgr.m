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


#import "TESLocationMgr.h"

#import "LocationInfo.h"
#import <MapKit/MapKit.h>


@implementation TESLocationMgr{
    BOOL sentAlert;
}

- (instancetype)initWithConfig:(nullable TESConfig *)config
{
    self = [super init];
    if (self) {
        self.config = config;
        self.backgroundMonitoring = NO;
        self.foregroundMonitoring = NO;
        self.visitMonitoring = NO;
        self.locationFixOnly = NO;

        self.locationManager = nil;
        self.lastLocation = nil;

        self.observer_fg = nil;
        self.observer_bg = nil;
    }
    return self;
}

- (void) dealloc {

    NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
    if (self.observer_fg != nil){
        [center removeObserver:self.observer_fg];
    }
    if (self.observer_bg != nil){
        [center removeObserver:self.observer_bg];
    }
}

#pragma mark - start by getting auth

- (void) startLocationManager: (BOOL) requireAlwaysAuth requestAuth: (BOOL) makeRequest
{
    self.authStatus = CLLocationManager.authorizationStatus;
    self.requireAlwaysAuth = requireAlwaysAuth;

    self.locationManager = [[CLLocationManager alloc] init];
    self.locationManager.delegate = self;

    if ([self isInBackground]) {
        [self _initBGLocationMgr];
    }
    else {
        [self _initFGLocationMgr];
    }

    if (self.config.useVisitMonitoring){
        [self _initVisitLocationMgr];
    }

    if (makeRequest) {
        if (self.requireAlwaysAuth)
            [self.locationManager requestAlwaysAuthorization];
        else
            [self.locationManager requestWhenInUseAuthorization];
    }

}

#pragma mark - info methods

- (BOOL) locationServicesEnabled
{
    return [CLLocationManager locationServicesEnabled];
}

- (BOOL) significantLocationChangeMonitoringAvailable
{
    return [CLLocationManager significantLocationChangeMonitoringAvailable];
}


- (CLLocation *) location
{
    return (self.locationManager) ? self.locationManager.location :  nil;
}

- (BOOL) isLocationStale
{
    CLLocation * loc = self.location;
    if (loc == nil)
        return YES;

    NSTimeInterval secs = -[loc.timestamp timeIntervalSinceNow];
    return secs > self.config.staleLocationThreshold;
}

- (BOOL) isUsingMetric
{
    NSLocale * locale = [NSLocale currentLocale];
    BOOL  metric = [[locale objectForKey:NSLocaleUsesMetricSystem] boolValue];
    return metric;
}

- (double) distanceInMeters: (double) distanceInLocaleUnits
{
    double meters = (self.isUsingMetric)? (distanceInLocaleUnits * METERS_PER_KM) : (distanceInLocaleUnits * METERS_PER_MILE);
    return meters;
}

- (CLLocationDistance) distanceFromCurrentLocation: (CLLocationCoordinate2D) pos
{
    if (self.location == nil)
        return 0.0f;

    CLLocation * posLoc = [[CLLocation alloc] initWithLatitude:pos.latitude longitude:pos.longitude];
    CLLocationDistance dist = [[self location] distanceFromLocation:posLoc];
    return dist;
}

- (CLLocationDistance) distanceFromLocation: (CLLocation *) loc forPosition: (CLLocationCoordinate2D) pos
{
    CLLocation * posLoc = [[CLLocation alloc] initWithLatitude:pos.latitude longitude:pos.longitude];
    CLLocationDistance dist = [loc distanceFromLocation:posLoc];
    return dist;
}

#pragma mark - monitoring modes

- (void) _initFGLocationMgr
{

    self.locationManager.distanceFilter = self.config.distacneFilter;
    self.locationManager.desiredAccuracy = self.config.accuracy;

    self.locationManager.activityType = self.config.activityType;
    self.locationManager.pausesLocationUpdatesAutomatically = NO;

}

-(void) _initBGLocationMgr
{

    self.locationManager.activityType = self.config.activityType;
    self.locationManager.pausesLocationUpdatesAutomatically = NO;

}

-(void) _initVisitLocationMgr
{
    self.locationManager.distanceFilter = self.config.distacneFilter;
    self.locationManager.desiredAccuracy = self.config.accuracy;
    self.locationManager.activityType = self.config.activityType;
    self.locationManager.pausesLocationUpdatesAutomatically = NO;

}


- (BOOL)startForegroundMonitoring: (BOOL) force
{
    if (self.locationServicesEnabled || force){
        if (self.backgroundMonitoring){
            [self stopBackgroungMonitoring];
        }

        self.foregroundMonitoring = YES;
        [self _initFGLocationMgr];
        [self.locationManager startUpdatingLocation];
        [self writeDebugMsg:nil msg:@"started foreground location monitoring"];

        // Define a weak self reference.
        TESLocationMgr * __weak weakSelf = self;
        BOOL forceFinal = force;

        // Subscribe to app state change notifications, so we can stop/start location services.

        // When our app is interrupted, stop the standard location service,
        // and start significant location change service, if available.
        if (self.observer_bg == nil) {
            self.observer_bg =[[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationWillResignActiveNotification object:nil queue:nil usingBlock:^(NSNotification *_Nonnull note) {
                if (weakSelf.config.useSignficantLocation) {
                    // Stop normal location updates and start significant location change updates for battery efficiency.
                    [weakSelf stopForegroundMonitoring];
                    [weakSelf startBackgroundMonitoring:forceFinal];
                }
            }];
        }

        // Stop the significant location change service, if available,
        // and start the standard location service.
        if (self.observer_fg) {
            self.observer_fg = [[NSNotificationCenter defaultCenter] addObserverForName:UIApplicationWillEnterForegroundNotification object:nil queue:nil usingBlock:^(NSNotification *_Nonnull note) {
                if (self.config.useForegroundMonitoring) {
                    // Stop significant location updates and start normal location updates again since the app is in the forefront.
                    [weakSelf stopBackgroungMonitoring];
                    [weakSelf startForegroundMonitoring:forceFinal];
                }
            }];
        }

        return YES;
    }
    else {
        [self writeDebugMsg:nil msg:@"Location manager not enabled."];
        return NO;
    }

}

-(void) stopForegroundMonitoring
{
    if (self.foregroundMonitoring) {
        [self.locationManager stopUpdatingLocation];
        self.foregroundMonitoring = NO;
        [self writeDebugMsg:nil msg:@"stopped  location monitoring"];
    }
}

- (BOOL)startBackgroundMonitoring: (BOOL) force
{
    if (self.locationServicesEnabled || force){
        if (self.significantLocationChangeMonitoringAvailable){
            if (self.foregroundMonitoring){
                [self stopForegroundMonitoring];
            }

            if (self.requireAlwaysAuth) {
                [self _initBGLocationMgr];
                [self.locationManager startMonitoringSignificantLocationChanges];
                self.backgroundMonitoring = YES;
                [self writeDebugMsg:nil msg:@"started significant location monitoring"];
                return YES;
            }
            else {
                [self writeDebugMsg:nil msg:@"requireAlwaysAuth not set - can't enable background location monitoring"];
            }
        }
        else {
            [self writeDebugMsg:nil msg:@"Significant location change service not available."];
        }
    }
    else {
        [self writeDebugMsg:nil msg:@"Location manager not enabled."];
    }
    return NO;
}

-(void) stopBackgroungMonitoring
{
    if (self.backgroundMonitoring){
        [self.locationManager stopMonitoringSignificantLocationChanges];
        self.backgroundMonitoring=NO;
        [self writeDebugMsg:nil msg:@"stopped significant location monitoring"];
    }
}

- (BOOL)startVisitMonitoring:(BOOL)force {

    if (self.locationServicesEnabled || force){
        self.visitMonitoring = YES;
        [self _initVisitLocationMgr];
        [self.locationManager startMonitoringVisits];
        [self writeDebugMsg:nil msg:@"started visit location monitoring"];
        return YES;
    }
    else {
        [self writeDebugMsg:nil msg:@"Location manager not enabled."];
        return NO;
    }
}

- (void)stopVisitMonitoring {
    if (self.visitMonitoring){
        [self.locationManager stopMonitoringVisits];
        self.visitMonitoring=NO;
        [self writeDebugMsg:nil msg:@"stopped visit location monitoring"];
    }
}


- (BOOL)ensureMonitoring {

    BOOL isInBackground = [self isInBackground];

    if (self.config.useVisitMonitoring){
        if (!self.visitMonitoring)
            [self startVisitMonitoring:FALSE];
    }

    BOOL result = NO;
    if (isInBackground){
        if (self.config.useSignficantLocation) {
            if (!self.backgroundMonitoring)
                result =[self startBackgroundMonitoring:FALSE];
        }
    }
    else {
        if (self.config.useForegroundMonitoring) {
            if (!self.foregroundMonitoring)
                result = [self startForegroundMonitoring:FALSE];
        }
    }
    return result;

}

- (void)ensureNotMonitoring {
    [self stopVisitMonitoring];
    [self stopBackgroungMonitoring];
    [self stopForegroundMonitoring];
}

- (BOOL) isInBackground {
    UIApplication * app = [UIApplication sharedApplication];
    return (app.applicationState == UIApplicationStateBackground);
}

/*
 This method creates a new region based on the center coordinate of the map view.
 A new annotation is created to represent the region and then the application starts monitoring the new region.
 */
- (BOOL) addRegion: (CLLocationCoordinate2D) coord andRadius: (double) radius  andIdentifier: (nullable NSString *) identifier
{
    if ([CLLocationManager isMonitoringAvailableForClass:[CLCircularRegion class]]) {
        if (radius < 0){
            radius = self.config.geoRadius;
        }

        if (identifier == nil || [identifier length]<1){
            identifier = [[NSUUID UUID] UUIDString];
        }
        // Create a new region based on the center of the map view.
       CLCircularRegion *newRegion = [[CLCircularRegion alloc] initWithCenter:coord
                                                                        radius:radius
                                                                    identifier:identifier];
        newRegion.notifyOnEntry = YES;
        newRegion.notifyOnExit = YES;

        // Start monitoring the newly created region.
        [self.locationManager startMonitoringForRegion:newRegion];
        return YES;

    }
    else {
        [self writeDebugMsg:nil  msg:@"Region monitoring is not available."];
        return NO;
    }
}

- (void) clearRegions {

    [self.locationManager.monitoredRegions enumerateObjectsUsingBlock:^(__kindof CLRegion * _Nonnull region, BOOL * _Nonnull stop) {
        NSLog(@"Stop monitoring region %@", region.identifier);
        [self.locationManager stopMonitoringForRegion:region];
    }];

}

- (BOOL) removeRegion:(nonnull NSString *)identifier
 {
    CLCircularRegion *region = [self _getMonitoredRegionWithIdentifier:identifier];

    if (region != nil) {
        NSLog(@"Stop monitoring region %@", region.identifier);
        [self.locationManager stopMonitoringForRegion:region];
        return YES;

    }
    return NO;
}

- (CLCircularRegion *)_getMonitoredRegionWithIdentifier:(nonnull NSString *)identifier {
    NSSet *regions = [self.locationManager.monitoredRegions objectsPassingTest:^BOOL(CLCircularRegion *region, BOOL *stop) {
        return ([region.identifier isEqualToString:identifier]);
    }];
    return [regions anyObject];
}

#pragma mark - location services delegates

- (void)locationManager:(CLLocationManager *)manager didUpdateLocations:(NSArray *)locations
{
    // if we are after a location fix only, just monitor till we get a fix thats less than 5 min old.
    if (self.locationFixOnly && self.foregroundMonitoring){
        CLLocation * newLocation = [locations lastObject];
        [self writeDebugInfo: manager newLocation:newLocation];

        NSTimeInterval secs = -[newLocation.timestamp timeIntervalSinceNow];
        if (secs <= self.config.staleLocationThreshold){
            NSLog(@"Got a relevant location fix %f seconds old", secs);
            [self sendLocationUpdate:locations];
            if (self.foregroundMonitoring){
                [self stopForegroundMonitoring];
            }
        }
        return;
    }

    // write all locations to the server
    for (CLLocation *newLocation in locations){
        [self writeDebugInfo: manager newLocation:newLocation];
    }
    [self sendLocationUpdate: locations];
}

- (void)locationManager:(CLLocationManager *)manager didVisit:(CLVisit *)visit
{
    [self writeDebugInfo:manager visit:visit];
    [self sendVisitUpdate:visit];

}

- (void)locationManager:(CLLocationManager *)manager didFailWithError:(NSError *)error
{
    [self writeDebugMsg:manager msg:[error description]];
    if (error.code == kCLErrorDenied){
        if (self.backgroundMonitoring){
            [self stopBackgroungMonitoring];
        }
        else if (self.foregroundMonitoring) {
            [self stopForegroundMonitoring];
        }
        if (self.visitMonitoring){
            [self stopVisitMonitoring];
        }
    }
    [self sendError:manager withError:error withMsg:@"Location Manager failure" isGeo:NO];
}

// The device entered a monitored region.
- (void)locationManager:(CLLocationManager *)manager didEnterRegion:(CLRegion *)region  {
    [self writeDebugMsg:manager msg: [NSString stringWithFormat:@"didEnterRegion %@ at %@", region.identifier, [NSDate date]]];
    CLLocation * loc = manager.location;
    [self sendGeoRegionUpdate:region andLocation:loc andState:CLRegionStateInside];
}

// The device exited a monitored region.
- (void)locationManager:(CLLocationManager *)manager didExitRegion:(CLRegion *)region {
    [self writeDebugMsg:manager msg: [NSString stringWithFormat:@"didExitRegion %@ at %@", region.identifier, [NSDate date]]];
    CLLocation * loc = manager.location;
    [self sendGeoRegionUpdate:region andLocation:loc andState:CLRegionStateOutside];
}

// A monitoring error occurred for a region.
- (void)locationManager:(CLLocationManager *)manager monitoringDidFailForRegion:(CLRegion *)region withError:(NSError *)error {
    [self writeDebugMsg:manager msg: [NSString stringWithFormat:@"monitoringDidFailForRegion %@: %@", region.identifier, error]];
    [self sendError:manager withError:error withMsg:@"Geo Monitor failure" isGeo:YES];
}

- (void)locationManagerDidPauseLocationUpdates:(CLLocationManager *)manager
{
   [self writeDebugMsg:manager msg:@"Paused Updates"];
}

- (void)locationManagerDidResumeLocationUpdates:(CLLocationManager *)manager
{
    [self writeDebugMsg:manager msg:@"Resumed Updates"];
}

- (void)locationManager:(CLLocationManager *)manager didChangeAuthorizationStatus:(CLAuthorizationStatus)status
{
    // note: this is only set to background when we really are in background.  Not while we are transitioning.
    [self writeDebugMsg:manager msg:[NSString stringWithFormat:@"Auth didChange: %d %@ BMon: %d FMon: %d old Status: %@", status, [self statusName: status], self.backgroundMonitoring, self.foregroundMonitoring, [self statusName:self.authStatus]]];

    self.authStatus = status;

    if (status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways) {
        // Start the standard location service.
        [self ensureMonitoring];
    }

    if (self.delegate)
        [self.delegate sendChangeAuthorizationStatus: status];
}

#pragma mark - device server update

-(void)sendLocationUpdate: (nonnull NSArray *)locations {
    if ([locations count]<1)
        return;

    BOOL isInBackground =[self isInBackground];
    NSMutableArray * newLocs = [[NSMutableArray alloc] initWithCapacity:[locations count]];
    for(CLLocation * location in locations){
        LocationInfo *locInfo = [[LocationInfo alloc] initWithLocation:location andBackgroundMode:isInBackground];
        [newLocs addObject:locInfo];
    }
    self.lastLocation = newLocs.lastObject;

    if (self.delegate)
        [self.delegate sendDeviceUpdate:newLocs  inBackground:isInBackground];
}

- (void)sendVisitUpdate:(nonnull CLVisit *)visit {
    BOOL isInBackground = [self isInBackground];

    LocationInfo *locInfo = [[LocationInfo alloc] initWithVisit:visit andBackgroundMode:isInBackground];
    self.lastLocation = locInfo;

    if (self.delegate)
        [self.delegate sendDeviceUpdate:@[locInfo] inBackground:isInBackground];
}

- (void)sendGeoRegionUpdate:(nonnull CLRegion *)region andLocation:(CLLocation *)location andState:(CLRegionState)state {
    BOOL isInBackground = [self isInBackground];

    LocationInfo *locInfo = [[LocationInfo alloc] initWithRegion:region andLocation: location andState:state andBackgroundMode:isInBackground];
    self.lastLocation = locInfo;

    if (self.delegate)
        [self.delegate sendRegionUpdate:region withLocation:locInfo inBackground:isInBackground];
}


- (void)sendError:(CLLocationManager *)manager withError:(NSError *)error withMsg:(NSString *)msg isGeo:(BOOL)geoError {

    if (self.delegate)
        [self.delegate sendError:error withMsg:(NSString *) msg inGeo:geoError];
}

-(NSString *) statusName: (CLAuthorizationStatus) status
{
    if (status==kCLAuthorizationStatusNotDetermined) {
         return @"Not Determined";
    }

    if (status==kCLAuthorizationStatusDenied) {
        return @"Denied";
    }

    if (status==kCLAuthorizationStatusRestricted) {
        return @"Restricted";
    }

    if (status==kCLAuthorizationStatusAuthorizedAlways) {
       return @"Always Allowed";
    }

    if (status==kCLAuthorizationStatusAuthorizedWhenInUse) {
        return @"When In Use Allowed";
    }
    return @"Unknown";

}

- (NSString  *) getErrorMsg
{
    return (self.errorMsg != nil) ? self.errorMsg : @"Unkwnown error";
};

- (void) clearErrorMsg
{
    self.errorMsg = nil;
}

#pragma mark - debugging code

// -------------------------------------------------------
// debugging code
// -------------------------------------------------------


-(void) writeDebugInfo:(CLLocationManager *)manager newLocation:(CLLocation *)newLocation;

{
    NSLog(@"Location: %@", [newLocation description]);
    if (!self.config.logLocInfo)
        return;

    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString * key  =@"_DEBUG_INFO";

    NSString * mode = (self.backgroundMonitoring) ? @"SigLoc":@"Normal";

    NSNumber * _longitude = @(newLocation.coordinate.longitude);
    NSNumber *_latitude = @(newLocation.coordinate.latitude);
    NSDate * _fix_timestamp = newLocation.timestamp;
    NSDate * _now = [NSDate date];

    NSNumber * _isInBackground = @([UIApplication sharedApplication].applicationState == UIApplicationStateBackground);

    NSDictionary * data = @{
            @"mode": mode,
            @"longitude": _longitude ,
            @"latitude": _latitude ,
            @"fix_timestamp": _fix_timestamp,
            @"now_timestamp":_now,
            @"background":_isInBackground
    };

    NSArray * debugData = [defaults arrayForKey:key];
    NSMutableArray * newData = (debugData != nil) ?[[NSMutableArray alloc] initWithArray:debugData] : [[NSMutableArray alloc] initWithCapacity:1];
    [newData addObject:data];

    [defaults setObject:newData forKey:key];
    [defaults synchronize];

}

- (void)writeDebugInfo:(nullable CLLocationManager *)manager visit:(nullable CLVisit *)visit {
    NSLog(@"Visit: %@", [visit description]);
    if (!self.config.logLocInfo)
        return;

    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString * key  =@"_DEBUG_INFO";

    NSString * mode = (self.backgroundMonitoring) ? @"SigLoc":@"Normal";

    NSNumber * _longitude = @(visit.coordinate.longitude);
    NSNumber *_latitude = @(visit.coordinate.latitude);
    NSDate * _arrival = visit.arrivalDate;
    NSDate * _departure = visit.departureDate;
    CLLocationAccuracy _horizontalAccuracy = visit.horizontalAccuracy;
    NSDate * _now = [NSDate date];

    NSNumber * _isInBackground = @([UIApplication sharedApplication].applicationState == UIApplicationStateBackground);

    NSDictionary * data = @{
            @"mode": mode,
            @"longitude": _longitude ,
            @"latitude": _latitude ,
            @"arrival": (_arrival) ? _arrival: [NSNull null],
            @"departure": (_departure) ? _departure : [NSNull null],
            @"accuracy": @(_horizontalAccuracy),
            @"now_timestamp":_now,
            @"background":_isInBackground
    };

    NSArray * debugData = [defaults arrayForKey:key];
    NSMutableArray * newData = (debugData != nil) ?[[NSMutableArray alloc] initWithArray:debugData] : [[NSMutableArray alloc] initWithCapacity:1];
    [newData addObject:data];

    [defaults setObject:newData forKey:key];
    [defaults synchronize];
}

-(void) writeDebugMsg:(nullable CLLocationManager *)manager msg: (nullable NSString*) msg;

{
    self.errorMsg = msg;
    NSLog(@"Msg: %@", msg);
    if (!self.config.logLocInfo)
        return;

    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    NSString * key  =@"_DEBUG_INFO";

    NSString * mode = (self.backgroundMonitoring) ? @"SigLoc":@"Normal";
    NSDate * _now = [NSDate date];

    NSNumber * _isInBackground = [NSNumber numberWithBool:([UIApplication sharedApplication].applicationState == UIApplicationStateBackground)];

    NSDictionary * data = @{
                            @"mode": mode,
                            @"now_timestamp":_now,
                            @"background":_isInBackground,
                            @"msg":msg
                            };

    NSArray * debugData = [defaults arrayForKey:key];
    NSMutableArray * newData = (debugData != nil) ?[[NSMutableArray alloc] initWithArray:debugData] : [[NSMutableArray alloc] initWithCapacity:1];
    [newData addObject:data];

    [defaults setObject:newData forKey:key];
    [defaults synchronize];

}

- (NSArray *) readDebugInfo
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    return [defaults arrayForKey:@"_DEBUG_INFO"];
}

- (void) clearDebugInfo
{
    NSUserDefaults *defaults = [NSUserDefaults standardUserDefaults];
    [defaults removeObjectForKey:@"_DEBUG_INFO"];
    [defaults synchronize];
}


@end
