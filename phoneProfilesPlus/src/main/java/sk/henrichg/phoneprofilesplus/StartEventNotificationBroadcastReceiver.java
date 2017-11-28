package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StartEventNotificationBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### StartEventNotificationBroadcastReceiver.onReceive", "xxx");
        CallsCounter.logCounter(context, "StartEventNotificationBroadcastReceiver.onReceive", "StartEventNotificationBroadcastReceiver_onReceive");

        long event_id = intent.getLongExtra(PPApplication.EXTRA_EVENT_ID, 0);
        if (event_id != 0) {
            DatabaseHandler databaseHandler = DatabaseHandler.getInstance(context.getApplicationContext());
            Event event = databaseHandler.getEvent(event_id);
            if (event != null)
                event.notifyEventStart(context.getApplicationContext());
        }
    }

    static void removeAlarm(Context context)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(context, StartEventNotificationBroadcastReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                PPApplication.logE("StartEventNotificationBroadcastReceiver.removeAlarm", "alarm found");

                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    static void setAlarm(Event event, Context context)
    {
        //if (!_permanentRun) {

            Calendar now = Calendar.getInstance();
            now.add(Calendar.MINUTE, event._repeatNotificationInterval);
            long alarmTime = now.getTimeInMillis();

            SimpleDateFormat sdf = new SimpleDateFormat("EE d.MM.yyyy HH:mm:ss:S");
            String result = sdf.format(alarmTime);
            PPApplication.logE("StartEventNotificationBroadcastReceiver.setAlarm", "alarmTime=" + result);

            Intent intent = new Intent(context, StartEventNotificationBroadcastReceiver.class);
            intent.putExtra(PPApplication.EXTRA_EVENT_ID, event._id);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
            if (alarmManager != null) {
                if (android.os.Build.VERSION.SDK_INT >= 23)
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                else if (android.os.Build.VERSION.SDK_INT >= 19)
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
                else
                    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
            }
        //}
    }

}
