package org.telegram.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.VeyraConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

public class VeyraSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;
    private int privacyHeaderRow;
    private int readOnReplyRow;
    private int antiDeleteRow;
    private int ghostModeRow;
    private int hideTypingRow;
    private int blockSecretChatRow;
    private int privacySectionRow;

    private int controlsHeaderRow;
    private int confirmCallRow;
    private int confirmLinkRow;
    private int cleanUrlsRow;
    private int disableUndoRow;
    private int disableLinkPreviewRow;
    private int disableVibrationRow;
    private int controlsSectionRow;

    private int uiHeaderRow;
    private int persianCalendarRow;
    private int showProfileIdRow;
    private int bypassRestrictionsRow;
    private int noAdsRow;
    private int unlimitedLimitsRow;
    private int uiSectionRow;

    private int proxyHeaderRow;
    private int webProxyRow;
    private int proxySectionRow;

    private int aboutHeaderRow;
    private int versionRow;
    private int githubRow;
    private int aboutSectionRow;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();
        updateRows();
        return true;
    }

    private void updateRows() {
        rowCount = 0;

        privacyHeaderRow = rowCount++;
        readOnReplyRow = rowCount++;
        antiDeleteRow = rowCount++;
        ghostModeRow = rowCount++;
        hideTypingRow = rowCount++;
        blockSecretChatRow = rowCount++;
        privacySectionRow = rowCount++;

        controlsHeaderRow = rowCount++;
        confirmCallRow = rowCount++;
        confirmLinkRow = rowCount++;
        cleanUrlsRow = rowCount++;
        disableUndoRow = rowCount++;
        disableLinkPreviewRow = rowCount++;
        disableVibrationRow = rowCount++;
        controlsSectionRow = rowCount++;

        uiHeaderRow = rowCount++;
        persianCalendarRow = rowCount++;
        showProfileIdRow = rowCount++;
        bypassRestrictionsRow = rowCount++;
        noAdsRow = rowCount++;
        unlimitedLimitsRow = rowCount++;
        uiSectionRow = rowCount++;

        proxyHeaderRow = rowCount++;
        webProxyRow = rowCount++;
        proxySectionRow = rowCount++;

        aboutHeaderRow = rowCount++;
        versionRow = rowCount++;
        githubRow = rowCount++;
        aboutSectionRow = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("VeyraSettings", R.string.VeyraSettings));
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

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setItemAnimator(new DefaultItemAnimator());
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = new ListAdapter(context);
        listView.setAdapter(listAdapter);

        listView.setOnItemClickListener((view, position, x, y) -> {
            boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
            if (position == readOnReplyRow) {
                VeyraConfig.setReadOnReply(!VeyraConfig.readOnReply);
                ((TextCheckCell) view).setChecked(VeyraConfig.readOnReply);
            } else if (position == antiDeleteRow) {
                VeyraConfig.setAntiDelete(!VeyraConfig.antiDelete);
                ((TextCheckCell) view).setChecked(VeyraConfig.antiDelete);
            } else if (position == ghostModeRow) {
                VeyraConfig.setGhostMode(!VeyraConfig.ghostMode);
                ((TextCheckCell) view).setChecked(VeyraConfig.ghostMode);
            } else if (position == hideTypingRow) {
                VeyraConfig.setHideTyping(!VeyraConfig.hideTyping);
                ((TextCheckCell) view).setChecked(VeyraConfig.hideTyping);
            } else if (position == blockSecretChatRow) {
                VeyraConfig.setBlockSecretChat(!VeyraConfig.blockSecretChat);
                ((TextCheckCell) view).setChecked(VeyraConfig.blockSecretChat);
            } else if (position == confirmCallRow) {
                VeyraConfig.setConfirmCall(!VeyraConfig.confirmCall);
                ((TextCheckCell) view).setChecked(VeyraConfig.confirmCall);
            } else if (position == confirmLinkRow) {
                VeyraConfig.setConfirmLink(!VeyraConfig.confirmLink);
                ((TextCheckCell) view).setChecked(VeyraConfig.confirmLink);
            } else if (position == cleanUrlsRow) {
                VeyraConfig.setCleanUrls(!VeyraConfig.cleanUrls);
                ((TextCheckCell) view).setChecked(VeyraConfig.cleanUrls);
            } else if (position == disableUndoRow) {
                VeyraConfig.setDisableUndo(!VeyraConfig.disableUndo);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableUndo);
            } else if (position == disableLinkPreviewRow) {
                VeyraConfig.setDisableLinkPreviewByDefault(!VeyraConfig.disableLinkPreviewByDefault);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableLinkPreviewByDefault);
            } else if (position == disableVibrationRow) {
                VeyraConfig.setDisableVibration(!VeyraConfig.disableVibration);
                ((TextCheckCell) view).setChecked(VeyraConfig.disableVibration);
            } else if (position == persianCalendarRow) {
                VeyraConfig.setPersianCalendar(!VeyraConfig.persianCalendar);
                ((TextCheckCell) view).setChecked(VeyraConfig.persianCalendar);
            } else if (position == showProfileIdRow) {
                VeyraConfig.setShowProfileId(!VeyraConfig.showProfileId);
                ((TextCheckCell) view).setChecked(VeyraConfig.showProfileId);
            } else if (position == bypassRestrictionsRow) {
                VeyraConfig.setIgnoreContentRestrictions(!VeyraConfig.ignoreContentRestrictions);
                ((TextCheckCell) view).setChecked(VeyraConfig.ignoreContentRestrictions);
            } else if (position == webProxyRow) {
                presentFragment(new ProxyListActivity());
            } else if (position == githubRow) {
                Browser.openUrl(getParentActivity(), "https://github.com/x1cen/Veyra");
            } else if (position == noAdsRow || position == unlimitedLimitsRow || position == versionRow) {
                BulletinFactory.of(VeyraSettingsActivity.this).createSimpleBulletin(
                        R.drawable.msg_info,
                        isFarsi ? "این قابلیت به طور پیش‌فرض فعال و فعال‌سازی شده است" : "This feature is active and permanently optimized"
                ).show();
            }
        });

        return fragmentView;
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
            int position = holder.getAdapterPosition();
            return position != privacyHeaderRow && position != privacySectionRow &&
                    position != controlsHeaderRow && position != controlsSectionRow &&
                    position != uiHeaderRow && position != uiSectionRow &&
                    position != proxyHeaderRow && position != proxySectionRow &&
                    position != aboutHeaderRow && position != aboutSectionRow;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view;
            switch (viewType) {
                case 1:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 2:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                default:
                    view = new ShadowSectionCell(mContext);
                    break;
            }
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            boolean isFarsi = "fa".equals(LocaleController.getInstance().getCurrentLocale().getLanguage());
            switch (holder.getItemViewType()) {
                case 1: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == privacyHeaderRow) {
                        headerCell.setText(isFarsi ? "امنیت و حریم خصوصی" : "Privacy & Security");
                    } else if (position == controlsHeaderRow) {
                        headerCell.setText(isFarsi ? "کنترل‌ها و عملکرد تعاملی" : "Controls & Interaction");
                    } else if (position == uiHeaderRow) {
                        headerCell.setText(isFarsi ? "رابط کاربری و امکانات ویژه" : "UI & Features");
                    } else if (position == proxyHeaderRow) {
                        headerCell.setText(isFarsi ? "شبکه و عبور از فیلترینگ" : "Network & Proxy");
                    } else if (position == aboutHeaderRow) {
                        headerCell.setText(isFarsi ? "درباره ویرا" : "About Veyra");
                    }
                    break;
                }
                case 2: {
                    TextCheckCell checkCell = (TextCheckCell) holder.itemView;
                    if (position == readOnReplyRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "سین فقط با پاسخ دادن" : "Mark as Read on Reply",
                                isFarsi ? "پیام‌های دریافتی تا زمان ارسال پاسخ برای مخاطب خوانده (سین) نمی‌شوند" : "Incoming messages are not marked as read until you send a reply",
                                VeyraConfig.readOnReply, true, true
                        );
                    } else if (position == antiDeleteRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "آنتی‌دیلیت پیام‌ها" : "Anti-Delete Messages",
                                isFarsi ? "حفظ پیام‌های پاک شده با برچسب 'حذف شده'" : "Retain deleted messages with 'Deleted' badge",
                                VeyraConfig.antiDelete, true, true
                        );
                    } else if (position == ghostModeRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "حالت روح کامل" : "Full Ghost Mode",
                                isFarsi ? "عدم ارسال وضعیت خوانده شدن برای کلیه پیام‌ها" : "Never send read receipts for any messages",
                                VeyraConfig.ghostMode, true, true
                        );
                    } else if (position == hideTypingRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "مخفی کردن وضعیت نوشتن" : "Hide Typing Status",
                                isFarsi ? "جلوگیری از ارسال وضعیت 'در حال نوشتن...' به دیگران" : "Do not broadcast 'typing...' status",
                                VeyraConfig.hideTyping, true, true
                        );
                    } else if (position == blockSecretChatRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "مسدودسازی سکرت چت" : "Block Secret Chats",
                                isFarsi ? "رد خودکار درخواست شروع سکرت چت از دیگران" : "Automatically decline incoming secret chat requests",
                                VeyraConfig.blockSecretChat, true, false
                        );
                    } else if (position == confirmCallRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تأییدیه پیش از تماس" : "Confirm VoIP Calls",
                                isFarsi ? "نمایش هشدار تایید قبل از شروع تماس صوتی یا تصویری" : "Show confirmation dialog before making voice or video calls",
                                VeyraConfig.confirmCall, true, true
                        );
                    } else if (position == confirmLinkRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تأییدیه باز کردن لینک‌ها" : "Confirm External Links",
                                isFarsi ? "درخواست تایید کاربر قبل از باز شدن لینک در مرورگر" : "Require user confirmation before opening external links",
                                VeyraConfig.confirmLink, true, true
                        );
                    } else if (position == cleanUrlsRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "پاک‌سازی ترکرها از لینک" : "Clean Copied URLs",
                                isFarsi ? "حذف خودکار پارامترهای ردیابی (utm, fbclid, si) از لینک‌های کپی شده" : "Automatically strip tracking parameters from copied URLs",
                                VeyraConfig.cleanUrls, true, true
                        );
                    } else if (position == disableUndoRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "حذف انتظار ۵ ثانیه‌ای دکمه لغو" : "Skip 5s Undo Countdown",
                                isFarsi ? "حذف نوار معطلی ۵ ثانیه برای پاک‌سازی یا لغو عملیات" : "Instantly execute actions without waiting for the 5-second undo toast",
                                VeyraConfig.disableUndo, true, true
                        );
                    } else if (position == disableLinkPreviewRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "غیرفعال‌سازی پیش‌نمایش لینک به طور پیش‌فرض" : "Disable Link Preview by Default",
                                isFarsi ? "جلوگیری از ارسال درخواست پیش‌نمایش لینک به سرور" : "Prevent server from fetching previews when pasting links",
                                VeyraConfig.disableLinkPreviewByDefault, true, true
                        );
                    } else if (position == disableVibrationRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "غیرفعال کردن ویبره" : "Disable Vibration",
                                isFarsi ? "قطع کلیه فیدبک‌های لرزشی و هپتیک اپلیکیشن" : "Globally disable all haptic feedback and vibrations",
                                VeyraConfig.disableVibration, true, false
                        );
                    } else if (position == persianCalendarRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "تقویم خورشیدی (شمسی)" : "Persian Solar Hijri Calendar",
                                isFarsi ? "نمایش تاریخ‌ها با ماه و ارقام خورشیدی" : "Display dates in Persian Solar calendar",
                                VeyraConfig.persianCalendar, true, true
                        );
                    } else if (position == showProfileIdRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "شناسه تلگرام و DC در پروفایل" : "Show Telegram ID & DC in Profile",
                                isFarsi ? "دسترسی و کپی سریع User ID و شناسه دیتاسنتر کاربر" : "Show clickable User ID and Datacenter in user profiles",
                                VeyraConfig.showProfileId, true, true
                        );
                    } else if (position == bypassRestrictionsRow) {
                        checkCell.setTextAndValueAndCheck(
                                isFarsi ? "رد محدودیت‌های محتوایی اندروید" : "Ignore Content Restrictions",
                                isFarsi ? "رفع فیلتر محتوای حساس و کانال‌های محدودشده در اندروید" : "Bypass Android-only sensitive content and channel restrictions",
                                VeyraConfig.ignoreContentRestrictions, true, false
                        );
                    }
                    break;
                }
                case 3: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == webProxyRow) {
                        textCell.setText(isFarsi ? "پروکسی وب (WEB Proxy Tunnel)" : "WEB Proxy Tunnel Settings", true);
                    } else if (position == githubRow) {
                        textCell.setText(isFarsi ? "مخزن رسمی گیت‌هاب (x1cen/Veyra)" : "GitHub Repository (x1cen/Veyra)", false);
                    }
                    break;
                }
                case 4: {
                    TextDetailSettingsCell detailCell = (TextDetailSettingsCell) holder.itemView;
                    if (position == noAdsRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "حذف کامل تبلیغات اسپانسر" : "Permanent Ad-Free",
                                isFarsi ? "تمام پست‌های تبلیغاتی اسپانسری بدون نیاز به پرمیوم حذف شده‌اند" : "All sponsored channel ads permanently disabled",
                                true
                        );
                    } else if (position == unlimitedLimitsRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "ارتقای سقف پین، استیکر و فولدرها" : "Unlocked Telegram Limits",
                                isFarsi ? "۱۰۰ پین چت، ۵۰۰ استیکر دلخواه، ۱۰۰۰ گیف، ۳۰ فولدر" : "100 pinned chats, 500 fave stickers, 1000 gifs, 30 folders",
                                false
                        );
                    } else if (position == versionRow) {
                        detailCell.setTextAndValue(
                                isFarsi ? "نسخه کلاینت ویرا" : "Veyra Client Version",
                                "0.1.1-alpha (arm64-v8a)",
                                true
                        );
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == privacyHeaderRow || position == controlsHeaderRow || position == uiHeaderRow || position == proxyHeaderRow || position == aboutHeaderRow) {
                return 1;
            } else if (position == readOnReplyRow || position == antiDeleteRow || position == ghostModeRow || position == hideTypingRow || position == blockSecretChatRow ||
                    position == confirmCallRow || position == confirmLinkRow || position == cleanUrlsRow || position == disableUndoRow || position == disableLinkPreviewRow ||
                    position == disableVibrationRow || position == persianCalendarRow || position == showProfileIdRow || position == bypassRestrictionsRow) {
                return 2;
            } else if (position == webProxyRow || position == githubRow) {
                return 3;
            } else if (position == noAdsRow || position == unlimitedLimitsRow || position == versionRow) {
                return 4;
            } else {
                return 0;
            }
        }
    }
}
