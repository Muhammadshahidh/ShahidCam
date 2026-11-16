package com.shahid.cam.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shahid.cam.FLog;
import com.shahid.cam.R;
import com.shahid.cam.services.UpdateCheckService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AboutFragment extends BaseFragment {

    private static final String DEVELOPER_EMAIL = "mshahidwrites@gmail.com";
    private View view;
    private ExecutorService executorService;
    private AlertDialog loadingDialog;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_about, container, false);
        executorService = Executors.newSingleThreadExecutor();
        initializeViews();
        return view;
    }

    private void initializeViews() {
        View back = view.findViewById(R.id.back_button);
        if (back != null) {
            back.setOnClickListener(v -> OverlayNavUtil.dismiss(requireActivity()));
        }

        ImageView appIcon = view.findViewById(R.id.app_icon);
        TextView appVersion = view.findViewById(R.id.app_version);
        View checkUpdatesButton = view.findViewById(R.id.check_updates_button);
        View emailRow = view.findViewById(R.id.email_row);
        LinearLayout privacyInfoCard = view.findViewById(R.id.privacy_info_card);
        ScrollView scrollView = view.findViewById(R.id.scroll_view);

        if (appIcon != null) {
            appIcon.setImageResource(R.drawable.ic_shahidcam_avatar);
        }

        if (appVersion != null) {
            try {
                PackageManager pm = requireContext().getPackageManager();
                String packageName = requireContext().getPackageName();
                PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
                String versionName = packageInfo.versionName;
                long versionCode;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    versionCode = packageInfo.getLongVersionCode();
                } else {
                    @SuppressWarnings("deprecation")
                    int legacyCode = packageInfo.versionCode;
                    versionCode = legacyCode;
                }
                appVersion.setText("v" + versionName + " (" + versionCode + ")  •  " + packageName);
            } catch (Exception e) {
                appVersion.setText("v4.0.0");
            }
        }

        if (checkUpdatesButton != null) {
            checkUpdatesButton.setOnClickListener(v -> checkForUpdates());
        }
        if (emailRow != null) {
            emailRow.setOnClickListener(v -> sendEmail());
        }
        if (privacyInfoCard != null && scrollView != null) {
            setupPrivacyInfo(privacyInfoCard, scrollView);
        }
    }

    private void setupPrivacyInfo(LinearLayout cardView, ScrollView scrollView) {
        String[] questions = getResources().getStringArray(R.array.questions_array);
        String[] answers = getResources().getStringArray(R.array.answers_array);

        StringBuilder qnaContent = new StringBuilder();
        int count = Math.min(questions.length, answers.length);
        for (int i = 0; i < count; i++) {
            qnaContent.append("<b><font color='#0F172A'>").append(questions[i]).append("</font></b><br>")
                    .append("<font color='#475569'>").append(answers[i]).append("</font><br><br>");
        }
        TextView privacyInfoContent = cardView.findViewById(R.id.privacy_info_content);
        if (privacyInfoContent == null) return;
        privacyInfoContent.setText(Html.fromHtml(qnaContent.toString(), Html.FROM_HTML_MODE_LEGACY));

        ImageView expandIcon = cardView.findViewById(R.id.expand_icon);
        LinearLayout headerLayout = cardView.findViewById(R.id.privacy_info_header);
        if (headerLayout == null) return;

        headerLayout.setOnClickListener(v -> {
            boolean isVisible = privacyInfoContent.getVisibility() == View.VISIBLE;
            if (!isVisible) {
                privacyInfoContent.setVisibility(View.VISIBLE);
            }

            privacyInfoContent.measure(
                    View.MeasureSpec.makeMeasureSpec(Math.max(1, cardView.getWidth() - cardView.getPaddingStart() - cardView.getPaddingEnd()), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            );
            int startHeight = isVisible ? privacyInfoContent.getHeight() : 0;
            int endHeight = isVisible ? 0 : privacyInfoContent.getMeasuredHeight();

            ValueAnimator heightAnimator = ValueAnimator.ofInt(startHeight, endHeight);
            heightAnimator.addUpdateListener(animation -> {
                privacyInfoContent.getLayoutParams().height = (int) animation.getAnimatedValue();
                privacyInfoContent.requestLayout();
            });
            heightAnimator.setDuration(260);
            heightAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (!isVisible) {
                        scrollView.postDelayed(() -> scrollView.smoothScrollTo(0, cardView.getTop()), 220);
                    }
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (isVisible) {
                        privacyInfoContent.setVisibility(View.GONE);
                    } else {
                        privacyInfoContent.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                    }
                }
            });
            heightAnimator.start();

            if (expandIcon != null) {
                ObjectAnimator.ofFloat(expandIcon, "rotation", isVisible ? 180f : 0f, isVisible ? 0f : 180f)
                        .setDuration(260)
                        .start();
            }
        });
    }

    private String getAppVersionForUpdates() {
        try {
            PackageManager pm = requireActivity().getPackageManager();
            PackageInfo pInfo = pm.getPackageInfo(requireActivity().getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "4.0.0";
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.about_update_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void sendEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + DEVELOPER_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_shahidcam_feedback));
        try {
            startActivity(intent);
        } catch (Exception e) {
            ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Developer Email", DEVELOPER_EMAIL));
            }
            Toast.makeText(requireContext(), getString(R.string.about_email_copied, DEVELOPER_EMAIL), Toast.LENGTH_LONG).show();
        }
    }

    private void checkForUpdates() {
        showLoadingDialog(getString(R.string.up_to_date_loading));
        ExecutorService updateExecutor = (executorService != null && !executorService.isShutdown())
                ? executorService : Executors.newSingleThreadExecutor();
        updateExecutor.execute(() -> {
            try {
                String currentVersion = getAppVersionForUpdates();
                UpdateCheckService.UpdateCheckResult result = UpdateCheckService.checkForUpdate(currentVersion);
                FLog.d("UpdateCheck", "About check: current=" + currentVersion
                        + " stable=" + result.stableVersion + "(" + result.hasStable + ")"
                        + " beta=" + result.betaVersion + "(" + result.hasBeta + ")"
                        + " error=" + result.errorOccurred);

                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    dismissLoadingDialog();
                    if (result.errorOccurred) {
                        showErrorDialog(getString(R.string.about_update_failed));
                    } else if (com.shahid.cam.BuildConfig.APPLICATION_ID.endsWith(".beta") && result.hasBeta) {
                        BetaUpdateBottomSheet.newInstance(result.betaVersion, result.betaUrl, currentVersion)
                                .show(getParentFragmentManager(), "BetaUpdateSheet");
                    } else if (result.hasStable) {
                        UpdateAvailableBottomSheet.newInstance(result.stableVersion, "", result.stableUrl)
                                .show(getParentFragmentManager(), "UpdateAvailableBottomSheet");
                    } else if (result.hasBeta) {
                        showUpToDateDialog(getString(R.string.about_beta_available, result.betaVersion), result.betaUrl);
                    } else {
                        showUpToDateDialog("", null);
                    }
                });
            } catch (Exception e) {
                if (!isAdded() || getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    dismissLoadingDialog();
                    showErrorDialog(getString(R.string.about_update_failed));
                });
            }
        });
    }

    private void showLoadingDialog(String message) {
        if (!isAdded()) return;
        TextView messageView = new TextView(requireContext());
        messageView.setText(message);
        messageView.setPadding(48, 36, 48, 36);
        messageView.setTextSize(15);
        loadingDialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(messageView)
                .setCancelable(false)
                .create();
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void showUpToDateDialog(@Nullable String extraMessage, @Nullable String betaUrl) {
        String msg = getString(R.string.up_to_date_description);
        if (extraMessage != null && !extraMessage.isEmpty()) {
            msg += extraMessage;
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.up_to_date))
                .setMessage(msg)
                .setPositiveButton(getString(R.string.about_dialog_ok), null);
        if (betaUrl != null && !betaUrl.isEmpty()) {
            builder.setNeutralButton(getString(R.string.about_get_beta), (d, w) -> openUrl(betaUrl));
        }
        builder.show();
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.about_dialog_error))
                .setMessage(message)
                .setPositiveButton(getString(R.string.about_dialog_ok), null)
                .show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
