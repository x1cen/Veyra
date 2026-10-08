package org.telegram.ui;

public class VeyraGhostExceptionsActivity extends VeyraGhostFeatureActivity {
    public VeyraGhostExceptionsActivity() {
        super(org.telegram.messenger.VeyraConfig.CATEGORY_GHOST_TYPING);
    }

    public VeyraGhostExceptionsActivity(int category) {
        super(category);
    }
}
