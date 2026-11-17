package com.shahid.cam.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shahid.cam.MainActivity;
import com.shahid.cam.R;
import com.shahid.cam.SharedPreferencesManager;

/**
 * Unified Permission Management Dashboard for ShahidCam.
 * Gives transparent control over Camera, Microphone, Media/Storage, Notifications,
 * Battery Optimization, Floating Overlay, and system app permissions without
 * requesting Device Admin or Accessibility privileges.
 */
public class PermissionsFragment extends Fragment {

    private TextView tvSummaryTitle;
    private TextView tvSummaryDesc;
    private ImageView ivSummaryIcon;

    private TextView btnCamera;
    private TextView btnMic;
    private TextView btnStorage;
    private TextView btnNotifications;
    private TextView btnBattery;
    private TextView btnOverlay;

    private String pendingRuntimePermission;

    private ActivityResultLauncher<String> singlePermissionLauncher;
    private ActivityResultLauncher<Intent> settingsActivityLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singlePermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    refreshPermissionStates();
                    if (!granted && pendingRuntimePermission != null && isAdded()) {
                        if (!shouldShowRequestPermissionRationale(pendingRuntimePermission)) {
                            showOpenSettingsDialog();
                        } else {
                            Toast.makeText(requireContext(), R.string.perm_denied_toast, Toast.LENGTH_SHORT).show();
                        }
                    }
                    pendingRuntimePermission = null;
                }
        );

        settingsActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> refreshPermissionStates()
        );
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_permissions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        com.shahid.cam.Utils.attachPressScaleToClickableRows(view);

        tvSummaryTitle = view.findViewById(R.id.tv_permissions_summary_title);
        tvSummaryDesc = view.findViewById(R.id.tv_permissions_summary_desc);
        ivSummaryIcon = view.findViewById(R.id.iv_permissions_summary_icon);

        btnCamera = view.findViewById(R.id.btn_perm_camera_action);
        btnMic = view.findViewById(R.id.btn_perm_mic_action);
        btnStorage = view.findViewById(R.id.btn_perm_storage_action);
        btnNotifications = view.findViewById(R.id.btn_perm_notifications_action);
        btnBattery = view.findViewById(R.id.btn_perm_battery_action);
        btnOverlay = view.findViewById(R.id.btn_perm_overlay_action);

        View rowCamera = view.findViewById(R.id.row_perm_camera);
        View rowMic = view.findViewById(R.id.row_perm_mic);
        View rowStorage = view.findViewById(R.id.row_perm_storage);
        View rowNotifications = view.findViewById(R.id.row_perm_notifications);
        View rowBattery = view.findViewById(R.id.row_perm_battery);
        View rowOverlay = view.findViewById(R.id.row_perm_overlay);
        View rowAppSettings = view.findViewById(R.id.row_perm_app_settings);
        View btnMore = view.findViewById(R.id.btn_permissions_more_menu);

        if (rowCamera != null) {
            rowCamera.setOnClickListener(v -> handleRuntimePermissionClick(
                    Manifest.permission.CAMERA,
                    R.string.perm_camera_title,
                    R.string.perm_camera_desc));
        }
        if (rowMic != null) {
            rowMic.setOnClickListener(v -> handleRuntimePermissionClick(
                    Manifest.permission.RECORD_AUDIO,
                    R.string.perm_mic_title,
                    R.string.perm_mic_desc));
        }
        if (rowStorage != null) {
            rowStorage.setOnClickListener(v -> {
                String storagePerm = getStoragePermissionForDevice();
                if (storagePerm == null) {
                    openAppDetailsSettings();
                } else {
                    handleRuntimePermissionClick(
                            storagePerm,
                            R.string.perm_storage_title,
                            R.string.perm_storage_desc);
                }
            });
        }
        if (rowNotifications != null) {
            rowNotifications.setOnClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    handleRuntimePermissionClick(
                            Manifest.permission.POST_NOTIFICATIONS,
                            R.string.perm_notifications_title,
                            R.string.perm_notifications_desc);
                } else {
                    openNotificationChannelSettings();
                }
            });
        }
        if (rowBattery != null) {
            rowBattery.setOnClickListener(v -> handleBatteryOptimizationClick());
        }
        if (rowOverlay != null) {
            rowOverlay.setOnClickListener(v -> handleOverlayPermissionClick());
        }
        if (rowAppSettings != null) {
            rowAppSettings.setOnClickListener(v -> openAppDetailsSettings());
        }
        if (btnMore != null) {
            btnMore.setOnClickListener(this::showOverflowMenu);
        }

        refreshPermissionStates();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPermissionStates();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            refreshPermissionStates();
        }
    }

    public static int getMissingCorePermissionCount(@NonNull Context context) {
        int missing = 0;
        if (!hasPermission(context, Manifest.permission.CAMERA)) {
            missing++;
        }
        if (!hasPermission(context, Manifest.permission.RECORD_AUDIO)) {
            missing++;
        }
        if (!hasStoragePermission(context)) {
            missing++;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)) {
            missing++;
        }
        return missing;
    }

    public void refreshPermissionStates() {
        if (!isAdded() || getContext() == null) return;
        Context ctx = requireContext();

        boolean cameraOk = hasPermission(ctx, Manifest.permission.CAMERA);
        boolean micOk = hasPermission(ctx, Manifest.permission.RECORD_AUDIO);
        boolean storageOk = hasStoragePermission(ctx);
        boolean notifOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || hasPermission(ctx, Manifest.permission.POST_NOTIFICATIONS);
        boolean batteryOk = isBatteryOptimizationIgnored(ctx);
        boolean overlayOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.M
                || Settings.canDrawOverlays(ctx);

        styleActionChip(btnCamera, cameraOk, R.string.perm_status_granted, R.string.perm_action_allow);
        styleActionChip(btnMic, micOk, R.string.perm_status_granted, R.string.perm_action_allow);
        styleActionChip(btnStorage, storageOk, R.string.perm_status_granted, R.string.perm_action_allow);
        styleActionChip(btnNotifications, notifOk, R.string.perm_status_granted, R.string.perm_action_allow);
        styleActionChip(btnBattery, batteryOk, R.string.perm_status_unrestricted, R.string.perm_action_optimize);
        styleActionChip(btnOverlay, overlayOk, R.string.perm_status_allowed, R.string.perm_action_manage);

        int missing = getMissingCorePermissionCount(ctx);
        if (tvSummaryTitle != null && tvSummaryDesc != null && ivSummaryIcon != null) {
            if (missing == 0) {
                tvSummaryTitle.setText(R.string.perm_summary_ready_title);
                tvSummaryDesc.setText(R.string.perm_summary_ready_desc);
                ivSummaryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.clay_success)));
            } else {
                tvSummaryTitle.setText(getString(R.string.perm_summary_attention_title, missing));
                tvSummaryDesc.setText(R.string.perm_summary_attention_desc);
                ivSummaryIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(ctx, R.color.clay_warning)));
            }
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshPermissionsBadge();
        }
    }

    private void styleActionChip(
            @Nullable TextView chip,
            boolean granted,
            int grantedTextRes,
            int actionTextRes) {
        if (chip == null || getContext() == null) return;
        Context ctx = requireContext();
        if (granted) {
            chip.setText(grantedTextRes);
            chip.setBackgroundResource(R.drawable.clay_card_inset_bg);
            chip.setTextColor(ContextCompat.getColor(ctx, R.color.clay_success));
        } else {
            chip.setText(actionTextRes);
            chip.setBackgroundResource(R.drawable.clay_button_primary_bg);
            chip.setTextColor(ContextCompat.getColor(ctx, android.R.color.white));
        }
    }

    private void handleRuntimePermissionClick(
            @NonNull String permission,
            int titleRes,
            int rationaleRes) {
        Context ctx = requireContext();
        if (hasPermission(ctx, permission)) {
            openAppDetailsSettings();
            return;
        }
        if (shouldShowRequestPermissionRationale(permission)) {
            new MaterialAlertDialogBuilder(ctx)
                    .setTitle(titleRes)
                    .setMessage(rationaleRes)
                    .setPositiveButton(R.string.perm_action_allow, (d, w) -> {
                        pendingRuntimePermission = permission;
                        singlePermissionLauncher.launch(permission);
                    })
                    .setNegativeButton(R.string.universal_cancel, null)
                    .show();
        } else {
            pendingRuntimePermission = permission;
            singlePermissionLauncher.launch(permission);
        }
    }

    @SuppressLint("BatteryLife")
    private void handleBatteryOptimizationClick() {
        Context ctx = requireContext();
        if (isBatteryOptimizationIgnored(ctx)) {
            Toast.makeText(ctx, R.string.perm_battery_already_unrestricted, Toast.LENGTH_SHORT).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
                settingsActivityLauncher.launch(intent);
                return;
            } catch (Exception ignored) {
            }
            try {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                settingsActivityLauncher.launch(fallback);
            } catch (Exception e) {
                openAppDetailsSettings();
            }
        }
    }

    private void handleOverlayPermissionClick() {
        Context ctx = requireContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + ctx.getPackageName()));
                settingsActivityLauncher.launch(intent);
            } catch (Exception e) {
                openAppDetailsSettings();
            }
        }
    }

    private void openNotificationChannelSettings() {
        Context ctx = requireContext();
        try {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
            } else {
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            }
            settingsActivityLauncher.launch(intent);
        } catch (Exception e) {
            openAppDetailsSettings();
        }
    }

    private void openAppDetailsSettings() {
        Context ctx = requireContext();
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            settingsActivityLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(ctx, R.string.perm_cannot_open_settings, Toast.LENGTH_SHORT).show();
        }
    }

    private void showOpenSettingsDialog() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.perm_app_settings_title)
                .setMessage(R.string.perm_open_settings_message)
                .setPositiveButton(R.string.perm_action_manage, (d, w) -> openAppDetailsSettings())
                .setNegativeButton(R.string.universal_cancel, null)
                .show();
    }

    private void showOverflowMenu(@NonNull View anchor) {
        PopupMenu popup = new PopupMenu(requireContext(), anchor);
        boolean isDark = SharedPreferencesManager.getInstance(requireContext()).isDarkModeEnabled();
        popup.getMenu().add(Menu.NONE, 1, 1, isDark ? R.string.menu_light_mode : R.string.menu_dark_mode);
        popup.getMenu().add(Menu.NONE, 2, 2, R.string.menu_intruder_history);
        popup.getMenu().add(Menu.NONE, 3, 3, R.string.menu_app_lock);
        popup.getMenu().add(Menu.NONE, 4, 4, R.string.menu_about);
        popup.setOnMenuItemClickListener(item -> {
            if (!isAdded()) return false;
            int id = item.getItemId();
            if (id == 1) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).toggleDarkMode(!isDark);
                }
                return true;
            } else if (id == 2) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).switchFragment(2, true);
                }
                return true;
            } else if (id == 3) {
                OverlayNavUtil.show(requireActivity(), new SecuritySettingsFragment(), "SecuritySettingsFragment");
                return true;
            } else if (id == 4) {
                OverlayNavUtil.show(requireActivity(), new AboutFragment(), "AboutFragment");
                return true;
            }
            return false;
        });
        popup.show();
    }

    private static boolean hasPermission(@NonNull Context context, @NonNull String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Nullable
    private static String getStoragePermissionForDevice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_VIDEO;
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return null;
    }

    private static boolean hasStoragePermission(@NonNull Context context) {
        String perm = getStoragePermissionForDevice();
        return perm == null || hasPermission(context, perm);
    }

    private static boolean isBatteryOptimizationIgnored(@NonNull Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
    }
}
