package com.orbitalix.gui.theme;

import java.awt.Color;

public interface Theme {
    String displayName();
    Color background();
    Color panel();
    Color panelRaised();
    Color foreground();
    Color muted();
    Color accent();
    Color nominal();
    Color warning();
    Color critical();
    Color grid();
}
