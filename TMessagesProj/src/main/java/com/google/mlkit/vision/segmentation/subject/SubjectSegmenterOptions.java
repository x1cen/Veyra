package com.google.mlkit.vision.segmentation.subject;

public class SubjectSegmenterOptions {
    public static class Builder {
        public Builder enableForegroundBitmap() { return this; }
        public Builder enableMultipleSubjects(SubjectResultOptions options) { return this; }
        public SubjectSegmenterOptions build() { return new SubjectSegmenterOptions(); }
    }

    public static class SubjectResultOptions {
        public static class Builder {
            public Builder enableSubjectBitmap() { return this; }
            public SubjectResultOptions build() { return new SubjectResultOptions(); }
        }
    }
}
