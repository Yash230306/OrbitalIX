package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;
import com.orbitalix.simulation.FaultType;
import com.orbitalix.simulation.SimulationController;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Intelligent AI Flight Director HUD translating complex telemetry into clear plain-English
 * diagnostic briefings and providing an automated one-click 60-second guided Viva demo tour.
 * Demonstrates: Model-View-Controller integration, Observer pattern (TickListener), user guidance.
 */
public class FlightDirectorPanel extends JPanel implements TickListener {

    private final SimulationController controller;
    private final JLabel statusTitleLabel;
    private final JLabel explanationLabel;
    private final JButton tourButton;

    private boolean tourRunning = false;
    private int tourStep = 0;
    private Timer tourTimer;

    public FlightDirectorPanel(SimulationController controller) {
        super(new BorderLayout(12, 4));
        this.controller = controller;

        setBackground(new Color(0x0E, 0x14, 0x28));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x1F, 0x2D, 0x56), 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        setPreferredSize(new Dimension(800, 56));

        // West: AI Flight Co-Pilot Tag & Diagnosis Text
        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);

        statusTitleLabel = new JLabel("● AI FLIGHT DIRECTOR // STATUS: NOMINAL FLIGHT");
        statusTitleLabel.setFont(Theme.FONT_LABEL_BOLD);
        statusTitleLabel.setForeground(Theme.STATUS_NOMINAL);

        explanationLabel = new JLabel("All 4 subsystems operating within normal parameters. Spacecraft flying nominally in LEO orbit.");
        explanationLabel.setFont(Theme.FONT_LABEL);
        explanationLabel.setForeground(Theme.TEXT_PRIMARY);

        textPanel.add(statusTitleLabel);
        textPanel.add(explanationLabel);
        add(textPanel, BorderLayout.CENTER);

        // East: 60-Second Guided Tour Button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 6));
        buttonPanel.setOpaque(false);

        tourButton = new JButton("★ 60-SEC VIVA TOUR");
        tourButton.setFont(Theme.FONT_LABEL_BOLD);
        tourButton.setBackground(new Color(0x1C, 0x2A, 0x58));
        tourButton.setForeground(Theme.ACCENT_CYAN);
        tourButton.setFocusPainted(false);
        tourButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
        tourButton.addActionListener(e -> toggleVivaTour());
        buttonPanel.add(tourButton);

        add(buttonPanel, BorderLayout.EAST);
    }

    private void toggleVivaTour() {
        if (tourRunning) {
            stopTour();
        } else {
            startTour();
        }
    }

    private void startTour() {
        tourRunning = true;
        tourStep = 0;
        tourButton.setText("⏹ STOP TOUR");
        tourButton.setForeground(Theme.STATUS_WARNING);
        SoundEffects.getInstance().playClick();

        // Load thermal runaway scenario for viva demonstration
        controller.loadScenario(2);
        controller.setSpeed(2);
        controller.start();

        if (tourTimer != null && tourTimer.isRunning()) {
            tourTimer.stop();
        }

        tourTimer = new Timer(3000, e -> {
            tourStep++;
            switch (tourStep) {
                case 1 -> {
                    updateBriefing("TOUR STEP 1: NOMINAL FLIGHT",
                            "Spacecraft flies normally in orbit. All 4 subsystems glow green on dashboard.",
                            Theme.STATUS_NOMINAL);
                }
                case 2 -> {
                    updateBriefing("TOUR STEP 2: FAULT INJECTED",
                            "Thermal runaway injected into the spacecraft core. Temperature begins climbing rapidly.",
                            Theme.STATUS_WARNING);
                    SoundEffects.getInstance().playWarning();
                }
                case 3 -> {
                    updateBriefing("TOUR STEP 3: ANOMALY DETECTED",
                            "RateOfChangeDetector catches rapid temperature delta. Alert raised, mode switches to WARNING.",
                            Theme.STATUS_WARNING);
                }
                case 4 -> {
                    updateBriefing("TOUR STEP 4: CRITICAL THRESHOLD BREACH",
                            "Core crosses 70°C critical limit! Status banner turns RED and begins pulsing.",
                            Theme.STATUS_CRITICAL);
                    SoundEffects.getInstance().playCritical();
                }
                case 5 -> {
                    updateBriefing("TOUR STEP 5: AUTONOMOUS SAFE MODE ENGAGED",
                            "Spacecraft sheds non-essential electrical loads and automatically initiates radiator cooling.",
                            Theme.STATUS_SAFE_MODE);
                    SoundEffects.getInstance().playRecovery();
                }
                case 6 -> {
                    updateBriefing("TOUR STEP 6: AUTONOMOUS RECOVERY COMPLETE",
                            "Core temperature returns to normal. Spacecraft successfully recovers without human intervention!",
                            Theme.STATUS_NOMINAL);
                    SoundEffects.getInstance().playClick();
                }
                case 7 -> {
                    stopTour();
                }
            }
        });
        tourTimer.start();
    }

    private void stopTour() {
        tourRunning = false;
        if (tourTimer != null) {
            tourTimer.stop();
        }
        tourButton.setText("★ 60-SEC VIVA TOUR");
        tourButton.setForeground(Theme.ACCENT_CYAN);
    }

    private void updateBriefing(String title, String explanation, Color color) {
        statusTitleLabel.setText("● " + title);
        statusTitleLabel.setForeground(color);
        explanationLabel.setText(explanation);
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        if (tourRunning) return; // Keep guided narration during tour

        String mode = snapshot.spacecraftState();
        Color modeColor = Theme.getModeColor(mode);

        switch (mode.toUpperCase()) {
            case "NOMINAL" -> updateBriefing("AI FLIGHT DIRECTOR // STATUS: NOMINAL FLIGHT",
                    "All 4 subsystems operating within normal parameters. Telemetry verified and nominal.",
                    modeColor);
            case "WARNING" -> updateBriefing("AI FLIGHT DIRECTOR // ATTENTION: SUB-THRESHOLD ANOMALY",
                    "Telemetry rate or threshold drift detected. Anomaly detectors monitoring for limit breaches.",
                    modeColor);
            case "CRITICAL" -> updateBriefing("AI FLIGHT DIRECTOR // EMERGENCY: CRITICAL LIMIT BREACH",
                    "Critical safety limits broken! Emergency overrides armed. Automated safe-mode sequence active.",
                    modeColor);
            case "SAFE MODE", "SAFE_MODE" -> updateBriefing("AI FLIGHT DIRECTOR // SAFE MODE: RECOVERY IN PROGRESS",
                    "Autonomous recovery active. Non-essential loads shed, thermal radiators engaged, systems stabilizing.",
                    modeColor);
            case "FAILED" -> updateBriefing("AI FLIGHT DIRECTOR // FAULT ISOLATION: REDUNDANCY LOCK",
                    "Cascading fault contained by isolation breakers. Failsafe procedures locked.",
                    modeColor);
        }
    }
}
