package com.orbitalix.gui;

import com.orbitalix.simulation.FaultType;
import com.orbitalix.simulation.SimulationController;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Control panel for scenario selection, simulation execution control, and manual fault injection.
 * Demonstrates: association (talks to SimulationController contract), MVC separation.
 */
public class ControlPanel extends JPanel {

    private final SimulationController controller;
    private final List<JButton> scenarioButtons = new ArrayList<>();
    private final JComboBox<String> subsystemCombo;
    private final JComboBox<FaultType> faultCombo;
    private int selectedScenarioIndex = 0;

    public ControlPanel(SimulationController controller) {
        this.controller = controller;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("MISSION CONTROLS"));
        setPreferredSize(new Dimension(280, 600));

        // 1. Scenarios section
        add(createSectionHeader("DEMO SCENARIOS"));
        JPanel scenarioCardPanel = new JPanel(new GridLayout(6, 1, 4, 4));
        scenarioCardPanel.setOpaque(false);
        scenarioCardPanel.setMaximumSize(new Dimension(270, 200));

        List<String> scenarioNames = controller.getScenarioNames();
        for (int i = 0; i < scenarioNames.size(); i++) {
            final int index = i;
            JButton btn = createStyledButton((i + 1) + ". " + scenarioNames.get(i), false);
            btn.addActionListener(e -> {
                selectedScenarioIndex = index;
                highlightSelectedScenario();
                controller.loadScenario(index);
            });
            scenarioButtons.add(btn);
            scenarioCardPanel.add(btn);
        }
        highlightSelectedScenario();
        add(scenarioCardPanel);
        add(Box.createVerticalStrut(12));

        // 2. Playback execution controls
        add(createSectionHeader("SIMULATION PLAYBACK"));
        JPanel playPanel = new JPanel(new GridLayout(1, 3, 6, 0));
        playPanel.setOpaque(false);
        playPanel.setMaximumSize(new Dimension(270, 34));

        JButton startBtn = createStyledButton("▶ Start", true);
        startBtn.setForeground(Theme.STATUS_NOMINAL);
        startBtn.addActionListener(e -> controller.start());

        JButton pauseBtn = createStyledButton("❚❚ Pause", true);
        pauseBtn.setForeground(Theme.STATUS_WARNING);
        pauseBtn.addActionListener(e -> controller.pause());

        JButton resetBtn = createStyledButton("↺ Reset", true);
        resetBtn.setForeground(Theme.STATUS_CRITICAL);
        resetBtn.addActionListener(e -> controller.reset());

        playPanel.add(startBtn);
        playPanel.add(pauseBtn);
        playPanel.add(resetBtn);
        add(playPanel);
        add(Box.createVerticalStrut(10));

        // Speed Slider
        JPanel speedPanel = new JPanel(new BorderLayout(5, 2));
        speedPanel.setOpaque(false);
        speedPanel.setMaximumSize(new Dimension(270, 50));
        JLabel speedLabel = new JLabel("Speed: 2 ticks/sec");
        speedLabel.setFont(Theme.FONT_LABEL);
        speedLabel.setForeground(Theme.TEXT_MUTED);

        JSlider speedSlider = new JSlider(1, 10, 2);
        speedSlider.setOpaque(false);
        speedSlider.setForeground(Theme.ACCENT_CYAN);
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedLabel.setText("Speed: " + val + " ticks/sec");
            controller.setSpeed(val);
        });

        speedPanel.add(speedLabel, BorderLayout.NORTH);
        speedPanel.add(speedSlider, BorderLayout.CENTER);
        add(speedPanel);
        add(Box.createVerticalStrut(14));

        // 3. Fault Injection section
        add(createSectionHeader("MANUAL FAULT INJECTION"));
        JPanel injectPanel = new JPanel();
        injectPanel.setLayout(new BoxLayout(injectPanel, BoxLayout.Y_AXIS));
        injectPanel.setOpaque(false);
        injectPanel.setMaximumSize(new Dimension(270, 130));

        JLabel subLabel = new JLabel("Target Subsystem:");
        subLabel.setFont(Theme.FONT_LABEL);
        subLabel.setForeground(Theme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subsystemCombo = new JComboBox<>(controller.getSubsystemNames().toArray(new String[0]));
        styleCombo(subsystemCombo);
        subsystemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel faultLabel = new JLabel("Anomaly Fault Type:");
        faultLabel.setFont(Theme.FONT_LABEL);
        faultLabel.setForeground(Theme.TEXT_MUTED);
        faultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        faultCombo = new JComboBox<>(FaultType.values());
        styleCombo(faultCombo);
        faultCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton injectBtn = createStyledButton("⚡ Inject Fault Now", true);
        injectBtn.setForeground(Theme.STATUS_WARNING);
        injectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        injectBtn.setMaximumSize(new Dimension(270, 32));
        injectBtn.addActionListener(e -> {
            String selectedSub = (String) subsystemCombo.getSelectedItem();
            FaultType selectedType = (FaultType) faultCombo.getSelectedItem();
            if (selectedSub != null && selectedType != null) {
                controller.injectFault(selectedSub, selectedType);
            }
        });

        injectPanel.add(subLabel);
        injectPanel.add(Box.createVerticalStrut(2));
        injectPanel.add(subsystemCombo);
        injectPanel.add(Box.createVerticalStrut(6));
        injectPanel.add(faultLabel);
        injectPanel.add(Box.createVerticalStrut(2));
        injectPanel.add(faultCombo);
        injectPanel.add(Box.createVerticalStrut(8));
        injectPanel.add(injectBtn);

        add(injectPanel);
        add(Box.createVerticalGlue());
    }

    private JLabel createSectionHeader(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.FONT_LABEL_BOLD);
        lbl.setForeground(Theme.ACCENT_CYAN);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton createStyledButton(String text, boolean bold) {
        JButton btn = new JButton(text);
        btn.setFont(bold ? Theme.FONT_LABEL_BOLD : Theme.FONT_LABEL);
        btn.setBackground(Theme.PANEL_CARD);
        btn.setForeground(Theme.TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.PANEL_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        return btn;
    }

    private <T> void styleCombo(JComboBox<T> combo) {
        combo.setBackground(Theme.PANEL_CARD);
        combo.setForeground(Theme.TEXT_PRIMARY);
        combo.setFont(Theme.FONT_LABEL);
        combo.setMaximumSize(new Dimension(270, 26));
    }

    private void highlightSelectedScenario() {
        for (int i = 0; i < scenarioButtons.size(); i++) {
            JButton btn = scenarioButtons.get(i);
            if (i == selectedScenarioIndex) {
                btn.setBackground(new Color(0x23, 0x31, 0x66));
                btn.setBorder(BorderFactory.createLineBorder(Theme.ACCENT_CYAN, 1));
            } else {
                btn.setBackground(Theme.PANEL_CARD);
                btn.setBorder(BorderFactory.createLineBorder(Theme.PANEL_BORDER, 1));
            }
        }
    }
}
