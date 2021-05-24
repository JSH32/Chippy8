package com.github.riku32.chippy8;

import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Objects;

public class About extends JFrame {
    public About() throws IOException {
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        InputStream iconStream = getClass().getClassLoader().getResourceAsStream("icon.png");
        ImageIcon icon = new ImageIcon(
                Objects.requireNonNull(iconStream).readAllBytes(), "Icon");
        iconStream.close();
        JLabel iconLabel = new JLabel(new ImageIcon(icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
        iconLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = new JLabel("Chippy8");
        title.setFont(new Font(title.getFont().getName(), Font.PLAIN, 25));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel desc = new JLabel("<html><center>A cross platform, open source,<br>CHIP-8 interpreter and debugger<br>made in Java</center></html>");
        desc.setFont(new Font(title.getFont().getName(), Font.PLAIN, 15));
        desc.setHorizontalAlignment(JLabel.CENTER);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        desc.setBorder(BorderFactory.createEmptyBorder(10, 20, 10,20));

        // Button that links to github
        JLabel source = new JLabel("Released under the MIT license");
        source.setAlignmentX(Component.CENTER_ALIGNMENT);
        source.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        source.setForeground(new Color(135, 206, 235));
        source.addMouseListener(new MouseAdapter() {
            @SneakyThrows
            @Override
            public void mouseClicked(MouseEvent e) {
                Desktop.getDesktop().browse(new URI("https://github.com/Riku32/Chippy8"));
            }
        });

        add(iconLabel);
        add(title);
        add(desc);
        add(source);
        pack();

        setResizable(false);
    }
}
