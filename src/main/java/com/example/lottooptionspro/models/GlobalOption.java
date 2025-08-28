package com.example.lottooptionspro.models;

/**
 * Represents a Global Option marking with coordinates and size information
 * Similar to ScannerMark but for Global Options like "Power Play", "Cash Value", etc.
 */
public class GlobalOption {
    private String name;        // The option name from dropdown (e.g., "Power Play")
    private double x;          // X coordinate
    private double y;          // Y coordinate  
    private double width;      // Width of the marking
    private double height;     // Height of the marking

    // Default constructor
    public GlobalOption() {
    }

    // Full constructor
    public GlobalOption(String name, double x, double y, double width, double height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // Constructor from coordinate and size (for migration)
    public GlobalOption(String name, Coordinate coordinate, double width, double height) {
        this.name = name;
        this.x = coordinate.getX();
        this.y = coordinate.getY();
        this.width = width;
        this.height = height;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    // Utility method to get coordinate
    public Coordinate getCoordinate() {
        return new Coordinate((int) x, (int) y);
    }

    // Utility method to update coordinate
    public void setCoordinate(Coordinate coordinate) {
        this.x = coordinate.getX();
        this.y = coordinate.getY();
    }

    @Override
    public String toString() {
        return "GlobalOption{" +
               "name='" + name + '\'' +
               ", x=" + x +
               ", y=" + y +
               ", width=" + width +
               ", height=" + height +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        GlobalOption that = (GlobalOption) o;
        
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.y, y) == 0 &&
               Double.compare(that.width, width) == 0 &&
               Double.compare(that.height, height) == 0 &&
               (name != null ? name.equals(that.name) : that.name == null);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = name != null ? name.hashCode() : 0;
        temp = Double.doubleToLongBits(x);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(width);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(height);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}