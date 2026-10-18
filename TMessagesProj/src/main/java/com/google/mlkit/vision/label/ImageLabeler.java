package com.google.mlkit.vision.label;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import java.util.Collections;
import java.util.List;

public class ImageLabeler {
    public Task<List<ImageLabel>> process(InputImage image) {
        return Tasks.forResult(Collections.emptyList());
    }
}
