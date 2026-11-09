package com.orbitalix.gui.theme;

import java.awt.Color;
import java.util.List;

/** Four interchangeable theme strategies, including high-contrast projector mode. */
public enum Themes implements Theme {
    DARK_MISSION("Dark Mission", new Color(10, 17, 29), new Color(19, 29, 45), new Color(26, 40, 59),
            new Color(231, 240, 250), new Color(134, 153, 174), new Color(59, 206, 220),
            new Color(77, 211, 153), new Color(250, 188, 81), new Color(255, 91, 104), new Color(43, 62, 82)),
    NEON("Neon", new Color(9, 8, 24), new Color(24, 18, 45), new Color(35, 25, 62),
            new Color(247, 237, 255), new Color(165, 146, 194), new Color(235, 84, 255),
            new Color(75, 255, 178), new Color(255, 221, 80), new Color(255, 72, 131), new Color(66, 47, 94)),
    PROJECTOR("Projector Light", new Color(232, 238, 241), new Color(250, 252, 253), new Color(222, 231, 237),
            new Color(26, 43, 57), new Color(93, 112, 128), new Color(0, 126, 158),
            new Color(0, 132, 86), new Color(171, 106, 0), new Color(188, 42, 48), new Color(197, 211, 220)),
    HIGH_CONTRAST("High Contrast", Color.BLACK, new Color(15, 15, 15), new Color(35, 35, 35),
            Color.WHITE, new Color(222, 222, 222), Color.CYAN, Color.GREEN, Color.YELLOW, Color.RED, Color.WHITE);

    private final String label;
    private final Color background, panel, panelRaised, foreground, muted, accent, nominal, warning, critical, grid;
    Themes(String label, Color background, Color panel, Color panelRaised, Color foreground, Color muted,
           Color accent, Color nominal, Color warning, Color critical, Color grid) {
        this.label = label; this.background = background; this.panel = panel; this.panelRaised = panelRaised;
        this.foreground = foreground; this.muted = muted; this.accent = accent; this.nominal = nominal;
        this.warning = warning; this.critical = critical; this.grid = grid;
    }

    @Override public String displayName() { return label; }
    @Override public Color background() { return background; }
    @Override public Color panel() { return panel; }
    @Override public Color panelRaised() { return panelRaised; }
    @Override public Color foreground() { return foreground; }
    @Override public Color muted() { return muted; }
    @Override public Color accent() { return accent; }
    @Override public Color nominal() { return nominal; }
    @Override public Color warning() { return warning; }
    @Override public Color critical() { return critical; }
    @Override public Color grid() { return grid; }
    public static List<Themes> all() { return List.of(values()); }
}
