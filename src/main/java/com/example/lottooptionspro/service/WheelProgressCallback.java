package com.example.lottooptionspro.service;

public interface WheelProgressCallback {
    void update(String message, double progress, int covered, int total, int lines);
}
