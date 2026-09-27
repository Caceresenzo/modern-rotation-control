package dev.caceresenzo.rotationcontrol.oss;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.caceresenzo.rotationcontrol.R;

public final class OssLicensesActivity extends AppCompatActivity {

    private ExecutorService backgroundExecutor;
    private OssLicenseRepository licenseRepository;

    private RecyclerView licenseListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.oss_licenses_activity);

        setTitle(R.string.oss_license_title);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        backgroundExecutor = Executors.newSingleThreadExecutor();
        licenseRepository = new OssLicenseRepository(getResources());

        licenseListView = findViewById(R.id.license_list);
        licenseListView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        loadLicenses();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        backgroundExecutor.shutdownNow();
    }

    private void loadLicenses() {
        backgroundExecutor.execute(() -> {
            try {
                List<OssLicense> licenses = licenseRepository.findAll();

                runOnUiThread(() -> licenseListView.setAdapter(new OssLicenseListAdapter(licenses, this::showLicenseText)));
            } catch (IOException exception) {
                runOnUiThread(() -> showError(exception));
            }
        });
    }

    private void showLicenseText(OssLicense license) {
        backgroundExecutor.execute(() -> {
            try {
                String licenseText = licenseRepository.getText(license);

                runOnUiThread(() -> showLicenseDialog(license.getLibraryName(), licenseText));
            } catch (IOException exception) {
                runOnUiThread(() -> showError(exception));
            }
        });
    }

    private void showLicenseDialog(String libraryName, String licenseText) {
        if (isFinishing()) {
            return;
        }

        View dialogView = getLayoutInflater().inflate(R.layout.oss_dialog_license_text, null);

        TextView contentTextView = dialogView.findViewById(R.id.content);
        contentTextView.setText(licenseText);

        new AlertDialog.Builder(this)
                .setTitle(libraryName)
                .setView(dialogView)
                .setPositiveButton(R.string.oss_license_dialog_close, null)
                .show();
    }

    private void showError(Exception exception) {
        Toast.makeText(this, getString(R.string.oss_license_error, exception.getMessage()), Toast.LENGTH_LONG).show();
    }

}