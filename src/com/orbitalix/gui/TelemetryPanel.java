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
 * Telemetry table displaying real-time readings, health statuses, and engineering units.
 * Demonstrates: Observer pattern (TickListener), encapsulation, Swing JTable customization.
 */
public class TelemetryPanel extends JPanel implements TickListener {

    private final JTable table;
    private final DefaultTableModel tableModel;
    private Consumer<String> onSensorSelected;

    private static final String[] COLUMNS = { "Subsystem", "Sensor ID", "Value", "Unit", "Status" };

    public TelemetryPanel() {
        super(new BorderLayout());
        setBackground(Theme.PANEL_BG);
        setBorder(Theme.createBorder("REAL-TIME TELEMETRY"));
        setPreferredSize(new Dimension(380, 220));

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setBackground(Theme.PANEL_CARD);
        table.setForeground(Theme.TEXT_PRIMARY);
        table.setGridColor(Theme.PANEL_BORDER);
        table.setFont(Theme.FONT_MONO);
        table.setRowHeight(22);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setBackground(Theme.PANEL_BG);
        table.getTableHeader().setForeground(Theme.ACCENT_CYAN);
        table.getTableHeader().setFont(Theme.FONT_LABEL_BOLD);

        // Custom status cell renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column);
                c.setBackground(isSelected ? new Color(0x23, 0x31, 0x66) : Theme.PANEL_CARD);
                c.setForeground(Theme.TEXT_PRIMARY);

                if (column == 4 && value != null) {
                    try {
                        HealthStatus st = HealthStatus.valueOf(value.toString());
                        c.setForeground(Theme.getHealthColor(st));
                        setFont(Theme.FONT_MONO_BOLD);
                    } catch (Exception ignored) {}
                } else if (column == 2) {
                    setHorizontalAlignment(JLabel.RIGHT);
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
        int selectedRow = table.getSelectedRow();
        String selectedSensor = selectedRow >= 0 ? (String) tableModel.getValueAt(selectedRow, 1) : null;

        tableModel.setRowCount(0);
        for (SubsystemSnapshot sub : snapshot.subsystems()) {
            for (SensorSnapshot sensor : sub.sensors()) {
                tableModel.addRow(new Object[] {
                        sub.name(),
                        sensor.sensorId(),
                        String.format(Locale.US, "%7.2f", sensor.value()),
                        sensor.unit(),
                        sensor.status().name()
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
    }
}
