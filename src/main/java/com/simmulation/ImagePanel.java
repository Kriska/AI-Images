package com.simmulation;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static java.lang.Math.min;
import static java.util.Objects.requireNonNull;

/**
 * Draws an given {@code BufferedImage}.
 */
final class ImagePanel extends JPanel {

    private BufferedImage _image;

    ImagePanel() {
    }

    public void setImage(final BufferedImage image) {
        _image = requireNonNull(image);
        repaint();
    }

    public BufferedImage getImage() {
        return _image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (_image != null) {
            g.drawImage(_image, 0, 0, width(), height(), null);
        }
    }

    private double scaleFactor() {
        final double sw = getWidth()/(double)_image.getWidth();
        final double sh = getHeight()/(double)_image.getHeight();
        return min(sw, sh);
    }

    private int width() {
        return (int)(_image.getWidth()*scaleFactor());
    }

    private int height() {
        return (int)(_image.getHeight()*scaleFactor());
    }

}
