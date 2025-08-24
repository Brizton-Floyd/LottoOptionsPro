package com.example.lottooptionspro.presenter;

public interface TemplateCreatorView {
    void showView();
    void showError(String message);
    void openFileChooser();
    void displayImage(String imagePath);
}
