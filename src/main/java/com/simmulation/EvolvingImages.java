package com.simmulation;

import io.jenetics.Phenotype;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.System.getProperty;

/**
 * Command line version
 * "Start with parameters:
 *    evolve
 *    [--engine-properties <engine.properties>]
 *    [--input-image <image.png>]
 *    [--output-dir <evolving-images>]
 *    [--generations <generation count>]
 *    [--image-generation <generation-gap between stored images>]
 *
 */
public final class EvolvingImages {

    private static final int DEFAULT_GENERATIONS = 100_000;
    private static final int DEFAULT_IMAGE_GENERATION = 100;

    private static final String PARAM_KEY = "--engine-properties";
    private static final String IMAGE_KEY = "--input-image";
    private static final String OUTPUT_DIR_KEY = "--output-dir";
    private static final String GENERATION_COUNT_KEY = "--generations";
    private static final String GENERATION_IMAGE_GAP_KEY = "--image-generation";

    private static final String IMAGE_PATTERN = "image-%07d.png";

    private EngineParam _engineParam;
    private BufferedImage _image;
    private File _outputDir;
    private int _generations;
    private int _imageGeneration;

    public EvolvingImages(Map<String, String> params) {
            _engineParam = Optional
                            .ofNullable(params.get(PARAM_KEY))
                            .map(this::readEngineParam)
                            .orElse(EngineParam.DEFAULT);
            _image = Optional
                            .ofNullable(params.get(IMAGE_KEY))
                            .map(this::readImage)
                            .orElseGet(this::defaultImage);

            _outputDir = Optional
                            .ofNullable(params.get(OUTPUT_DIR_KEY))
                            .map(x -> new File(System.getProperty("user.dir") + x))
                            .orElse(new File(getProperty("user.dir"), "default"));
            _generations = Optional
                            .ofNullable(params.get(GENERATION_COUNT_KEY))
                            .map(Integer::parseInt)
                            .orElse(DEFAULT_GENERATIONS);

            _imageGeneration = Optional
                            .ofNullable(params.get(GENERATION_IMAGE_GAP_KEY))
                            .map(Integer::parseInt)
                            .orElse(DEFAULT_IMAGE_GENERATION);
    }

    private EngineParam readEngineParam(final String name) {
        try (InputStream in = new FileInputStream(name)) {
            final Properties props = new Properties();
            props.load(in);

            return EngineParam.load(props);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedImage readImage(final String name) {

        try {
            return ImageIO.read(new File(System.getProperty("user.dir") + name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BufferedImage defaultImage() {
        try (InputStream in = getClass().getClassLoader()
                                        .getResourceAsStream("pomeran.png"))
        {
            return ImageIO.read(in);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    public boolean run() {
        if (_engineParam != null) {
            if (!_outputDir.isDirectory()) {
                if (!_outputDir.mkdirs()) {
                    throw new IllegalArgumentException(
                                    "Can't create output directory " + _outputDir
                    );
                }
            }

            println("Starting evolution:");
            println("* Output dir:           " + _outputDir);
            println("* Generation count:     " + _generations);
            println("* Generation image gap: " + _imageGeneration);
            println("Engine parameters:");
            println("");
            println(_engineParam);
            println("");

            evolve(
                            _engineParam,
                            _image,
                            _outputDir,
                            _generations,
                            _imageGeneration
            );
        }

        return _engineParam != null;
    }

    private static void evolve(
                    final EngineParam params,
                    final BufferedImage image,
                    final File outputDir,
                    final long generations,
                    final int generationGap
    ) {
        println("Starting evolution.");
        final EvolvingImagesWorker worker = EvolvingImagesWorker.of(params, image);

        final AtomicReference<Phenotype<PolygonGene, Double>> latest =
                        new AtomicReference<>();

        final AtomicLong time = new AtomicLong(0);

        worker.start((current, best) -> {
            final long generation = current.getGeneration();

            if (generation%generationGap == 0 || generation == 1) {
                final double duration = System.currentTimeMillis() - time.get();
                final double speed = generationGap/(duration/1000.0);
                time.set(System.currentTimeMillis());

                final File file = new File(
                                outputDir,
                                format(IMAGE_PATTERN, generation)
                );

                final Phenotype<PolygonGene, Double> pt = best.getBestPhenotype();
                if (latest.get() == null || latest.get().compareTo(pt) < 0) {
                    log(
                                    "Writing '%s': fitness=%1.4f, speed=%1.2f.",
                                    file, pt.getFitness(), speed
                    );

                    latest.set(pt);
                    final PolygonChromosome ch =
                                    (PolygonChromosome)pt.getGenotype().getChromosome();

                    writeImage(file, ch, image.getWidth(), image.getHeight());
                } else {
                    log(
                                    "No improvement - %07d: fitness=%1.4f, speed=%1.2f.",
                                    generation, pt.getFitness(), speed
                    );
                }
            }

//            if (generation >= generations) {
//                worker.stop();
//            }
        });

        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void writeImage(
                    final File file,
                    final PolygonChromosome chromosome,
                    final int width,
                    final int height
    ) {
        final double MIN_SIZE = 500;
        final double scale = max(max(MIN_SIZE/width, MIN_SIZE/height), 1.0);
        final int w = (int)round(scale*width);
        final int h = (int)round(scale*height);

        try {
            final BufferedImage image = new BufferedImage(w, h, TYPE_INT_ARGB);
            final Graphics2D graphics = image.createGraphics();
            chromosome.draw(graphics, w, h);

            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void println(final Object format, final Object... args) {
        System.out.printf(Objects.toString(format) + "\n", args);
    }

    private static void log(final Object pattern, final Object... args) {
        final LocalDateTime now = LocalDateTime.now();
        final String tss = now.toLocalDate().toString() + ' ' +
                        now.toLocalTime();

        final String p = format("%s - ", tss) + pattern;
        System.out.println(format(p, args));
    }

}
