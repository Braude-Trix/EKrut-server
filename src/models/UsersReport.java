package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.max;

/**
 * Entity that holds all data related to specific users report
 */
public class UsersReport implements Serializable {
    private final String region;
    private final String month;
    private final String year;
    // map of all activity of clients that holds <key: boundary, value: amountOfUsers>
    private final Map<String, Integer> usersActivityDistribution;
    // map of all orders of clients that holds <key: clientId, value: amountOfOrders>
    private final Map<String, Integer> clientsOrders;
    // map of all orders of subscribers that holds <key: clientId, value: amountOfOrders>
    private final Map<String, Integer> subscribersOrders;
    // sorted list of top 3 client names
    private List<String> top3ClientNames;
    // map of all orders of subscribers that holds <key: userId, value: amountOfOrders>
    private final Map<String, Integer> top3UserIdAndAmount;
    private final int maxAmountOfOrders;
    private final double clientsPercentage;
    private final double subscribersPercentage;

    public UsersReport(String region, String month, String year, Map<String, Integer> clientsOrders,
                       Map<String, Integer> subscribersOrders, List<String> top3ClientNames) {
        this.region = region;
        this.month = month;
        this.year = year;
        this.clientsOrders = clientsOrders;
        this.subscribersOrders = subscribersOrders;
        this.top3ClientNames = top3ClientNames;
        top3UserIdAndAmount = calculateTop3Users();
        maxAmountOfOrders = calculateMaxOfOrders();
        usersActivityDistribution = calculateUsersActivityDistribution();
        clientsPercentage = calculateClientsPercentage();
        subscribersPercentage = calculateSubscribersPercentage();
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

    public Map<String, Integer> getUsersActivityDistribution() {
        return usersActivityDistribution;
    }

    public List<String> getTop3ClientNames() {
        return top3ClientNames;
    }

    public Map<String, Integer> getTop3UserIdAndAmount() {
        return top3UserIdAndAmount;
    }

    public double getClientsPercentage() {
        return clientsPercentage;
    }

    public double getSubscribersPercentage() {
        return subscribersPercentage;
    }

    public void setTop3ClientNames(List<String> top3ClientNames) {
        this.top3ClientNames = top3ClientNames;
    }

    private Map<String, Integer> calculateTop3Users() {
        Map<String, Integer> top3Users = new LinkedHashMap<>();
        Map<String, Integer> totalOrders = new HashMap<>(clientsOrders);
        totalOrders.putAll(subscribersOrders);

        String topClient = getKeyOfMaxValue(totalOrders);
        if (topClient == null) return new LinkedHashMap<>();
        top3Users.put(topClient, totalOrders.get(topClient));
        totalOrders.remove(topClient);

        String secondClient = getKeyOfMaxValue(totalOrders);
        if (secondClient == null) return top3Users;
        top3Users.put(secondClient, totalOrders.get(secondClient));
        totalOrders.remove(secondClient);

        String thirdClient = getKeyOfMaxValue(totalOrders);
        if (thirdClient == null) return top3Users;
        top3Users.put(thirdClient, totalOrders.get(thirdClient));
        totalOrders.remove(thirdClient);

        return top3Users;
    }

    private int calculateMaxOfOrders() {
        return Math.max(maxOfCollection(clientsOrders.values()),
                maxOfCollection(subscribersOrders.values()));
    }

    private int maxOfCollection(Collection<Integer> collection) {
        if (collection == null || collection.isEmpty()) return 0;
        return max(collection);
    }

    private Map<String, Integer> calculateUsersActivityDistribution() {
        int numOfLevels = 10;
        if (maxAmountOfOrders == 0) return new HashMap<>();
        if (maxAmountOfOrders < 9) numOfLevels = maxAmountOfOrders;

        // init map with 0 amount
        Map<String, Integer> usersActivityDistribution = new LinkedHashMap<>();
        List<Boundary> boundaries = new ArrayList<>();
        for (int i = 0; i < numOfLevels; i++) {
            double diffForPresent = maxAmountOfOrders / (double) numOfLevels;
            int lowerBound = (int) Math.ceil(i * diffForPresent);
            int upperBound = (int) Math.ceil((i + 1) * diffForPresent) - 1;
            upperBound = i == (numOfLevels - 1) ? maxAmountOfOrders : upperBound; // setting top bound
            boundaries.add(new Boundary(lowerBound, upperBound));
        }
        for (Boundary boundary : boundaries) {
            boundary.addToBoundary(clientsOrders.values());
            boundary.addToBoundary(subscribersOrders.values());
            usersActivityDistribution.put(boundary.name, boundary.amountInBoundary);
        }
        return usersActivityDistribution;
    }

    private double calculateClientsPercentage() {
        int totalUsers = clientsOrders.size() + subscribersOrders.size();
        if (totalUsers == 0) return 0;
        return Math.floor((100.0 / totalUsers) * clientsOrders.size());
    }

    private double calculateSubscribersPercentage() {
        int totalUsers = clientsOrders.size() + subscribersOrders.size();
        if (totalUsers == 0) return 0;
        return Math.ceil((100.0 / totalUsers) * subscribersOrders.size());
    }

    private String getKeyOfMaxValue(Map<String, Integer> map) {
        if (map.keySet().isEmpty())
            return null;

        String maxKey = map.keySet().stream().findFirst().get();
        int max = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (max < entry.getValue()) {
                max = entry.getValue();
                maxKey = entry.getKey();
            }
        }
        return maxKey;
    }

    private class Boundary {
        final int lower;
        final int upper;
        final String name;
        int amountInBoundary;

        public Boundary(int lower, int upper) {
            this.lower = lower;
            this.upper = upper;
            if (lower == upper)
                name = String.format("%s", lower);
            else
                name = String.format("%s-%s", lower, upper);
        }

        void addToBoundary(Collection<Integer> list) {
            for (Integer amount : list) {
                if (amount >= lower && amount <= upper)
                    amountInBoundary++;
            }
        }
    }
}
