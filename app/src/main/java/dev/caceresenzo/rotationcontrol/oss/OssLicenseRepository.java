package dev.caceresenzo.rotationcontrol.oss;

import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.caceresenzo.rotationcontrol.R;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class OssLicenseRepository {

    private final Resources resources;

    public List<OssLicense> findAll() throws IOException {
        Map<String, OssLicense> licensesByLibraryName = new LinkedHashMap<>();

        try (
                InputStream rawResource = resources.openRawResource(R.raw.third_party_license_metadata);
                InputStreamReader streamReader = new InputStreamReader(rawResource, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(streamReader)
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                int separatorIndex = line.indexOf(' ');
                String[] offsetAndLength = line.substring(0, separatorIndex).split(":");
                String libraryName = line.substring(separatorIndex + 1);

                OssLicense license = new OssLicense(
                        libraryName,
                        Long.parseLong(offsetAndLength[0]),
                        Integer.parseInt(offsetAndLength[1])
                );

                licensesByLibraryName.putIfAbsent(libraryName, license);
            }
        }

        List<OssLicense> licenses = new ArrayList<>(licensesByLibraryName.values());
        licenses.sort(Comparator.comparing(OssLicense::getLibraryName, String.CASE_INSENSITIVE_ORDER));

        return licenses;
    }

    public String getText(OssLicense license) throws IOException {
        byte[] buffer = new byte[license.getLength()];

        try (
                InputStream rawResource = resources.openRawResource(R.raw.third_party_licenses);
                DataInputStream inputStream = new DataInputStream(rawResource)
        ) {
            long remainingBytesToSkip = license.getStartOffset();
            while (remainingBytesToSkip > 0) {
                long skippedBytes = inputStream.skip(remainingBytesToSkip);
                if (skippedBytes <= 0) {
                    throw new EOFException("License offset beyond end of file");
                }

                remainingBytesToSkip -= skippedBytes;
            }

            inputStream.readFully(buffer);
        }

        return new String(buffer, StandardCharsets.UTF_8);
    }

}