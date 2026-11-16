package com.shahid.cam.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.guardanis.applock.AppLock;
import com.guardanis.applock.dialogs.LockCreationDialogBuilder;
import com.guardanis.applock.dialogs.UnlockDialogBuilder;
import com.shahid.cam.MainActivity;
import com.shahid.cam.R;
import com.shahid.cam.SharedPreferencesManager;
import com.shahid.cam.security.IntruderManager;
import com.shahid.cam.security.IntruderStorage;

import java.util.ArrayList;

/**
 * SecuritySettingsFragment
 * Configures App Lock, Recents cloak, Privacy Black Mode, and Wrong-Password Intruder Selfie Detection.
 */
public class SecuritySettingsFragment extends Fragment {

    private SharedPreferencesManager sharedPreferencesManager;
    private TextView valueTabLock;
    private TextView valueCloakRecents;
    private TextView valuePrivacyBlackMode;
    private TextView valueIntruderSelfie;
    private TextView valueIntruderCount;
    private Boolean pendingToggleDesiredState;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (!isAdded()) return;
                    if (granted) {
                        sharedPreferencesManager.setIntruderSelfieEnabled(true);
                        Toast.makeText(requireContext(), R.string.intruder_enabled_toast, Toast.LENGTH_SHORT).show();
                        refreshIntruderValues();
                        maybeSuggestAppLock();
                    } else {
                        sharedPreferencesManager.setIntruderSelfieEnabled(false);
                        refreshIntruderValues();
                        if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.intruder_permission_needed_title)
                                    .setMessage(R.string.intruder_permission_settings_msg)
                                    .setPositiveButton(R.string.open_settings, (d, w) -> {
                                        try {
                                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                    Uri.fromParts("package", requireContext().getPackageName(), null));
                                            startActivity(intent);
                                        } catch (Exception ignored) {}
                                    })
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .show();
                        } else {
                            Toast.makeText(requireContext(), R.string.intruder_permission_denied_toast, Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings_security, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        com.shahid.cam.Utils.attachPressScaleToClickableRows(view);
        super.onViewCreated(view, savedInstanceState);
        sharedPreferencesManager = SharedPreferencesManager.getInstance(requireContext());
        valueTabLock = view.findViewById(R.id.value_tab_lock_status);
        valueCloakRecents = view.findViewById(R.id.value_cloak_recents_status);
        valuePrivacyBlackMode = view.findViewById(R.id.value_privacy_black_status);
        valueIntruderSelfie = view.findViewById(R.id.value_intruder_selfie_status);
        valueIntruderCount = view.findViewById(R.id.value_intruder_history_count);

        refreshAppLockValue();
        refreshCloakRecentsValue();
        refreshPrivacyBlackModeValue();
        refreshIntruderValues();

        View back = view.findViewById(R.id.back_button);
        if (back != null) {
            back.setOnClickListener(v -> OverlayNavUtil.dismiss(requireActivity()));
        }

        View rowTabLock = view.findViewById(R.id.row_tab_lock);
        if (rowTabLock != null) {
            rowTabLock.setOnClickListener(v -> showAppLockConfigDialog());
        }
        View rowCloak = view.findViewById(R.id.row_cloak_recents);
        if (rowCloak != null) {
            rowCloak.setOnClickListener(v -> showCloakRecentsConfig());
        }
        View rowPrivacyBlack = view.findViewById(R.id.row_privacy_black);
        if (rowPrivacyBlack != null) {
            rowPrivacyBlack.setOnClickListener(v -> showPrivacyBlackConfig());
        }
        View rowIntruderSelfie = view.findViewById(R.id.row_intruder_selfie);
        if (rowIntruderSelfie != null) {
            rowIntruderSelfie.setOnClickListener(v -> showIntruderSelfieConfig());
        }
        View rowIntruderHistory = view.findViewById(R.id.row_intruder_history);
        if (rowIntruderHistory != null) {
            rowIntruderHistory.setOnClickListener(v ->
                    OverlayNavUtil.show(requireActivity(), IntruderHistoryFragment.newInstance(true), "IntruderHistoryFragment"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshAppLockValue();
        refreshCloakRecentsValue();
        refreshPrivacyBlackModeValue();
        refreshIntruderValues();
    }

    private void refreshAppLockValue() {
        if (valueTabLock != null) {
            boolean enabled = sharedPreferencesManager.isAppLockEnabled();
            valueTabLock.setText(enabled ? getString(R.string.universal_enable) : getString(R.string.universal_disable));
        }
    }

    private void refreshCloakRecentsValue() {
        if (valueCloakRecents != null) {
            boolean enabled = sharedPreferencesManager.isCloakRecentsEnabled();
            valueCloakRecents.setText(enabled ? getString(R.string.universal_enable) : getString(R.string.universal_disable));
        }
    }

    private void refreshPrivacyBlackModeValue() {
        if (valuePrivacyBlackMode != null) {
            boolean enabled = sharedPreferencesManager.isPrivacyBlackModeEnabled();
            valuePrivacyBlackMode.setText(enabled ? getString(R.string.universal_enable) : getString(R.string.universal_disable));
        }
    }

    private void refreshIntruderValues() {
        if (!isAdded()) return;
        if (valueIntruderSelfie != null) {
            boolean hasFront = IntruderManager.hasFrontCamera(requireContext());
            boolean hasPerm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED;
            boolean enabled = sharedPreferencesManager.isIntruderSelfieEnabled();
            if (!hasFront) {
                valueIntruderSelfie.setText(getString(R.string.intruder_status_no_camera));
            } else if (enabled && hasPerm) {
                valueIntruderSelfie.setText(getString(R.string.universal_enable));
            } else if (enabled) {
                valueIntruderSelfie.setText(getString(R.string.intruder_permission_needed_short));
            } else {
                valueIntruderSelfie.setText(getString(R.string.universal_disable));
            }
        }
        if (valueIntruderCount != null) {
            int count = IntruderStorage.getRecordCount(requireContext());
            valueIntruderCount.setText(String.valueOf(count));
        }
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshIntruderNavBadge();
        }
    }

    private void showIntruderSelfieConfig() {
        if (!IntruderManager.hasFrontCamera(requireContext())) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.setting_intruder_selfie_title)
                    .setMessage(R.string.intruder_no_front_camera_msg)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        final String resultKey = "intruder_selfie_sheet_result";
        boolean enabled = sharedPreferencesManager.isIntruderSelfieEnabled();
        boolean hasPerm = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        ArrayList<com.shahid.cam.ui.picker.OptionItem> items = new ArrayList<>();
        items.add(new com.shahid.cam.ui.picker.OptionItem(
                "open_history",
                getString(R.string.intruder_history_title),
                getString(R.string.setting_intruder_history_subtitle),
                null,
                null,
                R.drawable.ic_arrow_right,
                false,
                false,
                null
        ));

        getParentFragmentManager().setFragmentResultListener(resultKey, this, (requestKey, bundle) -> {
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE)) {
                boolean desired = bundle.getBoolean(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE);
                if (desired) {
                    beginEnableIntruderDetection();
                } else {
                    sharedPreferencesManager.setIntruderSelfieEnabled(false);
                    refreshIntruderValues();
                    Toast.makeText(requireContext(), R.string.setting_disabled_msg, Toast.LENGTH_SHORT).show();
                }
            }
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID)) {
                String action = bundle.getString(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID);
                if ("open_history".equals(action)) {
                    OverlayNavUtil.show(requireActivity(), IntruderHistoryFragment.newInstance(true), "IntruderHistoryFragment");
                }
            }
        });

        com.shahid.cam.ui.picker.PickerBottomSheetFragment sheet =
                com.shahid.cam.ui.picker.PickerBottomSheetFragment.newInstanceWithSwitch(
                        getString(R.string.setting_intruder_selfie_title),
                        items,
                        null,
                        resultKey,
                        getString(R.string.intruder_disclosure_body),
                        getString(R.string.setting_intruder_selfie_switch),
                        enabled && hasPerm,
                        false
                );
        sheet.show(getParentFragmentManager(), "intruder_selfie_sheet");
    }

    private void beginEnableIntruderDetection() {
        if (!IntruderManager.hasFrontCamera(requireContext())) {
            sharedPreferencesManager.setIntruderSelfieEnabled(false);
            refreshIntruderValues();
            Toast.makeText(requireContext(), R.string.intruder_no_front_camera_msg, Toast.LENGTH_LONG).show();
            return;
        }

        if (!sharedPreferencesManager.isIntruderDisclosureAccepted()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.intruder_disclosure_title)
                    .setMessage(R.string.intruder_disclosure_body)
                    .setPositiveButton(R.string.intruder_disclosure_accept, (d, w) -> {
                        sharedPreferencesManager.setIntruderDisclosureAccepted(true);
                        requestCameraPermissionAndEnable();
                    })
                    .setNegativeButton(android.R.string.cancel, (d, w) -> {
                        sharedPreferencesManager.setIntruderSelfieEnabled(false);
                        refreshIntruderValues();
                    })
                    .show();
        } else {
            requestCameraPermissionAndEnable();
        }
    }

    private void requestCameraPermissionAndEnable() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            sharedPreferencesManager.setIntruderSelfieEnabled(true);
            Toast.makeText(requireContext(), R.string.intruder_enabled_toast, Toast.LENGTH_SHORT).show();
            refreshIntruderValues();
            maybeSuggestAppLock();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void maybeSuggestAppLock() {
        if (!isAdded()) return;
        if (!sharedPreferencesManager.isAppLockEnabled() || !AppLock.isEnrolled(requireContext())) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.setting_applock_title)
                    .setMessage(R.string.intruder_applock_recommended_msg)
                    .setPositiveButton(R.string.intruder_configure_applock, (d, w) -> showAppLockConfigDialog())
                    .setNegativeButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void showAppLockConfigDialog() {
        boolean isEnrolled = AppLock.isEnrolled(requireContext());
        boolean isEnabled = sharedPreferencesManager.isAppLockEnabled();
        ArrayList<com.shahid.cam.ui.picker.OptionItem> items = new ArrayList<>();
        String helper;
        if (!isEnrolled) {
            helper = getString(R.string.applock_helper_create);
        } else if (isEnabled) {
            helper = getString(R.string.applock_helper_enabled);
        } else {
            helper = getString(R.string.applock_helper_manage_disabled);
        }

        boolean showSwitch = isEnrolled;
        String switchTitle = getString(R.string.setting_applock_title);
        String resultKey = "applock_sheet_result";

        if (!isEnrolled) {
            items.add(new com.shahid.cam.ui.picker.OptionItem("set_pin_enable", getString(R.string.applock_set_pin_enable)));
        } else {
            items.add(new com.shahid.cam.ui.picker.OptionItem("change_pin", getString(R.string.applock_change_pin)));
            items.add(new com.shahid.cam.ui.picker.OptionItem("remove_pin", getString(R.string.applock_remove_pin)));
        }

        getParentFragmentManager().setFragmentResultListener(resultKey, this, (requestKey, bundle) -> {
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE)) {
                boolean newState = bundle.getBoolean(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE);
                handleEnableDisable(newState);
            }
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID)) {
                String action = bundle.getString(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID);
                if (action != null) {
                    switch (action) {
                        case "set_pin_enable":
                            showPinCreationDialog(true);
                            break;
                        case "change_pin":
                            verifyPinThenExecute(() -> showPinCreationDialog(isEnabled), R.string.applock_verify_to_change);
                            break;
                        case "remove_pin":
                            verifyPinThenExecute(() -> {
                                AppLock appLock = AppLock.getInstance(requireContext());
                                appLock.invalidateEnrollments();
                                setAppLockEnabled(false);
                                Toast.makeText(requireContext(), R.string.applock_pin_removed, Toast.LENGTH_SHORT).show();
                                row_tab_lock_postRefreshOpen();
                            }, R.string.applock_verify_to_remove);
                            break;
                    }
                }
            }
        });

        com.shahid.cam.ui.picker.PickerBottomSheetFragment sheet;
        if (showSwitch) {
            java.util.ArrayList<String> deps = new java.util.ArrayList<>();
            for (com.shahid.cam.ui.picker.OptionItem oi : items) {
                deps.add(oi.id);
            }
            sheet = com.shahid.cam.ui.picker.PickerBottomSheetFragment.newInstanceWithSwitchDependencies(
                    getString(R.string.applock_dialog_title),
                    items,
                    null,
                    resultKey,
                    helper,
                    switchTitle,
                    isEnabled,
                    deps
            );
        } else {
            sheet = com.shahid.cam.ui.picker.PickerBottomSheetFragment.newInstance(
                    getString(R.string.applock_dialog_title),
                    items,
                    null,
                    resultKey,
                    helper
            );
        }
        sheet.show(getParentFragmentManager(), "applock_sheet");
    }

    private void showCloakRecentsConfig() {
        String resultKey = "cloak_recents_sheet_result";
        String helper = getString(R.string.setting_cloak_recents_helper);
        boolean enabled = sharedPreferencesManager.isCloakRecentsEnabled();

        java.util.ArrayList<com.shahid.cam.ui.picker.OptionItem> items = new java.util.ArrayList<>();

        getParentFragmentManager().setFragmentResultListener(resultKey, this, (requestKey, bundle) -> {
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE)) {
                boolean state = bundle.getBoolean(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE);
                sharedPreferencesManager.setCloakRecentsEnabled(state);
                refreshCloakRecentsValue();
                Toast.makeText(requireContext(), state ? R.string.setting_enabled_msg : R.string.setting_disabled_msg, Toast.LENGTH_SHORT).show();
                try {
                    android.app.Activity act = requireActivity();
                    if (act instanceof MainActivity) {
                        ((MainActivity) act).applyCloakPreferenceNow(state);
                    }
                } catch (Throwable ignored) {}
            }
        });

        com.shahid.cam.ui.picker.PickerBottomSheetFragment sheet = com.shahid.cam.ui.picker.PickerBottomSheetFragment
                .newInstanceWithSwitch(getString(R.string.setting_cloak_recents_title), items, null, resultKey, helper,
                        getString(R.string.setting_cloak_recents_switch), enabled);
        sheet.show(getParentFragmentManager(), "cloak_recents_sheet");
    }

    private void showPrivacyBlackConfig() {
        String resultKey = "privacy_black_sheet_result";
        String helper = getString(R.string.privacy_black_mode_helper);
        boolean enabled = sharedPreferencesManager.isPrivacyBlackModeEnabled();

        java.util.ArrayList<com.shahid.cam.ui.picker.OptionItem> items = new java.util.ArrayList<>();
        items.add(new com.shahid.cam.ui.picker.OptionItem("exit_methods", getString(R.string.privacy_black_gestures_title), null, null, null, R.drawable.ic_arrow_right, false, false, null));

        getParentFragmentManager().setFragmentResultListener(resultKey, this, (requestKey, bundle) -> {
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE)) {
                boolean state = bundle.getBoolean(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SWITCH_STATE);
                sharedPreferencesManager.setPrivacyBlackModeEnabled(state);
                refreshPrivacyBlackModeValue();
                Toast.makeText(requireContext(), state ? R.string.setting_enabled_msg : R.string.setting_disabled_msg, Toast.LENGTH_SHORT).show();
            }
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID)) {
                String action = bundle.getString(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID);
                if ("exit_methods".equals(action)) {
                    showPrivacyBlackGesturesConfig();
                }
            }
        });

        com.shahid.cam.ui.picker.PickerBottomSheetFragment sheet = com.shahid.cam.ui.picker.PickerBottomSheetFragment
                .newInstanceWithSwitch(getString(R.string.privacy_black_mode_title), items, null, resultKey, helper,
                        getString(R.string.privacy_black_mode_enable_title), enabled, true);
        sheet.show(getParentFragmentManager(), "privacy_black_sheet");
    }

    private void showPrivacyBlackGesturesConfig() {
        String resultKey = "privacy_black_gestures_result";
        String helper = getString(R.string.privacy_black_gestures_helper);

        java.util.ArrayList<com.shahid.cam.ui.picker.OptionItem> items = new java.util.ArrayList<>();
        items.add(new com.shahid.cam.ui.picker.OptionItem("gesture_swipe", getString(R.string.privacy_black_gesture_swipe), null, null, null, null, true, sharedPreferencesManager.isPrivacyBlackSwipeUpEnabled(), null));
        items.add(new com.shahid.cam.ui.picker.OptionItem("gesture_triple", getString(R.string.privacy_black_gesture_triple), null, null, null, null, true, sharedPreferencesManager.isPrivacyBlackTripleTapEnabled(), null));
        items.add(new com.shahid.cam.ui.picker.OptionItem("gesture_long", getString(R.string.privacy_black_gesture_long), null, null, null, null, true, sharedPreferencesManager.isPrivacyBlackLongPressEnabled(), null));

        getParentFragmentManager().setFragmentResultListener(resultKey, this, (requestKey, bundle) -> {
            if (bundle.containsKey(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID)) {
                String action = bundle.getString(com.shahid.cam.ui.picker.PickerBottomSheetFragment.BUNDLE_SELECTED_ID);

                int enabledCount = 0;
                if (sharedPreferencesManager.isPrivacyBlackSwipeUpEnabled()) enabledCount++;
                if (sharedPreferencesManager.isPrivacyBlackTripleTapEnabled()) enabledCount++;
                if (sharedPreferencesManager.isPrivacyBlackLongPressEnabled()) enabledCount++;

                boolean blocked = false;
                if ("gesture_swipe".equals(action)) {
                    boolean current = sharedPreferencesManager.isPrivacyBlackSwipeUpEnabled();
                    if (current && enabledCount <= 1) {
                        blocked = true;
                    } else {
                        sharedPreferencesManager.setPrivacyBlackSwipeUpEnabled(!current);
                    }
                } else if ("gesture_triple".equals(action)) {
                    boolean current = sharedPreferencesManager.isPrivacyBlackTripleTapEnabled();
                    if (current && enabledCount <= 1) {
                        blocked = true;
                    } else {
                        sharedPreferencesManager.setPrivacyBlackTripleTapEnabled(!current);
                    }
                } else if ("gesture_long".equals(action)) {
                    boolean current = sharedPreferencesManager.isPrivacyBlackLongPressEnabled();
                    if (current && enabledCount <= 1) {
                        blocked = true;
                    } else {
                        sharedPreferencesManager.setPrivacyBlackLongPressEnabled(!current);
                    }
                }

                if (blocked) {
                    Toast.makeText(requireContext(), R.string.privacy_black_min_one_method, Toast.LENGTH_SHORT).show();
                    androidx.fragment.app.Fragment f = getParentFragmentManager().findFragmentByTag("privacy_black_gestures_sheet");
                    if (f instanceof androidx.fragment.app.DialogFragment) {
                        ((androidx.fragment.app.DialogFragment) f).dismiss();
                    }
                    showPrivacyBlackGesturesConfig();
                }
            }
        });

        com.shahid.cam.ui.picker.PickerBottomSheetFragment sheet = com.shahid.cam.ui.picker.PickerBottomSheetFragment
                .newInstance(getString(R.string.privacy_black_gestures_title), items, null, resultKey, helper, true);
        sheet.show(getParentFragmentManager(), "privacy_black_gestures_sheet");
    }

    private void row_tab_lock_postRefreshOpen() {
        refreshAppLockValue();
        row_tab_lock_postDelayed();
    }

    private void row_tab_lock_postDelayed() {
        View view = getView();
        if (view != null) {
            view.postDelayed(this::showAppLockConfigDialog, 150);
        }
    }

    private void handleEnableDisable(boolean desired) {
        boolean currently = sharedPreferencesManager.isAppLockEnabled();
        if (desired == currently) return;
        boolean isEnrolled = AppLock.isEnrolled(requireContext());
        if (desired) {
            if (isEnrolled) {
                pendingToggleDesiredState = desired;
                verifyPinThenExecute(() -> setAppLockEnabled(true), R.string.applock_verify_to_enable);
            } else {
                showPinCreationDialog(true);
            }
        } else {
            pendingToggleDesiredState = desired;
            verifyPinThenExecute(() -> setAppLockEnabled(false), R.string.applock_verify_to_disable);
        }
    }

    private void verifyPinThenExecute(Runnable action, int titleResId) {
        Toast.makeText(requireContext(), getString(titleResId), Toast.LENGTH_SHORT).show();
        new UnlockDialogBuilder(requireActivity())
                .onUnlocked(() -> {
                    if (action != null) action.run();
                    pendingToggleDesiredState = null;
                    refreshAppLockValue();
                })
                .onCanceled(() -> {
                    Toast.makeText(requireContext(), R.string.applock_verification_canceled, Toast.LENGTH_SHORT).show();
                    revertSwitchUIAfterCancel();
                })
                .show();
    }

    private void revertSwitchUIAfterCancel() {
        pendingToggleDesiredState = null;
        androidx.fragment.app.Fragment existing = getParentFragmentManager().findFragmentByTag("applock_sheet");
        if (existing instanceof com.shahid.cam.ui.picker.PickerBottomSheetFragment) {
            ((com.shahid.cam.ui.picker.PickerBottomSheetFragment) existing).dismiss();
        }
        View root = getView();
        if (root != null) {
            root.postDelayed(this::showAppLockConfigDialog, 120);
        }
    }

    private void showPinCreationDialog(boolean enableAfterCreation) {
        new LockCreationDialogBuilder(requireActivity())
                .onCanceled(() -> { })
                .onLockCreated(() -> {
                    setAppLockEnabled(true);
                    Toast.makeText(requireContext(), R.string.applock_pin_created, Toast.LENGTH_SHORT).show();
                    refreshAppLockValue();
                    row_tab_lock_postDelayed();
                })
                .show();
    }

    private void setAppLockEnabled(boolean enabled) {
        sharedPreferencesManager.setAppLockEnabled(enabled);
        String message = enabled ? getString(R.string.applock_enable) + " " + getString(R.string.universal_ok)
                : getString(R.string.applock_disable) + " " + getString(R.string.universal_ok);
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        refreshAppLockValue();
    }
}
