package com.example.lottooptionspro.models;

public class ScannerMark {
    private static int nextId = 1;
    
    private int id;
    private double x;
    private double y;
    private double width;
    private double height;

    public ScannerMark() {
        this.id = nextId++;
    }

    public ScannerMark(double x, double y, double width, double height) {
        this.id = nextId++;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    /**
     * Reset the nextId counter (used for renumbering)
     */
    public static void setNextId(int newNextId) {
        nextId = newNextId;
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
}
