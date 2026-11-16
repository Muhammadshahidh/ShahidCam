package com.shahid.cam.security;

import androidx.annotation.NonNull;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Immutable model representing a single locally stored intruder detection selfie.
 */
public class IntruderRecord {
    @NonNull
    public final String id;
    @NonNull
    public final String filePath;
    public final long timestampMs;
    @NonNull
    public final String reason;

    public IntruderRecord(@NonNull String id,
                          @NonNull String filePath,
                          long timestampMs,
                          @NonNull String reason) {
        this.id = id;
        this.filePath = filePath;
        this.timestampMs = timestampMs;
        this.reason = reason;
    }

    @NonNull
    public File getFile() {
        return new File(filePath);
    }

    public boolean exists() {
        File f = getFile();
        return f.exists() && f.isFile() && f.length() > 0;
    }

    @NonNull
    public String getFormattedDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.getDefault());
        return sdf.format(new Date(timestampMs));
    }

    @NonNull
    public String getFormattedDateShort() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestampMs));
    }

    @NonNull
    public String getFormattedTimeShort() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestampMs));
    }
}
