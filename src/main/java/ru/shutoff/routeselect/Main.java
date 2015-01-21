package ru.shutoff.routeselect;

import android.content.Intent;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

@SuppressWarnings("unused")
public class Main implements IXposedHookLoadPackage {

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
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

}
