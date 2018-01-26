package com.simmulation;

import com.simmulation.PolygonGene;
import io.jenetics.Chromosome;
import io.jenetics.Mutator;
import io.jenetics.MutatorResult;

import java.util.Random;

/**
 * Polygon mutator class.
 *
 * @param <C> the fitness type
 */
final class PolygonMutator<C extends Comparable<? super C>>
                extends Mutator<PolygonGene, C>
{

    private final float _rate;
    private final float _magnitude;

    PolygonMutator(final float rate, final float magnitude) {
        super(1.0);
        _rate = rate;
        _magnitude = magnitude;
    }

    @Override
    protected MutatorResult<Chromosome<PolygonGene>> mutate(
                    final Chromosome<PolygonGene> chromosome,
                    final double p,
                    final Random random
    ) {
        return MutatorResult.of(
                        chromosome.newInstance(chromosome.toSeq().map(this::mutate)),
                        chromosome.length()
        );
    }

    private PolygonGene mutate(final PolygonGene gene) {
        return gene.newInstance(gene.getAllele().mutate(_rate, _magnitude));
    }

}
