package com.github.riku32.chippy8;

import com.github.riku32.chippy8.VM.Chip8;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Objects;

public class Debugger extends JFrame implements Runnable {
    private final Chip8 chip8;

    // 0, 1 - PC and I
    // 2-18 - Registers
    // 19, 20 - DT and ST
    private final JTextField[] registerValues = new JTextField[20];

    private final JTable memoryTable;

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

            Icon stepIcon = new ImageIcon(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("step.png")).readAllBytes(), "Step");
            final JToggleButton stepButton = new JToggleButton(stepIcon);
            stepButton.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    chip8.cycle();
                }
            });

            stepButton.setEnabled(false);

            // Pause icon
            Icon pauseIcon = new ImageIcon(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("pause.png")).readAllBytes(), "Pause");
            JToggleButton pauseButton = new JToggleButton(pauseIcon);
            pauseButton.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    boolean enabled = e.getStateChange() == ItemEvent.SELECTED;
                    paused = enabled;
                    stepButton.setEnabled(enabled);
                }
            });

            toolBar.add(pauseButton);
            toolBar.add(stepButton);

            //constraints.anchor = GridBagConstraints.NORTHEAST;
            constraints.gridx = 0;
            constraints.gridy = 0;

            GridBagConstraints toolbarConstraints = new GridBagConstraints();
            toolbarConstraints.anchor = GridBagConstraints.NORTHEAST;
            toolbarConstraints.gridx = 0;
            toolbarConstraints.gridy = 0;
            add(toolBar, toolbarConstraints);
        }

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
                registerValues[i] = new JTextField(String.valueOf(i == 0 ? chip8.getPc() : chip8.getIndex()));
                registerValues[i].setEditable(false);

                groupLabels.addComponent(label);
                groupFields.addComponent(registerValues[i]);
                groupRows.addGroup(registerLayout.createParallelGroup()
                        .addComponent(label)
                        .addComponent(registerValues[i], GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
            }

            // Registers
            for (int i = 2; i < 18; i++) {
                JLabel label = new JLabel(String.format("V%s", i-1));
                registerValues[i] = new JTextField(String.valueOf(chip8.getRegister(i-2)));
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
                registerValues[i] = new JTextField(String.valueOf(i == 18 ? chip8.getDelayTimer() : chip8.getSoundTimer()));
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

            memoryTable = new JTable(new Object[21][3], columnNames) {
                public boolean editCellAt(int row, int column, java.util.EventObject e) {
                    return false;
                }

                public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                    Component c = super.prepareRenderer(renderer, row, column);
                    if (!isRowSelected(row)) {
                        c.setBackground(row == 10 ? Color.DARK_GRAY : getBackground());
                        memoryTable.getSelectionModel().clearSelection();
                    }
                    return c;
                }
            };

            memoryTable.getTableHeader().setReorderingAllowed(false);

            JScrollPane scrollPane = new JScrollPane(memoryTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            memoryPanel.add(scrollPane);
            scrollPane.setPreferredSize(new Dimension(500, 450));
            memoryTable.setFillsViewportHeight(true);
            memoryTable.addRowSelectionInterval(10, 10);
            memoryTable.setCellSelectionEnabled(false);

            tabbedPane.addTab("Memory", memoryPanel);
        }

        constraints.fill = GridBagConstraints.BOTH;
        add(tabbedPane, constraints);

        pack();
        this.setResizable(false);
    }

    public void run() {
        // Update registers
        for (int i = 0; i < 2; i++)
            registerValues[i].setText(String.format("%04X", i == 0 ? chip8.getPc() : chip8.getIndex()));
        for (int i = 2; i < 18; i++)
            registerValues[i].setText(String.format("%02X", chip8.getRegister(i-2) & 0xff));
        for (int i = 18; i < 20; i++)
            registerValues[i].setText(String.format("%02X", i == 18 ? chip8.getDelayTimer() : chip8.getSoundTimer()));

        int currentPos = chip8.getPc();
        short[] memory = chip8.getMemory();

        // Update memory table
        int tableI = 0;
        for (int i = currentPos - 20; i <= currentPos + 20; i += 2) {
            boolean inBounds = (i >= 0) && (i < memory.length);
            if (!inBounds)
                continue;

            memoryTable.setValueAt(String.format("%04X", i), tableI, 0);
            memoryTable.setValueAt(String.format("%04X", memory[i] << 8 | memory[i+1]), tableI, 1);
            memoryTable.setValueAt(chip8.disassembleOpcode((short) i), tableI, 2);
            tableI++;
        }
    }
}
