package com.orbitalix.gui;

import com.orbitalix.simulation.SensorSnapshot;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Friendly visual guide to the OOP concepts implemented by the live simulation.
 * Demonstrates: abstraction, composition, and Observer pattern.
 */
public class OopConceptsPanel extends JPanel implements TickListener {

    private final Map<String, JLabel> liveReadings = new LinkedHashMap<>();

    public OopConceptsPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("LEARN THE CODE  ·  OBJECT-ORIENTED CONCEPTS"));

        JLabel intro = new JLabel(
            "<html><b>How this spacecraft simulator demonstrates OOP</b><br>"
            + "Follow the class relationships, then watch live sensor objects update below.</html>"
        );
        intro.setFont(Theme.FONT_LABEL);
        intro.setForeground(Theme.TEXT_PRIMARY);
        add(intro, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(4, 2, 8, 8));
        cards.setOpaque(false);
        cards.add(card("1  CLASSES & OBJECTS",
            "A class is a blueprint; an object is a working instance.",
            "Spacecraft is the blueprint. OrbitalIX-1 is the running spacecraft object."));
        cards.add(card("2  FIELDS & METHODS",
            "Fields hold object state; methods define what it can do.",
            "Sensor keeps private id, baseline and history; read() samples a reading."));
        cards.add(card("3  CONSTRUCTOR OVERLOADING",
            "Constructors build objects with different valid inputs.",
            "Spacecraft(name, subsystems) and Spacecraft(subsystems) are both available."));
        cards.add(card("4  ENCAPSULATION",
            "Keep state private; expose safe operations instead of raw internals.",
            "Sensor fields are private. History is read-only; snapshots are immutable records."));
        cards.add(card("5  ABSTRACTION",
            "Use a simple contract without depending on every implementation detail.",
            "Abstract Sensor.measure() and the SimulationController contract hide implementation."));
        cards.add(card("6  INHERITANCE",
            "Specialized classes inherit common state and behavior from a parent.",
            "Sensor  →  TemperatureSensor · VoltageSensor · SignalSensor · GyroSensor"));

        JPanel polymorphism = card("7  POLYMORPHISM  ·  LIVE",
            "The same read() call runs the correct sensor-specific measure() at runtime.",
            "Latest sensor values are read from this simulation:");
        JPanel readingList = new JPanel(new GridLayout(0, 1, 0, 1));
        readingList.setOpaque(false);
        for (String id : new String[] { "TEMP-CORE", "VOLT-BUS", "SIG-ANT", "GYRO-PRI" }) {
            JLabel reading = new JLabel(id + "    waiting for telemetry");
            reading.setFont(new Font("Consolas", Font.BOLD, 10));
            reading.setForeground(Theme.ACCENT_CYAN);
            liveReadings.put(id, reading);
            readingList.add(reading);
        }
        polymorphism.add(readingList, BorderLayout.SOUTH);
        cards.add(polymorphism);
        cards.add(card("8  OBJECT RELATIONSHIPS",
            "Composition: Spacecraft owns Subsystems; each Subsystem owns its Sensors.",
            "Aggregation: MissionControl watches a separately created Spacecraft.  "
                + "Association: Anomaly refers to a Sensor without owning it."));

        JScrollPane scroll = new JScrollPane(cards);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);

        JLabel patterns = new JLabel("<html><b>Also in use:</b> Strategy (anomaly detectors) · Observer "
            + "(tick and alert listeners) · State (spacecraft modes) · Command (recovery actions) "
            + "· Factory (sensor assembly) · MVC</html>");
        patterns.setFont(Theme.FONT_LABEL);
        patterns.setForeground(Theme.TEXT_MUTED);
        patterns.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        add(patterns, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(450, 420));
    }

    private JPanel card(String title, String explanation, String proof) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setBackground(Theme.PANEL_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Theme.PANEL_BORDER),
            BorderFactory.createEmptyBorder(8, 9, 8, 9)
        ));

        JLabel heading = new JLabel(title, SwingConstants.LEFT);
        heading.setFont(Theme.FONT_LABEL_BOLD);
        heading.setForeground(Theme.ACCENT_CYAN);

        JLabel description = new JLabel("<html>" + explanation + "<br><br><span style='color:#B8C6E5;'>"
                + proof + "</span></html>");
        description.setFont(Theme.FONT_LABEL);
        description.setForeground(Theme.TEXT_PRIMARY);
        description.setPreferredSize(new Dimension(225, 78));
        panel.add(heading, BorderLayout.NORTH);
        panel.add(description, BorderLayout.CENTER);
        panel.setMinimumSize(new Dimension(190, 100));
        return panel;
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        for (SubsystemSnapshot subsystem : snapshot.subsystems()) {
            for (SensorSnapshot sensor : subsystem.sensors()) {
                JLabel reading = liveReadings.get(sensor.sensorId());
                if (reading != null) {
                    reading.setText(String.format("%s    %6.1f %s", sensor.sensorId(),
                            sensor.value(), sensor.unit()));
                    reading.setForeground(Theme.getHealthColor(sensor.status()));
                }
            }
        }
    }
}
