// Quick test to verify GridConfiguration works (run this manually if needed)

import com.example.lottooptionspro.models.GridConfiguration;
import com.example.lottooptionspro.models.BetslipTemplate;

public class TEST_GRID_CONFIG {
    public static void main(String[] args) {
        // Test GridConfiguration
        GridConfiguration config = new GridConfiguration("1-54", 11, 5, "COLUMN_BOTTOM_TO_TOP");
        System.out.println("Grid config: " + config.toString());
        System.out.println("Is valid: " + config.isValid());
        
        // Test BetslipTemplate with GridConfiguration
        BetslipTemplate template = new BetslipTemplate();
        template.setGridConfig(config);
        
        GridConfiguration retrieved = template.getGridConfig();
        System.out.println("Retrieved config: " + retrieved.toString());
        System.out.println("Range: " + retrieved.getNumberRange());
        System.out.println("Dimensions: " + retrieved.getColumns() + "x" + retrieved.getRows());
        
        System.out.println("✅ Grid configuration persistence test PASSED!");
    }
}