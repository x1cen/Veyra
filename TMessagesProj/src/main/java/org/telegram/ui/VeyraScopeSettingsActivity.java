package org.telegram.ui;

import org.veyra.client.VeyraScope;

import java.util.ArrayList;
import java.util.List;

// Veyra: advanced per-feature scope screen, shared by Ghost read-hiding, anti-delete and
// edit/reaction history. Each feature passes its scope id; the screen edits that VeyraScope
// instance: master limit switch, per-chat-type toggles, skip own/admin, and the mutually
// exclusive Always Include / Always Exclude chat lists (picked via the native folder picker).
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
                actionVerb() + " everywhere when off. Turn on to restrict by the chat types and lists below",
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
        r.add(typeToggle("Bots", null, VeyraScope.PEER_BOT, false));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Owner & Admin"));
        r.add(VeyraSettingsRow.toggle(
                "Skip Chats I Own or Admin",
                "Don't apply in groups and channels where you're owner or admin",
                () -> scope.skipOwnAdmin,
                val -> scope.setSkipOwnAdmin(val),
                false
        ).enabledWhen(() -> scope.limitEnabled));
        r.add(VeyraSettingsRow.shadow());

        r.add(VeyraSettingsRow.header("Exceptions"));
        r.add(VeyraSettingsRow.detail(
                "Always Include",
                () -> countLabel(scope.getIncluded().size()),
                true,
                () -> openPicker(true)
        ).enabledWhen(() -> scope.limitEnabled));
        r.add(VeyraSettingsRow.detail(
                "Always Exclude",
                () -> countLabel(scope.getExcluded().size()),
                false,
                () -> openPicker(false)
        ).enabledWhen(() -> scope.limitEnabled));
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

    private String countLabel(int n) {
        if (n == 0) return "None";
        if (n == 1) return "1 chat";
        return n + " chats";
    }

    // A chat added to one list is automatically dropped from the other (VeyraScope enforces it),
    // so a chat can never sit in both Include and Exclude at once.
    private void openPicker(boolean include) {
        ArrayList<Long> current = include ? scope.getIncluded() : scope.getExcluded();
        UsersSelectActivity fragment = new UsersSelectActivity(include, current, 0);
        fragment.noChatTypes = true;
        fragment.setDelegate((ids, flags) -> {
            if (include) {
                scope.setIncluded(ids);
            } else {
                scope.setExcluded(ids);
            }
            reloadRows();
        });
        presentFragment(fragment);
    }
}
