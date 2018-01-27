package fmi.simmulation;

import io.jenetics.Gene;
import io.jenetics.util.ISeq;
import io.jenetics.util.MSeq;
import io.jenetics.util.Mean;

import static java.util.Objects.requireNonNull;

/**
 * Represents a fixed size polygon with its fill color.
 */
final class PolygonGene implements Gene<Polygon, PolygonGene>, Mean<PolygonGene> {
    private final Polygon _polygon;

    private PolygonGene(final Polygon polygon) {
        _polygon = requireNonNull(polygon);
    }

    public Polygon getAllele() {
        return _polygon;
    }

    public boolean isValid() {
        return true;
    }

    public PolygonGene newInstance() {
        return new PolygonGene(Polygon.newRandom(_polygon.length()));
    }

    public PolygonGene newInstance(final Polygon polygon) {
        return of(polygon);
    }

    public PolygonGene mean(final PolygonGene other) {
        return of(getAllele().mean(other.getAllele()));
    }

    static ISeq<PolygonGene> seq(final int polygonCount, final int polygonLength) {
        return MSeq.<PolygonGene>ofLength(polygonCount)
                        .fill(() -> of(Polygon.newRandom(polygonLength)))
                        .toISeq();
    }

    public static PolygonGene of(final Polygon polygon) {
        return new PolygonGene(polygon);
    }

}
