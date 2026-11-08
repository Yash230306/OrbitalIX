package com.orbitalix.gui;

import com.orbitalix.model.HealthStatus;
import com.orbitalix.simulation.SensorSnapshot;
import com.orbitalix.simulation.SimulationSnapshot;
import com.orbitalix.simulation.SubsystemSnapshot;
import com.orbitalix.simulation.TickListener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * High-tech telemetry grid displaying live bus telemetry, engineering units, and visual health indicators.
 * Demonstrates: Observer pattern (TickListener), Swing JTable advanced cell rendering.
 */
public class TelemetryPanel extends JPanel implements TickListener {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private Consumer<String> onSensorSelected;

    private static final String[] COLUMNS = { "SUBSYSTEM", "SENSOR ID", "VALUE", "UNIT", "STATUS" };

    public TelemetryPanel() {
        super(new BorderLayout());
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("REAL-TIME TELEMETRY MATRIX"));
        setPreferredSize(new Dimension(420, 240));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setBackground(Theme.PANEL_CARD);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setGridColor(new Color(0x1A, 0x24, 0x46));
        table.setFont(Theme.FONT_MONO);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setBackground(new Color(0x0E, 0x14, 0x2A));
        table.getTableHeader().setForeground(Theme.ACCENT_CYAN);
        table.getTableHeader().setFont(Theme.FONT_LABEL_BOLD);

        // Custom high-tech cell renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);

                // Alternating row styling
                if (isSelected) {
                    c.setBackground(new Color(0x1C, 0x2B, 0x5E));
                } else {
                    c.setBackground(row % 2 == 0 ? Theme.PANEL_CARD : new Color(0x14, 0x1C, 0x38));
                }
                c.setForeground(Theme.TEXT_PRIMARY);

                if (column == 4 && value != null) {
                    String strVal = value.toString().replace("● ", "").replace("▲ ", "").replace("✖ ", "").replace("■ ", "");
                    try {
                        HealthStatus st = HealthStatus.valueOf(strVal);
                        c.setForeground(Theme.getHealthColor(st));
                        setFont(Theme.FONT_MONO_BOLD);
                    } catch (Exception ignored) {}
                    setHorizontalAlignment(JLabel.CENTER);
                } else if (column == 2) {
                    setHorizontalAlignment(JLabel.RIGHT);
                    c.setForeground(Theme.ACCENT_CYAN);
                    setFont(Theme.FONT_MONO_BOLD);
                } else if (column == 1) {
                    setHorizontalAlignment(JLabel.LEFT);
                    setFont(Theme.FONT_LABEL_BOLD);
                } else {
                    setHorizontalAlignment(JLabel.LEFT);
                }
                return c;
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0 && onSensorSelected != null) {
                String sensorId = (String) tableModel.getValueAt(table.getSelectedRow(), 1);
                onSensorSelected.accept(sensorId);
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Theme.PANEL_CARD);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setOnSensorSelected(Consumer<String> onSensorSelected) {
        this.onSensorSelected = onSensorSelected;
    }

    @Override
    public void onTick(SimulationSnapshot snapshot) {
        Runnable update = () -> {
            int selectedRow = table.getSelectedRow();
            String selectedSensor = selectedRow >= 0 ? (String) tableModel.getValueAt(selectedRow, 1) : null;

            tableModel.setRowCount(0);
            for (SubsystemSnapshot sub : snapshot.subsystems()) {
                for (SensorSnapshot sensor : sub.sensors()) {
                    String icon = switch (sensor.status()) {
                        case NOMINAL -> "● ";
                        case WARNING -> "▲ ";
                        case CRITICAL -> "✖ ";
                        case FAILED -> "■ ";
                    };

                    tableModel.addRow(new Object[] {
                            sub.name(),
                            sensor.sensorId(),
                            String.format(Locale.US, "%7.2f", sensor.value()),
                            sensor.unit(),
                            icon + sensor.status().name()
                    });
                }
            }

            // Restore selection if applicable
            if (selectedSensor != null) {
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    if (selectedSensor.equals(tableModel.getValueAt(r, 1))) {
                        table.setRowSelectionInterval(r, r);
                        break;
                    }
                }
            }
        };

        if (javax.swing.SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            javax.swing.SwingUtilities.invokeLater(update);
        }
    }
}
