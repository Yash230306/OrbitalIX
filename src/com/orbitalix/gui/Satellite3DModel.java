package com.orbitalix.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Procedural 3D mesh generator for the OrbitalIX spacecraft.
 * Generates geometric vertices and faces for the Thermal bus, Power solar arrays,
 * Communication parabolic dish, and Attitude gyro stabilization ring.
 * Demonstrates: Factory pattern (generates 3D mesh), procedural geometry, encapsulation.
 */
public class Satellite3DModel {

    public static List<Polygon3D> buildMesh() {
        List<Polygon3D> mesh = new ArrayList<>();

        // -------------------------------------------------------------
        // 1. THERMAL / MAIN CENTRAL BUS (Hexagonal or Rectangular Cuboid)
        // -------------------------------------------------------------
        double bw = 55;  // half-width
        double bh = 65;  // half-height
        double bd = 55;  // half-depth

        Color goldFoil = new Color(0xC5, 0x9B, 0x27);
        Color darkPanel = new Color(0x1B, 0x24, 0x3D);
        Color radiatorFoil = new Color(0x2E, 0x3E, 0x66);

        // Front face (+Z)
        mesh.add(createQuad(
                new Vec3(-bw, bh, bd), new Vec3(bw, bh, bd),
                new Vec3(bw, -bh, bd), new Vec3(-bw, -bh, bd),
                "Thermal", goldFoil, false, false));

        // Front Core Radiator Window (Emissive)
        mesh.add(createQuad(
                new Vec3(-25, 25, bd + 1), new Vec3(25, 25, bd + 1),
                new Vec3(25, -25, bd + 1), new Vec3(-25, -25, bd + 1),
                "Thermal", new Color(0x00, 0xE5, 0xFF), false, true));

        // Back face (-Z)
        mesh.add(createQuad(
                new Vec3(bw, bh, -bd), new Vec3(-bw, bh, -bd),
                new Vec3(-bw, -bh, -bd), new Vec3(bw, -bh, -bd),
                "Thermal", goldFoil, false, false));

        // Left face (-X)
        mesh.add(createQuad(
                new Vec3(-bw, bh, -bd), new Vec3(-bw, bh, bd),
                new Vec3(-bw, -bh, bd), new Vec3(-bw, -bh, -bd),
                "Thermal", radiatorFoil, false, false));

        // Right face (+X)
        mesh.add(createQuad(
                new Vec3(bw, bh, bd), new Vec3(bw, bh, -bd),
                new Vec3(bw, -bh, -bd), new Vec3(bw, -bh, bd),
                "Thermal", radiatorFoil, false, false));

        // Top face (+Y)
        mesh.add(createQuad(
                new Vec3(-bw, bh, -bd), new Vec3(bw, bh, -bd),
                new Vec3(bw, bh, bd), new Vec3(-bw, bh, bd),
                "Thermal", darkPanel, false, false));

        // Bottom face (-Y)
        mesh.add(createQuad(
                new Vec3(-bw, -bh, bd), new Vec3(bw, -bh, bd),
                new Vec3(bw, -bh, -bd), new Vec3(-bw, -bh, -bd),
                "Thermal", darkPanel, false, false));

        // -------------------------------------------------------------
        // 2. POWER SUBSYSTEM (Left & Right 3D Solar Array Wings)
        // -------------------------------------------------------------
        Color solarCellColor = new Color(0x0B, 0x3C, 0x6E);
        Color trussColor = new Color(0x60, 0x72, 0x9B);

        // Left Boom Truss
        mesh.add(createQuad(
                new Vec3(-bw, 6, 6), new Vec3(-85, 6, 6),
                new Vec3(-85, -6, 6), new Vec3(-bw, -6, 6),
                "Power", trussColor, true, false));

        // Left Solar Panels (Multi-panel wing)
        double pw = 65; // panel segment width
        double ph = 48; // panel height

        for (int i = 0; i < 2; i++) {
            double x1 = -85 - (i * pw);
            double x2 = -85 - ((i + 1) * pw);

            // Front solar face
            mesh.add(createQuad(
                    new Vec3(x1, ph, 2), new Vec3(x2, ph, 2),
                    new Vec3(x2, -ph, 2), new Vec3(x1, -ph, 2),
                    "Power", solarCellColor, true, false));

            // Gold framing border
            mesh.add(createQuad(
                    new Vec3(x1, ph, -2), new Vec3(x2, ph, -2),
                    new Vec3(x2, -ph, -2), new Vec3(x1, -ph, -2),
                    "Power", goldFoil, true, false));
        }

        // Right Boom Truss
        mesh.add(createQuad(
                new Vec3(bw, 6, 6), new Vec3(85, 6, 6),
                new Vec3(85, -6, 6), new Vec3(bw, -6, 6),
                "Power", trussColor, true, false));

        // Right Solar Panels
        for (int i = 0; i < 2; i++) {
            double x1 = 85 + (i * pw);
            double x2 = 85 + ((i + 1) * pw);

            // Front solar face
            mesh.add(createQuad(
                    new Vec3(x1, ph, 2), new Vec3(x2, ph, 2),
                    new Vec3(x2, -ph, 2), new Vec3(x1, -ph, 2),
                    "Power", solarCellColor, true, false));

            // Back gold backing
            mesh.add(createQuad(
                    new Vec3(x2, ph, -2), new Vec3(x1, ph, -2),
                    new Vec3(x1, -ph, -2), new Vec3(x2, -ph, -2),
                    "Power", goldFoil, true, false));
        }

        // -------------------------------------------------------------
        // 3. COMMUNICATION SUBSYSTEM (3D Parabolic High-Gain Dish Antenna)
        // -------------------------------------------------------------
        // Mast from top of bus (+Y)
        mesh.add(createQuad(
                new Vec3(-5, bh + 30, 0), new Vec3(5, bh + 30, 0),
                new Vec3(5, bh, 0), new Vec3(-5, bh, 0),
                "Communication", trussColor, true, false));

        // Faceted 3D Parabolic Dish on top of mast
        double dishRadius = 38;
        double dishDepth = 15;
        double dishCenterY = bh + 45;
        int dishSegments = 10;
        Vec3 dishCenter = new Vec3(0, dishCenterY - dishDepth, 0);

        for (int i = 0; i < dishSegments; i++) {
            double a1 = 2 * Math.PI * i / dishSegments;
            double a2 = 2 * Math.PI * (i + 1) / dishSegments;

            Vec3 rim1 = new Vec3(Math.cos(a1) * dishRadius, dishCenterY, Math.sin(a1) * dishRadius);
            Vec3 rim2 = new Vec3(Math.cos(a2) * dishRadius, dishCenterY, Math.sin(a2) * dishRadius);

            // Triangle from dish center to rim
            mesh.add(new Polygon3D(List.of(dishCenter, rim1, rim2), "Communication",
                    new Color(0xD0, 0xDC, 0xF5), true, false));
        }

        // Antenna Feedhorn Tripod
        Vec3 feedHorn = new Vec3(0, dishCenterY + 18, 0);
        mesh.add(new Polygon3D(List.of(dishCenter, feedHorn, new Vec3(0, dishCenterY + 20, 0)),
                "Communication", new Color(0x00, 0xE5, 0xFF), true, true));

        // -------------------------------------------------------------
        // 4. ATTITUDE SUBSYSTEM (Gyro Torus Ring & Reaction Wheel Assembly)
        // -------------------------------------------------------------
        double ringRadius = 46;
        double ringY = -bh - 20;
        int ringSegments = 12;

        for (int i = 0; i < ringSegments; i++) {
            double a1 = 2 * Math.PI * i / ringSegments;
            double a2 = 2 * Math.PI * (i + 1) / ringSegments;

            Vec3 r1Upper = new Vec3(Math.cos(a1) * ringRadius, ringY + 6, Math.sin(a1) * ringRadius);
            Vec3 r2Upper = new Vec3(Math.cos(a2) * ringRadius, ringY + 6, Math.sin(a2) * ringRadius);
            Vec3 r2Lower = new Vec3(Math.cos(a2) * ringRadius, ringY - 6, Math.sin(a2) * ringRadius);
            Vec3 r1Lower = new Vec3(Math.cos(a1) * ringRadius, ringY - 6, Math.sin(a1) * ringRadius);

            mesh.add(createQuad(r1Upper, r2Upper, r2Lower, r1Lower,
                    "Attitude", new Color(0x38, 0x4B, 0x78), true, false));

            // Interior spoke connecting to central shaft
            if (i % 3 == 0) {
                mesh.add(createQuad(
                        new Vec3(0, ringY, 0), r1Upper,
                        r1Lower, new Vec3(0, ringY, 0),
                        "Attitude", new Color(0x00, 0xE5, 0xFF), true, true));
            }
        }

        return mesh;
    }

    private static Polygon3D createQuad(Vec3 v0, Vec3 v1, Vec3 v2, Vec3 v3,
                                       String subsystem, Color color, boolean twoSided, boolean emissive) {
        return new Polygon3D(List.of(v0, v1, v2, v3), subsystem, color, twoSided, emissive);
    }
}
