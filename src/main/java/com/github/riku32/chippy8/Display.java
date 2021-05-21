package com.github.riku32.chippy8;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;

public class Display extends JPanel {
    // The size of each pixel on the rendered screen
    private static final int SCALE = 10;

    private static final int WIDTH = 64 * SCALE;
    private static final int HEIGHT = 32 * SCALE;

    @Setter
    private byte[] videoMemory;

    public Dimension getPreferredSize() {
        return new Dimension(WIDTH, HEIGHT);
    }

    @Setter
    @Getter
    private Color foreground;

    @Setter
    @Getter
    private Color background;

    public Display(byte[] videoMemory, Color foreground, Color background) {
        this.videoMemory = videoMemory;
        this.foreground = foreground;
        this.background = background;
        setIgnoreRepaint(true);
    }

    public Display(byte[] videoMemory) {
        this(videoMemory, Color.WHITE, Color.BLACK);
    }

    private void blit(Graphics g) {
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; ++x) {
                g.setColor(videoMemory[(y * 64) + x] == 0 ? background : foreground);
                g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
            }
        }
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(background);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        blit(g);

        g.dispose();
    }
}
