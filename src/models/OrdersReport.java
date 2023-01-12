package models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity that holds all data related to specific orders report
 */
public class OrdersReport implements Serializable {
    private final String region;
    private final String month;
    private final String year;
    // List of days with map that holds <key: MachineName, value: amountOfOrders in EK>
    private final List<Map<String, Integer>> ekOrders;
    // List of days with map that holds <key: MachineName, value: amountOfOrders in latePickup>
    private final List<Map<String, Integer>> latePickupOrders;
    // List of days with map that holds <key: MachineName, value: amountOfOrders in EK or latePickup>
    private final Map<String, Integer> monthlyOrders;
    private final double ekOrdersPercentage;
    private final double latePickupOrdersPercentage;
    private final int totalMonthlyOrders;

    public OrdersReport(String region, String month, String year,
                        List<Map<String, Integer>> ekOrders, List<Map<String, Integer>> latePickupOrders) {
        this.region = region;
        this.month = month;
        this.year = year;
        this.ekOrders = ekOrders;
        this.latePickupOrders = latePickupOrders;
        this.monthlyOrders = calculateMonthlyOrders();
        this.totalMonthlyOrders = calculateTotalMonthlyOrders();
        this.ekOrdersPercentage = calculateEkOrdersPercentage();
        this.latePickupOrdersPercentage = calculateLatePickupOrdersPercentage();
    }

    public String getRegion() {
        return region;
    }

    public String getMonth() {
        return month;
    }

    public String getYear() {
        return year;
    }

    public List<Map<String, Integer>> getEkOrders() {
        return ekOrders;
    }

    public List<Map<String, Integer>> getLatePickupOrders() {
        return latePickupOrders;
    }

    public Map<String, Integer> getMonthlyOrders() {
        return monthlyOrders;
    }

    public double getEkOrdersPercentage() {
        return ekOrdersPercentage;
    }

    public double getLatePickupOrdersPercentage() {
        return latePickupOrdersPercentage;
    }

    private double calculateEkOrdersPercentage() {
        Integer ekOrdersSum = 0;
        for (Map<String, Integer> dailyEkOrders : ekOrders) {
            for (Integer machineMonthOrder : dailyEkOrders.values()) {
                ekOrdersSum += machineMonthOrder;
            }
        }

        return Math.floor((100.0 / totalMonthlyOrders) * ekOrdersSum);
    }

    private double calculateLatePickupOrdersPercentage() {
        Integer latePickupOrdersSum = 0;
        for (Map<String, Integer> dailyLatePickupOrders : latePickupOrders) {
            for (Integer machineMonthOrder : dailyLatePickupOrders.values()) {
                latePickupOrdersSum += machineMonthOrder;
            }
        }
        return Math.ceil((100.0 / totalMonthlyOrders) * latePickupOrdersSum);
    }

    private Map<String, Integer> calculateMonthlyOrders() {
        Map<String, Integer> monthlyOrders = new HashMap<>();
        for (Map<String, Integer> dailyEkOrders : ekOrders) {
            unionMaps(monthlyOrders, dailyEkOrders);
        }
        for (Map<String, Integer> dailyLatePickupOrders : latePickupOrders) {
            unionMaps(monthlyOrders, dailyLatePickupOrders);
        }
        return monthlyOrders;
    }

    private Integer calculateTotalMonthlyOrders() {
        Integer sum = 0;
        for (Integer machineMonthOrder : this.monthlyOrders.values()) {
            sum += machineMonthOrder;
        }
        return sum;
    }

    private void unionMaps(Map<String, Integer> srcMap, Map<String, Integer> mapToAdd) {
        for (Map.Entry<String, Integer> entry : mapToAdd.entrySet()) {
            if (srcMap.containsKey(entry.getKey())) {
                Integer newAmount = srcMap.get(entry.getKey()) + entry.getValue();
                srcMap.put(entry.getKey(), newAmount);
            } else {
                srcMap.put(entry.getKey(), entry.getValue());
            }
        }
    }
}
