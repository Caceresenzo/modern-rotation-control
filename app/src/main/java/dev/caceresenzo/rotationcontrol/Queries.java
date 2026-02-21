package dev.caceresenzo.rotationcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

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

        Set<String> launcherPackages = new HashSet<>();
        for (ResolveInfo info : resolveInfoList) {
            if (info.activityInfo != null) {
                launcherPackages.add(info.activityInfo.packageName);
            }
        }

        return launcherPackages;
    }

}