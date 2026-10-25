package com.shahid.cam.safety;

import android.content.Context;
import android.os.StatFs;
import com.shahid.cam.FLog;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class StorageRecycler {
    private static final String TAG = "StorageRecycler";
    public static final long DEFAULT_MIN_FREE_BYTES = 1536L * 1024L * 1024L; // 1.5 GB

    public static int maintainFreeSpace(File storageDir, long minFreeBytes) {
        if (storageDir == null || !storageDir.exists()) return 0;
        try {
            StatFs stat = new StatFs(storageDir.getAbsolutePath());
            long availableBytes = stat.getAvailableBytes();
            if (availableBytes >= minFreeBytes) return 0;

            FLog.i(TAG, "Low storage (" + (availableBytes / (1024 * 1024)) + "MB free). Recycling oldest unprotected files...");
            File[] files = storageDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return (lower.endsWith(".mp4") || lower.endsWith(".fmp4"))
                        && !lower.contains("locked")
                        && !lower.contains("incident")
                        && !lower.contains("fav");
            });

            if (files == null || files.length == 0) return 0;
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));

            int deletedCount = 0;
            for (File file : files) {
                long fileSize = file.length();
                if (file.delete()) {
                    deletedCount++;
                    availableBytes += fileSize;
                }
                if (availableBytes >= minFreeBytes) break;
            }
            FLog.i(TAG, "Storage recycling complete. Reclaimed space across " + deletedCount + " files.");
            return deletedCount;
        } catch (Exception e) {
            FLog.e(TAG, "Error in maintainFreeSpace", e);
            return 0;
        }
    }
}
