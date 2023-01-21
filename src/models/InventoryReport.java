package models;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Entity that holds all data related to specific inventory report
 */
public class InventoryReport implements Serializable {
    private final String machineName;
    private final String month;
    private final String year;
    // List of days with map that holds <key: ProductName, value: amount>
    private final List<Map<String, Integer>> dailyInventory;
    // List of days with map that holds amount of products below threshold
    private final List<Integer> belowThresholdAmount;
    // List of days with map that holds amount of unavailable products
    private final List<Integer> unavailableAmount;

    public InventoryReport(String machineName, String month, String year, List<Map<String, Integer>> dailyInventory,
                           List<Integer> belowThresholdAmount, List<Integer> unavailableAmount) {
        this.machineName = machineName;
        this.month = month;
        this.year = year;
        this.dailyInventory = dailyInventory;
        this.belowThresholdAmount = belowThresholdAmount;
        this.unavailableAmount = unavailableAmount;
    }

    public String getMachineName() {
        return machineName;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    public List<Map<String, Integer>> getDailyInventory() {
        return dailyInventory;
    }

    public List<Integer> getBelowThresholdAmount() {
        return belowThresholdAmount;
    }

    public List<Integer> getUnavailableAmount() {
        return unavailableAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryReport that = (InventoryReport) o;
        return Objects.equals(machineName, that.machineName) && Objects.equals(month, that.month) && Objects.equals(year, that.year) && Objects.equals(dailyInventory, that.dailyInventory) && Objects.equals(belowThresholdAmount, that.belowThresholdAmount) && Objects.equals(unavailableAmount, that.unavailableAmount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(machineName, month, year, dailyInventory, belowThresholdAmount, unavailableAmount);
    }
}
