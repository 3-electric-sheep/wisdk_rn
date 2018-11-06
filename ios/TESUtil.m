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

#import "TESUtil.h"
#include <xlocale.h>

@implementation TESUtil {

}

+ (NSDate *) dateFromString: (NSString *) dateString
{
if (dateString == nil)
return nil;

NSDate * result = TESDateFromISO8601String(dateString);
return result;
}

+ (NSString *) stringFromDate: (NSDate *) date
{
if (date == nil)
return nil;

NSTimeZone *timeZone = [NSTimeZone timeZoneWithName:@"UTC"];

NSDateFormatter * dateFormatter = [[NSDateFormatter alloc] init];
[dateFormatter setTimeZone:timeZone];
[dateFormatter setDateFormat:@"yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'"];

NSString * str = [dateFormatter stringFromDate:date];
return str;
}

+ (NSDate *) dateFromComponents: (NSInteger) year
        month: (NSInteger) month
        day: (NSInteger) day
        hour: (NSInteger) hour
        minute: (NSInteger) minute
        second: (NSInteger) second
        timezone: (NSTimeZone *) timezone
{
NSCalendar *gregorian = [[NSCalendar alloc] initWithCalendarIdentifier:NSCalendarIdentifierGregorian];
NSDateComponents *comps = [[NSDateComponents alloc] init];

if (year >= 0) [comps setYear:year];
if (month >= 0) [comps setMonth:month];
if (day >= 0) [comps setDay:day];
if (hour >= 0) [comps setHour:hour];
if (minute >= 0) [comps setMinute:minute];
if (second >= 0) [comps setSecond:second];
if (timezone) [comps setTimeZone:timezone];

return [gregorian dateFromComponents:comps];
}

#define AF_ISO8601_MAX_LENGTH 25

// Adopted from SSToolkit NSDate+SSToolkitAdditions
// Created by Sam Soffes
// Copyright (c) 2008-2012 Sam Soffes
// https://github.com/soffes/sstoolkit/

NSDate * TESDateFromISO8601String(NSString *ISO8601String) {
    if (!ISO8601String) {
        return nil;
    }

    const char *str = [ISO8601String cStringUsingEncoding:NSUTF8StringEncoding];
    char newStr[AF_ISO8601_MAX_LENGTH];
    bzero(newStr, AF_ISO8601_MAX_LENGTH);

    size_t len = strlen(str);
    if (len == 0) {
        return nil;
    }

    // UTC dates ending with Z
    if (len == 20 && str[len - 1] == 'Z') {
        memcpy(newStr, str, len - 1);
        strncpy(newStr + len - 1, "+0000\0", 6);
    }

        // Timezone includes a semicolon (not supported by strptime)
    else if (len == 25 && str[22] == ':') {
        memcpy(newStr, str, 22);
        memcpy(newStr + 22, str + 23, 2);
    }

        // Fallback: date was already well-formatted OR any other case (bad-formatted)
    else {
        memcpy(newStr, str, len > AF_ISO8601_MAX_LENGTH - 1 ? AF_ISO8601_MAX_LENGTH - 1 : len);
    }

    // Add null terminator
    newStr[sizeof(newStr) - 1] = 0;

    struct tm tm = {
            .tm_sec = 0,
            .tm_min = 0,
            .tm_hour = 0,
            .tm_mday = 0,
            .tm_mon = 0,
            .tm_year = 0,
            .tm_wday = 0,
            .tm_yday = 0,
            .tm_isdst = -1,
    };

    strptime_l(newStr, "%FT%T%z", &tm, NULL);

    return [NSDate dateWithTimeIntervalSince1970:mktime(&tm)];
}

@end;
