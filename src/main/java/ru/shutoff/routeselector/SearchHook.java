package ru.shutoff.routeselector;

import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SearchHook implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(LoadPackageParam loadPackageParam) throws Throwable {
        if (!loadPackageParam.packageName.equals("com.google.android.quicksearhcbox"))
            return;
        appendLog("Hooked");
        findAndHookMethod("android.content.Context", loadPackageParam.classLoader, "startActivity", new XC_MethodHook(){
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                appendLog("StartActivity");
                Intent intent = (Intent)param.args[0];
                String pkg = intent.getPackage();
                appendLog("Pkg: pkg");
                super.beforeHookedMethod(param);
            }
        });
    }

    static public void appendLog(String text) {
        Log.v("v", text);

        File logFile = Environment.getExternalStorageDirectory();
        logFile = new File(logFile, "route.log");
        if (!logFile.exists()) {
            try {
                if (!logFile.createNewFile())
                    return;
            } catch (IOException e) {
                // ignore
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            Date d = new Date();
            buf.append(d.toLocaleString());
            buf.append(" ");
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // ignore
        }
    }
}
