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

#import "LocationInfo.h"
#import "TESUtil.h"

@implementation LocationInfo {
}

-(nullable instancetype) initWithLocation: (nonnull CLLocation *) loc andBackgroundMode: (BOOL) inBackground
{
    self = [super init];
    if (self){
        _longitude = @(loc.coordinate.longitude);
        _latitude = @(loc.coordinate.latitude);
        _accuracy = @(loc.horizontalAccuracy);
        _speed = @(loc.speed);
        _course = @(loc.course);
        _altitude = @(loc.altitude);
        _fix_timestamp = loc.timestamp;
        _inBackground = @(inBackground);
        _arrival = nil;
        _departure = nil;
        _didEnter = @(NO);
        _didExit = @(NO);
        _regionIdentifier = nil;


        return self;
    }
    return nil;
}

- (nullable instancetype)initWithVisit:(nonnull CLVisit *)visit andBackgroundMode:(BOOL)inBackground {
    self = [super init];
    if (self){
        _longitude = @(visit.coordinate.longitude);
        _latitude = @(visit.coordinate.latitude);
        _accuracy = @(visit.horizontalAccuracy);
        _speed = @(-1);
        _course = @(-1);
        _altitude = @(-1);
        _arrival = visit.arrivalDate;
        _departure = visit.departureDate;
        _fix_timestamp =  [NSDate date];
        _inBackground = @(inBackground);
        _didEnter = @(visit.arrivalDate != nil);
        _didExit = @(visit.departureDate != nil);
        _regionIdentifier = nil;

        return self;
    }
    return nil;
}

- (nullable instancetype)initWithRegion:(nonnull CLRegion *)region andLocation:(nullable CLLocation *)location andState:(CLRegionState)state andBackgroundMode:(BOOL)mode {
    self = [super init];
    if (self){
        NSNumberFormatter * fmt = [[NSNumberFormatter alloc] init];
        [fmt setNumberStyle:NSNumberFormatterDecimalStyle];

        _longitude = (location != nil) ? @(location.coordinate.longitude) : @(-1);
        _latitude = (location != nil) ? @(location.coordinate.latitude) : @(-1);
        _accuracy = (location != nil) ? @(location.horizontalAccuracy): @(-1);
        _speed = (location != nil) ? @(location.speed): @(-1);
        _course = (location != nil) ?  @(location.course): @(-1);
        _fix_timestamp = (location != nil) ? location.timestamp: nil;
        _altitude = (location != nil) ? @(location.altitude): nil;

        _arrival = nil;
        _departure = nil;
        _inBackground = @(mode);
        _didEnter = @(state == CLRegionStateInside);
        _didExit = @(state == CLRegionStateOutside);
        _regionIdentifier = region.identifier;

        return self;
    }
    return nil;
}

- (nullable instancetype)initWithAttributes:(nonnull NSDictionary *)attributes
{
    self = [super init];
    if (self){
        NSNumberFormatter * fmt = [[NSNumberFormatter alloc] init];
        [fmt setNumberStyle:NSNumberFormatterDecimalStyle];

        _longitude = [fmt numberFromString: [attributes valueForKey:@"longitude"] ];
        _latitude = [fmt numberFromString: [attributes valueForKey:@"latitude"] ];
        _accuracy = [fmt numberFromString: [attributes valueForKey:@"accuracy"] ];
        _speed = [fmt numberFromString: [attributes valueForKey:@"speed"] ];
        _course = [fmt numberFromString: [attributes valueForKey:@"course"] ];
        _altitude = [fmt numberFromString: [attributes valueForKey:@"altitude"] ];
        _fix_timestamp = [TESUtil dateFromString:[attributes valueForKey:@"fix_timestamp"]];
        _arrival = ([attributes valueForKey:@"arrival"] != [NSNull null]) ? [TESUtil dateFromString:[attributes valueForKey:@"arrival"]]:nil;
        _departure = ([attributes valueForKey:@"departure"] != [NSNull null]) ? [TESUtil dateFromString:[attributes valueForKey:@"departure"]]:nil;
        _inBackground = [attributes valueForKey:@"in_background"];
        _didEnter = [attributes valueForKey:@"did_enter"];
        _didExit = [attributes valueForKey:@"did_exit"];
        _regionIdentifier = [attributes valueForKey:@"region_identifier"];

        return self;
    }
    return nil;
}

+ (nonnull LocationInfo *) CreateEmptyLocation; {
    LocationInfo * loc = [[LocationInfo alloc] initWithAttributes:@{
            @"longitude":@"0",
            @"latitude": @"0",
            @"accuracy": @"-1",
            @"speed": @"-1",
            @"course": @"-1",
            @"altitude": @"-1",
            @"fix_timestamp": [TESUtil stringFromDate:[NSDate date]],
            @"arrival":  [NSNull null],
            @"departure": [NSNull null],
            @"in_background": @NO,
            @"did_enter": @NO,
            @"did_exit": @NO,
            @"region_identifiers": @[[NSNull null]]
    }];
    return loc;
}

- (nonnull NSMutableDictionary *) toDictionary
{
    NSMutableDictionary *attributes = [[NSMutableDictionary alloc] initWithDictionary:@{
            @"longitude": _longitude,
            @"latitude": _latitude,
            @"accuracy": _accuracy,
            @"speed": _speed,
            @"course": _course,
            @"altitude": _altitude,
            @"fix_timestamp": [TESUtil stringFromDate:_fix_timestamp],
            @"arrival": (_arrival != nil) ? [TESUtil stringFromDate:_arrival] : [NSNull null],
            @"departure": (_departure != nil) ? [TESUtil stringFromDate:_departure] : [NSNull null],
            @"in_background": _inBackground
    }];
    if (_regionIdentifier != nil){
        attributes[@"did_enter"] = _didEnter;
        attributes[@"did_exit"] = _didExit;
        attributes[@"region_identifier"] = @[_regionIdentifier];
    }
    return attributes;

}

@end
