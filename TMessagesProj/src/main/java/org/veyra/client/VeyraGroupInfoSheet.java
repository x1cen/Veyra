/* Veyra Project — Group/Channel Info Sheet
 * Shows real data received from Telegram servers,
 * gracefully hiding admin-only fields when not available.
 */

package org.veyra.client;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class VeyraGroupInfoSheet extends BottomSheet {

    public VeyraGroupInfoSheet(Context context, TLRPC.Chat chat, TLRPC.ChatFull chatFull, int currentAccount) {
        super(context, false);
        setApplyTopPadding(true);
        setApplyBottomPadding(true);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(20));

        // Title
        TextView title = new TextView(context);
        title.setText(chat != null ? (chat.title != null ? chat.title : "Group Info") : "Chat Info");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(16));
        container.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        boolean isChannel = ChatObject.isChannel(chat);
        boolean isMegagroup = chat != null && chat.megagroup;
        boolean isAdmin = ChatObject.hasAdminRights(chat);
        boolean canViewParticipants = chatFull != null && chatFull.can_view_participants;

        // ── General info (always visible) ───────────────────────────────
        addSection(container, context, "General");

        if (chat != null) {
            String chatType;
            if (isChannel && !isMegagroup) chatType = "Channel";
            else if (isMegagroup) chatType = "Supergroup (Megagroup)";
            else if (isChannel) chatType = "Channel/Group";
            else chatType = "Group";
            addRow(container, context, "Type", chatType, false);

            // ID
            addRow(container, context, "ID", String.valueOf(chat.id), false);

            // Username
            if (!TextUtils.isEmpty(chat.username)) {
                addRow(container, context, "Username", "@" + chat.username, false);
            }

            // Date created
            if (chat.date > 0) {
                addRow(container, context, "Created", formatDate(chat.date), false);
            }

            // Verified / Scam / Fake
            if (chat.verified) addRow(container, context, "Verified", "Yes", false);
            if (chat.scam) addRow(container, context, "Scam", "Yes", true);
            if (chat.fake) addRow(container, context, "Fake", "Yes", true);

            // Restricted
            if (chat.restricted && chat.restriction_reason != null && !chat.restriction_reason.isEmpty()) {
                addRow(container, context, "Restricted", chat.restriction_reason.get(0).reason, true);
            }

            // Forwarding restriction
            if (chat.noforwards) addRow(container, context, "No Forwards", "Enabled", false);

            // Join to send
            if (chat.join_to_send) addRow(container, context, "Join to Send", "Required", false);

            // Join request approval
            if (chat.join_request) addRow(container, context, "Join Requests", "Approval required", false);
        }

        // ── About (from ChatFull — available to all members) ────────────
        if (chatFull != null && !TextUtils.isEmpty(chatFull.about)) {
            addSection(container, context, "About");
            addMultilineRow(container, context, chatFull.about);
        }

        // ── Members (from ChatFull) ──────────────────────────────────────
        addSection(container, context, "Members");

        if (chatFull != null) {
            if (chatFull.participants_count > 0) {
                addRow(container, context, "Total Members", String.valueOf(chatFull.participants_count), false);
            }
            if (chatFull.online_count > 0) {
                addRow(container, context, "Online Now", String.valueOf(chatFull.online_count), false);
            }
            if (chatFull.participants_hidden) {
                addRow(container, context, "Participants", "Hidden by admin", false);
            }
            if (canViewParticipants) {
                // Only shown when server grants it
                if (chatFull.admins_count > 0) {
                    addRow(container, context, "Admins", String.valueOf(chatFull.admins_count), false);
                }
                if (chatFull.kicked_count > 0) {
                    addRow(container, context, "Removed Users", String.valueOf(chatFull.kicked_count), false);
                }
                if (chatFull.banned_count > 0) {
                    addRow(container, context, "Restricted Users", String.valueOf(chatFull.banned_count), false);
                }
            }
            if (chatFull.requests_pending > 0) {
                addRow(container, context, "Pending Join Requests", String.valueOf(chatFull.requests_pending), false);
            }
        } else if (chat != null) {
            // Fallback to chat object participants_count if chatFull not loaded
            TLRPC.Chat freshChat = MessagesController.getInstance(currentAccount).getChat(chat.id);
            if (freshChat != null && freshChat.participants_count > 0) {
                addRow(container, context, "Total Members", freshChat.participants_count + " (approximate)", false);
            }
        }

        // ── Chat settings (available to all) ───────────────────────────
        if (chatFull != null) {
            addSection(container, context, "Settings");

            if (chatFull.slowmode_seconds > 0) {
                addRow(container, context, "Slow Mode", formatSlowmode(chatFull.slowmode_seconds), false);
            }
            if (chatFull.ttl_period > 0) {
                addRow(container, context, "Auto-Delete", formatTTL(chatFull.ttl_period), false);
            }
            if (chatFull.linked_chat_id != 0) {
                TLRPC.Chat linked = MessagesController.getInstance(currentAccount).getChat(chatFull.linked_chat_id);
                String linkedName = linked != null ? (linked.title + " (" + chatFull.linked_chat_id + ")") : String.valueOf(chatFull.linked_chat_id);
                addRow(container, context, isChannel ? "Linked Group" : "Linked Channel", linkedName, false);
            }
            if (chatFull.antispam) {
                addRow(container, context, "Anti-Spam", "Enabled", false);
            }
            if (!TextUtils.isEmpty(chatFull.theme_emoticon)) {
                addRow(container, context, "Theme", chatFull.theme_emoticon, false);
            }

            // Pinned message
            if (chatFull.pinned_msg_id > 0) {
                addRow(container, context, "Pinned Message ID", String.valueOf(chatFull.pinned_msg_id), false);
            }

            // Available reactions
            if (chatFull.available_reactions != null) {
                String reactType;
                if (chatFull.available_reactions instanceof TLRPC.TL_chatReactionsAll) {
                    reactType = "All";
                } else if (chatFull.available_reactions instanceof TLRPC.TL_chatReactionsSome) {
                    int cnt = ((TLRPC.TL_chatReactionsSome) chatFull.available_reactions).reactions != null
                            ? ((TLRPC.TL_chatReactionsSome) chatFull.available_reactions).reactions.size() : 0;
                    reactType = "Custom (" + cnt + ")";
                } else {
                    reactType = "None";
                }
                addRow(container, context, "Reactions", reactType, false);
            }
            if (chatFull.reactions_limit > 0) {
                addRow(container, context, "Reactions Per Message Limit", String.valueOf(chatFull.reactions_limit), false);
            }
        }

        // ── Admin-only info ─────────────────────────────────────────────
        if (isAdmin && chatFull != null) {
            addSection(container, context, "Admin Info");

            if (chatFull.stats_dc > 0) {
                addRow(container, context, "Stats DC", String.valueOf(chatFull.stats_dc), false);
            }
            if (chatFull.pts > 0) {
                addRow(container, context, "Channel PTS", String.valueOf(chatFull.pts), false);
            }
            if (chatFull.boosts_applied > 0) {
                addRow(container, context, "Boosts Applied", String.valueOf(chatFull.boosts_applied), false);
            }
            if (chatFull.boosts_unrestrict > 0) {
                addRow(container, context, "Boosts to Unrestrict", String.valueOf(chatFull.boosts_unrestrict), false);
            }
            if (chatFull.can_view_revenue) {
                addRow(container, context, "Revenue Access", "Available", false);
            }
            if (chatFull.exported_invite != null && chatFull.exported_invite instanceof TLRPC.TL_chatInviteExported) {
                TLRPC.TL_chatInviteExported inv = (TLRPC.TL_chatInviteExported) chatFull.exported_invite;
                addRow(container, context, "Invite Link", inv.link != null ? inv.link : "Set", false);
            }
        }

        // Note when chatFull not yet loaded
        if (chatFull == null) {
            addSection(container, context, "Note");
            addMultilineRow(container, context, "Full chat info is still loading. Re-open this sheet after visiting the chat profile.");
        }

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(container);
        setCustomView(scrollView);
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void addSection(LinearLayout parent, Context ctx, String label) {
        TextView tv = new TextView(ctx);
        tv.setText(label.toUpperCase(Locale.US));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        tv.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(4));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private void addRow(LinearLayout parent, Context ctx, String key, String value, boolean danger) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(6));

        GradientDrawable divider = new GradientDrawable();
        divider.setColor(Theme.getColor(Theme.key_divider));

        TextView keyTv = new TextView(ctx);
        keyTv.setText(key);
        keyTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        keyTv.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        row.addView(keyTv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 0.42f));

        TextView valTv = new TextView(ctx);
        valTv.setText(value);
        valTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        valTv.setTextColor(danger
                ? 0xFFFF4444
                : Theme.getColor(Theme.key_dialogTextBlack));
        valTv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        valTv.setGravity(Gravity.END);
        row.addView(valTv, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 0.58f));

        parent.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // divider line
        View line = new View(ctx);
        line.setBackgroundColor(Theme.getColor(Theme.key_divider));
        parent.addView(line, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));
    }

    private void addMultilineRow(LinearLayout parent, Context ctx, String text) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tv.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        tv.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));
        parent.addView(tv, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    private String formatDate(int unixTime) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date((long) unixTime * 1000));
        } catch (Exception e) {
            return String.valueOf(unixTime);
        }
    }

    private String formatSlowmode(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m";
        return (seconds / 3600) + "h";
    }

    private String formatTTL(int seconds) {
        if (seconds < 86400) return (seconds / 3600) + "h";
        return (seconds / 86400) + " day(s)";
    }
}
