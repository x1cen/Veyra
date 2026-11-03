package org.telegram.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.DialogObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.GroupCreateUserCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.veyra.client.VeyraScope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

// Veyra: single Included Chats list for one VeyraScope. Every chat is ticked from Chat Types
// (and Skip Own/Admin) by default; tapping a chat flips it and stores a manual override for
// that chat only. The action-bar sync button drops all overrides so the list matches Chat
// Types again.
public class VeyraIncludedChatsActivity extends BaseFragment {

    private static final int MENU_SEARCH = 1;
    private static final int MENU_SYNC = 2;

    private final int scopeId;
    private final VeyraScope scope;

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private final ArrayList<TLObject> allChats = new ArrayList<>();
    private final ArrayList<TLObject> searchResult = new ArrayList<>();
    private boolean searching;

    public VeyraIncludedChatsActivity(int scopeId) {
        this.scopeId = scopeId;
        this.scope = VeyraScope.byId(scopeId);
    }

    @Override
    public boolean onFragmentCreate() {
        if (scope == null) {
            return false;
        }
        loadChats();
        return super.onFragmentCreate();
    }

    private void loadChats() {
        allChats.clear();
        HashSet<Long> seen = new HashSet<>();
        ArrayList<TLRPC.Dialog> dialogs = getMessagesController().getAllDialogs();
        if (dialogs != null) {
            for (int a = 0; a < dialogs.size(); a++) {
                TLRPC.Dialog d = dialogs.get(a);
                if (d != null) {
                    addChat(d.id, seen);
                }
            }
        }
        // Chats with a manual change stay visible even if they left the dialog list.
        ArrayList<Long> ids = scope.getOverrideIds();
        for (int a = 0; a < ids.size(); a++) {
            addChat(ids.get(a), seen);
        }
        Collections.sort(allChats, (o1, o2) -> {
            int t1 = typeOrder(getDialogId(o1));
            int t2 = typeOrder(getDialogId(o2));
            if (t1 != t2) {
                return t1 - t2;
            }
            return getName(o1).compareToIgnoreCase(getName(o2));
        });
    }

    private void addChat(long did, HashSet<Long> seen) {
        if (did == 0 || seen.contains(did) || DialogObject.isEncryptedDialog(did)) {
            return;
        }
        TLObject obj = null;
        if (did > 0) {
            TLRPC.User user = VeyraScope.findUser(currentAccount, did);
            if (user != null && !UserObject.isUserSelf(user) && !UserObject.isDeleted(user)) {
                obj = user;
            }
        } else {
            TLRPC.Chat chat = VeyraScope.findChat(currentAccount, -did);
            if (chat != null) {
                obj = chat;
            }
        }
        if (obj != null) {
            seen.add(did);
            allChats.add(obj);
        }
    }

    private int typeOrder(long did) {
        int t = VeyraScope.classify(currentAccount, did);
        return t < 0 ? VeyraScope.PEER_COUNT : t;
    }

    private static long getDialogId(TLObject obj) {
        if (obj instanceof TLRPC.User) {
            return ((TLRPC.User) obj).id;
        } else if (obj instanceof TLRPC.Chat) {
            return -((TLRPC.Chat) obj).id;
        }
        return 0;
    }

    private static String getName(TLObject obj) {
        if (obj instanceof TLRPC.User) {
            String n = UserObject.getUserName((TLRPC.User) obj);
            return n != null ? n : "";
        } else if (obj instanceof TLRPC.Chat) {
            String t = ((TLRPC.Chat) obj).title;
            return t != null ? t : "";
        }
        return "";
    }

    private static String getUsername(TLObject obj) {
        String u = null;
        if (obj instanceof TLRPC.User) {
            u = ((TLRPC.User) obj).username;
        } else if (obj instanceof TLRPC.Chat) {
            u = ((TLRPC.Chat) obj).username;
        }
        return u != null ? u : "";
    }

