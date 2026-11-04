package org.veyra.client;

import android.content.Context;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

public class VeyraMediaConfirm {

    public static void showConfirm(Context context, BaseFragment fragment, String title, String message, Runnable onConfirm) {
        if (context == null && fragment != null) {
            context = fragment.getParentActivity();
        }
        if (context == null) {
            onConfirm.run();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(LocaleController.getString(R.string.Send), (dialog, which) -> onConfirm.run());
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        AlertDialog dialog = builder.create();
        if (fragment != null) {
            fragment.showDialog(dialog);
        } else {
            dialog.show();
        }
    }
}
