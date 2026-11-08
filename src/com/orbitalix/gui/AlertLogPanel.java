package com.orbitalix.gui;

import com.orbitalix.model.Severity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * High-tech scrolling terminal telemetry console with live anomaly severity telemetry counts,
 * monospaced ANSI styling, and buffer management.
 * Demonstrates: single responsibility (S in SOLID), Swing text components.
 */
public class AlertLogPanel extends JPanel {

    private final JTextPane textPane;
    private final StyledDocument document;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private final JLabel counterLabel;
    private int criticalCount = 0;
    private int warningCount = 0;
    private int totalAlerts = 0;

    private Style styleTimestamp;
    private Style styleWarning;
    private Style styleCritical;
    private Style styleInfo;

    public AlertLogPanel() {
        super(new BorderLayout());
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("MISSION ANOMALY & FLIGHT LOG CONSOLE"));
        setPreferredSize(new Dimension(800, 160));

        // Sub-header toolbar with live anomaly statistics
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        counterLabel = new JLabel("LOG BUFFER ACTIVE // TOTAL: 0 | CRITICAL: 0 | WARNING: 0");
        counterLabel.setFont(Theme.FONT_MONO);
        counterLabel.setForeground(Theme.TEXT_MUTED);
        toolbar.add(counterLabel, BorderLayout.WEST);

        JButton clearBtn = new JButton("CLEAR TERMINAL");
        clearBtn.setFont(Theme.FONT_MONO);
        clearBtn.setBackground(new Color(0x18, 0x22, 0x45));
        clearBtn.setForeground(Theme.TEXT_MUTED);
        clearBtn.setFocusPainted(false);
        clearBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x23, 0x31, 0x5C)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
        ));
        clearBtn.addActionListener(e -> clear());
        toolbar.add(clearBtn, BorderLayout.EAST);

        add(toolbar, BorderLayout.NORTH);

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(new Color(0x06, 0x0A, 0x14));
        textPane.setFont(Theme.FONT_MONO);

        document = textPane.getStyledDocument();
        initStyles();

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(0x1A, 0x24, 0x44)));
        scrollPane.getViewport().setBackground(new Color(0x06, 0x0A, 0x14));
        add(scrollPane, BorderLayout.CENTER);

        logInfo("Mission control telemetry logger online. Ready for telemetry stream.");
    }

    private void initStyles() {
        styleTimestamp = textPane.addStyle("Timestamp", null);
        StyleConstants.setForeground(styleTimestamp, Theme.TEXT_MUTED);

        styleWarning = textPane.addStyle("Warning", null);
        StyleConstants.setForeground(styleWarning, Theme.STATUS_WARNING);
        StyleConstants.setBold(styleWarning, true);

        styleCritical = textPane.addStyle("Critical", null);
        StyleConstants.setForeground(styleCritical, Theme.STATUS_CRITICAL);
        StyleConstants.setBold(styleCritical, true);

        styleInfo = textPane.addStyle("Info", null);
        StyleConstants.setForeground(styleInfo, Theme.ACCENT_CYAN);
    }

    public void appendAlert(Severity severity, String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                totalAlerts++;
                if (severity == Severity.CRITICAL) criticalCount++;
                if (severity == Severity.WARNING) warningCount++;
                updateCounter();

                String timestamp = "[" + timeFormat.format(new Date()) + "] ";
                document.insertString(document.getLength(), timestamp, styleTimestamp);

                Style msgStyle;
                if (severity == Severity.CRITICAL) {
                    msgStyle = styleCritical;
                } else if (severity == Severity.WARNING) {
                    msgStyle = styleWarning;
                } else {
                    msgStyle = styleInfo;
                }

                String badge = "[" + severity.name() + "] ";
                document.insertString(document.getLength(), badge, msgStyle);
                document.insertString(document.getLength(), text + "\n", msgStyle);

                // Auto-scroll to bottom
                textPane.setCaretPosition(document.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void logInfo(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = "[" + timeFormat.format(new Date()) + "] ";
                document.insertString(document.getLength(), timestamp, styleTimestamp);
                document.insertString(document.getLength(), "[INFO] " + message + "\n", styleInfo);
                textPane.setCaretPosition(document.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void clear() {
        SwingUtilities.invokeLater(() -> {
            try {
                document.remove(0, document.getLength());
                criticalCount = 0;
                warningCount = 0;
                totalAlerts = 0;
                updateCounter();
                logInfo("Terminal log buffer flushed.");
            } catch (BadLocationException ignored) {}
        });
    }

    private void updateCounter() {
        counterLabel.setText(String.format("LOG BUFFER ACTIVE // TOTAL: %d | CRITICAL: %d | WARNING: %d",
                totalAlerts, criticalCount, warningCount));
    }
}