    private String getStatus(long did) {
        String type;
        int peer = VeyraScope.classify(currentAccount, did);
        switch (peer) {
            case VeyraScope.PEER_PRIVATE: type = "Private Chat"; break;
            case VeyraScope.PEER_BOT: type = "Bot"; break;
            case VeyraScope.PEER_PRIVATE_GROUP: type = "Private Group"; break;
            case VeyraScope.PEER_PUBLIC_GROUP: type = "Public Group"; break;
            case VeyraScope.PEER_PRIVATE_CHANNEL: type = "Private Channel"; break;
            case VeyraScope.PEER_PUBLIC_CHANNEL: type = "Public Channel"; break;
            default: type = "Chat"; break;
        }
        if (peer == VeyraScope.PEER_PRIVATE_GROUP || peer == VeyraScope.PEER_PUBLIC_GROUP) {
            int size = VeyraScope.groupSize(currentAccount, did);
            if (size == VeyraScope.SIZE_SMALL) {
                type += " \u2022 Small";
            } else if (size == VeyraScope.SIZE_LARGE) {
                type += " \u2022 Large";
            }
        }
        if (scope.getOverride(did) != null) {
            return type + " \u2022 Manual";
        }
        return type;
    }

    private void updateSubtitle() {
        if (actionBar == null) {
            return;
        }
        int included = 0;
        for (int a = 0; a < allChats.size(); a++) {
            if (scope.isIncluded(currentAccount, getDialogId(allChats.get(a)), true)) {
                included++;
            }
        }
        actionBar.setSubtitle(included + " of " + allChats.size() + " included");
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Included Chats");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == MENU_SYNC) {
                    showSyncAlert();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem searchItem = menu.addItem(MENU_SEARCH, R.drawable.outline_header_search).setIsSearchField(true, false);
        searchItem.setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {
            @Override
            public void onSearchExpand() {
                searching = true;
            }

            @Override
            public void onSearchCollapse() {
                searching = false;
                searchResult.clear();
                if (listAdapter != null) {
                    listAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                processSearch(editText.getText().toString());
            }
        });
        searchItem.setSearchFieldHint("Search");
        menu.addItem(MENU_SYNC, R.drawable.msg_retry).setContentDescription("Sync with Chat Types");

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));

        EmptyTextProgressView emptyView = new EmptyTextProgressView(context);
        emptyView.showTextView();
        emptyView.setText("No chats");
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(true);
        listView.setEmptyView(emptyView);
        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position) -> {
            ArrayList<TLObject> list = searching ? searchResult : allChats;
            if (position < 0 || position >= list.size()) {
                return;
            }
            TLObject obj = list.get(position);
            long did = getDialogId(obj);
            boolean newVal = !scope.isIncluded(currentAccount, did, true);
            scope.setIncluded(currentAccount, did, newVal);
            if (view instanceof GroupCreateUserCell) {
                GroupCreateUserCell cell = (GroupCreateUserCell) view;
                cell.setObject(obj, null, getStatus(did), false);
                cell.setChecked(newVal, true);
            }
            updateSubtitle();
        });
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateSubtitle();
        return fragmentView;
    }

    private void processSearch(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();
        searchResult.clear();
        if (TextUtils.isEmpty(q)) {
            searching = false;
        } else {
            searching = true;
            for (int a = 0; a < allChats.size(); a++) {
                TLObject obj = allChats.get(a);
                if (getName(obj).toLowerCase().contains(q) || getUsername(obj).toLowerCase().contains(q)) {
                    searchResult.add(obj);
                }
            }
        }
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
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
            if (listAdapter != null) {
                listAdapter.notifyDataSetChanged();
            }
            updateSubtitle();
        });
        builder.setNegativeButton("Cancel", null);
        showDialog(builder.create());
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {
        private final Context mContext;

        ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return searching ? searchResult.size() : allChats.size();
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new GroupCreateUserCell(mContext, 1, 0, false));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ArrayList<TLObject> list = searching ? searchResult : allChats;
            if (position < 0 || position >= list.size()) {
                return;
            }
            TLObject obj = list.get(position);
            long did = getDialogId(obj);
            GroupCreateUserCell cell = (GroupCreateUserCell) holder.itemView;
            cell.setObject(obj, null, getStatus(did), false);
            cell.setChecked(scope.isIncluded(currentAccount, did, true), false);
        }
    }
}
