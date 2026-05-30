package dev.caceresenzo.rotationcontrol.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import dev.caceresenzo.rotationcontrol.R;

public class Links {

    public static final String GITHUB_URL = "https://github.com/Caceresenzo/modern-rotation-control";

    public static void openGitHub(Context context) {
        openInBrowser(context, GITHUB_URL);
    }

    public static void openInBrowser(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(GITHUB_URL));

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(context, R.string.no_browser_found, Toast.LENGTH_SHORT).show();
        }
    }

}