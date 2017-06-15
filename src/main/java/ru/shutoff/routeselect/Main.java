package ru.shutoff.routeselect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Telephony;
import android.view.Window;
import android.view.WindowManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

@SuppressWarnings("unused")
public class Main implements IXposedHookLoadPackage {

    final static String CG_EXIT = "cityguide.probki.net.EXIT";
    final static String CG_IS_RUN = "cityguide.probki.net.IS_RUN";
    static Activity cgActivity;
    static BroadcastReceiver br;
    static boolean cg_foreground;

    private SmsReceiver smsReceiver;
    private SmsReceiver mmsReceiver;

    private static void putPhoneIdAndSubIdExtra(Object thisObject, Intent intent) {
        /*
        PhoneBase phone = (PhoneBase) XposedHelpers.getObjectField(thisObject, "mPhone");
        XposedHelpers.callStaticMethod(SubscriptionManager.class, "putPhoneIdAndSubIdExtra",
                intent, phone.getPhoneId());
        */
    }

    private static void callSendBroadcastAsUser(Object thisObject, Context context, Intent intent,
                                                UserHandle user, String perm, int appOp, Bundle opts,
                                                BroadcastReceiver receiver) {
        XposedHelpers.callMethod(context, "sendOrderedBroadcastAsUser", intent, user, perm,
                appOp, opts, receiver, XposedHelpers.callMethod(thisObject, "getHandler"), Activity.RESULT_OK, null, null);
    }

    private static void callSendBroadcastAsUser(Object thisObject, Context context, Intent intent,
                                                UserHandle user, String perm, int appOp,
                                                BroadcastReceiver receiver) {
        XposedHelpers.callMethod(context, "sendOrderedBroadcastAsUser", intent, user, perm,
                appOp, receiver, XposedHelpers.callMethod(thisObject, "getHandler"), Activity.RESULT_OK, null, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static boolean isAllUser(UserHandle userHandle) {
        final Object allUser = XposedHelpers.getStaticObjectField(UserHandle.class, "ALL");
        return userHandle.equals(allUser);
    }

    private static void callSendBroadcast(Object thisObject, Context context, Intent intent, String perm, int appOp, BroadcastReceiver receiver) {
        XposedHelpers.callMethod(context, "sendOrderedBroadcast", intent, perm,
                appOp, receiver, XposedHelpers.callMethod(thisObject, "getHandler"), Activity.RESULT_OK, null, null);
    }

    private void handleSearch(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.google.android.googlequicksearchbox"))
            return;
        findAndHookMethod("android.app.Activity", lpparam.classLoader, "startActivityForResult", "android.content.Intent", "int", "android.os.Bundle", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                Intent intent = (Intent) param.args[0];
                String intent_pkg = intent.getPackage();
                if ((intent_pkg != null) && intent_pkg.equals("com.google.android.apps.maps"))
                    intent.setPackage(null);
                super.beforeHookedMethod(param);
            }
        });
    }

