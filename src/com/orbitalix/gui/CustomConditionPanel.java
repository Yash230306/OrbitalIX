package com.orbitalix.gui;

import com.orbitalix.simulation.SimulationController;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

/**
 * Plain-language controls for building a live spacecraft condition.
 * Demonstrates: abstraction, MVC, and the Observer pattern.
 */
public class CustomConditionPanel extends JPanel {

    private static final String[] SUBSYSTEMS = {
        "Thermal", "Power", "Communication", "Attitude"
    };
    private static final String[] TITLES = {
        "🌡  CORE TEMPERATURE", "⚡  POWER BUS", "📡  ANTENNA SIGNAL", "🌀  SPACECRAFT SPIN"
    };
    private static final String[] DESCRIPTIONS = {
        "Heat the core; the battery follows the same temperature shift.",
        "Set bus voltage; solar-array voltage follows the same shift.",
        "Set antenna signal quality. Lower values mean a weaker link.",
        "Set gyro drift. Either direction can make the spacecraft tumble."
    };
    private static final String[] UNITS = { "°C", "V", "%", "°/s" };
    private static final String[] LOW_LABELS = {
        "0°  FROZEN", "14 V  LOW POWER", "NO SIGNAL", "−10°/s"
    };
    private static final String[] HIGH_LABELS = {
        "90°  OVERHEAT", "32 V  STRONG", "FULL SIGNAL", "+10°/s"
    };
    private static final int[] MINIMUMS = { 0, 14, 0, -10 };
    private static final int[] MAXIMUMS = { 90, 32, 100, 10 };
    private static final int[] NOMINALS = { 25, 28, 96, 0 };
    private static final double[] BASELINES = { 25, 28, 96, 0 };

    private final SimulationController controller;
    private final JSlider[] sliders = new JSlider[SUBSYSTEMS.length];
    private final JLabel[] valueLabels = new JLabel[SUBSYSTEMS.length];
    private final JLabel[] stateLabels = new JLabel[SUBSYSTEMS.length];

    public CustomConditionPanel(SimulationController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("BUILD YOUR OWN FLIGHT CONDITION"));

        JPanel introduction = new JPanel();
        introduction.setLayout(new BoxLayout(introduction, BoxLayout.Y_AXIS));
        introduction.setOpaque(false);
        JLabel instruction = new JLabel(
            "<html><b>Make your own test</b><br>"
            + "Adjust a system, then press Start. The dashboard shows how the spacecraft responds."
            + "</html>"
        );
        instruction.setFont(Theme.FONT_LABEL);
        instruction.setForeground(Theme.TEXT_PRIMARY);
        instruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel legend = new JLabel("GREEN normal   •   AMBER warning   •   RED critical");
        legend.setFont(Theme.FONT_LABEL);
        legend.setForeground(Theme.TEXT_MUTED);
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        introduction.add(instruction);
        introduction.add(Box.createVerticalStrut(5));
        introduction.add(legend);
        add(introduction, BorderLayout.NORTH);

