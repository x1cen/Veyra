package com.google.mlkit.vision.label;

import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

public class ImageLabeling {
    public static ImageLabeler getClient(ImageLabelerOptions options) {
        return new ImageLabeler();
    }
}
