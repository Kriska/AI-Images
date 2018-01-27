package fmi.simmulation;

import io.jenetics.AbstractChromosome;
import io.jenetics.Chromosome;
import io.jenetics.util.ISeq;

import java.awt.*;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

final class PolygonChromosome extends AbstractChromosome<PolygonGene> {
    private static final long serialVersionUID = 1L;

    public PolygonChromosome(final ISeq<PolygonGene> genes) {
        super(genes);
    }

    public PolygonChromosome(final int polygonCount, final int polygonLength) {
        super(PolygonGene.seq(polygonCount, polygonLength));
    }

    public Chromosome<PolygonGene> newInstance(final ISeq<PolygonGene> genes) {
        return new PolygonChromosome(genes);
    }

    public Chromosome<PolygonGene> newInstance() {
        return new PolygonChromosome(length(), getGene().getAllele().length());
    }

    public void draw(final Graphics2D g, final int width, final int height) {
        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, width, height);

        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        for (PolygonGene gene : this) {
            gene.getAllele().draw(g, width, height);
        }
    }

}
