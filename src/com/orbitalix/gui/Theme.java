package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

/**
 * Central theme palette and styling definitions for the mission control dashboard.
 * Dark space theme using high-contrast status colors and clean typography.
 * Demonstrates: encapsulation, single responsibility (S in SOLID).
 */
public final class Theme {

    private Theme() {}

    // Colors
    public static final Color BG_DARK = new Color(0x0B, 0x10, 0x20);
    public static final Color PANEL_BG = new Color(0x12, 0x1A, 0x33);
    public static final Color PANEL_CARD = new Color(0x18, 0x22, 0x45);
    public static final Color PANEL_BORDER = new Color(0x23, 0x31, 0x5C);
    public static final Color TEXT_PRIMARY = new Color(0xE0, 0xE6, 0xFF);
    public static final Color TEXT_MUTED = new Color(0x8A, 0x9B, 0xC7);
    public static final Color ACCENT_CYAN = new Color(0x00, 0xE5, 0xFF);

    public static final Color STATUS_NOMINAL = new Color(0x00, 0xE6, 0x76);
    public static final Color STATUS_WARNING = new Color(0xFF, 0xB3, 0x00);
    public static final Color STATUS_SAFE_MODE = new Color(0x29, 0x79, 0xFF);
    public static final Color STATUS_CRITICAL = new Color(0xFF, 0x17, 0x44);
    public static final Color STATUS_FAILED = new Color(0x75, 0x75, 0x75);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_LABEL_BOLD = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_BOLD = new Font("Consolas", Font.BOLD, 13);
    public static final Font FONT_MONO_LARGE = new Font("Consolas", Font.BOLD, 16);

    public static Color getHealthColor(HealthStatus status) {
        if (status == null) return STATUS_NOMINAL;
        return switch (status) {
            case NOMINAL -> STATUS_NOMINAL;
            case WARNING -> STATUS_WARNING;
            case CRITICAL -> STATUS_CRITICAL;
            case FAILED -> STATUS_FAILED;
        };
    }

    public static Color getModeColor(String mode) {
        if (mode == null) return STATUS_NOMINAL;
        return switch (mode.toUpperCase()) {
            case "NOMINAL" -> STATUS_NOMINAL;
            case "WARNING" -> STATUS_WARNING;
            case "CRITICAL" -> STATUS_CRITICAL;
            case "SAFE MODE", "SAFE_MODE" -> STATUS_SAFE_MODE;
            case "FAILED" -> STATUS_FAILED;
            default -> ACCENT_CYAN;
        };
    }

    public static Border createBorder(String title) {
        Border line = BorderFactory.createLineBorder(PANEL_BORDER, 1);
        TitledBorder titled = BorderFactory.createTitledBorder(line, title,
                TitledBorder.LEFT, TitledBorder.TOP, FONT_HEADER, ACCENT_CYAN);
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4),
                BorderFactory.createCompoundBorder(titled, BorderFactory.createEmptyBorder(6, 6, 6, 6))
        );
    }
}
