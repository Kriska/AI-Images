package fmi.simmulation;

import io.jenetics.Genotype;
import io.jenetics.MeanAlterer;
import io.jenetics.Optimize;
import io.jenetics.TournamentSelector;
import io.jenetics.TruncationSelector;
import io.jenetics.engine.Codec;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.stat.MinMax;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import static java.util.Objects.requireNonNull;

/**
 * Performs the evolution.
 */
final class EvolvingImagesWorker {

    private final BufferedImage _image;
    private final BufferedImage _refImage;
    private final int[] _refImagePixels;
    private final ThreadLocal<BufferedImage> _workingImage;
    private final Engine<PolygonGene, Double> _engine;

    private volatile Thread _thread;

    private boolean _paused = false;
    private final Lock _pauseLock = new ReentrantLock();
    private final Condition _pauseCondition = _pauseLock.newCondition();

    /**
     * Create an new worker instance with the input parameters.
     */
    private EvolvingImagesWorker(final EngineParam param, final BufferedImage image) {
        _image = requireNonNull(image);

        _refImage = resizeImage( _image, param.getReferenceImageSize().width, param.getReferenceImageSize().height,
                                BufferedImage.TYPE_INT_ARGB);

        _workingImage = ThreadLocal.withInitial(() -> new BufferedImage(_refImage.getWidth(), _refImage.getHeight(),
                        BufferedImage.TYPE_INT_ARGB));

        _refImagePixels = _refImage.getData().getPixels(0, 0, _refImage.getWidth(), _refImage.getHeight(), (int[])null);

        final Codec<PolygonChromosome, PolygonGene> codec = Codec.of(
                                        Genotype.of(new PolygonChromosome(param.getPolygonCount(), param.getPolygonLength())),
                                        gt -> (PolygonChromosome) gt.getChromosome());

        _engine = Engine.builder(this::fitness, codec)
                        .populationSize(param.getPopulationSize())
                        .optimize(Optimize.MAXIMUM)
                        .maximalPhenotypeAge(50)
                        .survivorsSelector(new TruncationSelector<>())
                        .offspringSelector(new TournamentSelector<>(param.getTournamentSize()))
                        .alterers(
                                        new MeanAlterer<>(0.175),
                                        new PolygonMutator<>(param.getMutationRate(), param.getMutationMultitude()),
                                        new UniformCrossover<>(0.5))
                        .build();
    }

    private static BufferedImage resizeImage(final BufferedImage image, final int width, final int height, final int type) {
        final BufferedImage resizedImage = new BufferedImage(width, height, type);
        final Graphics2D g = resizedImage.createGraphics();
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return resizedImage;
    }

    BufferedImage getImage() {
        return _image;
    }

    /**
     * Calculate the fitness function - check pixel by pixel.
     */
    private double fitness(final PolygonChromosome chromosome) {
        final BufferedImage image = _workingImage.get();
        final Graphics2D g2 = image.createGraphics();
        final int width = image.getWidth();
        final int height = image.getHeight();

        chromosome.draw(g2, width, height);
        g2.dispose();

        final int[] refPixels = _refImagePixels;
        final int[] testPixels = image.getData().getPixels(0, 0, width, height, (int[])null);

        int difference = 0;
        int p = width*height*4 - 1; // 4 channels: rgba
        int idx = 0;
        do {
            if (idx++%4 != 0) { // ignore the alpha channel for fitness
                int dp = testPixels[p] - refPixels[p];
                difference += (dp < 0) ? -dp : dp;
            }
        } while (--p > 0);

        return 1.0 - difference/(width*height*3.0*256);
    }

    /**
     * Starts the evolution worker with the given evolution result callback. The
     * callback may be null.
     *
     * @param callback the {@code EvolutionResult} callback. The first parameter
     *        contains the current result and the second the best.
     */
    public void start(final BiConsumer<EvolutionResult<PolygonGene, Double>,EvolutionResult<PolygonGene, Double>> callback) {
        final Thread thread = new Thread(() -> {
                final MinMax<EvolutionResult<PolygonGene, Double>> best = MinMax.of();
                _engine.stream().limit(result -> !Thread.currentThread().isInterrupted())
                   .peek(best).forEach(r -> {
                                                waiting();
                                                if (callback != null) {
                                                    callback.accept(r, best.getMax());
                                                }
                    });
                });
        thread.start();
        _thread = thread;
    }

    private void waiting() {
        _pauseLock.lock();
        try {
            while (_paused) {
                try {
                    _pauseCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } finally {
            _pauseLock.unlock();
        }
    }

    public void stop() {
        resume();
        final Thread thread = _thread;
        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                _thread = null;
            }
        }
    }

    public void join() throws InterruptedException {
        final Thread thread = _thread;
        if (thread != null) {
            thread.join();
        }
    }

    public void pause() {
        _pauseLock.lock();
        try {
            _paused =  true;
        } finally {
            _pauseLock.unlock();
        }
    }

    public void resume() {
        _pauseLock.lock();
        try {
            _paused = false;
            _pauseCondition.signalAll();
        } finally {
            _pauseLock.unlock();
        }
    }

    public static EvolvingImagesWorker of( final EngineParam param,final BufferedImage image) {
        return new EvolvingImagesWorker(param, image);
    }

}
