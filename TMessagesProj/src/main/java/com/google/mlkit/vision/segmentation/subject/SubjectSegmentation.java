package com.google.mlkit.vision.segmentation.subject;

public class SubjectSegmentation {
    public static SubjectSegmenter getClient(SubjectSegmenterOptions options) {
        return new SubjectSegmenter();
    }
}
