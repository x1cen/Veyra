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
        public void onPipelineFinished(SharedConfig.ProxyInfo winner, int prunedCount) {
            AndroidUtilities.runOnUIThread(() -> {
                reloadRows();
                if (getParentActivity() != null) {
                    String msg = winner != null ? "Champion selected: " + winner.settings.getAddress() : "Discovery finished";
                    if (prunedCount > 0) {
                        msg += " (" + prunedCount + " dead pruned)";
                    }
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
                    if (!isRunning) {
                        ProxyDiscoveryPipeline.getInstance().runTournament(true);
                        BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Starting P2C tournament...").show();
                        reloadRows();
                    }
                }
        ));

        r.add(VeyraSettingsRow.shadow());

        // 2. Pipeline Rules
        r.add(VeyraSettingsRow.header("Quality Pipeline"));

        r.add(VeyraSettingsRow.toggle(
                "Prune Dead Proxies",
                "Automatically remove unreachable proxies",
                ProxyDiscoveryConfig::isPruneDeadEnabled,
                isChecked -> {
                    ProxyDiscoveryConfig.setPruneDeadEnabled(isChecked);
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

        int pruned = ProxyDiscoveryConfig.getLastPrunedCount();
        r.add(VeyraSettingsRow.detail(
                "Dead Proxies Pruned",
                () -> String.valueOf(pruned),
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
