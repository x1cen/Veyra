package org.telegram.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.VeyraConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class VeyraGhostFeatureActivity extends BaseFragment {

    private final int category;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    // Row indices
    private int masterHeaderRow = -1;
    private int masterToggleRow = -1;
    private int masterDividerRow = -1;

    // Scopes section
    private int scopesHeaderRow = -1;
    private int scopePrivateRow = -1;
    private int scopeGroupsRow = -1;
    private int scopeBotsRow = -1;
    private int scopePublicChannelsRow = -1;
    private int scopePrivateChannelsRow = -1;
    private int scopesInfoRow = -1;

    // Feature-specific actions/options
    private int optionsHeaderRow = -1;
    private int typingTextRow = -1;
    private int typingVoiceRow = -1;
    private int typingVideoRow = -1;
    private int typingFilesRow = -1;
    private int readOnReplyRow = -1;
    private int readContentsRow = -1;
    private int secretReadRow = -1;
    private int optionsDividerRow = -1;

    // Exceptions section
    private int exceptionsHeaderRow = -1;
    private int addExceptionRow = -1;
    private int exceptionsStartRow = -1;
    private int exceptionsEndRow = -1;
    private int deleteAllExceptionsRow = -1;
    private int exceptionsInfoRow = -1;

    private int rowCount = 0;

    private final ArrayList<Long> exceptionsList = new ArrayList<>();

    public VeyraGhostFeatureActivity(int category) {
        super();
        this.category = category;
    }

    public VeyraGhostFeatureActivity(Bundle args) {
        super(args);
        this.category = args != null ? args.getInt("category", VeyraConfig.CATEGORY_GHOST_TYPING) : VeyraConfig.CATEGORY_GHOST_TYPING;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;
        masterHeaderRow = -1;
        masterToggleRow = -1;
        masterDividerRow = -1;

        scopesHeaderRow = -1;
        scopePrivateRow = -1;
        scopeGroupsRow = -1;
        scopeBotsRow = -1;
        scopePublicChannelsRow = -1;
        scopePrivateChannelsRow = -1;
        scopesInfoRow = -1;

        optionsHeaderRow = -1;
        typingTextRow = -1;
        typingVoiceRow = -1;
        typingVideoRow = -1;
        typingFilesRow = -1;
        readOnReplyRow = -1;
        readContentsRow = -1;
        secretReadRow = -1;
        optionsDividerRow = -1;

        exceptionsHeaderRow = -1;
        addExceptionRow = -1;
        exceptionsStartRow = -1;
        exceptionsEndRow = -1;
        deleteAllExceptionsRow = -1;
        exceptionsInfoRow = -1;

        // Master Control
        masterHeaderRow = rowCount++;
        masterToggleRow = rowCount++;
        masterDividerRow = rowCount++;

        // Scopes
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING || category == VeyraConfig.CATEGORY_GHOST_READ) {
            scopesHeaderRow = rowCount++;
            scopePrivateRow = rowCount++;
            scopeGroupsRow = rowCount++;
            scopeBotsRow = rowCount++;
            scopesInfoRow = rowCount++;
        } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
            scopesHeaderRow = rowCount++;
            scopePublicChannelsRow = rowCount++;
            scopePrivateChannelsRow = rowCount++;
            scopesInfoRow = rowCount++;
        }

        // Sub-options
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
            optionsHeaderRow = rowCount++;
            typingTextRow = rowCount++;
            typingVoiceRow = rowCount++;
            typingVideoRow = rowCount++;
            typingFilesRow = rowCount++;
            optionsDividerRow = rowCount++;
        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
            optionsHeaderRow = rowCount++;
            readOnReplyRow = rowCount++;
            readContentsRow = rowCount++;
            secretReadRow = rowCount++;
            optionsDividerRow = rowCount++;
        }

        // Exceptions
        exceptionsHeaderRow = rowCount++;
        addExceptionRow = rowCount++;

        exceptionsList.clear();
        HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
        synchronized (map) {
            exceptionsList.addAll(map.keySet());
        }
        Collections.sort(exceptionsList, (o1, o2) -> {
            boolean isUser1 = DialogObject.isUserDialog(o1);
            boolean isUser2 = DialogObject.isUserDialog(o2);
            if (isUser1 && !isUser2) return -1;
            if (!isUser1 && isUser2) return 1;
            return Long.compare(Math.abs(o1), Math.abs(o2));
        });

        if (!exceptionsList.isEmpty()) {
            exceptionsStartRow = rowCount;
            rowCount += exceptionsList.size();
            exceptionsEndRow = rowCount;
            deleteAllExceptionsRow = rowCount++;
        }

        exceptionsInfoRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);

        String title;
        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
            title = "Chat Actions & Typing";
        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
            title = "Read Receipts";
        } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
            title = "Channel Browsing";
        } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
            title = "Story Views";
        } else {
            title = "Ghost Settings";
        }
        actionBar.setTitle(title);

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        listAdapter = new ListAdapter(context);

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == masterToggleRow) {
                boolean cur;
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    cur = VeyraConfig.ghostHideTyping;
                    VeyraConfig.setGhostHideTyping(!cur);
                } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                    cur = VeyraConfig.ghostHideRead;
                    VeyraConfig.setGhostHideRead(!cur);
                } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
                    cur = VeyraConfig.ghostHideChannelViews;
                    VeyraConfig.setGhostHideChannelViews(!cur);
                } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
                    cur = VeyraConfig.ghostHideStories;
                    VeyraConfig.setGhostHideStories(!cur);
                }
                updateRows();
            } else if (position == scopePrivateRow) {
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    VeyraConfig.setGhostTypingPrivate(!VeyraConfig.ghostTypingPrivate);
                } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                    VeyraConfig.setGhostReadPrivate(!VeyraConfig.ghostReadPrivate);
                }
                updateRows();
            } else if (position == scopeGroupsRow) {
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    VeyraConfig.setGhostTypingGroups(!VeyraConfig.ghostTypingGroups);
                } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                    VeyraConfig.setGhostReadGroups(!VeyraConfig.ghostReadGroups);
                }
                updateRows();
            } else if (position == scopeBotsRow) {
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    VeyraConfig.setGhostTypingBots(!VeyraConfig.ghostTypingBots);
                } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                    VeyraConfig.setGhostReadBots(!VeyraConfig.ghostReadBots);
                }
                updateRows();
            } else if (position == scopePublicChannelsRow) {
                VeyraConfig.setGhostChannelPublic(!VeyraConfig.ghostChannelPublic);
                updateRows();
            } else if (position == scopePrivateChannelsRow) {
                VeyraConfig.setGhostChannelPrivate(!VeyraConfig.ghostChannelPrivate);
                updateRows();
            } else if (position == typingTextRow) {
                VeyraConfig.setGhostTypingText(!VeyraConfig.ghostTypingText);
                updateRows();
            } else if (position == typingVoiceRow) {
                VeyraConfig.setGhostTypingVoice(!VeyraConfig.ghostTypingVoice);
                updateRows();
            } else if (position == typingVideoRow) {
                VeyraConfig.setGhostTypingVideo(!VeyraConfig.ghostTypingVideo);
                updateRows();
            } else if (position == typingFilesRow) {
                VeyraConfig.setGhostTypingFiles(!VeyraConfig.ghostTypingFiles);
                updateRows();
            } else if (position == readOnReplyRow) {
                VeyraConfig.setGhostReadOnReply(!VeyraConfig.ghostReadOnReply);
                updateRows();
            } else if (position == readContentsRow) {
                VeyraConfig.setGhostHideReadContents(!VeyraConfig.ghostHideReadContents);
                updateRows();
            } else if (position == secretReadRow) {
                VeyraConfig.setGhostHideSecretRead(!VeyraConfig.ghostHideSecretRead);
                updateRows();
            } else if (position == addExceptionRow) {
                openAddExceptionDialog();
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                int index = position - exceptionsStartRow;
                if (index >= 0 && index < exceptionsList.size()) {
                    long did = exceptionsList.get(index);
                    showEditExceptionAlert(did);
                }
            } else if (position == deleteAllExceptionsRow) {
                showDeleteAllExceptionsAlert();
            }
        });

        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    private void openAddExceptionDialog() {
        Bundle args = new Bundle();
        args.putBoolean("onlySelect", true);
        args.putBoolean("checkCanWrite", false);
        args.putBoolean("allowGlobalSearch", false);

        if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_CHANNELS_ONLY);
        } else {
            args.putInt("dialogsType", DialogsActivity.DIALOGS_TYPE_DEFAULT);
        }

        DialogsActivity activity = new DialogsActivity(args);
        activity.setDelegate((fragment, dids, message, param, notify, scheduleDate, scheduleRepeatPeriod, topicsFragment) -> {
            if (dids != null && !dids.isEmpty()) {
                long did = dids.get(0).dialogId;
                if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                    if (did < 0) {
                        TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-did);
                        if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup) {
                            BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Channels do not support chat actions").show();
                            return true;
                        }
                    }
                }
                showSelectExceptionModeDialog(did);
            }
            return true;
        });
        presentFragment(activity);
    }

    private void showSelectExceptionModeDialog(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Add Exception");

        String[] options;
        if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
            options = new String[]{"Never Increment Views (Always Stealth)", "Count Views Normally (Allow)"};
        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
            options = new String[]{"Always Hide Read Receipts (Never Mark Read)", "Always Mark Read (Normal)"};
        } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
            options = new String[]{"Never Mark Viewed (Always Stealth)", "Mark Viewed Normally (Allow)"};
        } else {
            options = new String[]{"Always Hide Actions (Never Send Typing)", "Always Send Actions (Allow Typing)"};
        }

        builder.setItems(options, (dialog, which) -> {
            boolean hide = (which == 0);
            VeyraConfig.addException(category, dialogId, hide);
            updateRows();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Exception added").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showEditExceptionAlert(long dialogId) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle("Manage Exception");

        Boolean currentVal = VeyraConfig.getException(category, dialogId);
        boolean currentlyHiding = currentVal != null && currentVal;

        String switchText = currentlyHiding ? "Switch to: Allow / Normal" : "Switch to: Always Stealth";
        String[] options = new String[]{switchText, "Delete Exception"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                VeyraConfig.addException(category, dialogId, !currentlyHiding);
                updateRows();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, "Exception updated").show();
            } else if (which == 1) {
                VeyraConfig.removeException(category, dialogId);
                updateRows();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "Exception removed").show();
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showDeleteAllExceptionsAlert() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(LocaleController.getString("NotificationsDeleteAllExceptionTitle", R.string.NotificationsDeleteAllExceptionTitle));
        builder.setMessage(LocaleController.getString("NotificationsDeleteAllExceptionAlert", R.string.NotificationsDeleteAllExceptionAlert));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialog, which) -> {
            HashMap<Long, Boolean> map = VeyraConfig.getExceptions(category);
            synchronized (map) {
                map.clear();
            }
            updateRows();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.fire_on, "All exceptions removed").show();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alert = builder.create();
        showDialog(alert);
        TextView btn = (TextView) alert.getButton(DialogInterface.BUTTON_POSITIVE);
        if (btn != null) {
            btn.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int pos = holder.getAdapterPosition();
            return pos == masterToggleRow
                    || pos == scopePrivateRow || pos == scopeGroupsRow || pos == scopeBotsRow
                    || pos == scopePublicChannelsRow || pos == scopePrivateChannelsRow
                    || pos == typingTextRow || pos == typingVoiceRow || pos == typingVideoRow || pos == typingFilesRow
                    || pos == readOnReplyRow || pos == readContentsRow || pos == secretReadRow
                    || pos == addExceptionRow
                    || (pos >= exceptionsStartRow && pos < exceptionsEndRow)
                    || pos == deleteAllExceptionsRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 0:
                    view = new TextCheckCell(mContext);
                    break;
                case 1:
                    view = new TextInfoPrivacyCell(mContext);
                    break;
                case 2:
                    HeaderCell headerCell = new HeaderCell(mContext);
                    headerCell.setBackgroundColor(0);
                    view = headerCell;
                    break;
                case 3:
                    view = new TextCell(mContext);
                    break;
                case 4:
                    view = new UserCell(mContext, 4, 0, false, false);
                    break;
                case 5:
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            int topRad = 0;
            int bottomRad = 0;
            boolean isCardItem = false;

            if (position == masterToggleRow) {
                topRad = 14;
                bottomRad = 14;
                isCardItem = true;
            } else if (position == scopePrivateRow || position == scopePublicChannelsRow) {
                topRad = 14;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == scopeGroupsRow) {
                topRad = 0;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == scopeBotsRow || position == scopePrivateChannelsRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            } else if (position == typingTextRow || position == readOnReplyRow) {
                topRad = 14;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == typingVoiceRow || position == typingVideoRow || position == readContentsRow) {
                topRad = 0;
                bottomRad = 0;
                isCardItem = true;
            } else if (position == typingFilesRow || position == secretReadRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            } else if (position == addExceptionRow) {
                topRad = 14;
                bottomRad = exceptionsList.isEmpty() ? 14 : 0;
                isCardItem = true;
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                topRad = 0;
                bottomRad = (position == exceptionsEndRow - 1 && deleteAllExceptionsRow == -1) ? 14 : 0;
                isCardItem = true;
            } else if (position == deleteAllExceptionsRow) {
                topRad = 0;
                bottomRad = 14;
                isCardItem = true;
            }

            if (isCardItem) {
                int bgColor = Theme.getColor(Theme.key_windowBackgroundWhite);
                int selColor = Theme.getColor(Theme.key_listSelector);
                holder.itemView.setBackground(Theme.createRadSelectorDrawable(bgColor, selColor, topRad, bottomRad));
            }

            switch (holder.getItemViewType()) {
                case 0: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == masterToggleRow) {
                        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                            checkCell.setTextAndCheck("Hide Typing & Actions", VeyraConfig.ghostHideTyping, false);
                        } else if (category == VeyraConfig.CATEGORY_GHOST_READ) {
                            checkCell.setTextAndCheck("Hide Read Receipts", VeyraConfig.ghostHideRead, false);
                        } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
                            checkCell.setTextAndCheck("Anonymous Channel Browsing", VeyraConfig.ghostHideChannelViews, false);
                        } else if (category == VeyraConfig.CATEGORY_GHOST_STORIES) {
                            checkCell.setTextAndCheck("Hide Story Views", VeyraConfig.ghostHideStories, false);
                        }
                    } else if (position == scopePrivateRow) {
                        boolean val = category == VeyraConfig.CATEGORY_GHOST_TYPING ? VeyraConfig.ghostTypingPrivate : VeyraConfig.ghostReadPrivate;
                        checkCell.setTextAndCheck("Private Chats (PV)", val, true);
                    } else if (position == scopeGroupsRow) {
                        boolean val = category == VeyraConfig.CATEGORY_GHOST_TYPING ? VeyraConfig.ghostTypingGroups : VeyraConfig.ghostReadGroups;
                        checkCell.setTextAndCheck("Groups & Supergroups", val, true);
                    } else if (position == scopeBotsRow) {
                        boolean val = category == VeyraConfig.CATEGORY_GHOST_TYPING ? VeyraConfig.ghostTypingBots : VeyraConfig.ghostReadBots;
                        checkCell.setTextAndCheck("Bots", val, false);
                    } else if (position == scopePublicChannelsRow) {
                        checkCell.setTextAndCheck("Public Channels", VeyraConfig.ghostChannelPublic, true);
                    } else if (position == scopePrivateChannelsRow) {
                        checkCell.setTextAndCheck("Private Channels", VeyraConfig.ghostChannelPrivate, false);
                    } else if (position == typingTextRow) {
                        checkCell.setTextAndCheck("Text Typing Indicator", VeyraConfig.ghostTypingText, true);
                    } else if (position == typingVoiceRow) {
                        checkCell.setTextAndCheck("Voice Recording Indicator", VeyraConfig.ghostTypingVoice, true);
                    } else if (position == typingVideoRow) {
                        checkCell.setTextAndCheck("Video Recording Indicator", VeyraConfig.ghostTypingVideo, true);
                    } else if (position == typingFilesRow) {
                        checkCell.setTextAndCheck("File Uploading Indicator", VeyraConfig.ghostTypingFiles, false);
                    } else if (position == readOnReplyRow) {
                        checkCell.setTextAndCheck("Read on Reply", VeyraConfig.ghostReadOnReply, true);
                    } else if (position == readContentsRow) {
                        checkCell.setTextAndCheck("Hide Media Receipts (Audio/Video)", VeyraConfig.ghostHideReadContents, true);
                    } else if (position == secretReadRow) {
                        checkCell.setTextAndCheck("Hide Secret Chat Receipts", VeyraConfig.ghostHideSecretRead, false);
                    }
                    break;
                }
                case 1: {
                    TextInfoPrivacyCell infoCell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == scopesInfoRow) {
                        if (category == VeyraConfig.CATEGORY_GHOST_TYPING) {
                            infoCell.setText("Broadcast channels do not send typing or chat actions in Telegram MTProto.");
                        } else if (category == VeyraConfig.CATEGORY_GHOST_CHANNEL_VIEWS) {
                            infoCell.setText("View counting only exists for broadcast channel posts.");
                        } else {
                            infoCell.setText("Choose chat scopes where stealth read receipts apply.");
                        }
                    } else if (position == exceptionsInfoRow) {
                        infoCell.setText("Added exceptions override general chat settings for the selected conversations.");
                    }
                    break;
                }
                case 2: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == masterHeaderRow) {
                        headerCell.setText("Master Control");
                    } else if (position == scopesHeaderRow) {
                        headerCell.setText("Applicable Chat Scopes");
                    } else if (position == optionsHeaderRow) {
                        headerCell.setText(category == VeyraConfig.CATEGORY_GHOST_TYPING ? "Actions to Suppress" : "Additional Options");
                    } else if (position == exceptionsHeaderRow) {
                        headerCell.setText("Exceptions");
                    }
                    break;
                }
                case 3: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == addExceptionRow) {
                        textCell.setTextAndIcon(LocaleController.getString("NotificationsAddAnException", R.string.NotificationsAddAnException), R.drawable.msg_contact_add, !exceptionsList.isEmpty());
                        textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    } else if (position == deleteAllExceptionsRow) {
                        textCell.setText(LocaleController.getString("NotificationsDeleteAllException", R.string.NotificationsDeleteAllException), false);
                        textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedBold);
                    }
                    break;
                }
                case 4: {
                    UserCell userCell = (UserCell) holder.itemView;
                    int index = position - exceptionsStartRow;
                    if (index >= 0 && index < exceptionsList.size()) {
                        long did = exceptionsList.get(index);
                        TLObject object;
                        if (DialogObject.isUserDialog(did)) {
                            object = MessagesController.getInstance(currentAccount).getUser(did);
                        } else {
                            object = MessagesController.getInstance(currentAccount).getChat(-did);
                        }
                        Boolean excVal = VeyraConfig.getException(category, did);
                        boolean isHide = excVal != null && excVal;
                        String statusStr = isHide ? "Always Hidden (Stealth)" : "Always Sent / Counted (Normal)";
                        userCell.setData(object, null, statusStr, 0);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == masterToggleRow
                    || position == scopePrivateRow || position == scopeGroupsRow || position == scopeBotsRow
                    || position == scopePublicChannelsRow || position == scopePrivateChannelsRow
                    || position == typingTextRow || position == typingVoiceRow || position == typingVideoRow || position == typingFilesRow
                    || position == readOnReplyRow || position == readContentsRow || position == secretReadRow) {
                return 0;
            } else if (position == scopesInfoRow || position == exceptionsInfoRow) {
                return 1;
            } else if (position == masterHeaderRow || position == scopesHeaderRow || position == optionsHeaderRow || position == exceptionsHeaderRow) {
                return 2;
            } else if (position == addExceptionRow || position == deleteAllExceptionsRow) {
                return 3;
            } else if (position >= exceptionsStartRow && position < exceptionsEndRow) {
                return 4;
            }
            return 5;
        }
    }
}
