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

package com.welcomeinterruption.rnwisdk;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by pfrantz on 30/08/2017.
 * <p>
 * Copyright 3 electric sheep 2012-2017
 */

public class TesLocationInfo {
    public double longitude = 0;
    public double latitude =0;
    public float accuracy = 0;
    public float speed = 0;
    public float course = 0;
    public double altitude =0;
    public @NonNull Date fix_timestamp = null;
    public boolean inBackground = false;
    public @Nullable Date arrival=null;
    public @Nullable Date departure = null;

    static final String TAG = "TesLocationInfo";

    public TesLocationInfo(@NonNull Location loc, boolean inBackground){
        this.longitude = loc.getLongitude();
        this.latitude = loc.getLatitude();
        this.accuracy = loc.getAccuracy();
        this.speed = loc.getSpeed();
        this.course = loc.getBearing();
        this.altitude = loc.getAltitude();
        this.fix_timestamp = new Date(loc.getTime());
        this.inBackground = inBackground;
        this.arrival = null;
        this.departure = null;
    }

    public TesLocationInfo(@NonNull JSONObject attributes) throws JSONException {
        this.longitude = attributes.getDouble("longitude");
        this.latitude = attributes.getDouble("latitude");
        this.accuracy = (float) attributes.optDouble("accuracy", 0);
        this.speed = (float) attributes.optDouble("speed", 0);
        this.course = (float) attributes.optDouble("course", 0);
        this.altitude = attributes.optDouble("altitude", 0);
        try {
            this.fix_timestamp = TesUtils.dateFromString(attributes.optString("fix_timestamp"));
            this.arrival = TesUtils.dateFromString(attributes.optString("arrival"));
            this.departure = TesUtils.dateFromString(attributes.optString("departure"));
        } catch (ParseException e) {
             String err = e.getLocalizedMessage();
             throw new JSONException(String.format("Invalid date found: %s", err));
        }
        this.inBackground = attributes.optBoolean("in_background", false);

    }

    public boolean LocationEquals(TesLocationInfo rhs) {
          return ((this.latitude == rhs.latitude) && (this.longitude == rhs.longitude));
    }

    public @NonNull JSONObject toDictionary() throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("longitude", this.longitude);
        obj.put("latitude", this.latitude);
        obj.put("accuracy", this.accuracy);
        obj.put("speed", this.speed);
        obj.put("course", this.course);
        obj.put("altitude", this.altitude);
        obj.put("fix_timestamp", TesUtils.stringFromDate(this.fix_timestamp));
        obj.put("arrival", TesUtils.stringFromDate(this.arrival));
        obj.put("departure", TesUtils.stringFromDate(this.departure));
        obj.put("in_background", this.inBackground);
        return obj;
    }

    public static @NonNull TesLocationInfo createEmptyLocation()  throws JSONException{
        JSONObject obj = new JSONObject();
        obj.put("longitude", 0.0);
        obj.put("latitude", 0.0);
        obj.put("accuracy", -1.0);
        obj.put("speed", -1.0);
        obj.put("course", -1.0);
        obj.put("altitude", 0.0);
        obj.put("fix_timestamp", TesUtils.stringFromDate(new Date()));
        obj.put("arrival", null);
        obj.put("departure", null);
        obj.put("in_background", false);

        TesLocationInfo loc  = new TesLocationInfo(obj);
        return loc;

    }
}
