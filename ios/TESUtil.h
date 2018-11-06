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

#ifndef TESUtil_h
#define TESUtil_h

#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#import <CoreLocation/CoreLocation.h>

#define SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(v)  ([[[UIDevice currentDevice] systemVersion] compare:v options:NSNumericSearch] != NSOrderedAscending)

static BOOL atLeastIOS(NSString * _Nonnull ver) {
    return SYSTEM_VERSION_GREATER_THAN_OR_EQUAL_TO(ver);
}

@interface TESUtil : NSObject
///-----------------------
/// @name utility methods
///----------------------

/**
 converts date to string using ISO date format
 **/
+ (nullable NSDate *) dateFromString: (nullable NSString *) dateString;

/**
 converts string to date using ISO date format
 **/

+ (nullable NSString *) stringFromDate: (nullable NSDate *) date;

/**
 creates a date from components - use -1 for nil in NSInteger fields
 **/
+ (nullable NSDate *) dateFromComponents: (NSInteger) year
        month: (NSInteger) month
        day: (NSInteger) day
        hour: (NSInteger) hour
        minute: (NSInteger) minute
        second: (NSInteger) second
        timezone: (nullable NSTimeZone *) timezone;

///----------------
/// @name Functions
///----------------

/**
 convert a ISO8601 formatted string to a date
 */
extern NSDate * _Nullable TESDateFromISO8601String(NSString  * _Nullable   ISO8601String);

@end;

#endif /* TESUtil_h */
