package com.simmulation;

import io.jenetics.Crossover;
import io.jenetics.Gene;
import io.jenetics.util.MSeq;
import io.jenetics.util.RandomRegistry;

import java.util.Random;

/**
 * Perform Uniform Crossover [UX] on the specified genes. A fixed mixing ratio
 * is used to combine genes from the first and second parents, e.g. using a
 * ratio of 0.5 would result in approximately 50% of genes coming from each
 * parent. This is typically a poor method of crossover, but empirical evidence
 * suggests that it is more exploratory and results in a larger part of the
 * problem space being searched.
 * <p>
 * This crossover policy evaluates each gene of the parent genes by choosing a
 * uniform random number {@code p} in the range [0, 1]. If {@code p} &lt;
 * {@code ratio}, the parent genes are swapped. This means with a ratio of 0.7,
 * 30% of the genes from the first parent and 70% from the second parent will
 * be selected for the first offspring (and vice versa for the second offspring).
 * <p>
 * The genes must have same lengths.
 *
 * @see <a href="http://en.wikipedia.org/wiki/Crossover_%28genetic_algorithm%29">
 *         Crossover techniques (Wikipedia)</a>
 * @see <a href="http://www.obitko.com/tutorials/genetic-algorithms/crossover-mutation.php">
 *         Crossover (Obitko.com)</a>
 * @see <a href="http://www.tomaszgwiazda.com/uniformX.htm">Uniform crossover</a>
 */
final class UniformCrossover<
                G extends Gene<?, G>,
                C extends Comparable<? super C>
                >
                extends Crossover<G, C>
{

    /**
     * Constructs an alterer with a given recombination probability.
     *
     * @param probability the crossover probability.
     * @throws IllegalArgumentException if the {@code probability} is not in the
     *         valid range of {@code [0, 1]}.
     */
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

    // Package private for testing purpose.
    static <T> void crossover(
                    final MSeq<T> that,
                    final MSeq<T> other,
                    final int index
    ) {
        assert index >= 0 : String.format(
                        "Crossover index must be within [0, %d) but was %d",
                        that.length(), index
        );

        that.swap(index, index + 1, other, index);
    }

}
