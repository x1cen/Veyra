package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageDetailsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private final MessageObject messageObject;
    private TLRPC.Chat fromChat;
    private TLRPC.User fromUser;
    private String filePath;
    private String fileName;
    private StringBuilder fwdBuilder;

    public MessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
        if (messageObject != null && messageObject.messageOwner != null) {
            if (messageObject.messageOwner.peer_id != null && messageObject.messageOwner.peer_id.channel_id != 0) {
                fromChat = getMessagesController().getChat(messageObject.messageOwner.peer_id.channel_id);
            } else if (messageObject.messageOwner.peer_id != null && messageObject.messageOwner.peer_id.chat_id != 0) {
                fromChat = getMessagesController().getChat(messageObject.messageOwner.peer_id.chat_id);
            }
            if (messageObject.messageOwner.from_id != null && messageObject.messageOwner.from_id.user_id != 0) {
                fromUser = getMessagesController().getUser(messageObject.messageOwner.from_id.user_id);
            }
            filePath = messageObject.messageOwner.attachPath;
            if (!TextUtils.isEmpty(filePath)) {
                File temp = new File(filePath);
                if (!temp.exists()) {
                    filePath = null;
                }
            }
            if (TextUtils.isEmpty(filePath)) {
                try {
                    File p = FileLoader.getInstance(currentAccount).getPathToMessage(messageObject.messageOwner);
                    if (p != null && p.exists()) {
                        filePath = p.toString();
                    }
                } catch (Exception ignored) {}
            }
            if (TextUtils.isEmpty(filePath) && messageObject.getDocument() != null) {
                try {
                    File p = FileLoader.getInstance(currentAccount).getPathToAttach(messageObject.getDocument(), true);
                    if (p != null && p.isFile()) {
                        filePath = p.toString();
                    }
                } catch (Exception ignored) {}
            }
            if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.document != null) {
                if (TextUtils.isEmpty(messageObject.messageOwner.media.document.file_name)) {
                    for (int a = 0; a < messageObject.messageOwner.media.document.attributes.size(); a++) {
                        if (messageObject.messageOwner.media.document.attributes.get(a) instanceof TLRPC.TL_documentAttributeFilename) {
                            fileName = messageObject.messageOwner.media.document.attributes.get(a).file_name;
                        }
                    }
                } else {
                    fileName = messageObject.messageOwner.media.document.file_name;
                }
            }
            if (messageObject.isForwarded()) {
                fwdBuilder = new StringBuilder();
                if (messageObject.messageOwner.fwd_from != null) {
                    if (messageObject.messageOwner.fwd_from.from_id != null) {
                        if (messageObject.messageOwner.fwd_from.from_id.channel_id != 0) {
                            TLRPC.Chat chat = getMessagesController().getChat(messageObject.messageOwner.fwd_from.from_id.channel_id);
                            if (chat != null) {
                                fwdBuilder.append(chat.title);
                                if (!TextUtils.isEmpty(chat.username)) {
                                    fwdBuilder.append(" (@").append(chat.username).append(")");
                                }
                                fwdBuilder.append(" [ID: ").append(chat.id).append("]");
                            }
                        } else if (messageObject.messageOwner.fwd_from.from_id.user_id != 0) {
                            TLRPC.User user = getMessagesController().getUser(messageObject.messageOwner.fwd_from.from_id.user_id);
                            if (user != null) {
                                fwdBuilder.append(ContactsController.formatName(user.first_name, user.last_name));
                                if (!TextUtils.isEmpty(user.username)) {
                                    fwdBuilder.append(" (@").append(user.username).append(")");
                                }
                                fwdBuilder.append(" [ID: ").append(user.id).append("]");
                            }
                        }
                    } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.from_name)) {
                        fwdBuilder.append(messageObject.messageOwner.fwd_from.from_name);
                    }
                }
            }
        }
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        return true;
    }

    @SuppressLint({"NewApi", "RtlHardcoded"})
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("MessageDetails", R.string.MessageDetails));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            MessageDetailItem item = listAdapter.getItem(position);
            if (item == null) return;
            if (item.viewType == MessageDetailItem.VIEW_TYPE_DETAIL) {
                if (!TextUtils.isEmpty(item.value)) {
                    AndroidUtilities.addToClipboard(item.value);
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                }
            } else if (item.viewType == MessageDetailItem.VIEW_TYPE_EXPORT) {
                try {
                    JSONObject json = new JSONObject();
                    if (messageObject != null && messageObject.messageOwner != null) {
                        json.put("id", messageObject.messageOwner.id);
                        json.put("date", messageObject.messageOwner.date);
                        json.put("message", messageObject.messageOwner.message);
                        if (messageObject.messageOwner.from_id != null) {
                            json.put("from_id", messageObject.messageOwner.from_id.user_id);
                        }
                        if (messageObject.messageOwner.peer_id != null) {
                            json.put("channel_id", messageObject.messageOwner.peer_id.channel_id);
                            json.put("chat_id", messageObject.messageOwner.peer_id.chat_id);
                        }
                        if (fromUser != null) {
                            JSONObject sender = new JSONObject();
                            sender.put("id", fromUser.id);
                            sender.put("first_name", fromUser.first_name != null ? fromUser.first_name : "");
                            sender.put("last_name", fromUser.last_name != null ? fromUser.last_name : "");
                            sender.put("username", fromUser.username != null ? fromUser.username : "");
                            json.put("sender", sender);
                        }
                        if (fromChat != null) {
                            JSONObject chat = new JSONObject();
                            chat.put("id", fromChat.id);
                            chat.put("title", fromChat.title != null ? fromChat.title : "");
                            chat.put("username", fromChat.username != null ? fromChat.username : "");
                            json.put("chat", chat);
                        }
                        if (messageObject.messageOwner.edit_date != 0) {
                            json.put("edit_date", messageObject.messageOwner.edit_date);
                        }
                        if (messageObject.messageOwner.views > 0) {
                            json.put("views", messageObject.messageOwner.views);
                        }
                        if (messageObject.messageOwner.forwards > 0) {
                            json.put("forwards", messageObject.messageOwner.forwards);
                        }
                        if (!TextUtils.isEmpty(fileName)) {
                            json.put("file_name", fileName);
                        }
                        if (messageObject.getSize() > 0) {
                            json.put("file_size", messageObject.getSize());
                        }
                    }
                    String jsonStr = json.toString(2);

                    // Save to Downloads
                    String outFileName = "veyra_msg_" + (messageObject != null ? messageObject.messageOwner.id : "0") + ".json";
                    java.io.File downloadsDir;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        downloadsDir = getParentActivity().getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS);
                    } else {
                        downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                    }
                    if (downloadsDir != null && !downloadsDir.exists()) downloadsDir.mkdirs();
                    java.io.File outFile = new java.io.File(downloadsDir, outFileName);
                    try (java.io.FileWriter fw = new java.io.FileWriter(outFile)) {
                        fw.write(jsonStr);
                    }

                    // Share via intent
                    Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        getParentActivity(),
                        ApplicationLoader.getApplicationId() + ".provider",
                        outFile
                    );
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/json");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    getParentActivity().startActivity(Intent.createChooser(shareIntent, "Share JSON"));

                    BulletinFactory.of(this).createSimpleBulletin(
                        org.telegram.messenger.R.raw.ic_done,
                        "Saved to Downloads: " + outFileName
                    ).show();
                } catch (Exception e) {
                    FileLog.e(e);
                    // Fallback: copy to clipboard
                    try {
                        JSONObject json = new JSONObject();
                        if (messageObject != null && messageObject.messageOwner != null) {
                            json.put("id", messageObject.messageOwner.id);
                            json.put("message", messageObject.messageOwner.message);
                        }
                        AndroidUtilities.addToClipboard(json.toString(2));
                        BulletinFactory.of(this).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                    } catch (Exception ignored) {}
                }
            }
        });

        listView.setOnItemLongClickListener((view, position) -> {
            MessageDetailItem item = listAdapter.getItem(position);
            if (item == null) return false;
            if (item.actionType == ActionType.ACTION_SHARE_FILE && !TextUtils.isEmpty(filePath)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/octet-stream");
                    File f = new File(filePath);
                    Uri uri = FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", f);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setClipData(ClipData.newRawUri(null, uri));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
                    return true;
                } catch (Exception ignored) {}
            } else if (item.actionType == ActionType.ACTION_OPEN_CHAT && fromChat != null) {
                Bundle args = new Bundle();
                args.putLong("chat_id", fromChat.id);
                presentFragment(new ProfileActivity(args));
                return true;
            } else if (item.actionType == ActionType.ACTION_OPEN_USER && fromUser != null) {
                Bundle args = new Bundle();
                args.putLong("user_id", fromUser.id);
                presentFragment(new ProfileActivity(args));
                return true;
            }
            return false;
        });

        return fragmentView;
    }

    private enum ActionType {
        NONE,
        ACTION_SHARE_FILE,
        ACTION_OPEN_CHAT,
        ACTION_OPEN_USER
    }

    private static class MessageDetailItem {
        static final int VIEW_TYPE_DIVIDER = 0;
        static final int VIEW_TYPE_DETAIL = 1;
        static final int VIEW_TYPE_EXPORT = 2;
        static final int VIEW_TYPE_HEADER = 3;

        int viewType;
        String title;
        CharSequence value;
        ActionType actionType = ActionType.NONE;

        public MessageDetailItem(String title, CharSequence value) {
            this.viewType = VIEW_TYPE_DETAIL;
            this.title = title;
            this.value = value;
        }

        public MessageDetailItem(String title, CharSequence value, ActionType actionType) {
            this.viewType = VIEW_TYPE_DETAIL;
            this.title = title;
            this.value = value;
            this.actionType = actionType;
        }

        public MessageDetailItem(int viewType) {
            this.viewType = viewType;
        }

        public MessageDetailItem(int viewType, String title) {
            this.viewType = viewType;
            this.title = title;
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;
        private final List<MessageDetailItem> items = new ArrayList<>();

        public ListAdapter(Context context) {
            mContext = context;
            buildItems();
        }

        public MessageDetailItem getItem(int position) {
            if (position >= 0 && position < items.size()) {
                return items.get(position);
            }
            return null;
        }

        private void buildItems() {
            items.clear();
            if (messageObject == null || messageObject.messageOwner == null) {
                return;
            }
            items.add(new MessageDetailItem(MessageDetailItem.VIEW_TYPE_HEADER, "Message Info"));
            items.add(new MessageDetailItem("ID", String.valueOf(messageObject.messageOwner.id)));

            if (messageObject.scheduled) {
                items.add(new MessageDetailItem("Scheduled", "Yes"));
            }

            if (!TextUtils.isEmpty(messageObject.messageOwner.message)) {
                items.add(new MessageDetailItem("Message Text", messageObject.messageOwner.message));
            }

            if (fromChat != null) {
                items.add(new MessageDetailItem(fromChat.broadcast ? "Channel" : "Group", fromChat.title, ActionType.ACTION_OPEN_CHAT));
                items.add(new MessageDetailItem("Chat ID", String.valueOf(fromChat.id)));
            }

            if (fromUser != null) {
                String name = ContactsController.formatName(fromUser.first_name, fromUser.last_name);
                items.add(new MessageDetailItem("Sender", name, ActionType.ACTION_OPEN_USER));
                items.add(new MessageDetailItem("Sender ID", String.valueOf(fromUser.id)));
                if (!TextUtils.isEmpty(fromUser.username)) {
                    items.add(new MessageDetailItem("Username", "@" + fromUser.username));
                }
            } else if (messageObject.messageOwner.post_author != null) {
                items.add(new MessageDetailItem("Author", messageObject.messageOwner.post_author));
            }

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            if (messageObject.messageOwner.date != 0) {
                items.add(new MessageDetailItem("Date", format.format(new Date(messageObject.messageOwner.date * 1000L))));
            }
            if (messageObject.messageOwner.edit_date != 0) {
                items.add(new MessageDetailItem("Edited Date", format.format(new Date(messageObject.messageOwner.edit_date * 1000L))));
            }

            if (messageObject.messageOwner.views > 0) {
                items.add(new MessageDetailItem("Views", String.valueOf(messageObject.messageOwner.views)));
            }
            if (messageObject.messageOwner.forwards > 0) {
                items.add(new MessageDetailItem("Forwards", String.valueOf(messageObject.messageOwner.forwards)));
            }

            if (fwdBuilder != null && fwdBuilder.length() > 0) {
                items.add(new MessageDetailItem("Forward From", fwdBuilder.toString()));
            }

            int dcId = 0;
            if (messageObject.messageOwner.media != null) {
                if (messageObject.messageOwner.media.photo != null) {
                    dcId = messageObject.messageOwner.media.photo.dc_id;
                } else if (messageObject.messageOwner.media.document != null) {
                    dcId = messageObject.messageOwner.media.document.dc_id;
                }
            }
            if (dcId > 0) {
                items.add(new MessageDetailItem("DC", "Datacenter " + dcId));
            }

            if (!TextUtils.isEmpty(fileName)) {
                items.add(new MessageDetailItem("File Name", fileName));
            }
            if (!TextUtils.isEmpty(filePath)) {
                items.add(new MessageDetailItem("File Path", filePath, ActionType.ACTION_SHARE_FILE));
            }
            if (messageObject.getSize() > 0) {
                items.add(new MessageDetailItem("File Size", AndroidUtilities.formatFileSize(messageObject.getSize()) + " (" + messageObject.getSize() + " bytes)"));
            }

            items.add(new MessageDetailItem(MessageDetailItem.VIEW_TYPE_DIVIDER));
            items.add(new MessageDetailItem(MessageDetailItem.VIEW_TYPE_EXPORT, "Export as JSON"));
            items.add(new MessageDetailItem(MessageDetailItem.VIEW_TYPE_DIVIDER));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == MessageDetailItem.VIEW_TYPE_DETAIL || type == MessageDetailItem.VIEW_TYPE_EXPORT;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case MessageDetailItem.VIEW_TYPE_HEADER:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case MessageDetailItem.VIEW_TYPE_EXPORT:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case MessageDetailItem.VIEW_TYPE_DIVIDER:
                    view = new ShadowSectionCell(mContext);
                    break;
                case MessageDetailItem.VIEW_TYPE_DETAIL:
                default:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MessageDetailItem item = items.get(position);
            switch (item.viewType) {
                case MessageDetailItem.VIEW_TYPE_HEADER:
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    headerCell.setText(item.title);
                    break;
                case MessageDetailItem.VIEW_TYPE_EXPORT:
                    TextSettingsCell exportCell = (TextSettingsCell) holder.itemView;
                    exportCell.setText(LocaleController.getString("ExportAsJson", R.string.ExportAsJson), false);
                    exportCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                    break;
                case MessageDetailItem.VIEW_TYPE_DETAIL:
                    TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                    cell.setTextAndValue(item.title, item.value, true);
                    break;
            }
        }
    }
}
