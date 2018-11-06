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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONException;

import java.util.ArrayList;


/**
 * On Android geofences are cleared after a device restart, so we tell the sdk we have been rebooted
 */
public class RNWiBootReceiver extends BroadcastReceiver
{
	private static final String TAG = "RNWiBootReceiver";
	@Override
	public void onReceive(Context context, Intent intent)
	{
		Log.e(TAG, "In boot receiver");
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			TesJobDispatcher jm = new TesJobDispatcher(context);
			TesConfig cfg = new TesConfig();
			try {
				cfg = TesConfig.getSavedConfig(context);
			} catch (JSONException e) {
				Log.e(TAG, "Failed to get saved configs - using defaults. invalid JSON found");
				e.printStackTrace();
			}

			jm.scheduleJob(RNWiBootService.class,
					cfg.delay,
					cfg.deadline,
					cfg.networkType,
					cfg.requireIdle,
					cfg.requireCharging, null);
		}

	}
}
