package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.messenger.SharedConfig;
import org.veyra.client.proxy.ProxyDiscoveryConfig;
import org.veyra.client.proxy.ProxyDiscoveryPipeline;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProxyDiscoveryActivity extends VeyraSettingsBaseActivity {

    private final ProxyDiscoveryPipeline.PipelineListener pipelineListener = new ProxyDiscoveryPipeline.PipelineListener() {
        @Override
        public void onPipelineStarted() {
            AndroidUtilities.runOnUIThread(() -> reloadRows());
        }

        @Override
        public void onTournamentProgress(String status, int currentRound, int totalCandidates) {
            AndroidUtilities.runOnUIThread(() -> reloadRows());
        }

        @Override
        public void onPipelineFinished(SharedConfig.ProxyInfo winner, int inactiveCount) {
            AndroidUtilities.runOnUIThread(() -> {
                reloadRows();
                if (getParentActivity() != null) {
                    String msg = winner != null ? "Champion selected: " + winner.settings.getAddress() : "Discovery finished";
                    BulletinFactory.of(ProxyDiscoveryActivity.this).createSimpleBulletin(R.raw.contact_check, msg).show();
                }
            });
        }
    };

    @Override
    public boolean onFragmentCreate() {
        ProxyDiscoveryPipeline.getInstance().addListener(pipelineListener);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        ProxyDiscoveryPipeline.getInstance().removeListener(pipelineListener);
        super.onFragmentDestroy();
    }

    @Override
    protected String getScreenTitle() {
        return "Auto Discovery";
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();

        // 1. Status Section
        r.add(VeyraSettingsRow.header("Tournament & Selection"));

        boolean isRunning = ProxyDiscoveryPipeline.getInstance().isRunning();
        String sub = isRunning ? "Running P2C tournament..." :
                (ProxyDiscoveryConfig.isEnabled() ? "Enabled • Scheduled check" : "Disabled");

        r.add(VeyraSettingsRow.toggle(
                "Enable Auto Discovery",
                sub,
                ProxyDiscoveryConfig::isEnabled,
                isChecked -> {
                    if (isChecked && (SharedConfig.currentProxy == null || org.telegram.ui.ProxyListActivity.isAetherProxy(SharedConfig.currentProxy))) {
                        new AlertDialog.Builder(getParentActivity())
                                .setTitle(LocaleController.getString("AppName", R.string.AppName))
                                .setMessage("Auto Discovery only applies to regular Telegram proxies.\nPlease select a proxy from the main proxy pool first.")
                                .setPositiveButton(LocaleController.getString("OK", R.string.OK), null)
                                .show();
                        reloadRows();
                        return;
                    }
                    ProxyDiscoveryConfig.setEnabled(isChecked);
                    if (isChecked) {
                        ProxyDiscoveryPipeline.getInstance().runTournament(false);
                    }
                    reloadRows();
                },
                true
        ));

        r.add(VeyraSettingsRow.detail(
                "Run Tournament Now",
                () -> isRunning ? "In Progress..." : "Start P2C Selection",
                false,
                () -> {
                    if (SharedConfig.currentProxy == null || org.telegram.ui.ProxyListActivity.isAetherProxy(SharedConfig.currentProxy)) {
                        new AlertDialog.Builder(getParentActivity())
                                .setTitle(LocaleController.getString("AppName", R.string.AppName))
                                .setMessage("Auto Discovery only applies to regular Telegram proxies.\nPlease select a proxy from the main proxy pool first.")
                                .setPositiveButton(LocaleController.getString("OK", R.string.OK), null)
                                .show();
                        return;
                    }
                    if (!isRunning) {
                        ProxyDiscoveryPipeline.getInstance().runTournament(true);
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Starting P2C tournament...").show();
                        reloadRows();
                    }
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        // 2. Selection & Optimization Strategy
        r.add(VeyraSettingsRow.header("Selection Strategy"));

        int currentStrategy = ProxyDiscoveryConfig.getStrategy();
        String strategyName;
        if (currentStrategy == ProxyDiscoveryConfig.STRATEGY_LOWEST_LATENCY) {
            strategyName = "Lowest Latency (Fastest Ping)";
        } else if (currentStrategy == ProxyDiscoveryConfig.STRATEGY_STABILITY) {
            strategyName = "Maximum Stability (Reliable Handshake)";
        } else {
            strategyName = "Balanced P2C Tournament";
        }

        r.add(VeyraSettingsRow.detail(
                "Optimization Mode",
                () -> strategyName,
                false,
                this::showStrategyPicker
        ));

        r.add(VeyraSettingsRow.toggle(
                "Auto Switch on Failure",
                "Automatically switch to best standby proxy when connection drops",
                ProxyDiscoveryConfig::isAutoSwitchOnFailure,
                isChecked -> {
                    ProxyDiscoveryConfig.setAutoSwitchOnFailure(isChecked);
                    reloadRows();
                },
                true
        ));

        r.add(VeyraSettingsRow.toggle(
                "Throughput Speed Test",
                "Evaluate bandwidth speed in addition to ping",
                ProxyDiscoveryConfig::isSpeedTestEnabled,
                isChecked -> {
                    ProxyDiscoveryConfig.setSpeedTestEnabled(isChecked);
                    reloadRows();
                },
                true
        ));

        int interval = ProxyDiscoveryConfig.getIntervalMinutes();
        r.add(VeyraSettingsRow.detail(
                "Check Interval",
                () -> interval + " minutes",
                false,
                this::showIntervalPicker
        ));

        int timeoutMs = ProxyDiscoveryConfig.getTimeoutMs();
        r.add(VeyraSettingsRow.detail(
                "Probe Timeout",
                () -> (timeoutMs / 1000.0f) + " seconds",
                false,
                this::showTimeoutPicker
        ));

        r.add(VeyraSettingsRow.shadow());

        // 3. History & Stats
        r.add(VeyraSettingsRow.header("Statistics"));

        String winner = ProxyDiscoveryConfig.getLastWinner();
        r.add(VeyraSettingsRow.detail(
                "Last Active Winner",
                () -> TextUtils.isEmpty(winner) ? "None" : winner,
                true,
                null
        ));

        r.add(VeyraSettingsRow.detail(
                "Total Proxies",
                () -> String.valueOf(SharedConfig.proxyList.size()),
                true,
                null
        ));

        long lastRun = ProxyDiscoveryConfig.getLastRunTime();
        String timeStr = lastRun > 0 ? new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(lastRun)) : "Never";
        r.add(VeyraSettingsRow.detail(
                "Last Run Time",
                () -> timeStr,
                false,
                null
        ));

        return r;
    }

    private void showStrategyPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Optimization Mode");
        String[] options = new String[]{
                "Lowest Latency (Fastest Ping)",
                "Maximum Stability (Reliable Handshake)",
                "Balanced P2C Tournament (Default)"
        };
        builder.setItems(options, (d, which) -> {
            ProxyDiscoveryConfig.setStrategy(which);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showTimeoutPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Probe Timeout");
        int[] timeouts = new int[]{1500, 2500, 4000};
        String[] options = new String[]{
                "1.5 seconds (Fast)",
                "2.5 seconds (Default)",
                "4.0 seconds (Relaxed)"
        };
        builder.setItems(options, (d, which) -> {
            ProxyDiscoveryConfig.setTimeoutMs(timeouts[which]);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showIntervalPicker() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Check Interval");
        int[] minutes = new int[]{5, 10, 15, 30, 60};
        String[] options = new String[]{
                "5 minutes",
                "10 minutes",
                "15 minutes (Default)",
                "30 minutes",
                "60 minutes"
        };
        builder.setItems(options, (d, which) -> {
            ProxyDiscoveryConfig.setIntervalMinutes(minutes[which]);
            reloadRows();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }
}
