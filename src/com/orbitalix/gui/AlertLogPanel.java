package com.orbitalix.gui;

import com.orbitalix.model.Severity;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 * Terminal-style scrolling log for mission alerts, colored by severity.
 * Demonstrates: single responsibility (S in SOLID), Swing text components.
 */
public class AlertLogPanel extends JPanel {

    private final JTextPane textPane;
    private final StyledDocument document;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    private Style styleTimestamp;
    private Style styleWarning;
    private Style styleCritical;
    private Style styleInfo;

    public AlertLogPanel() {
        super(new BorderLayout());
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("MISSION ALERTS & LOGS"));
        setPreferredSize(new Dimension(800, 150));

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(new Color(0x06, 0x0A, 0x14));
        textPane.setFont(Theme.FONT_MONO);

        document = textPane.getStyledDocument();
        initStyles();

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(0x06, 0x0A, 0x14));
        add(scrollPane, BorderLayout.CENTER);

        logInfo("Mission control alert subsystem initialized.");
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
                logInfo("Alert log cleared.");
            } catch (BadLocationException ignored) {}
        });
    }
}
