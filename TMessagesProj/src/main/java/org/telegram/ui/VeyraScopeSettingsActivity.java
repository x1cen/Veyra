package org.telegram.ui;

import org.telegram.ui.ActionBar.AlertDialog;
import org.veyra.client.VeyraScope;

import java.util.ArrayList;
import java.util.List;

// Veyra: advanced per-feature scope screen, shared by Ghost read-hiding, anti-delete and
// edit/reaction history. Each feature passes its scope id; the screen edits that VeyraScope
// instance: master limit switch, per-chat-type toggles, skip own/admin, and one Included
// Chats list whose ticks start from Chat Types and can be changed per chat.
public class VeyraScopeSettingsActivity extends VeyraSettingsBaseActivity {

    private final int scopeId;
    private final VeyraScope scope;

    public VeyraScopeSettingsActivity(int scopeId) {
        this.scopeId = scopeId;
        this.scope = VeyraScope.byId(scopeId);
    }

    @Override
    protected String getScreenTitle() {
        switch (scopeId) {
            case VeyraScope.SCOPE_GHOST_READ: return "Read Receipt Scope";
            case VeyraScope.SCOPE_ANTI_DELETE: return "Anti-Delete Scope";
            case VeyraScope.SCOPE_HISTORY: return "History Scope";
            default: return "Scope";
        }
    }

    private String actionVerb() {
        switch (scopeId) {
            case VeyraScope.SCOPE_GHOST_READ: return "Read receipts are hidden";
            case VeyraScope.SCOPE_ANTI_DELETE: return "Deleted messages are kept";
            case VeyraScope.SCOPE_HISTORY: return "Edits and reactions are logged";
            default: return "The feature applies";
        }
    }

    @Override
    protected List<VeyraSettingsRow> buildRows() {
        List<VeyraSettingsRow> r = new ArrayList<>();
        if (scope == null) {
            return r;
        }

        r.add(VeyraSettingsRow.header("Scope"));
        r.add(VeyraSettingsRow.toggle(
                "Limit Scope",
                actionVerb() + " everywhere when off. Turn on to restrict to the chats ticked in Included Chats",
                () -> scope.limitEnabled,
                val -> { scope.setLimitEnabled(val); reloadRows(); },
                false
        ));
        r.add(VeyraSettingsRow.shadow());

        String small = "Up to " + VeyraScope.LARGE_GROUP_MIN_MEMBERS + " members (shows a Seen list)";
        String large = "Over " + VeyraScope.LARGE_GROUP_MIN_MEMBERS + " members (no Seen list)";

        r.add(VeyraSettingsRow.header("Chats"));
        r.add(opt("Private Chats", null, VeyraScope.OPT_PRIVATE, false));
        r.add(opt("Bots", null, VeyraScope.OPT_BOTS, false));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Groups"));
        r.add(opt("Private Groups", "Groups without a public link", VeyraScope.OPT_PRIVATE_GROUPS, true));
        r.add(subOpt("Small Groups", small, VeyraScope.OPT_PRIVATE_GROUPS_SMALL, VeyraScope.OPT_PRIVATE_GROUPS));
        r.add(subOpt("Large Groups", large, VeyraScope.OPT_PRIVATE_GROUPS_LARGE, VeyraScope.OPT_PRIVATE_GROUPS));
        r.add(opt("Public Groups", "Groups with a public @username link", VeyraScope.OPT_PUBLIC_GROUPS, true));
        r.add(subOpt("Small Groups", small, VeyraScope.OPT_PUBLIC_GROUPS_SMALL, VeyraScope.OPT_PUBLIC_GROUPS));
        r.add(subOpt("Large Groups", large, VeyraScope.OPT_PUBLIC_GROUPS_LARGE, VeyraScope.OPT_PUBLIC_GROUPS));
        r.add(opt("Skip Groups I Own or Admin", "Untick groups where you're owner or admin by default", VeyraScope.OPT_SKIP_GROUPS_OWN_ADMIN, false));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Channels"));
        r.add(opt("Private Channels", "Channels without a public link", VeyraScope.OPT_PRIVATE_CHANNELS, false));
        r.add(opt("Public Channels", "Channels with a public @username link", VeyraScope.OPT_PUBLIC_CHANNELS, false));
        r.add(opt("Skip Channels I Own or Admin", "Untick channels where you're owner or admin by default", VeyraScope.OPT_SKIP_CHANNELS_OWN_ADMIN, false));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Included Chats"));
        r.add(VeyraSettingsRow.detail(
                "Included Chats",
                () -> {
                    int n = scope.getOverrideCount();
                    if (n == 0) return "Follows Chat Types";
                    if (n == 1) return "1 manual change";
                    return n + " manual changes";
                },
                true,
                () -> presentFragment(new VeyraIncludedChatsActivity(scopeId))
        ).enabledWhen(() -> scope.limitEnabled));
        r.add(VeyraSettingsRow.button(
                "Sync with Chat Types",
                false,
                false,
                this::showSyncAlert
        ).enabledWhen(() -> scope.limitEnabled && scope.getOverrideCount() > 0));
        r.add(VeyraSettingsRow.shadow());
        return r;
    }

    // Top-level option. Parents reload rows so their sub-options enable/disable right away.
    private VeyraSettingsRow opt(String title, String subtitle, String key, boolean isParent) {
        return VeyraSettingsRow.toggle(
                title, subtitle,
                () -> scope.isOn(key),
                val -> { scope.setOn(key, val); if (isParent) reloadRows(); },
                true
        ).enabledWhen(() -> scope.limitEnabled);
    }

    // Sub-option: only tickable while its parent option is on.
    private VeyraSettingsRow subOpt(String title, String subtitle, String key, String parentKey) {
        return VeyraSettingsRow.toggle(
                title, subtitle,
                () -> scope.isOn(key),
                val -> scope.setOn(key, val),
                true
        ).enabledWhen(() -> scope.limitEnabled && scope.isOn(parentKey));
    }

    private void showSyncAlert() {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Sync with Chat Types");
        builder.setMessage("Remove all manual changes? Every chat will be ticked again based on Chat Types.");
        builder.setPositiveButton("Sync", (dialog, which) -> {
            scope.clearOverrides();
            reloadRows();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }
}
