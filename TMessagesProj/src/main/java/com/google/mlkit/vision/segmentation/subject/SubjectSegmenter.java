package com.google.mlkit.vision.segmentation.subject;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import java.util.Collections;

public class SubjectSegmenter {
    public Task<SubjectSegmentationResult> process(InputImage image) {
        return Tasks.forResult(new SubjectSegmentationResult());
    }
}
