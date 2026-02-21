package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Queries {

    /**
     * Gets a list of package names for all installed launcher applications.
     *
     * @param context The application context.
     * @return A Set of strings containing the package names.
     */
    public static Set<String> getAllLauncherPackageNames(Context context) {
        PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);

        List<ResolveInfo> resolveInfoList;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            resolveInfoList = packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL)
            );
        } else {
            resolveInfoList = packageManager.queryIntentActivities(
                    intent,
                    PackageManager.MATCH_ALL
            );
        }

        Set<String> packageNames = new HashSet<>();
        for (ResolveInfo info : resolveInfoList) {
            if (info.activityInfo != null) {
                packageNames.add(info.activityInfo.packageName);
            }
        }

        return packageNames;
    }

    /**
     * Gets a list of package names for all installed Input Method Editors (keyboards).
     *
     * @param context The application context.
     * @return A Set of strings containing the package names.
     */
    public static Set<String> getAllInputMethodPackageNames(Context context) {
        Set<String> packageNames = new HashSet<>();

        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            List<InputMethodInfo> inputMethods = inputMethodManager.getInputMethodList();

            for (InputMethodInfo info : inputMethods) {
                packageNames.add(info.getPackageName());
            }
        }

        return packageNames;
    }

}