package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.BetslipTemplate;

public class TemplateCreatorPresenter {
    private BetslipTemplate model;
    private TemplateCreatorView view;

    public TemplateCreatorPresenter(BetslipTemplate model, TemplateCreatorView view) {
        this.model = model;
        this.view = view;
    }

    public void loadImage() {
        view.openFileChooser();
    }

    public void onImageSelected(String imageUri, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            model.setImagePath(imagePath);
            view.displayImage(imageUri);
        }
    }
}
