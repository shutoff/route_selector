package ru.shutoff.routeselect;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Window;
import android.view.WindowManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

@SuppressWarnings("unused")
public class Main implements IXposedHookLoadPackage {

    final static String CG_EXIT = "cityguide.probki.net.EXIT";
    final static String CG_IS_RUN = "cityguide.probki.net.IS_RUN";
    static Activity cgActivity;
    static BroadcastReceiver br;
    static boolean cg_foreground;

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("com.google.android.googlequicksearchbox")) {
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
            return;
        }

        if (!lpparam.packageName.equals("cityguide.probki.net"))
            return;

        XposedBridge.log("Load " + lpparam.packageName);

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

}
