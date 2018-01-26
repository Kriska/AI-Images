package com.simmulation;

import javax.swing.*;
import java.awt.*;

import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

/**
 * Draws the given {@code PolygonChromosome}.
 */
final class PolygonPanel extends JPanel {
    private int _width;
    private int _height;
    private PolygonChromosome _chromosome;

    public PolygonPanel(int width, int height) {
        _width = width;
        _height = height;
    }

    public PolygonPanel() {
        this(10, 10);
    }

    public void setDimension(final int width, final int height) {
        _width = width;
        _height = height;
    }

    public Dimension getDimension() {
        return new Dimension(_width, _height);
    }

    public void setChromosome(final PolygonChromosome chromosome) {
        _chromosome = requireNonNull(chromosome);
    }

    public PolygonChromosome getChromosome() {
        return _chromosome;
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (_chromosome != null) {
            _chromosome.draw((Graphics2D) g, width(), height());
        } else {
            g.setColor(Color.white);
            g.clearRect(0, 0, width(), height());
        }
    }

    private double scaleFactor() {
        final double sw = getWidth()/(double)_width;
        final double sh = getHeight()/(double)_height;
        return min(sw, sh);
    }

    private int width() {
        return (int) (_width*scaleFactor());
    }

    private int height() {
        return (int) (_height*scaleFactor());
    }

}
