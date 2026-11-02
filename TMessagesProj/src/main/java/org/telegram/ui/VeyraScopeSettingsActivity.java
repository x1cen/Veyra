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

        r.add(VeyraSettingsRow.header("Chat Types"));
        r.add(typeToggle("Private Chats", null, VeyraScope.PEER_PRIVATE, true));
        r.add(typeToggle("Small Groups", "Up to " + VeyraScope.LARGE_GROUP_MIN_MEMBERS + " members (shows a Seen list)", VeyraScope.PEER_SMALL_GROUP, true));
        r.add(typeToggle("Large Groups", "Over " + VeyraScope.LARGE_GROUP_MIN_MEMBERS + " members (no Seen list)", VeyraScope.PEER_LARGE_GROUP, true));
        r.add(typeToggle("Channels", null, VeyraScope.PEER_CHANNEL, true));
        r.add(typeToggle("Bots", null, VeyraScope.PEER_BOT, true));
        r.add(VeyraSettingsRow.toggle(
                "Skip Chats I Own or Admin",
                "Untick groups and channels where you're owner or admin by default",
                () -> scope.skipOwnAdmin,
                val -> scope.setSkipOwnAdmin(val),
                false
        ).enabledWhen(() -> scope.limitEnabled));
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

    private VeyraSettingsRow typeToggle(String title, String subtitle, int peerType, boolean divider) {
        return VeyraSettingsRow.toggle(
                title, subtitle,
                () -> scope.isPeerEnabled(peerType),
                val -> scope.setPeerEnabled(peerType, val),
                divider
        ).enabledWhen(() -> scope.limitEnabled);
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
