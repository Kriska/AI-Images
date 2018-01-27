package fmi.simmulation;

import io.jenetics.Crossover;
import io.jenetics.Gene;
import io.jenetics.util.MSeq;
import io.jenetics.util.RandomRegistry;

import java.util.Random;

final class UniformCrossover<G extends Gene<?, G>, C extends Comparable<? super C>> extends Crossover<G, C> {

    public UniformCrossover(final double probability) {
        super(probability);
    }

    @Override
    protected int crossover(final MSeq<G> that, final MSeq<G> other) {
        assert that.length() == other.length();

        int alteredGenes = 0;
        final Random random = RandomRegistry.getRandom();
        for (int i = 0; i < that.length(); ++i) {
            if (random.nextFloat() < getProbability()) {
                crossover(that, other, i);
                ++alteredGenes;
            }
        }

        return alteredGenes;
    }

    static <T> void crossover(final MSeq<T> that, final MSeq<T> other, final int index) {
        assert index >= 0 : String.format("Crossover index must be within [0, %d) but was %d", that.length(), index);

        that.swap(index, index + 1, other, index);
    }

}
