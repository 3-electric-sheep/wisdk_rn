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
import android.os.PowerManager;
import android.support.annotation.NonNull;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;
import com.facebook.react.jstasks.HeadlessJsTaskContext;
import com.facebook.react.jstasks.HeadlessJsTaskEventListener;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.annotation.Nullable;
import android.support.annotation.NonNull;

import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;


/**
 * Base class for running JS without a UI. Generally, you only need to override
 * {@link #getTaskConfig}, which is called for every {@link #onStartCommand}. The
 * result, if not {@code null}, is used to run a JS task.
 *
 * If you need more fine-grained control over how tasks are run, you can override
 * {@link #onStartCommand} and call {@link #startTask} depending on your custom logic.
 *
 * If you're starting a {@code HeadlessJsTaskService} from a {@code BroadcastReceiver} (e.g.
 * handling push notifications), make sure to call {@link #acquireWakeLockNow} before returning from
 * {@link BroadcastReceiver#onReceive}, to make sure the device doesn't go to sleep before the
 * service is started.
 */
public abstract class HeadlessJsJobService extends Worker implements HeadlessJsTaskEventListener {

    private final Set<Integer> mActiveTasks = new CopyOnWriteArraySet<>();

    public HeadlessJsJobService(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Worker.Result doWork() {
        Data job = this.getInputData();
        final HeadlessJsTaskConfig taskConfig = getTaskConfig(job);
        if (taskConfig != null) {
            UiThreadUtil.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        startTask(taskConfig);
                        finishtask();
                    }
                }
            );
            return Result.SUCCESS;
        }

        return Result.FAILURE;
    }


    /**
     * Called from {@link #onStartCommand} to create a {@link HeadlessJsTaskConfig} for this intent.
     * @param job the {@link JobParameters} received in {@link #onStartCommand}.
     * @return a {@link HeadlessJsTaskConfig} to be used with {@link #startTask}, or
     *         {@code null} to ignore this command.
     */
    protected @Nullable HeadlessJsTaskConfig getTaskConfig(Data job) {
        return null;
    }

    /**
     * Start a task. This method handles starting a new React instance if required.
     *
     * Has to be called on the UI thread.
     *
     * @param taskConfig describes what task to start and the parameters to pass to it
     */
    protected void startTask(final HeadlessJsTaskConfig taskConfig) {
        UiThreadUtil.assertOnUiThread();
        final ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
        ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
        if (reactContext == null) {
            reactInstanceManager
                    .addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        @Override
                        public void onReactContextInitialized(ReactContext reactContext) {
                            invokeStartTask(reactContext, taskConfig);
                            reactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });
            if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
                reactInstanceManager.createReactContextInBackground();
            }
        } else {
            invokeStartTask(reactContext, taskConfig);
        }
    }

    private void invokeStartTask(ReactContext reactContext, final HeadlessJsTaskConfig taskConfig) {
        final HeadlessJsTaskContext headlessJsTaskContext = HeadlessJsTaskContext.getInstance(reactContext);
        headlessJsTaskContext.addTaskEventListener(this);
        int taskId = headlessJsTaskContext.startTask(taskConfig);
        mActiveTasks.add(taskId);
    }


    private void finishtask() {
        if (getReactNativeHost().hasInstance()) {
            ReactInstanceManager reactInstanceManager = getReactNativeHost().getReactInstanceManager();
            ReactContext reactContext = reactInstanceManager.getCurrentReactContext();
            if (reactContext != null) {
                HeadlessJsTaskContext headlessJsTaskContext = HeadlessJsTaskContext.getInstance(reactContext);
                headlessJsTaskContext.removeTaskEventListener(this);
            }
        }
    }

    @Override
    public void onHeadlessJsTaskStart(int taskId) { }

    @Override
    public void onHeadlessJsTaskFinish(int taskId) {
        mActiveTasks.remove(taskId);
    }

    /**
     * Get the {@link ReactNativeHost} used by this app. By default, assumes {@link #getApplication()}
     * is an instance of {@link ReactApplication} and calls
     * {@link ReactApplication#getReactNativeHost()}. Override this method if your application class
     * does not implement {@code ReactApplication} or you simply have a different mechanism for
     * storing a {@code ReactNativeHost}, e.g. as a static field somewhere.
     */
    protected ReactNativeHost getReactNativeHost() {
        return ((ReactApplication) this.getApplicationContext()).getReactNativeHost();
    }
}
