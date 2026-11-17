package com.shahid.cam.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.guardanis.applock.AppLock;
import com.guardanis.applock.dialogs.UnlockDialogBuilder;
import com.shahid.cam.Constants;
import com.shahid.cam.MainActivity;
import com.shahid.cam.R;
import com.shahid.cam.SharedPreferencesManager;
import com.shahid.cam.security.IntruderManager;
import com.shahid.cam.security.IntruderRecord;
import com.shahid.cam.security.IntruderStorage;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Displays all Wrong Password / Wrong PIN front-camera intruder selfies
 * stored strictly inside ShahidCam private internal storage.
 */
public class IntruderHistoryFragment extends Fragment implements IntruderHistoryAdapter.Listener {
    private static final String ARG_IS_OVERLAY = "arg_is_overlay";

    private RecyclerView recyclerView;
    private View emptyState;
    private View btnClearAll;
    private TextView tvHeaderSubtitle;
    private TextView tvStatusTitle;
    private TextView tvStatusDesc;
    private Button btnStatusAction;
    private IntruderHistoryAdapter adapter;
    private ExecutorService ioExecutor;
    private boolean isOverlay = false;
    private boolean unlockPromptShowing = false;

    private final BroadcastReceiver captureReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            reloadRecords();
        }
    };

    public static IntruderHistoryFragment newInstance(boolean isOverlay) {
        IntruderHistoryFragment f = new IntruderHistoryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_OVERLAY, isOverlay);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intruder_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ioExecutor = Executors.newSingleThreadExecutor();

        View backBtn = view.findViewById(R.id.back_button);
        isOverlay = getArguments() != null && getArguments().getBoolean(ARG_IS_OVERLAY, false);
        if (backBtn != null) {
            backBtn.setVisibility(isOverlay ? View.VISIBLE : View.GONE);
            backBtn.setOnClickListener(v -> OverlayNavUtil.dismiss(requireActivity()));
        }

        btnClearAll = view.findViewById(R.id.btn_clear_all_intruders);
        View btnSecurity = view.findViewById(R.id.btn_intruder_security_settings);
        tvHeaderSubtitle = view.findViewById(R.id.tv_intruder_header_subtitle);
        tvStatusTitle = view.findViewById(R.id.tv_intruder_status_title);
        tvStatusDesc = view.findViewById(R.id.tv_intruder_status_desc);
        btnStatusAction = view.findViewById(R.id.btn_intruder_status_action);
        emptyState = view.findViewById(R.id.layout_intruder_empty);
        recyclerView = view.findViewById(R.id.recycler_intruder_history);

        int spanCount = getResources().getConfiguration().screenWidthDp >= 600 ? 3 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        adapter = new IntruderHistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        View.OnClickListener openSecurity = v -> openSecuritySettings();
        if (btnSecurity != null) btnSecurity.setOnClickListener(openSecurity);
        if (btnStatusAction != null) btnStatusAction.setOnClickListener(openSecurity);
        if (btnClearAll != null) btnClearAll.setOnClickListener(v -> confirmClearAll());

        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                captureReceiver, new IntentFilter(Constants.BROADCAST_ON_INTRUDER_CAPTURED));

        refreshStatusCard();
        checkAppLockAndLoad();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatusCard();
        checkAppLockAndLoad();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded()) {
            refreshStatusCard();
            checkAppLockAndLoad();
        }
    }

    public void onFragmentBecameVisible() {
        if (isAdded()) {
            refreshStatusCard();
            checkAppLockAndLoad();
        }
    }

    private void checkAppLockAndLoad() {
        if (!isAdded()) return;
        SharedPreferencesManager spm = SharedPreferencesManager.getInstance(requireContext());
        boolean lockRequired = spm.isAppLockEnabled()
                && !spm.isAppLockSessionUnlocked()
                && AppLock.isEnrolled(requireContext());
        if (!lockRequired) {
            if (recyclerView != null) recyclerView.setAlpha(1f);
            reloadRecords();
            return;
        }
        if (unlockPromptShowing) return;
        unlockPromptShowing = true;
        if (recyclerView != null) recyclerView.setVisibility(View.INVISIBLE);
        if (emptyState != null) emptyState.setVisibility(View.GONE);
        new UnlockDialogBuilder(requireActivity())
                .onUnlocked(() -> {
                    unlockPromptShowing = false;
                    spm.setAppLockSessionUnlocked(true);
                    if (recyclerView != null) recyclerView.setAlpha(1f);
                    reloadRecords();
                })
                .onCanceled(() -> {
                    unlockPromptShowing = false;
                    if (!isAdded()) return;
                    if (isOverlay) {
                        OverlayNavUtil.dismiss(requireActivity());
                    } else if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).switchFragment(0, true);
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(captureReceiver);
        } catch (Exception ignored) {
        }
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
            ioExecutor = null;
        }
    }

    private void openSecuritySettings() {
        if (!isAdded()) return;
        OverlayNavUtil.show(requireActivity(), new SecuritySettingsFragment(), "SecuritySettingsFragment");
    }

    private void refreshStatusCard() {
        if (!isAdded()) return;
        Context ctx = requireContext();
        IntruderManager manager = IntruderManager.getInstance(ctx);
        boolean hasFrontCam = IntruderManager.hasFrontCamera(ctx);
        boolean hasPerm = IntruderManager.hasCameraPermission(ctx);
        boolean enabled = manager.isFeatureEnabled();
        boolean lockReady = SharedPreferencesManager.getInstance(ctx).isAppLockEnabled() && AppLock.isEnrolled(ctx);

        if (!hasFrontCam) {
            tvStatusTitle.setText(R.string.intruder_status_no_camera);
            tvStatusDesc.setText(R.string.intruder_status_no_camera_desc);
            btnStatusAction.setVisibility(View.GONE);
        } else if (enabled && !hasPerm) {
            tvStatusTitle.setText(R.string.intruder_status_perm_needed);
            tvStatusDesc.setText(R.string.intruder_status_perm_needed_desc);
            btnStatusAction.setVisibility(View.VISIBLE);
            btnStatusAction.setText(R.string.intruder_configure_btn);
        } else if (enabled && !lockReady) {
            tvStatusTitle.setText(R.string.intruder_status_applock_needed);
            tvStatusDesc.setText(R.string.intruder_status_applock_needed_desc);
            btnStatusAction.setVisibility(View.VISIBLE);
            btnStatusAction.setText(R.string.intruder_configure_btn);
        } else if (enabled) {
            tvStatusTitle.setText(R.string.intruder_status_active);
            tvStatusDesc.setText(R.string.intruder_local_storage_note);
            btnStatusAction.setVisibility(View.VISIBLE);
            btnStatusAction.setText(R.string.intruder_manage_btn);
        } else {
            tvStatusTitle.setText(R.string.intruder_status_disabled);
            tvStatusDesc.setText(R.string.intruder_status_disabled_desc);
            btnStatusAction.setVisibility(View.VISIBLE);
            btnStatusAction.setText(R.string.intruder_enable_btn);
        }
    }

    public void reloadRecords() {
        if (!isAdded() || ioExecutor == null || ioExecutor.isShutdown()) return;
        SharedPreferencesManager spm = SharedPreferencesManager.getInstance(requireContext());
        if (spm.isAppLockEnabled() && !spm.isAppLockSessionUnlocked() && AppLock.isEnrolled(requireContext())) {
            return;
        }
        final Context appCtx = requireContext().getApplicationContext();
        ioExecutor.execute(() -> {
            List<IntruderRecord> records = IntruderStorage.listRecords(appCtx);
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.submitList(records);
                boolean empty = records.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                if (btnClearAll != null) {
                    btnClearAll.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
                if (tvHeaderSubtitle != null) {
                    tvHeaderSubtitle.setText(empty
                            ? getString(R.string.intruder_history_subtitle)
                            : getResources().getQuantityString(
                                    R.plurals.intruder_count_format, records.size(), records.size()));
                }
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshIntruderNavBadge();
                }
            });
        });
    }

    @Override
    public void onViewRecord(@NonNull IntruderRecord record) {
        if (!isAdded()) return;
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_intruder_viewer, null, false);
        ImageView ivFull = dialogView.findViewById(R.id.iv_intruder_full);
        TextView tvReason = dialogView.findViewById(R.id.tv_viewer_reason);
        TextView tvTime = dialogView.findViewById(R.id.tv_viewer_time);
        View btnDelete = dialogView.findViewById(R.id.btn_viewer_delete);
        View btnClose = dialogView.findViewById(R.id.btn_viewer_close);

        tvReason.setText(record.reason);
        tvTime.setText(record.getFormattedDateTime());
        Glide.with(requireContext()).load(record.getFile()).fitCenter().into(ivFull);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            onDeleteRecord(record);
        });
        dialog.show();
    }

    @Override
    public void onDeleteRecord(@NonNull IntruderRecord record) {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.intruder_delete_single_title)
                .setMessage(R.string.intruder_delete_single_msg)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    boolean removed = IntruderStorage.deleteRecord(requireContext(), record.filePath);
                    Toast.makeText(requireContext(),
                            removed ? R.string.intruder_deleted_toast : R.string.intruder_delete_failed_toast,
                            Toast.LENGTH_SHORT).show();
                    reloadRecords();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmClearAll() {
        if (!isAdded()) return;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.intruder_clear_all)
                .setMessage(R.string.intruder_clear_all_msg)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    IntruderStorage.clearAll(requireContext());
                    Toast.makeText(requireContext(), R.string.intruder_cleared_toast, Toast.LENGTH_SHORT).show();
                    reloadRecords();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
