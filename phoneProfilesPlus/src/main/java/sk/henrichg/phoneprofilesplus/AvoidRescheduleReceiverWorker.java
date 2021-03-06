package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

// https://issuetracker.google.com/issues/115575872#comment16

@SuppressWarnings("WeakerAccess")
public class AvoidRescheduleReceiverWorker extends Worker {

    public AvoidRescheduleReceiverWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            //PPApplication.logE("AvoidRescheduleReceiverWorker.doWork", "xxx");

            PPApplication.startHandlerThreadPPScanners();
            final Handler handler = new Handler(PPApplication.handlerThreadPPScanners.getLooper());
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    enqueueWork();
                }
            }, 500);

            return Result.success();
        } catch (Exception e) {
            //Log.e("AvoidRescheduleReceiverWorker.doWork", Log.getStackTraceString(e));
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

    static void enqueueWork() {
        PhoneProfilesService.cancelWork(PPApplication.AVOID_RESCHEDULE_RECEIVER_WORK_TAG);
        OneTimeWorkRequest avoidRescheduleReceiverWorker =
                new OneTimeWorkRequest.Builder(AvoidRescheduleReceiverWorker.class)
                        .addTag(PPApplication.AVOID_RESCHEDULE_RECEIVER_WORK_TAG)
                        .setInitialDelay(30 * 3, TimeUnit.DAYS)
                        .build();
        try {
            WorkManager workManager = PPApplication.getWorkManagerInstance();
            //PPApplication.logE("##### PPApplication.onCreate", "workManager="+workManager);
            if (workManager != null)
                workManager.enqueueUniqueWork(PPApplication.AVOID_RESCHEDULE_RECEIVER_WORK_TAG, ExistingWorkPolicy.KEEP, avoidRescheduleReceiverWorker);
        } catch (Exception e) {
            PPApplication.recordException(e);
        }
    }

}
