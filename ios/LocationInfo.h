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


@interface LocationInfo : NSObject

@property (nonnull, nonatomic, strong) NSNumber * longitude;
@property (nonnull,nonatomic, strong) NSNumber * latitude;
@property (nullable, nonatomic, strong) NSNumber * accuracy;
@property (nullable, nonatomic, strong) NSNumber * speed;
@property (nullable, nonatomic, strong) NSNumber * course;
@property (nonnull,nonatomic, strong) NSDate * fix_timestamp;
@property (nonnull,nonatomic, strong) NSNumber * inBackground;
@property (nullable, nonatomic, strong) NSDate * arrival;
@property (nullable, nonatomic, strong) NSDate * departure;
@property (nullable, nonatomic, strong) NSNumber * didEnter;
@property (nullable, nonatomic, strong) NSNumber * didExit;
@property (nullable, nonatomic, strong) NSString * regionIdentifier;
@property (nullable, nonatomic, strong) NSNumber * altitude;


- (nullable instancetype) initWithLocation: (nonnull CLLocation *) loc andBackgroundMode: (BOOL) inBackground;
- (nullable instancetype) initWithVisit: (nonnull CLVisit *) visit andBackgroundMode: (BOOL) inBackground;
- (nullable instancetype) initWithAttributes:(nonnull NSDictionary *)attributes;

+ (nonnull LocationInfo *) CreateEmptyLocation;

- (nullable instancetype)initWithRegion:(nonnull CLRegion *)region andLocation:(nullable CLLocation *)location andState:(CLRegionState)state andBackgroundMode:(BOOL)mode;

- (nonnull NSMutableDictionary *) toDictionary;

@end
