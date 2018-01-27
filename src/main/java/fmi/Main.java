package fmi;

import fmi.recognition.Recognizer;
import fmi.simmulation.EvolvingImages;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.System.getProperty;

public class Main {

    private static final String IMAGE_DIR = "--image-dir";
    private static final String INCEPTION_DIR = "--inception-dir";

    public static void main(String[] args) {
        if (args.length >= 1 && "evolve".equalsIgnoreCase(args[0])) {
            final Map<String, String> params = toMap(args);

            File inceptionDirFile = Optional
                            .ofNullable(params.get(INCEPTION_DIR))
                            .map(x -> new File(System.getProperty("user.dir") + x))
                            .orElse(new File(getProperty("user.dir"), "default"));
            File imageDirFile = Optional
                            .ofNullable(params.get(IMAGE_DIR))
                            .map(x -> new File(System.getProperty("user.dir") + x))
                            .orElse(new File(getProperty("user.dir"), "default"));

            new Recognizer(inceptionDirFile, imageDirFile);
            EvolvingImages ev = new EvolvingImages(params);
            ev.run();
        }
    }


    private static Map<String, String> toMap(final String[] args) {
        final Map<String, String> props = new HashMap<>();
        for (int i = 1; i < args.length; i += 2) {
            props.put(args[i], i + 1 < args.length ? args[i + 1] : null);
        }

        return props;
    }
}
