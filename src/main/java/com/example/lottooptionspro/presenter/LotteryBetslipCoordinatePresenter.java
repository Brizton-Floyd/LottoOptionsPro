package com.example.lottooptionspro.presenter;

import com.example.lottooptionspro.models.LotteryGameBetSlipCoordinates;
import com.example.lottooptionspro.util.ImageUtils;
import com.example.lottooptionspro.util.LotteryBetslipProcessor;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class LotteryBetslipCoordinatePresenter {

    private final LotteryBetslipCoordinateView view;
    private LotteryBetslipProcessor processor;

    public LotteryBetslipCoordinatePresenter(LotteryBetslipCoordinateView view) {
        this.view = view;
    }

    public void updateProcessor() {
        String state = view.getState().trim().toUpperCase();
        String game = view.getGame().trim();
        String panelCountText = view.getPanelCountText();
        String mainBallRowsText = view.getMainBallRowsText();
        String mainBallColumnsText = view.getMainBallColumnsText();
        String bonusBallRowsText = view.getBonusBallRowsText();
        String bonusBallColumnsText = view.getBonusBallColumnsText();
        String xOffsetsText = view.getXOffsetsText();
        String yOffsetsText = view.getYOffsetsText();
        String bonusXOffsetsText = view.getBonusXOffsetsText();
        String bonusYOffsetsText = view.getBonusYOffsetsText();

        if (state.isEmpty() || game.isEmpty() || panelCountText.isEmpty() ||
                mainBallRowsText.isEmpty() || mainBallColumnsText.isEmpty() ||
                xOffsetsText.isEmpty() || yOffsetsText.isEmpty() ||
                (view.isBonusGameChecked() && (bonusBallRowsText.isEmpty() || bonusBallColumnsText.isEmpty() || bonusXOffsetsText.isEmpty() || bonusYOffsetsText.isEmpty()))) {
            view.showAlert("Error", "All required fields must be filled out.");
            return;
        }

        String imagePath = "src/main/resources/images/" + state + "/" + game + ".jpg";

        try {
            int panelCount = Integer.parseInt(panelCountText);
            int mainBallRows = Integer.parseInt(mainBallRowsText);
            int mainBallColumns = Integer.parseInt(mainBallColumnsText);
            int bonusBallRows = view.isBonusGameChecked() ? Integer.parseInt(bonusBallRowsText) : 0;
            int bonusBallColumns = view.isBonusGameChecked() ? Integer.parseInt(bonusBallColumnsText) : 0;

            Point jackpotOptionCoordinate = null;
            if (view.isJackpotOptionChecked()) {
                int x = Integer.parseInt(view.getJackpotOptionXText());
                int y = Integer.parseInt(view.getJackpotOptionYText());
                jackpotOptionCoordinate = new Point(x, y);
            }

            int[] xOffsets = parseOffsets(xOffsetsText, panelCount);
            int[] yOffsets = parseOffsets(yOffsetsText, panelCount);
            int[] bonusXOffsets = view.isBonusGameChecked() ? parseOffsets(bonusXOffsetsText, panelCount) : new int[panelCount];
            int[] bonusYOffsets = view.isBonusGameChecked() ? parseOffsets(bonusYOffsetsText, panelCount) : new int[panelCount];

            if (xOffsets == null || yOffsets == null || (view.isBonusGameChecked() && (bonusXOffsets == null || bonusYOffsets == null))) {
                view.showAlert("Error", "Offsets must be valid integers separated by commas, matching the panel count.");
                return;
            }

            LotteryGameBetSlipCoordinates gameCoordinates = new LotteryGameBetSlipCoordinates(
                    new HashMap<>(), new HashMap<>(), jackpotOptionCoordinate, view.isVerticalOrientationChecked(), 0.0
            );

            processor = new LotteryBetslipProcessor(imagePath, panelCount, mainBallRows,
                    bonusBallRows, mainBallColumns, bonusBallColumns, xOffsets, yOffsets, bonusXOffsets,
                    bonusYOffsets, jackpotOptionCoordinate, gameCoordinates, view.isVerticalOrientationChecked(), view.isBottomToTopChecked());

            processor.setSpacing(view.getMainBallHorizontalSpacing(), view.getBonusBallHorizontalSpacing(), view.getVerticalSpacing());
            processor.setMarkingProperties(view.getMarkingSize());
            updateImage();

        } catch (NumberFormatException e) {
            view.showAlert("Error", "Panel count, rows, columns, and offsets must be valid integers.");
        } catch (IOException e) {
            String absolutePath = new File(imagePath).getAbsolutePath();
            view.showAlert("Image Load Error", "Could not load the image file. Please check that the file exists and the state/game names are correct.\n\nAttempted path:\n" + absolutePath);
        }
    }

    private int[] parseOffsets(String offsetsText, int panelCount) {
        String[] parts = offsetsText.split(",");
        if (parts.length != panelCount) {
            return null;
        }
        int[] offsets = new int[panelCount];
        try {
            for (int i = 0; i < panelCount; i++) {
                offsets[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return offsets;
    }

    private void updateImage() {
        if (processor != null) {
            view.setImage(ImageUtils.convertToFxImage(processor.plotMarkings()));
        }
    }

    public void saveCoordinates() {
        if (processor != null) {
            String directoryPath = "Serialized Files/" + view.getState().trim().toUpperCase();
            String initialFileName = view.getGame().trim() + ".ser";
            File file = view.showSaveDialog(directoryPath, initialFileName);
            if (file != null) {
                try {
                    processor.saveCoordinatesToFile(file.getAbsolutePath());
                    view.showAlert("Success", "Coordinates saved successfully.");
                } catch (IOException e) {
                    view.showAlert("Error", "Cannot save coordinates: " + e.getMessage());
                }
            }
        }
    }
}
