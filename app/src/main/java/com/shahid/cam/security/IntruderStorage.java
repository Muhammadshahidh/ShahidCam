package com.shahid.cam.security;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.shahid.cam.FLog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Private internal-storage repository for Intruder Detection captures.
 * All files are saved strictly inside context.getFilesDir()/intruder_selfies/
 * so they remain private to ShahidCam, hidden from public MediaStore galleries,
 * and never uploaded externally.
 */
public final class IntruderStorage {
    private static final String TAG = "IntruderStorage";
    private static final String DIR_NAME = "intruder_selfies";
    private static final int MAX_STORED_RECORDS = 100;

    private IntruderStorage() {}

    @NonNull
    public static File getPrivateDirectory(@NonNull Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), DIR_NAME);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }

    @Nullable
    public static synchronized IntruderRecord saveCapture(@NonNull Context context,
                                                           @NonNull byte[] jpegBytes,
                                                           @NonNull String reasonTag) {
        if (jpegBytes.length == 0) {
            return null;
        }
        long now = System.currentTimeMillis();
        String safeReason = reasonTag.toUpperCase(Locale.US).replaceAll("[^A-Z0-9_]", "");
        if (safeReason.isEmpty()) {
            safeReason = "PIN";
        }
        String fileName = String.format(Locale.US, "intruder_%d_%s.jpg", now, safeReason);
        File dir = getPrivateDirectory(context);
        File outFile = new File(dir, fileName);

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(jpegBytes);
            fos.flush();
        } catch (IOException e) {
            FLog.e(TAG, "Failed writing intruder selfie to private storage", e);
            return null;
        }

        trimOldestIfNeeded(dir);
        return new IntruderRecord(fileName, outFile.getAbsolutePath(), now, formatReasonLabel(safeReason));
    }

    @NonNull
    public static synchronized List<IntruderRecord> listRecords(@NonNull Context context) {
        File dir = getPrivateDirectory(context);
        File[] files = dir.listFiles((d, name) -> name.startsWith("intruder_") && name.endsWith(".jpg"));
        List<IntruderRecord> result = new ArrayList<>();
        if (files == null || files.length == 0) {
            return result;
        }

        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        for (File file : files) {
            if (!file.isFile() || file.length() <= 0) {
                continue;
            }
            ParsedMeta meta = parseMetaFromName(file);
            result.add(new IntruderRecord(
                    file.getName(),
                    file.getAbsolutePath(),
                    meta.timestampMs,
                    meta.reasonLabel
            ));
        }
        result.sort((a, b) -> Long.compare(b.timestampMs, a.timestampMs));
        return result;
    }

    public static synchronized int getRecordCount(@NonNull Context context) {
        return listRecords(context).size();
    }

    public static synchronized boolean deleteRecord(@NonNull Context context, @NonNull String filePath) {
        try {
            File target = new File(filePath).getCanonicalFile();
            File allowedRoot = getPrivateDirectory(context).getCanonicalFile();
            if (!target.getPath().startsWith(allowedRoot.getPath() + File.separator)) {
                FLog.w(TAG, "Refused deletion outside private intruder directory: " + filePath);
                return false;
            }
            return target.exists() && target.delete();
        } catch (IOException e) {
            FLog.e(TAG, "Failed deleting intruder record", e);
            return false;
        }
    }

    public static synchronized int clearAll(@NonNull Context context) {
        File dir = getPrivateDirectory(context);
        File[] files = dir.listFiles((d, name) -> name.startsWith("intruder_") && name.endsWith(".jpg"));
        if (files == null) return 0;
        int deleted = 0;
        for (File file : files) {
            if (file.isFile() && file.delete()) {
                deleted++;
            }
        }
        return deleted;
    }

    private static void trimOldestIfNeeded(@NonNull File dir) {
        File[] files = dir.listFiles((d, name) -> name.startsWith("intruder_") && name.endsWith(".jpg"));
        if (files == null || files.length <= MAX_STORED_RECORDS) {
            return;
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        int toDelete = files.length - MAX_STORED_RECORDS;
        for (int i = 0; i < toDelete; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }

    @NonNull
    private static ParsedMeta parseMetaFromName(@NonNull File file) {
        String name = file.getName();
        long ts = file.lastModified();
        String reasonTag = "PIN";
        try {
            String stripped = name.replace("intruder_", "").replace(".jpg", "");
            int idx = stripped.indexOf('_');
            if (idx > 0) {
                ts = Long.parseLong(stripped.substring(0, idx));
                reasonTag = stripped.substring(idx + 1);
            } else {
                ts = Long.parseLong(stripped);
            }
        } catch (Exception ignored) {
        }
        return new ParsedMeta(ts, formatReasonLabel(reasonTag));
    }

    @NonNull
    private static String formatReasonLabel(@NonNull String rawTag) {
        if ("PASSWORD".equalsIgnoreCase(rawTag)) {
            return "Wrong Password Attempt";
        }
        return "Wrong PIN Attempt";
    }

    private static class ParsedMeta {
        final long timestampMs;
        final String reasonLabel;

        ParsedMeta(long timestampMs, String reasonLabel) {
            this.timestampMs = timestampMs;
            this.reasonLabel = reasonLabel;
        }
    }
}
