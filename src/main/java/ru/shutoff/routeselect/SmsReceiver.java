package ru.shutoff.routeselect;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;

public class SmsReceiver extends BroadcastReceiver {
    private final ComponentName componentName;

    public SmsReceiver(ComponentName componentName) {
        this.componentName = componentName;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            intent.setAction(Telephony.Sms.Intents.SMS_DELIVER_ACTION);
        } else if (intent.getAction().equals(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION)) {
            intent.setAction(Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION);
        }
        intent.setComponent(componentName);

        context.sendBroadcast(intent);
    }

    public ComponentName getComponentName() {
        return componentName;
    }
}