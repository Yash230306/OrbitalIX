package com.orbitalix.gui;

import com.orbitalix.simulation.FaultType;
import com.orbitalix.simulation.SimulationController;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Tactical control console: scripted scenario loader, simulation playback, subsystem health
 * LED matrix, manual anomaly injector, AND the new Custom Condition Builder for live slider control.
 * A two-tab switcher at the top lets users pick between pre-built scenarios and free-form sliders.
 * Demonstrates: CardLayout tab switching, Observer pattern, MVC separation, Facade pattern.
 */
public class ControlPanel extends JPanel implements TickListener {

    private final SimulationController controller;
    private final List<JButton> scenarioButtons = new ArrayList<>();
    private final JComboBox<String> subsystemCombo;
    private final JComboBox<FaultType> faultCombo;
    private final Map<String, JLabel> subsystemLeds = new HashMap<>();
    private final CustomConditionPanel customConditionPanel;
    private int selectedScenarioIndex = 0;

    // Tab switching
    private final CardLayout modeCardLayout = new CardLayout();
    private final JPanel modeCardPanel = new JPanel(modeCardLayout);

    public ControlPanel(SimulationController controller) {
        this.controller = controller;
        this.customConditionPanel = new CustomConditionPanel(controller);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("MISSION CONTROL"));
        setPreferredSize(new Dimension(300, 700));

        // ── Mode switcher tab bar ─────────────────────────────────────────────
        JPanel modeBar = new JPanel(new GridLayout(1, 2, 2, 0));
        modeBar.setOpaque(false);
        modeBar.setMaximumSize(new Dimension(290, 32));
        modeBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton scenarioTab = createTabButton("SCENARIOS", false);
        JButton customTab   = createTabButton("BUILD CONDITIONS", true);

        scenarioTab.addActionListener(e -> switchMode("SCENARIOS", scenarioTab, customTab));
        customTab.addActionListener(e -> switchMode("CUSTOM", customTab, scenarioTab));

        modeBar.add(scenarioTab);
        modeBar.add(customTab);
        add(modeBar);
        add(Box.createVerticalStrut(6));

        // ── Card 1: Scenario Mode ─────────────────────────────────────────────
        JPanel scenarioCard = new JPanel();
        scenarioCard.setLayout(new BoxLayout(scenarioCard, BoxLayout.Y_AXIS));
        scenarioCard.setOpaque(false);

        scenarioCard.add(createSectionHeader("TACTICAL SCENARIOS"));
        JPanel scenarioCardPanel = new JPanel(new GridLayout(6, 1, 4, 4));
        scenarioCardPanel.setOpaque(false);
        scenarioCardPanel.setMaximumSize(new Dimension(285, 205));
        scenarioCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<String> scenarioNames = controller.getScenarioNames();
        for (int i = 0; i < scenarioNames.size(); i++) {
            final int index = i;
            JButton btn = createStyledButton((i + 1) + ". " + scenarioNames.get(i), false);
            btn.addActionListener(e -> {
                selectedScenarioIndex = index;
                highlightSelectedScenario();
                controller.loadScenario(index);
                customConditionPanel.resetToNominal();
            });
            scenarioButtons.add(btn);
            scenarioCardPanel.add(btn);
        }
        highlightSelectedScenario();
        scenarioCard.add(scenarioCardPanel);
        scenarioCard.add(Box.createVerticalStrut(10));

        // ── Playback controls ─────────────────────────────────────────────────
        scenarioCard.add(createSectionHeader("SIMULATION PLAYBACK"));
        JPanel playPanel = new JPanel(new GridLayout(1, 3, 6, 0));
        playPanel.setOpaque(false);
        playPanel.setMaximumSize(new Dimension(285, 34));
        playPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton startBtn = createStyledButton("▶ START", true);
        startBtn.setForeground(Theme.STATUS_NOMINAL);
        startBtn.addActionListener(e -> controller.start());

        JButton pauseBtn = createStyledButton("⏸ PAUSE", true);
        pauseBtn.setForeground(Theme.STATUS_WARNING);
        pauseBtn.addActionListener(e -> controller.pause());

        JButton resetBtn = createStyledButton("↺ RESET", true);
        resetBtn.setForeground(Theme.STATUS_CRITICAL);
        resetBtn.addActionListener(e -> {
            controller.reset();
            customConditionPanel.resetToNominal();
        });

        playPanel.add(startBtn);
        playPanel.add(pauseBtn);
        playPanel.add(resetBtn);
        scenarioCard.add(playPanel);
        scenarioCard.add(Box.createVerticalStrut(8));

        // Speed Slider
        JPanel speedPanel = new JPanel(new BorderLayout(5, 2));
        speedPanel.setOpaque(false);
        speedPanel.setMaximumSize(new Dimension(285, 48));
        speedPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel speedLabel = new JLabel("Engine Speed: 2 ticks/sec");
        speedLabel.setFont(Theme.FONT_LABEL);
        speedLabel.setForeground(Theme.TEXT_MUTED);

        JSlider speedSlider = new JSlider(1, 10, 2);
        speedSlider.setOpaque(false);
        speedSlider.setForeground(Theme.ACCENT_CYAN);
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(e -> {
            int val = speedSlider.getValue();
            speedLabel.setText("Engine Speed: " + val + " ticks/sec");
            controller.setSpeed(val);
        });