    private void handleMapcam(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("info.mapcam.droid"))
            return;
        findAndHookMethod("android.view.WindowManagerImpl", lpparam.classLoader, "addView", "android.view.View", "android.view.ViewGroup.LayoutParams", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1] instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.args[1];
                    lp.flags |= WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED;
                }
                super.beforeHookedMethod(param);
            }
        });
    }

    private void handleCG(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("cityguide.probki.net"))
            return;

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onCreate", "android.os.Bundle",
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Activity.onCreate");
                        cgActivity = (Activity) param.thisObject;
                        br = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                XposedBridge.log("Receive " + intent.getAction());
                                if (CG_EXIT.equals(intent.getAction())) {
                                    if (cgActivity != null) {
                                        cgActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                cgActivity.finish();
                                            }
                                        });
                                    }
                                    return;
                                }
                                if (CG_IS_RUN.equals(intent.getAction())) {
                                    if (cgActivity != null)
                                        setResultCode(cg_foreground ? 1 : 2);
                                }
                            }
                        };
                        IntentFilter intentFilter = new IntentFilter(CG_EXIT);
                        intentFilter.addAction(CG_IS_RUN);
                        cgActivity.registerReceiver(br, intentFilter);
                    }

                });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onDestroy",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Activity.onDestory");
                        NotificationManager notificationManager = (NotificationManager) cgActivity.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancelAll();
                        cgActivity.unregisterReceiver(br);
                        cgActivity = null;
                    }

                });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onResume",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Activity.onResume");
                        cg_foreground = true;
                    }

                });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onPause",
                new XC_MethodHook() {

                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Activity.onPause");
                        cg_foreground = false;
                    }

                });

        findAndHookMethod("android.app.Activity", lpparam.classLoader, "onAttachedToWindow",
                new XC_MethodHook() {

                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        XposedBridge.log("Activity.onAttachedToWindow");
                        Activity activity = (Activity) param.thisObject;
                        Window window = activity.getWindow();
                        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                                + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                                + WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                                + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                    }

                });
    }

    private void updateDefaultSmsReceiver(Context context, ComponentName componentName, boolean isMms) {
        SmsReceiver receiver = isMms ? mmsReceiver : smsReceiver;
        if (receiver != null) {
            if (receiver.getComponentName().equals(componentName)) {
                return;
            }
            context.unregisterReceiver(receiver);

            if (isMms) {
                mmsReceiver = null;
            } else {
                smsReceiver = null;
            }
        }

        if (componentName == null) {
            return;
        }

        receiver = new SmsReceiver(componentName);
        context.registerReceiver(receiver, new IntentFilter(isMms
                ? Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION : Telephony.Sms.Intents.SMS_RECEIVED_ACTION));
        if (isMms) {
            mmsReceiver = receiver;
        } else {
            smsReceiver = receiver;
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void hookDispatchIntent19(LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
                param1Type, param2Type, param3Type, param4Type, new DispatchIntentHook(3));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void hookDispatchIntent21(LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = BroadcastReceiver.class;
        Class<?> param5Type = UserHandle.class;

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
                param1Type, param2Type, param3Type, param4Type, param5Type, new DispatchIntentHook(3));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void hookDispatchIntent23(LoadPackageParam lpparam) {
        String className = "com.android.internal.telephony.InboundSmsHandler";
        String methodName = "dispatchIntent";
        Class<?> param1Type = Intent.class;
        Class<?> param2Type = String.class;
        Class<?> param3Type = int.class;
        Class<?> param4Type = Bundle.class;
        Class<?> param5Type = BroadcastReceiver.class;
        Class<?> param6Type = UserHandle.class;

        XposedHelpers.findAndHookMethod(className, lpparam.classLoader, methodName,
                param1Type, param2Type, param3Type, param4Type, param5Type, param6Type, new DispatchIntentHook(4));
    }

    private void handlePhone(final LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.android.phone"))
            return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hookDispatchIntent23(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            hookDispatchIntent21(lpparam);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            hookDispatchIntent19(lpparam);
        }
    }

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        handleSearch(lpparam);
        handleCG(lpparam);
        handleMapcam(lpparam);
        handlePhone(lpparam);
    }

    private class DispatchIntentHook extends XC_MethodHook {
        private final int mReceiverIndex;

        public DispatchIntentHook(int receiverIndex) {
            mReceiverIndex = receiverIndex;
        }

        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            try {
                Intent intent = (Intent) param.args[0];
                final String action = intent.getAction();

                boolean isMms = false;
                switch (action) {
                    case Telephony.Sms.Intents.SMS_DELIVER_ACTION:
                        intent.setAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
                        break;
                    case Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION:
                        isMms = true;
                        intent.setAction(Telephony.Sms.Intents.WAP_PUSH_RECEIVED_ACTION);
                        break;
                    default:
                        // not interesting
                        return;
                }

                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
                updateDefaultSmsReceiver(context, intent.getComponent(), isMms);
                intent.setComponent(null);

                String perm = (String) param.args[1];
                int appOp = ((Integer) param.args[2]);
                BroadcastReceiver receiver = (BroadcastReceiver) param.args[mReceiverIndex];

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    final Bundle opts = (Bundle) param.args[3];
                    UserHandle userHandle = (UserHandle) param.args[5];
                    putPhoneIdAndSubIdExtra(param.thisObject, intent);
                    if (isAllUser(userHandle))
                        userHandle = (UserHandle) XposedHelpers.getStaticObjectField(UserHandle.class, "OWNER");
                    callSendBroadcastAsUser(param.thisObject, context, intent, userHandle, perm, appOp, opts, receiver);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    UserHandle userHandle = (UserHandle) param.args[4];
                    putPhoneIdAndSubIdExtra(param.thisObject, intent);
                    if (isAllUser(userHandle))
                        userHandle = (UserHandle) XposedHelpers.getStaticObjectField(UserHandle.class, "OWNER");
                    callSendBroadcastAsUser(param.thisObject, context, intent, userHandle, perm, appOp, receiver);
                } else {
                    callSendBroadcast(param.thisObject, context, intent, perm, appOp, receiver);
                }
                // skip original method
                param.setResult(null);
            } catch (Throwable e) {
                XposedBridge.log(e);
                throw e;
            }
        }
    }

}
