package com.github.riku32.chippy8;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.riku32.chippy8.VM.Chip8;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Chippy8 {
    private final Display display;

    private final Chip8 chip8;
    private final Debugger debugger;

    public Chippy8() throws IOException {
        Input keypad = new Input();
        this.chip8 = new Chip8(keypad);

        // Register custom theme because Java is ugly
        FlatDarkLaf.setup();

        final JFrame frame = new JFrame("Chippy8");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        display = new Display(chip8.getVideoMemory());
        frame.add(display);
        frame.setResizable(false);
        frame.setVisible(true);

        debugger = new Debugger(chip8);
        debugger.setVisible(false);

        final JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menuBar.add(menu);

        menu.add(new JMenuItem(new AbstractAction("Open") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter(
                        "CHIP-8 ROM", "rom", "ch8");
                chooser.setFileFilter(filter);

                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try {
                        chip8.loadRom(Files.readAllBytes(chooser.getSelectedFile().toPath()));
                    } catch (Exception ignored) {
                        System.err.println("ROM file could not be read");
                    }
                }
            }
        }));

        menu.add(new JMenuItem(new AbstractAction("Debugger") {
            public void actionPerformed(ActionEvent e) {
                debugger.setVisible(true);
            }
        }));

        final JMenu displayMenu = new JMenu("Display");
        menuBar.add(displayMenu);

        displayMenu.add(new JMenuItem(new AbstractAction("Background Color") {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(
                        frame,
                        "Choose Background Color",
                        display.getBackground());
                if (color != null) {
                    display.setBackground(color);
                    display.repaint();
                }
            }
        }));

        displayMenu.add(new JMenuItem(new AbstractAction("Foreground Color") {
            public void actionPerformed(ActionEvent e) {
                Color color = JColorChooser.showDialog(
                        frame,
                        "Choose Foreground Color",
                        display.getForeground());
                if (color != null) {
                    display.setForeground(color);
                    display.repaint();
                }
            }
        }));

        frame.setJMenuBar(menuBar);
        frame.pack();

        // Too many updates to swing will cause lag for the emulator
        // Debugger will run on another thread of its own at 60hz intervals
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(debugger, 0, 60, TimeUnit.MILLISECONDS);
    }

    private static final int FREQUENCY = 600;
    private static final long PERIOD_NANOS = 1000000000 / FREQUENCY;
    private static final int REFRESH_CYCLES = FREQUENCY / 60;

    // Game loop
    public void loop() {
        int refreshCycles = 0;

        while (true) {
            long initTime = System.nanoTime();

            if (!debugger.isPaused())
                chip8.cycle();

            // Screen, delay, and sound are all locked to 60hz
            if (refreshCycles % (REFRESH_CYCLES) == 0) {
                refreshCycles = 0;

                if (chip8.drawFlag) {
                    display.repaint();
                    chip8.drawFlag = false;
                }

                if (chip8.getDelayTimer() > 0)
                    chip8.setDelayTimer((byte) (chip8.getDelayTimer() - 0x01));

                // Audio not implemented, do timer anyways
                if (chip8.getSoundTimer() > 0)
                    chip8.setSoundTimer((byte) (chip8.getSoundTimer() - 0x01));
            }

            long endTime = System.nanoTime();
            refreshCycles++;

            long initNanos = System.nanoTime();
            while (System.nanoTime() < initNanos + PERIOD_NANOS - (endTime - initTime));
        }
    }

    public static void main(String[] args) throws IOException {
        Chippy8 chippy8 = new Chippy8();

        chippy8.loop();
    }
}
