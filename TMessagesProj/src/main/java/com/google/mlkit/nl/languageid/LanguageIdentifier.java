package com.google.mlkit.nl.languageid;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class LanguageIdentifier {
    public Task<String> identifyLanguage(String text) {
        return Tasks.forResult("und");
    }
}