        speedPanel.add(speedLabel, BorderLayout.NORTH);
        speedPanel.add(speedSlider, BorderLayout.CENTER);
        scenarioCard.add(speedPanel);
        scenarioCard.add(Box.createVerticalStrut(10));

        // ── Subsystem LED health matrix ───────────────────────────────────────
        scenarioCard.add(createSectionHeader("SUBSYSTEM HEALTH MATRIX"));
        JPanel ledGrid = new JPanel(new GridLayout(1, 4, 4, 0));
        ledGrid.setOpaque(false);
        ledGrid.setMaximumSize(new Dimension(285, 52));
        ledGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<String> subNames = controller.getSubsystemNames();
        for (String sub : subNames) {
            JPanel item = new JPanel();
            item.setLayout(new BoxLayout(item, BoxLayout.Y_AXIS));
            item.setBackground(Theme.PANEL_CARD);
            item.setBorder(BorderFactory.createLineBorder(new Color(0x23, 0x31, 0x5C)));

            JLabel dot = new JLabel("●");
            dot.setFont(Theme.FONT_TITLE);
            dot.setForeground(Theme.STATUS_NOMINAL);
            dot.setAlignmentX(Component.CENTER_ALIGNMENT);
            subsystemLeds.put(sub, dot);

            JLabel nameLbl = new JLabel(sub.substring(0, Math.min(4, sub.length())).toUpperCase());
            nameLbl.setFont(Theme.FONT_LABEL_BOLD);
            nameLbl.setForeground(Theme.TEXT_PRIMARY);
            nameLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            item.add(Box.createVerticalGlue());
            item.add(dot);
            item.add(nameLbl);
            item.add(Box.createVerticalGlue());
            ledGrid.add(item);
        }
        scenarioCard.add(ledGrid);
        scenarioCard.add(Box.createVerticalStrut(10));

        // ── Fault injection ───────────────────────────────────────────────────
        scenarioCard.add(createSectionHeader("MANUAL ANOMALY INJECTION"));
        JPanel injectPanel = new JPanel();
        injectPanel.setLayout(new BoxLayout(injectPanel, BoxLayout.Y_AXIS));
        injectPanel.setOpaque(false);
        injectPanel.setMaximumSize(new Dimension(285, 135));
        injectPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLabel = new JLabel("Target Subsystem:");
        subLabel.setFont(Theme.FONT_LABEL);
        subLabel.setForeground(Theme.TEXT_MUTED);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subsystemCombo = new JComboBox<>(controller.getSubsystemNames().toArray(new String[0]));
        styleCombo(subsystemCombo);
        subsystemCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel faultLabel = new JLabel("Anomaly Fault Model:");
        faultLabel.setFont(Theme.FONT_LABEL);
        faultLabel.setForeground(Theme.TEXT_MUTED);
        faultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        faultCombo = new JComboBox<>(FaultType.values());
        styleCombo(faultCombo);
        faultCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton injectBtn = createStyledButton("⚡ INJECT FAULT LIVE", true);
        injectBtn.setForeground(Theme.STATUS_WARNING);
        injectBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        injectBtn.setMaximumSize(new Dimension(285, 32));
        injectBtn.addActionListener(e -> {
            String selectedSub  = (String) subsystemCombo.getSelectedItem();
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
        scenarioCard.add(injectPanel);
        scenarioCard.add(Box.createVerticalGlue());

        // ── Card 2: Custom Condition Builder ──────────────────────────────────
        // ── Wire cards into CardLayout ─────────────────────────────────────────
        modeCardPanel.setOpaque(false);
        modeCardPanel.add(scenarioCard, "SCENARIOS");
        modeCardPanel.add(customConditionPanel, "CUSTOM");
        modeCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(modeCardPanel);
        modeCardLayout.show(modeCardPanel, "CUSTOM");
    }

    // ── Tab switching ─────────────────────────────────────────────────────────
    private void switchMode(String cardName, JButton selected, JButton deselected) {
        SoundEffects.getInstance().playClick();
        modeCardLayout.show(modeCardPanel, cardName);
        activateTab(selected);
        deactivateTab(deselected);
    }

    private void activateTab(JButton btn) {
        btn.setBackground(new Color(0x23, 0x31, 0x66));
        btn.setForeground(Theme.ACCENT_CYAN);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.ACCENT_CYAN, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void deactivateTab(JButton btn) {
        btn.setBackground(new Color(0x14, 0x1C, 0x38));
        btn.setForeground(Theme.TEXT_MUTED);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0x23, 0x31, 0x5C), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JButton createTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_LABEL_BOLD);
        btn.setFocusPainted(false);
        if (active) activateTab(btn); else deactivateTab(btn);
        return btn;
    }

    private JLabel createSectionHeader(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(Theme.FONT_LABEL_BOLD);
        lbl.setForeground(Theme.ACCENT_CYAN);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
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
        combo.setMaximumSize(new Dimension(285, 26));
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

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        for (SubsystemSnapshot sub : snapshot.subsystems()) {
            JLabel led = subsystemLeds.get(sub.name());
            if (led != null) {
                led.setForeground(Theme.getHealthColor(sub.status()));
            }
        }
    }
}
