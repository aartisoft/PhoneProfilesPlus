package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.os.PowerManager;

public class LocationModeChangedBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        //PPApplication.logE("##### LocationModeChangedBroadcastReceiver.onReceive", "xxx");

        //CallsCounter.logCounter(context, "LocationModeChangedBroadcastReceiver.onReceive", "LocationModeChangedBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(true))
            // application is not started
            return;

        if (Event.getGlobalEventsRunning())
        {
            //PPApplication.logE("@@@ LocationModeChangedBroadcastReceiver.onReceive", "xxx");

            final String action = intent.getAction();
            PPApplication.startHandlerThread(/*"LocationModeChangedBroadcastReceiver.onReceive"*/);
            final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    PowerManager powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    try {
                        if (powerManager != null) {
                            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PPApplication.PACKAGE_NAME + ":LocationModeChangedBroadcastReceiver_onReceive");
                            wakeLock.acquire(10 * 60 * 1000);
                        }

                        if ((action != null) && action.matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
                            //PPApplication.logE("****** EventsHandler.handleEvents", "START run - from=LocationModeChangedBroadcastReceiver.onReceive");

                            EventsHandler eventsHandler = new EventsHandler(appContext);
                            eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_RADIO_SWITCH);

                            //PPApplication.logE("****** EventsHandler.handleEvents", "END run - from=LocationModeChangedBroadcastReceiver.onReceive");
                        }

                        synchronized (PPApplication.geofenceScannerMutex) {
                            if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.getInstance().isGeofenceScannerStarted()) {
                                PhoneProfilesService.getInstance().getGeofencesScanner().clearAllEventGeofences();
                                //PPApplication.logE("LocationModeChangedBroadcastReceiver.onReceive", "updateTransitionsByLastKnownLocation");
                                PhoneProfilesService.getInstance().getGeofencesScanner().updateTransitionsByLastKnownLocation(true);
                            }
                        }
                    } finally {
                        if ((wakeLock != null) && wakeLock.isHeld()) {
                            try {
                                wakeLock.release();
                            } catch (Exception ignored) {}
                        }
                    }
                }
            });
        }

    }

}
