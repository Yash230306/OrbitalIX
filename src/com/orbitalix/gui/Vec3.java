package com.orbitalix.gui;

import java.awt.Point;

/**
 * 3D vector and coordinate math for pure-Java software 3D rendering and projection.
 * Demonstrates: encapsulation, immutability (record), linear algebra and trigonometric transformations.
 */
public record Vec3(double x, double y, double z) {

    public Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    public Vec3 sub(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    public Vec3 scale(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    public Vec3 rotateX(double rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(x, y * cos - z * sin, y * sin + z * cos);
    }

    public Vec3 rotateY(double rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(x * cos + z * sin, y, -x * sin + z * cos);
    }

    public Vec3 rotateZ(double rad) {
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(x * cos - y * sin, x * sin + y * cos, z);
    }

    public double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    public Vec3 cross(Vec3 o) {
        return new Vec3(
                y * o.z - z * o.y,
                z * o.x - x * o.z,
                x * o.y - y * o.x
        );
    }

    public double length() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vec3 normalize() {
        double len = length();
        if (len < 1e-9) return new Vec3(0, 0, 1);
        return new Vec3(x / len, y / len, z / len);
    }

    /**
     * Projects 3D world coordinates into 2D screen coordinates using perspective projection.
     */
    public Point project(double fov, double distance, int width, int height) {
        double pz = z + distance;
        if (pz < 1.0) pz = 1.0;
        double scale = fov / pz;
        int sx = (int) (width / 2.0 + x * scale);
        int sy = (int) (height / 2.0 - y * scale); // Invert Y for screen coordinates
        return new Point(sx, sy);
    }
}
