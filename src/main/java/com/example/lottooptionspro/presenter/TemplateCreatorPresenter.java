package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.BetslipTemplate;
import com.example.lottooptionspro.models.Coordinate;
import com.example.lottooptionspro.models.Mark;
import com.example.lottooptionspro.models.PlayPanel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TemplateCreatorPresenter {
    private BetslipTemplate model;
    private final TemplateCreatorView view;
    private final List<Runnable> undoStack = new ArrayList<>();

    private Coordinate originalCoordinate;
    private File currentFile; // Remember the current file for saving

    public TemplateCreatorPresenter(BetslipTemplate model, TemplateCreatorView view) {
        this.model = model;
        this.view = view;
        if (this.model.getMark() == null) this.model.setMark(new Mark(20, 20));
        if (this.model.getPlayPanels() == null) this.model.setPlayPanels(new ArrayList<>());
        if (this.model.getGlobalOptions() == null) this.model.setGlobalOptions(new HashMap<>());
    }

    public void onPanelOrModeChanged() {
        String currentPanelId = view.getSelectedPanel();
        String currentMode = view.getSelectedMappingMode();
        if (currentPanelId == null || currentMode == null) return;

        PlayPanel panel = getOrCreatePlayPanel(currentPanelId);
        int nextNumber = 1;
        if ("Main Number".equals(currentMode)) {
            nextNumber = panel.getMainNumbers().size() + 1;
        } else if ("Bonus Number".equals(currentMode)) {
            nextNumber = panel.getBonusNumbers().size() + 1;
        }
        view.setNextNumber(String.valueOf(nextNumber));
    }

    public void saveTemplate() {
        if (currentFile != null) {
            writeTemplateToFile(currentFile);
        } else {
            saveTemplateAs();
        }
    }

    public void saveTemplateAs() {
        String gameName = view.getGameName();
        String jurisdiction = view.getJurisdiction();

        if (gameName == null || gameName.trim().isEmpty() || jurisdiction == null || jurisdiction.trim().isEmpty()) {
            view.showError("Please enter both a Game Name and Jurisdiction before saving.");
            return;
        }

        String initialFileName = String.format("%s-%s.json", jurisdiction.toLowerCase().replace(" ", ""), gameName.toLowerCase().replace(" ", ""));
        File file = view.showSaveDialog(initialFileName);
        if (file != null) {
            writeTemplateToFile(file);
        }
    }

    private void writeTemplateToFile(File file) {
        model.setGameName(view.getGameName());
        model.setJurisdiction(view.getJurisdiction());

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(model, writer);
            this.currentFile = file; // Remember the file path
            view.showSuccess("Template saved successfully to " + file.getName());
        } catch (IOException e) {
            view.showError("Failed to save template: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadTemplate() {
        File file = view.showOpenTemplateDialog();
        if (file != null) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                this.model = gson.fromJson(reader, BetslipTemplate.class);
                this.currentFile = file; // Remember the file path
                updateViewFromModel();
                view.showSuccess("Template loaded successfully from " + file.getName());
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                view.showError("Failed to load template: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void previewTemplate() {
        view.clearPreviewRectangles();
        String panelId = view.getSelectedPanel();
        if (panelId == null || panelId.isEmpty()) {
            view.showError("Please select a panel to preview.");
            return;
        }
        PlayPanel panel = getOrCreatePlayPanel(panelId);

        view.askForPreviewNumbers().ifPresent(numbersString -> {
            int marksDrawn = 0;
            String[] parts = numbersString.split(",");

            if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                String[] mainNumbers = parts[0].trim().split("\\s+");
                for (String num : mainNumbers) {
                    Coordinate coord = panel.getMainNumbers().get(num);
                    if (coord != null) {
                        view.drawPreviewRectangle(coord, model.getMark().getWidth(), model.getMark().getHeight());
                        marksDrawn++;
                    }
                }
            }

            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                String[] bonusNumbers = parts[1].trim().split("\\s+");
                for (String num : bonusNumbers) {
                    Coordinate coord = panel.getBonusNumbers().get(num);
                    if (coord != null) {
                        view.drawPreviewRectangle(coord, model.getMark().getWidth(), model.getMark().getHeight());
                        marksDrawn++;
                    }
                }
            }

            if (marksDrawn == 0) {
                view.showSuccess("Preview Complete: No coordinates were found for the entered numbers on this panel.");
            }
        });
    }

    private void updateViewFromModel() {
        view.setGameName(model.getGameName());
        view.setJurisdiction(model.getJurisdiction());
        if (model.getImagePath() != null && !model.getImagePath().isEmpty()) {
            view.displayImage(new File(model.getImagePath()).toURI().toString());
        }
        redrawAllMarkings();
        onPanelOrModeChanged();
    }

    public void loadImage() {
        File file = view.showOpenImageDialog();
        if (file != null) {
            onImageSelected(file.toURI().toString(), file.getAbsolutePath());
        }
    }

    public void onImageSelected(String imageUri, String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            model.setImagePath(imagePath);
            view.displayImage(imageUri);
            this.currentFile = null; // New image means it's a new template
            undoStack.clear();
            redrawAllMarkings();
        }
    }

    public void onPaneClicked(double x, double y) {
        view.clearPreviewRectangles();
        String mappingMode = view.getSelectedMappingMode();
        if (mappingMode == null || mappingMode.isEmpty()) {
            view.showError("Please select a mapping mode first.");
            return;
        }
        Coordinate newCoordinate = new Coordinate((int) x, (int) y);
        switch (mappingMode) {
            case "Main Number":
            case "Bonus Number":
            case "Quick Pick":
                handlePanelMapping(newCoordinate, mappingMode);
                break;
            case "Global Option":
                handleGlobalOptionMapping(newCoordinate);
                break;
        }
    }

    private void handlePanelMapping(Coordinate coordinate, String mappingMode) {
        String panelId = view.getSelectedPanel();
        if (panelId == null || panelId.isEmpty()) {
            view.showError("Please select a panel first.");
            return;
        }
        PlayPanel panel = getOrCreatePlayPanel(panelId);

        if ("Quick Pick".equals(mappingMode)) {
            Coordinate oldQp = panel.getQuickPick();
            panel.setQuickPick(coordinate);
            undoStack.add(() -> panel.setQuickPick(oldQp));
        } else { // Main or Bonus
            String numberKey = view.getNextNumber();
            if (numberKey == null || numberKey.trim().isEmpty()) {
                view.showError("Please enter a value in the 'Next Number' field.");
                return;
            }
            if ("Main Number".equals(mappingMode)) {
                Coordinate oldMain = panel.getMainNumbers().put(numberKey, coordinate);
                undoStack.add(() -> {
                    if (oldMain != null) panel.getMainNumbers().put(numberKey, oldMain); else panel.getMainNumbers().remove(numberKey);
                    onPanelOrModeChanged();
                });
            } else { // Bonus Number
                Coordinate oldBonus = panel.getBonusNumbers().put(numberKey, coordinate);
                undoStack.add(() -> {
                    if (oldBonus != null) panel.getBonusNumbers().put(numberKey, oldBonus); else panel.getBonusNumbers().remove(numberKey);
                    onPanelOrModeChanged();
                });
            }
            onPanelOrModeChanged();
        }
        redrawAllMarkings();
    }

    private void handleGlobalOptionMapping(Coordinate coordinate) {
        String optionName = view.getGlobalOptionName();
        if (optionName == null || optionName.trim().isEmpty()) {
            view.showError("Please enter a name for the Global Option.");
            return;
        }
        Coordinate oldGlobal = model.getGlobalOptions().put(optionName, coordinate);
        undoStack.add(() -> model.getGlobalOptions().put(optionName, oldGlobal));
        redrawAllMarkings();
    }

    public void removeLastMarking() {
        view.clearPreviewRectangles();
        if (!undoStack.isEmpty()) {
            undoStack.remove(undoStack.size() - 1).run();
            redrawAllMarkings();
        }
    }

    public void updateMarkSize(int width, int height) {
        model.getMark().setWidth(width);
        model.getMark().setHeight(height);
        redrawAllMarkings();
    }

    public void startCoordinateMove(Coordinate coord) {
        this.originalCoordinate = new Coordinate(coord.getX(), coord.getY());
    }

    public void finishCoordinateMove(Coordinate coordToMove, int newX, int newY) {
        final int oldX = this.originalCoordinate.getX();
        final int oldY = this.originalCoordinate.getY();
        undoStack.add(() -> {
            coordToMove.setX(oldX);
            coordToMove.setY(oldY);
            redrawAllMarkings();
        });

        coordToMove.setX(newX);
        coordToMove.setY(newY);

        this.originalCoordinate = null;
        redrawAllMarkings();
    }

    private void redrawAllMarkings() {
        view.clearAllRectangles();
        int w = model.getMark().getWidth();
        int h = model.getMark().getHeight();
        for (PlayPanel panel : model.getPlayPanels()) {
            panel.getMainNumbers().values().forEach(c -> view.drawRectangle(c, w, h));
            panel.getBonusNumbers().values().forEach(c -> view.drawRectangle(c, w, h));
            if (panel.getQuickPick() != null) view.drawRectangle(panel.getQuickPick(), w, h);
        }
        model.getGlobalOptions().values().forEach(c -> view.drawRectangle(c, w, h));
    }

    private PlayPanel getOrCreatePlayPanel(String panelId) {
        return model.getPlayPanels().stream()
                .filter(p -> panelId.equals(p.getPanelId()))
                .findFirst()
                .orElseGet(() -> {
                    PlayPanel newPanel = new PlayPanel(panelId, new HashMap<>(), new HashMap<>(), null);
                    model.getPlayPanels().add(newPanel);
                    return newPanel;
                });
    }
}