        JPanel controls = new JPanel(new GridLayout(SUBSYSTEMS.length, 1, 0, 7));
        controls.setOpaque(false);
        controls.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        for (int i = 0; i < SUBSYSTEMS.length; i++) {
            controls.add(buildConditionCard(i));
        }
        add(controls, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 2, 6, 0));
        actions.setOpaque(false);
        actions.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

        JButton startButton = createActionButton("▶  START", Theme.STATUS_NOMINAL);
        startButton.addActionListener(e -> controller.start());
        JButton resetButton = createActionButton("↺  RESET", Theme.TEXT_PRIMARY);
        resetButton.addActionListener(e -> {
            controller.reset();
            resetToNominal();
        });
        actions.add(startButton);
        actions.add(resetButton);
        add(actions, BorderLayout.SOUTH);
    }

    private JPanel buildConditionCard(int index) {
        JPanel card = new JPanel(new BorderLayout(3, 2));
        card.setBackground(Theme.PANEL_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_BORDER),
            BorderFactory.createEmptyBorder(5, 7, 4, 7)
        ));

        JPanel heading = new JPanel(new BorderLayout(4, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel(TITLES[index]);
        title.setFont(Theme.FONT_LABEL_BOLD);
        title.setForeground(Theme.TEXT_PRIMARY);
        JLabel value = new JLabel();
        value.setFont(Theme.FONT_MONO_BOLD);
        value.setHorizontalAlignment(JLabel.RIGHT);
        value.setPreferredSize(new Dimension(75, 20));
        valueLabels[index] = value;
        heading.add(title, BorderLayout.CENTER);
        heading.add(value, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);

        JSlider slider = new JSlider(MINIMUMS[index], MAXIMUMS[index], NOMINALS[index]);
        slider.setOpaque(false);
        slider.setForeground(Theme.ACCENT_CYAN);
        slider.setToolTipText(DESCRIPTIONS[index]);
        sliders[index] = slider;

        JPanel scale = new JPanel(new BorderLayout());
        scale.setOpaque(false);
        JLabel low = new JLabel(LOW_LABELS[index]);
        low.setFont(Theme.FONT_LABEL);
        low.setForeground(Theme.TEXT_MUTED);
        JLabel high = new JLabel(HIGH_LABELS[index]);
        high.setFont(Theme.FONT_LABEL);
        high.setForeground(Theme.TEXT_MUTED);
        scale.add(low, BorderLayout.WEST);
        scale.add(high, BorderLayout.EAST);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(slider, BorderLayout.CENTER);
        center.add(scale, BorderLayout.SOUTH);
        card.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(4, 0));
        footer.setOpaque(false);
        JLabel description = new JLabel("<html>" + DESCRIPTIONS[index] + "</html>");
        description.setFont(Theme.FONT_LABEL);
        description.setForeground(Theme.TEXT_MUTED);
        JLabel state = new JLabel();
        state.setFont(Theme.FONT_LABEL_BOLD);
        state.setHorizontalAlignment(JLabel.RIGHT);
        stateLabels[index] = state;
        footer.add(description, BorderLayout.CENTER);
        footer.add(state, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);

        final int selected = index;
        slider.addChangeListener(e -> applySlider(selected));
        applySlider(index);
        return card;
    }

    private JButton createActionButton(String text, java.awt.Color foreground) {
        JButton button = new JButton(text);
        button.setFont(Theme.FONT_LABEL_BOLD);
        button.setBackground(Theme.PANEL_CARD);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_BORDER),
            BorderFactory.createEmptyBorder(8, 5, 8, 5)
        ));
        return button;
    }

    public void resetToNominal() {
        for (int i = 0; i < sliders.length; i++) {
            sliders[i].setValue(NOMINALS[i]);
            applySlider(i);
        }
    }

    private void applySlider(int index) {
        int value = sliders[index].getValue();
        controller.setLiveCondition(SUBSYSTEMS[index], value - BASELINES[index]);
        valueLabels[index].setText(value + " " + UNITS[index]);

        int severity = severity(index, value);
        if (severity >= 2) {
            stateLabels[index].setText("CRITICAL");
            stateLabels[index].setForeground(Theme.STATUS_CRITICAL);
            valueLabels[index].setForeground(Theme.STATUS_CRITICAL);
        } else if (severity == 1) {
            stateLabels[index].setText("WARNING");
            stateLabels[index].setForeground(Theme.STATUS_WARNING);
            valueLabels[index].setForeground(Theme.STATUS_WARNING);
        } else {
            stateLabels[index].setText("NORMAL");
            stateLabels[index].setForeground(Theme.STATUS_NOMINAL);
            valueLabels[index].setForeground(Theme.STATUS_NOMINAL);
        }
    }

    private int severity(int index, int value) {
        return switch (index) {
            case 0 -> value >= 70 ? 2 : value >= 45 ? 1 : 0;
            case 1 -> value <= 20 ? 2 : value <= 24 ? 1 : 0;
            case 2 -> value <= 20 ? 2 : value <= 60 ? 1 : 0;
            case 3 -> Math.abs(value) >= 8 ? 2 : Math.abs(value) >= 4 ? 1 : 0;
            default -> 0;
        };
    }
}
