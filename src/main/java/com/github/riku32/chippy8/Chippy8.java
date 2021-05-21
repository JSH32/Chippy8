package com.github.riku32.chippy8;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.riku32.chippy8.VM.Chip8;
import lombok.Getter;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Chippy8 {
    private final Display display;

    private final Chip8 chip8;
    private final Debugger debugger;
    private final About about;

    /**
     * Check if file has extensions
     */
    private boolean hasExtension(String name, String ... extensions) {
        for (String e : extensions)
            if (name.toLowerCase(Locale.ROOT).endsWith(e))
                return true;
        return false;
    }

    public Chippy8(int frequency) throws IOException {
        Input keypad = new Input();
        this.chip8 = new Chip8(keypad);

        setFrequency(frequency);

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

        about = new About();
        about.setVisible(false);


        final JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        fileMenu.add(new JMenuItem(new AbstractAction("Open ROM or State") {
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Load rom or state");
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                        "CHIP-8 ROM", "ch8"));
                chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                        "Chippy State", "state"));

                if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    byte[] buffer;
                    try {
                        buffer = Files.readAllBytes(chooser.getSelectedFile().toPath());
                    } catch (Exception ignored) {
                        JOptionPane.showMessageDialog(frame, "Could not read ROM file",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (hasExtension(chooser.getSelectedFile().getName(), "state")) {
                        try {
                            chip8.loadState(buffer);
                            display.repaint();
                        } catch (Exception ignored) {
                            JOptionPane.showMessageDialog(frame, "There was a problem deserializing the state",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } else if (hasExtension(chooser.getSelectedFile().getName(), "ch8", "rom")) {
                        chip8.loadRom(buffer);
                    } else {
                        JOptionPane.showMessageDialog(frame, "Invalid file type provided",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }));

        fileMenu.add(new JMenuItem(new AbstractAction("Save state") {
            public void actionPerformed(ActionEvent e) {

                byte[] state;
                try {
                    state = chip8.saveState();
                } catch (Exception ignored) {
                    JOptionPane.showMessageDialog(frame, "Could not get the state",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                JFileChooser chooser = new JFileChooser();
                chooser.setDialogTitle("Save state");
                chooser.setFileFilter(new FileNameExtensionFilter(
                        "Chippy State", "state"));

                if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
                    try {
                        String pathToSave = chooser.getSelectedFile().getAbsolutePath();
                        if (!pathToSave.endsWith(".state"))
                            pathToSave += ".state";
                        File file = new File(pathToSave);

                        if (file.createNewFile()) {
                            FileOutputStream outputStream = new FileOutputStream(file);
                            outputStream.write(state);
                        } else {
                            JOptionPane.showMessageDialog(frame, "That file already exists!",
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ignored) {
                        JOptionPane.showMessageDialog(frame, "Could not save the state",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }));

        final JMenu systemMenu = new JMenu("System");
        menuBar.add(systemMenu);

        systemMenu.add(new JMenuItem(new AbstractAction("Debugger") {
            public void actionPerformed(ActionEvent e) {
                debugger.setVisible(true);
            }
        }));

        systemMenu.add(new JMenuItem(new AbstractAction("Frequency") {
            public void actionPerformed(ActionEvent e) {
                SpinnerNumberModel sModel = new SpinnerNumberModel(getFrequency(), 100, 10000, 1);
                JSpinner spinner = new JSpinner(sModel);
                int option = JOptionPane.showOptionDialog(
                        null,
                        spinner,
                        "Set frequency",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        null,
                        null);
                if (option == JOptionPane.OK_OPTION) {
                    setFrequency((Integer) spinner.getValue());
                }
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

        final JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        helpMenu.add(new JMenuItem(new AbstractAction("About") {
            public void actionPerformed(ActionEvent e) {
                about.setVisible(true);
            }
        }));

        frame.setJMenuBar(menuBar);
        frame.pack();

        // Too many updates to swing will cause lag for the emulator
        // Debugger will run on another thread of its own at 60hz intervals
        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(debugger, 0, 60, TimeUnit.MILLISECONDS);
    }

    @Getter
    private int frequency;
    private long periodNanos;
    private int refreshCycles;

    /**
     * Change frequency
     *
     * @param frequency in hz
     */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
        this.periodNanos = 1000000000 / frequency;
        this.refreshCycles = frequency / 60;
    }

    // Game loop
    public void loop() {
        int refreshCycles = 0;

        while (true) {
            long initTime = System.nanoTime();

            if (!debugger.isPaused())
                chip8.cycle();

            // Screen, delay, and sound are all locked to 60hz
            if (refreshCycles % (this.refreshCycles) == 0) {
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
            while (System.nanoTime() < initNanos + periodNanos - (endTime - initTime));
        }
    }

    public static void main(String[] args) throws IOException {
        Chippy8 chippy8 = new Chippy8(600);

        chippy8.loop();
    }
}
