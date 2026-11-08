package com.orbitalix.gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Polygon;
import java.util.List;

/**
 * 3D polygon face representation supporting depth-sorting, directional shading, and perspective rasterization.
 * Demonstrates: encapsulation, 3D graphics pipeline (Painter's algorithm, Lambertian diffuse lighting).
 */
public class Polygon3D implements Comparable<Polygon3D> {

    private final Vec3[] vertices;
    private final String subsystemName;
    private final Color defaultColor;
    private final boolean twoSided;
    private final boolean emissive;

    // Computed per frame
    private double avgZ;
    private Vec3 normal;

    public Polygon3D(List<Vec3> verts, String subsystemName, Color defaultColor, boolean twoSided, boolean emissive) {
        this.vertices = verts.toArray(new Vec3[0]);
        this.subsystemName = subsystemName;
        this.defaultColor = defaultColor;
        this.twoSided = twoSided;
        this.emissive = emissive;
    }

    public Polygon3D(List<Vec3> verts, String subsystemName, Color defaultColor) {
        this(verts, subsystemName, defaultColor, false, false);
    }

    /**
     * Transforms and projects the polygon for the current view rotation and camera position.
     */
    public Polygon3D transform(double pitch, double yaw, double roll) {
        Vec3[] transformed = new Vec3[vertices.length];
        double zSum = 0;
        for (int i = 0; i < vertices.length; i++) {
            Vec3 v = vertices[i]
                    .rotateX(pitch)
                    .rotateY(yaw)
                    .rotateZ(roll);
            transformed[i] = v;
            zSum += v.z();
        }

        Polygon3D p = new Polygon3D(List.of(transformed), subsystemName, defaultColor, twoSided, emissive);
        p.avgZ = zSum / vertices.length;

        // Compute normal vector from first 3 vertices
        if (transformed.length >= 3) {
            Vec3 edge1 = transformed[1].sub(transformed[0]);
            Vec3 edge2 = transformed[2].sub(transformed[0]);
            p.normal = edge1.cross(edge2).normalize();
        } else {
            p.normal = new Vec3(0, 0, 1);
        }
        return p;
    }

    public Polygon toScreenPolygon(double fov, double distance, int width, int height) {
        Polygon poly = new Polygon();
        for (Vec3 v : vertices) {
            Point pt = v.project(fov, distance, width, height);
            poly.addPoint(pt.x, pt.y);
        }
        return poly;
    }

    public Color computeShadedColor(Color dynamicColor, Vec3 lightDir) {
        Color base = dynamicColor != null ? dynamicColor : defaultColor;
        if (emissive) {
            return base;
        }

        double dot = normal.dot(lightDir);
        if (twoSided && dot < 0) {
            dot = -dot * 0.75; // Backside receives secondary light
        }
        double factor = Math.max(0.25, Math.min(1.0, 0.35 + 0.65 * Math.max(0, dot)));

        int r = (int) (base.getRed() * factor);
        int g = (int) (base.getGreen() * factor);
        int b = (int) (base.getBlue() * factor);
        return new Color(r, g, b, base.getAlpha());
    }

    public boolean isVisible() {
        if (twoSided) return true;
        // Standard backface culling for solid bodies
        return normal.z() < 0.25;
    }

    public double getAvgZ() {
        return avgZ;
    }

    public String getSubsystemName() {
        return subsystemName;
    }

    public Vec3[] getVertices() {
        return vertices;
    }

    public boolean isEmissive() {
        return emissive;
    }

    @Override
    public int compareTo(Polygon3D o) {
        // Painter's algorithm: sort descending by avgZ (furthest drawn first)
        return Double.compare(o.avgZ, this.avgZ);
    }
}
