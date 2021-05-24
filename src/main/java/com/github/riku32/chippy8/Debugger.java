package com.github.riku32.chippy8;

import com.github.riku32.chippy8.VM.Chip8;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class Debugger extends JFrame implements Runnable {
    private final Chip8 chip8;

    // 0, 1 - PC and I
    // 2-18 - Registers
    // 19, 20 - DT and ST
    private final JTextArea[] registerValues = new JTextArea[20];

    private final JTable disassemblyTable;

    @Getter
    private boolean paused = false;

    public Debugger(final Chip8 chip8) throws IOException {
        this.chip8 = chip8;

        setTitle("Debugger");
        setSize(300, 600);
        setResizable(true);
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.VERTICAL;

        JTabbedPane tabbedPane = new JTabbedPane();

        // Toolbar
        {
            JToolBar toolBar = new JToolBar();
            toolBar.setFloatable(false);

            InputStream stepStream = getClass().getClassLoader().getResourceAsStream("step.png");
            Icon stepIcon = new ImageIcon(
                    Objects.requireNonNull(stepStream).readAllBytes(), "Step");
            stepStream.close();
            final JToggleButton stepButton = new JToggleButton(stepIcon);
            stepButton.addItemListener(e -> chip8.cycle());

            stepButton.setEnabled(false);

            // Pause icon
            InputStream pauseStream = getClass().getClassLoader().getResourceAsStream("pause.png");
            Icon pauseIcon = new ImageIcon(
                    Objects.requireNonNull(pauseStream).readAllBytes(), "Pause");
            pauseStream.close();
            JToggleButton pauseButton = new JToggleButton(pauseIcon);
            pauseButton.addItemListener(e -> {
                boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
                paused = enabled;
                stepButton.setEnabled(enabled);
            });

            toolBar.add(pauseButton);
            toolBar.add(stepButton);

            constraints.gridx = 0;
            constraints.gridy = 0;

            GridBagConstraints toolbarConstraints = new GridBagConstraints();
            toolbarConstraints.anchor = GridBagConstraints.NORTHEAST;
            toolbarConstraints.gridx = 0;
            toolbarConstraints.gridy = 0;
            add(toolBar, toolbarConstraints);
        }

        // Set for text areas to still have backgrounds despite being not editable, done for aesthetics
        UIManager.put("TextArea.inactiveBackground", UIManager.get("TextArea.background"));

        // Registers
        {
            JPanel registerPanel = new JPanel();
            GroupLayout registerLayout = new GroupLayout(registerPanel);
            registerPanel.setLayout(registerLayout);
            registerLayout.setAutoCreateContainerGaps(true);
            registerLayout.setAutoCreateGaps(true);

            GroupLayout.Group groupLabels = registerLayout.createParallelGroup();
            GroupLayout.Group groupFields = registerLayout.createParallelGroup();
            GroupLayout.Group groupRows = registerLayout.createSequentialGroup();

            registerLayout.setHorizontalGroup(registerLayout.createSequentialGroup()
                    .addGroup(groupLabels)
                    .addGroup(groupFields));

            registerLayout.setVerticalGroup(groupRows);

            // PC, I
            for (int i = 0; i < 2; i++) {
                JLabel label = new JLabel(i == 0 ? "PC" : "I");
                registerValues[i] = new JTextArea(String.valueOf(i == 0 ? chip8.getPc() : chip8.getIndex()));
                registerValues[i].setEditable(false);

                groupLabels.addComponent(label);
                groupFields.addComponent(registerValues[i]);
                groupRows.addGroup(registerLayout.createParallelGroup()
                        .addComponent(label)
                        .addComponent(registerValues[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
            }

            // Registers
            for (int i = 2; i < 18; i++) {
                JLabel label = new JLabel(String.format("V%s", i - 2));
                registerValues[i] = new JTextArea(String.valueOf(chip8.getV()[i - 2]));
                registerValues[i].setEditable(false);

                groupLabels.addComponent(label);
                groupFields.addComponent(registerValues[i]);
                groupRows.addGroup(registerLayout.createParallelGroup()
                        .addComponent(label)
                        .addComponent(registerValues[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
            }

            // Timers
            for (int i = 18; i < 20; i++) {
                JLabel label = new JLabel(i == 18 ? "DT" : "ST");
                registerValues[i] = new JTextArea(String.valueOf(i == 18 ? chip8.getDelayTimer() : chip8.getSoundTimer()));
                registerValues[i].setEditable(false);

                groupLabels.addComponent(label);
                groupFields.addComponent(registerValues[i]);
                groupRows.addGroup(registerLayout.createParallelGroup()
                        .addComponent(label)
                        .addComponent(registerValues[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
            }

            tabbedPane.addTab("Registers", registerPanel);
        }

        // Memory
        {
            JPanel memoryPanel = new JPanel();

            String[] columnNames = {
                    "Location",
                    "Value",
                    "Opcode"
            };

            disassemblyTable = new JTable(new Object[27][3], columnNames) {
                public boolean editCellAt(int row, int column, java.util.EventObject e) {
                    return false;
                }

                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!isRowSelected(row)) {
                        c.setBackground(row == 13 ? Color.DARK_GRAY : getBackground());
                        disassemblyTable.getSelectionModel().clearSelection();
                    }
                    return c;
                }
            };

            disassemblyTable.getTableHeader().setReorderingAllowed(false);

            JScrollPane scrollPane = new JScrollPane(disassemblyTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            memoryPanel.add(scrollPane);
            scrollPane.setPreferredSize(new Dimension(500, 570));
            disassemblyTable.setFillsViewportHeight(true);
            disassemblyTable.addRowSelectionInterval(10, 10);
            disassemblyTable.setCellSelectionEnabled(false);

            tabbedPane.addTab("Disassembly", memoryPanel);
        }

        constraints.fill = GridBagConstraints.BOTH;
        add(tabbedPane, constraints);

        pack();
        this.setResizable(false);
    }

    public void run() {
        // PC and Index
        registerValues[0].setText(String.format("%04X", chip8.getPc()));
        registerValues[1].setText(String.format("%04X", chip8.getIndex()));

        // Registers
        byte[] registers = chip8.getV();
        for (int i = 0; i < 16; i++)
            registerValues[i + 2].setText(String.format("%02X", registers[i] & 0xff));

        // Timers
        registerValues[18].setText(String.format("%02X", chip8.getDelayTimer()));
        registerValues[19].setText(String.format("%02X", chip8.getSoundTimer()));

        int currentPos = chip8.getPc();
        short[] memory = chip8.getMemory();

        // Update disassembly table
        int tableI = 0;
        for (int i = currentPos - 26; i <= currentPos + 26; i += 2) {
            boolean inBounds = (i >= 0) && (i < memory.length);
            if (!inBounds) continue;

            disassemblyTable.setValueAt(String.format("%04X", i), tableI, 0);
            disassemblyTable.setValueAt(String.format("%04X", memory[i] << 8 | memory[i+1]), tableI, 1);
            disassemblyTable.setValueAt(chip8.disassembleOpcode((short) i), tableI, 2);
            tableI++;
        }
    }
}
