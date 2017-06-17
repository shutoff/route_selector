package ru.shutoff.routeselect;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Date;

public class AlarmReceiver extends BroadcastReceiver {

    static final String ACTION = "ru.shutoff.routeselect.ACTION";

    static final long INTERVAL = 10 * 3600 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AlarmManager.AlarmClockInfo info = alarmManager.getNextAlarmClock();
            if (info == null)
                return;
            long delta = info.getTriggerTime() - new Date().getTime();
            boolean hide = false;
            if (delta > INTERVAL) {
                hide = true;
                Intent i = new Intent(ACTION);
                PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.setExact(AlarmManager.RTC, System.currentTimeMillis() + delta - INTERVAL + 60000, pi);
            }
            Intent hideIntent = new Intent("gravitybox.intent.action.CENTER_CLOCK_CHANGED");
            hideIntent.putExtra("alarmHide", hide);
            context.sendBroadcast(hideIntent);
        }
    }
}
