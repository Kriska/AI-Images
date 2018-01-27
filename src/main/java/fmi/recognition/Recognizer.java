package fmi.recognition;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

import fmi.simmulation.EvolvingImages;

import static java.lang.System.getProperty;

/**
 *
 * pre-trained inception model from here: https://storage.googleapis.com/download.tensorflow.org/models/inception_dec_2015.zip
 *
 */
public class Recognizer {

    private static final String TENSORFLOW_GRAPH = "tensorflow_inception_graph.pb";
    private static final String TENSORFLOW_LABEL_STRING = "imagenet_comp_graph_label_strings.txt";

    private byte[] graphDef;
    private List<String> labels;

    public Recognizer(File inceptionDirFile, File imageDirFile) {

        graphDef = readAllBytesOrExit(Paths.get(inceptionDirFile.getAbsolutePath(),TENSORFLOW_GRAPH));
        labels = readAllLinesOrExit(Paths.get(inceptionDirFile.getAbsolutePath(),TENSORFLOW_LABEL_STRING));
        byte[] imageBytes = readAllBytesOrExit(imageDirFile.toPath());
        if(imageBytes == null) {
            throw  new RuntimeException("Image bytes is null.");
        }
        try (Tensor image = Tensor.create(imageBytes)) {
            float[] labelProbabilities = executeInceptionGraph(graphDef, image);
            int bestLabelIdx = maxIndex(labelProbabilities);
            System.out.println(
                    String.format(
                            "BEST MATCH: %s (%.2f%% likely)",
                            labels.get(bestLabelIdx), labelProbabilities[bestLabelIdx] * 100f));
        }
    }

    private static float[] executeInceptionGraph(byte[] graphDef, Tensor image) {
        try (Graph g = new Graph()) {
            g.importGraphDef(graphDef);
            try (Session s = new Session(g);
                    Tensor result = s.runner().feed("DecodeJpeg/contents", image).fetch("softmax").run().get(0)) {
                final long[] rshape = result.shape();
                if (result.numDimensions() != 2 || rshape[0] != 1) {
                    throw new RuntimeException(
                            String.format(
                                    "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                                    Arrays.toString(rshape)));
                }
                int nlabels = (int) rshape[1];
                return result.copyTo(new float[1][nlabels])[0];
            }
        }
    }

    private static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }

    private static byte[] readAllBytesOrExit(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(1);
        }
        return null;
    }

    private static List<String> readAllLinesOrExit(Path path) {
        try {
            return Files.readAllLines(path, Charset.forName("UTF-8"));
        } catch (IOException e) {
            System.err.println("Failed to read [" + path + "]: " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

}
