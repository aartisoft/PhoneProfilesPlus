package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
public class GeofenceScanWorker extends Worker {

    private final Context context;

    static final String WORK_TAG  = "GeofenceScannerJob";
    static final String WORK_TAG_SHORT  = "GeofenceScannerJobShort";

    public GeofenceScanWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            //PPApplication.logE("GeofenceScanWorker.doWork", "---------------------------------------- START");

            //CallsCounter.logCounter(context, "GeofenceScanWorker.doWork", "GeofenceScanWorker_doWork");

            if (!PPApplication.getApplicationStarted(true))
                // application is not started
                return Result.success();

            if (Event.isEventPreferenceAllowed(EventPreferencesLocation.PREF_EVENT_LOCATION_ENABLED, context).allowed !=
                    PreferenceAllowed.PREFERENCE_ALLOWED) {
                cancelWork(false/*, null*/);
                /*if (PPApplication.logEnabled()) {
                    PPApplication.logE("GeofenceScanWorker.doWork", "return - not allowed geofence scanning");
                    PPApplication.logE("GeofenceScanWorker.doWork", "---------------------------------------- END");
                }*/
                return Result.success();
            }

            //boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
            boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
            if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode.equals("2")) {
                //PPApplication.logE("GeofenceScanWorker.doWork", "update in power save mode is not allowed");
                cancelWork(false/*, null*/);
                //PPApplication.logE("GeofenceScanWorker.doWork", "---------------------------------------- END");
                return Result.success();
            }

            if (Event.getGlobalEventsRunning()) {
                boolean geofenceScannerUpdatesStarted = false;
                synchronized (PPApplication.geofenceScannerMutex) {
                    if ((PhoneProfilesService.getInstance() != null) && (PhoneProfilesService.getInstance().getGeofencesScanner() != null)) {
                        GeofencesScanner scanner = PhoneProfilesService.getInstance().getGeofencesScanner();
                        if (scanner.mUpdatesStarted) {
                            //PPApplication.logE("GeofenceScanWorker.doWork", "location updates started - save to DB");

                            //if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.getInstance().isGeofenceScannerStarted())
                            scanner.updateGeofencesInDB();

                            geofenceScannerUpdatesStarted = true;
                        }
                    }
                }

                if (geofenceScannerUpdatesStarted) {
                    //PPApplication.logE("GeofenceScanWorker.doWork", "location updates started - start EventsHandler");

                    // start events handler
                    //PPApplication.logE("****** EventsHandler.handleEvents", "START run - from=GeofenceScanWorker.doWork");

                    EventsHandler eventsHandler = new EventsHandler(context);
                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_GEOFENCES_SCANNER);

                    //PPApplication.logE("****** EventsHandler.handleEvents", "END run - from=GeofenceScanWorker.doWork");
                }
            }

            //PPApplication.logE("GeofenceScanWorker.doWork - handler", "schedule work");
            scheduleWork(context.getApplicationContext(), false);

            /*PPApplication.startHandlerThreadPPScanners();
            final Handler handler = new Handler(PPApplication.handlerThreadPPScanners.getLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    PPApplication.logE("GeofenceScanWorker.doWork - handler", "schedule work");
                    scheduleWork(context, false, null, false);
                }
            }, 500);*/

            //PPApplication.logE("GeofenceScanWorker.doWork", "---------------------------------------- END");
            return Result.success();
        } catch (Exception e) {
            //Log.e("GeofenceScanWorker.doWork", Log.getStackTraceString(e));
            PPApplication.recordException(e);
            /*Handler _handler = new Handler(getApplicationContext().getMainLooper());
            Runnable r = new Runnable() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            };
            _handler.postDelayed(r, 1000);*/
            return Result.failure();
        }
    }

    public void onStopped () {
        //PPApplication.logE("GeofenceScanWorker.onStopped", "xxx");

        //CallsCounter.logCounter(context, "GeofenceScanWorker.onStopped", "GeofenceScanWorker_onStopped");
    }

    private static void _scheduleWork(final Context context, boolean shortInterval/*, final boolean forScreenOn*/) {
        try {
            if (PPApplication.getApplicationStarted(true)) {
                WorkManager workManager = PPApplication.getWorkManagerInstance();
                if (workManager != null) {

                    //PPApplication.logE("GeofenceScanWorker._scheduleWork", "---------------------------------------- START");

                    int interval;
                    synchronized (PPApplication.geofenceScannerMutex) {
                        /*if (PPApplication.logEnabled()) {
                            if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.getInstance().isGeofenceScannerStarted())
                                PPApplication.logE("GeofenceScanWorker._scheduleWork", "mUpdatesStarted=" + PhoneProfilesService.getInstance().getGeofencesScanner().mUpdatesStarted);
                            else {
                                PPApplication.logE("GeofenceScanWorker._scheduleWork", "scanner is not started");
                                PPApplication.logE("GeofenceScanWorker._scheduleWork", "PhoneProfilesService.getInstance()=" + PhoneProfilesService.getInstance());
                            }
                        }*/

                        // look at GeofenceScanner:UPDATE_INTERVAL_IN_MILLISECONDS
                        //int updateDuration = 30;

                        if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.getInstance().isGeofenceScannerStarted() &&
                                PhoneProfilesService.getInstance().getGeofencesScanner().mUpdatesStarted) {
                            interval = ApplicationPreferences.applicationEventLocationUpdateInterval * 60;
                            //boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
                            boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
                            if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode.equals("1"))
                                interval = 2 * interval;
                            //interval = interval - updateDuration;
                        } else {
                            interval = 5;
                            shortInterval = true;
                        }

                        //PPApplication.logE("GeofenceScanWorker._scheduleWork", "interval=" + interval);
                    }

                    if (!shortInterval) {
                        //PPApplication.logE("GeofenceScanWorker._scheduleWork", "delay work");
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(GeofenceScanWorker.class)
                                .setInitialDelay(interval, TimeUnit.SECONDS)
                                .addTag(GeofenceScanWorker.WORK_TAG)
                                .build();
                        workManager.enqueueUniqueWork(GeofenceScanWorker.WORK_TAG, ExistingWorkPolicy.KEEP, workRequest);
                    } else {
                        //PPApplication.logE("GeofenceScanWorker._scheduleWork", "start now work");
                        //waitForFinish();
                        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(GeofenceScanWorker.class)
                                .addTag(GeofenceScanWorker.WORK_TAG_SHORT)
                                .build();
                        workManager.enqueueUniqueWork(GeofenceScanWorker.WORK_TAG_SHORT, ExistingWorkPolicy.KEEP, workRequest);
                    }

                    //PPApplication.logE("GeofenceScanWorker._scheduleWork", "---------------------------------------- END");
                }
            }
        } catch (Exception e) {
            //Log.e("GeofenceScanWorker._scheduleWork", Log.getStackTraceString(e));
            PPApplication.recordException(e);
        }
    }

    static void scheduleWork(final Context context, /*final boolean useHandler,*/
                            final boolean shortInterval/*, final boolean forScreenOn*/) {
        //PPApplication.logE("GeofenceScanWorker.scheduleWork", "startScanning="+startScanning);

        //if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.isGeofenceScannerStarted()) {
        //if (useHandler/* && (_handler == null)*/) {
            PPApplication.startHandlerThreadPPScanners();
            final Handler handler = new Handler(PPApplication.handlerThreadPPScanners.getLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    _scheduleWork(context, shortInterval/*, forScreenOn*/);
                }
            }, 500);
        //}
        //else {
        //    _scheduleWork(context, startScanning/*, forScreenOn*/);
        //}
        //}
        //else
        //    PPApplication.logE("GeofenceScanWorker.scheduleWork", "scanner is not started");
    }

    private static void _cancelWork() {
        if (isWorkScheduled(false) || isWorkScheduled(true)) {
            try {
                waitForFinish(false);
                waitForFinish(true);

                PhoneProfilesService.cancelWork(WORK_TAG);
                PhoneProfilesService.cancelWork(WORK_TAG_SHORT);

                //PPApplication.logE("GeofenceScanWorker._cancelWork", "CANCELED");

            } catch (Exception e) {
                //Log.e("GeofenceScanWorker._cancelWork", Log.getStackTraceString(e));
                PPApplication.recordException(e);
            }
        }
    }

    private static void waitForFinish(boolean shortWork) {
        if (!isWorkRunning(shortWork)) {
            //PPApplication.logE("GeofenceScanWorker.waitForFinish", "NOT RUNNING");
            return;
        }

        try {
            if (PPApplication.getApplicationStarted(true)) {
                WorkManager workManager = PPApplication.getWorkManagerInstance();
                if (workManager != null) {

                    //PPApplication.logE("GeofenceScanWorker.waitForFinish", "START WAIT FOR FINISH");
                    long start = SystemClock.uptimeMillis();
                    do {

                        ListenableFuture<List<WorkInfo>> statuses;
                        if (shortWork)
                            statuses = workManager.getWorkInfosByTag(WORK_TAG_SHORT);
                        else
                            statuses = workManager.getWorkInfosByTag(WORK_TAG);
                        boolean allFinished = true;
                        //noinspection TryWithIdenticalCatches
                        try {
                            List<WorkInfo> workInfoList = statuses.get();
                            for (WorkInfo workInfo : workInfoList) {
                                WorkInfo.State state = workInfo.getState();
                                if (!state.isFinished()) {
                                    allFinished = false;
                                    break;
                                }
                            }
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (allFinished) {
                            //PPApplication.logE("GeofenceScanWorker.waitForFinish", "FINISHED");
                            break;
                        }

                        //try { Thread.sleep(100); } catch (InterruptedException e) { }
                        SystemClock.sleep(100);
                    } while (SystemClock.uptimeMillis() - start < 10 * 1000);

                    //PPApplication.logE("GeofenceScanWorker.waitForFinish", "END WAIT FOR FINISH");
                }
            }
        } catch (Exception e) {
            //Log.e("GeofenceScanWorker.waitForFinish", Log.getStackTraceString(e));
            PPApplication.recordException(e);
        }
    }

    static void cancelWork(final boolean useHandler/*, final Handler _handler*/) {
        //PPApplication.logE("GeofenceScanWorker.cancelWork", "xxx");

        if (useHandler /*&& (_handler == null)*/) {
            PPApplication.startHandlerThreadPPScanners();
            final Handler handler = new Handler(PPApplication.handlerThreadPPScanners.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    _cancelWork();
                }
            });
        }
        else {
            _cancelWork();
        }
    }

    private static boolean isWorkRunning(boolean shortWork) {
        try {
            if (PPApplication.getApplicationStarted(true)) {
                WorkManager workManager = PPApplication.getWorkManagerInstance();
                if (workManager != null) {
                    ListenableFuture<List<WorkInfo>> statuses;
                    if (shortWork)
                        statuses = workManager.getWorkInfosByTag(WORK_TAG_SHORT);
                    else
                        statuses = workManager.getWorkInfosByTag(WORK_TAG);
                    //noinspection TryWithIdenticalCatches
                    try {
                        List<WorkInfo> workInfoList = statuses.get();
                        //PPApplication.logE("GeofenceScanWorker.isWorkScheduled", "workInfoList.size()="+workInfoList.size());
                        //return workInfoList.size() != 0;
                        boolean running = false;
                        for (WorkInfo workInfo : workInfoList) {
                            WorkInfo.State state = workInfo.getState();
                            running = state == WorkInfo.State.RUNNING;
                        }
                        return running;
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        return false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                else
                    return false;
            }
            else
                return false;
        } catch (Exception e) {
            //Log.e("GeofenceScanWorker.isWorkRunning", Log.getStackTraceString(e));
            PPApplication.recordException(e);
            return false;
        }
    }

    static boolean isWorkScheduled(boolean shortWork) {
        //PPApplication.logE("GeofenceScanWorker.isWorkScheduled", "xxx");
        try {
            if (PPApplication.getApplicationStarted(true)) {
                WorkManager workManager = PPApplication.getWorkManagerInstance();
                if (workManager != null) {
                    ListenableFuture<List<WorkInfo>> statuses;
                    if (shortWork)
                        statuses = workManager.getWorkInfosByTag(WORK_TAG_SHORT);
                    else
                        statuses = workManager.getWorkInfosByTag(WORK_TAG);
                    //noinspection TryWithIdenticalCatches
                    try {
                        List<WorkInfo> workInfoList = statuses.get();
                        //PPApplication.logE("GeofenceScanWorker.isWorkScheduled", "workInfoList.size()="+workInfoList.size());
                        //return workInfoList.size() != 0;
                        boolean running = false;
                        for (WorkInfo workInfo : workInfoList) {
                            WorkInfo.State state = workInfo.getState();
                            running = (state == WorkInfo.State.RUNNING) || (state == WorkInfo.State.ENQUEUED);
                        }
                        return running;
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        return false;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                else
                    return false;
            }
            else
                return false;
        } catch (Exception e) {
            //Log.e("GeofenceScanWorker.isWorkScheduled", Log.getStackTraceString(e));
            PPApplication.recordException(e);
            return false;
        }
    }

}
