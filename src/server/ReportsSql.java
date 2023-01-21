package server;

import gui.IServerGui;
import models.InventoryReport;
import models.Order;
import models.OrdersReport;
import models.PickUpMethod;
import models.ProductInMachine;
import models.Regions;
import models.ReportType;
import models.Response;
import models.ResponseCode;
import models.StatusInMachine;
import models.User;
import models.UsersReport;
import serverModels.ProductInMachineHistory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static server.mysqlController.*;

public class ReportsSql implements IReportsSql {
    private ITime iTime;
    private IServerGui iServerGui;
    private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";

    public ReportsSql(ITime iTime, IServerGui iServerGui) {
    	this.iTime = iTime;
        this.iServerGui = iServerGui;
    }
    
    @Override
    public Boolean checkIfReportsAreAlreadyCreated() {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM saved_reports WHERE year_creation = ? AND month_creation = ?";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            stmt.setInt(1, iTime.getReportsYear());
            stmt.setInt(2, iTime.getReportsMonth());
            rs = stmt.executeQuery();
            boolean hasReport = rs.next();
            rs.close();
            return hasReport;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public List<ProductInMachineHistory> getAllProductsHistory() {
        PreparedStatement stmt;
        ResultSet rs;
        List<ProductInMachineHistory> productInMachineHistories = new ArrayList<>();

        String query = "SELECT * FROM product_in_machine_history WHERE updated_month = ?";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            stmt.setInt(1, iTime.getReportsMonth());
            rs = stmt.executeQuery();
            while (rs.next()) {
                String machineId = String.valueOf(rs.getInt("machineId"));
                String productId = String.valueOf(rs.getInt("productId"));
                StatusInMachine statusInMachine = StatusInMachine.valueOf(rs.getString("statusInMachine"));
                Integer amount = rs.getInt("amountInMachine");
                int month = rs.getInt("updated_month");
                int day = rs.getInt("updated_day");
                ProductInMachineHistory product = new ProductInMachineHistory(
                        machineId, productId, statusInMachine, amount, month, day);
                productInMachineHistories.add(product);
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
        return productInMachineHistories;
    }

    @Override
    public String getNameByProductId(Response response, Integer productId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM products WHERE productId = ?";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("productName"));
            } else {
            	mysqlController.editResponse(response, ResponseCode.INVALID_DATA, "Couldn't find name of given product id", null);
            	return null;
            }
            mysqlController.editResponse(response, ResponseCode.OK, "Successfully get name by product id", res);
            return res.get(0).toString();
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while searching for name of product", null);
            e.printStackTrace();
            iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
    }

    @Override
    public List<String> getRegionAndNameByMachineId(Response response, Integer machineId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("region"));
                res.add(rs.getString("machineName"));
            } else {
            	mysqlController.editResponse(response, ResponseCode.INVALID_DATA, 
            			"Couldn't find region and name of given machine id", null);
            	return null;
            }
            mysqlController.editResponse(response, ResponseCode.OK, "Successfully get region by machine id", res);
            return res.stream().map(Object::toString).collect(Collectors.toList());
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR, 
        			"There was an error while searching region and name of a machine id", null);
            e.printStackTrace();
            iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
    }

    @Override
    public Boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, Integer machineId) {
    	byte[] inventoryBytes;
        Response responseForSaveReport = new Response();
        try {
            inventoryBytes = getSerializedObject(inventoryReport);
        } catch (Exception e) {
            responseForSaveReport.setDescription("There was an error in serializing InventoryReport object");
            appendDescription(responseForSaveReport, response);
            return false;
        }
        saveReportInDB(responseForSaveReport, ReportType.INVENTORY,
                region, machineId, inventoryBytes);
        if (responseForSaveReport.getCode() != ResponseCode.OK) {
        	appendDescription(responseForSaveReport, response);
        	return false;
        }
        
        return true;
    }

    @Override
    public void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes) {
    	 PreparedStatement stmt;
         String query = "INSERT into " +
                 "saved_reports (year_creation, month_creation, report_type, region, machineId, report_data) " +
                 "VALUES (?, ?, ?, ?, ?, ?)";
         try {
             stmt = mysqlController.conn.prepareStatement(query);
             stmt.setInt(1, iTime.getReportsYear());
             stmt.setInt(2, iTime.getReportsMonth());
             stmt.setString(3, type.name());
             stmt.setString(4, region.name());
             stmt.setInt(5, machineId);
             stmt.setBytes(6, blobBytes);

             stmt.executeUpdate();
             String msg = String.format("Successfully saved report %s of region %s", type.name(), region.name());
             if (machineId != -1)
                 msg = String.format("%s of machine %s", msg, machineId);
             mysqlController.editResponse(response, ResponseCode.OK, msg, null);
             iServerGui.setPrintToConsole(msg);
         } catch (SQLException e) {
             String msg = String.format("There was an error in saving report %s of region %s", type.name(), region.name());
             if (machineId != -1)
                 msg = String.format("%s of machine %s", msg, machineId);
             mysqlController.editResponse(response, ResponseCode.DB_ERROR, msg, null);
             iServerGui.setPrintToConsole(msg);
             e.printStackTrace();
             iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
         }
    }

    /**
     * Calls the actual saveReportInDB with default machineId value of -1 (not relevant to that reports)
     */
    private void saveReportInDB(Response response, ReportType type, Regions region, byte[] blobBytes) {
        saveReportInDB(response, type, region, -1, blobBytes);
    }

    @Override
    public Boolean deleteLastMonthFromTable(Response response) {
        int currentMonth = iTime.getCurrentMonth();
        PreparedStatement stmt;
        String query = "DELETE FROM product_in_machine_history WHERE updated_month != ?";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            stmt.setInt(1, currentMonth);
            stmt.executeUpdate();
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR,
                    "[Report] Failed to delete previous month's inventory data", null);
            System.out.println(e.getMessage());
            iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return false;
        }

        mysqlController.editResponse(response, ResponseCode.OK,
                "[Report] Successfully deleted previous month's inventory data", null);
        return true;
    }

    @Override
    public Boolean transferDataFromProductInMachineToHistory(Response response) {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM productinmachine";
        try {
            stmt = mysqlController.conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                ProductInMachine productInMachine = new ProductInMachine(
                        Integer.toString(rs.getInt("machineId")),
                        Integer.toString(rs.getInt("productId")),
                        StatusInMachine.valueOf(rs.getString("statusInMachine")),
                        rs.getInt("amountInMachine"));
                Server.mysqlController.insertProductInMachineToHistory(response, productInMachine);
                if (response.getCode() != ResponseCode.OK)
                    break;
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (response.getCode() != ResponseCode.OK)
                iServerGui.setPrintToConsole(response.getDescription(), true);
            else
                iServerGui.setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return false;
        }
        mysqlController.editResponse(response, ResponseCode.OK, "[Report] Successfully inserting all of current month's inventory data to table", null);
        return true;
    }

    @Override
    public boolean generateOrdersReport(Response response) {
        boolean isReportOfARegionFailed = false;
        // map that holds key: region, value: it's orders
        Map<String, List<Order>> ordersByRegion = new HashMap<>();
        // map that holds key: machineId, value: it's Region (as String)
        Map<Integer, String> regionByMachineId = new HashMap<>();
        // map that holds key: machineId, value: it's Name (as String)
        Map<Integer, String> nameByMachineId = new HashMap<>();
        List<Order> filteredByDateOrder = Server.mysqlController.getAllNonDeliveryOrders();
        if (filteredByDateOrder == null) {
            response.setDescription("failed in getAllNonDeliveryOrders");
            return false;
        }
        Set<Integer> machineIdsSet = filteredByDateOrder.stream()
                .map(Order::getMachineId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        // mapping all machines to their regions and names
        for (Integer integer : machineIdsSet) {
            Response responseToRegion = new Response();
            getRegionAndNameByMachineId(responseToRegion, integer);
            if (responseToRegion.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getRegionByMachineId");
                return false;
            }
            String machineRegion = responseToRegion.getBody().get(0).toString();
            String machineName = responseToRegion.getBody().get(1).toString();
            if (Regions.valueOf(machineRegion) == Regions.All)
                continue;
            regionByMachineId.put(integer, machineRegion);
            nameByMachineId.put(integer, machineName);
        }
        // init all orders by region
        ordersByRegion.put(Regions.North.name(), new ArrayList<>());
        ordersByRegion.put(Regions.South.name(), new ArrayList<>());
        ordersByRegion.put(Regions.UAE.name(), new ArrayList<>());

        for (Order order : filteredByDateOrder) {
            String orderRegion = regionByMachineId.get(Integer.parseInt(order.getMachineId()));
            ordersByRegion.get(orderRegion).add(order);
        }

        // ---- Generating Reports ----
        // Iterating over all regions and filling relevant data
        for (Map.Entry<String,List<Order>> orderOfRegion : ordersByRegion.entrySet()) {
            // List of days with map that holds <key: MachineName, value: amountOfOrders in EK>
            List<Map<String, Integer>> ekOrders = new ArrayList<>();
            // List of days with map that holds <key: MachineName, value: amountOfOrders in latePickup>
            List<Map<String, Integer>> latePickupOrders = new ArrayList<>();

            // init maps with default values
            for (int i = 1; i <= iTime.getDaysInMonthOfReport(); i++) {
                Map<String, Integer> ekMap = new HashMap<>();
                Map<String, Integer> latePickupMap = new HashMap<>();
                for (Map.Entry<Integer, String> machineNameById : nameByMachineId.entrySet()) {
                    String regionOfMachine = regionByMachineId.get(machineNameById.getKey());
                    if (orderOfRegion.getKey().equals(regionOfMachine)) {
                        ekMap.put(machineNameById.getValue(), 0);
                        latePickupMap.put(machineNameById.getValue(), 0);
                    }
                }
                ekOrders.add(ekMap);
                latePickupOrders.add(latePickupMap);
            }
            // iterating over all days in current month and filling with actual values
            for (int iteratedDay = 1; iteratedDay <= iTime.getDaysInMonthOfReport(); iteratedDay++) {
                int dayIndex = iteratedDay-1;

                // filling with actual values
                for (Order order : orderOfRegion.getValue()) {
                    // excluding orders that was done in day != to iterated day
                    if (!iTime.isDayInDate(iteratedDay, order.getDate()))
                        continue;
                    if (order.getPickUpMethod() == PickUpMethod.selfPickUp) {
                        String machineName = nameByMachineId.get(Integer.parseInt(order.getMachineId()));
                        Map<String, Integer> dayMap = ekOrders.get(dayIndex);
                        dayMap.put(machineName, dayMap.get(machineName) + 1); // increment counter of orders
                    }
                    else if (order.getPickUpMethod() == PickUpMethod.latePickUp) {
                        String machineName = nameByMachineId.get(Integer.parseInt(order.getMachineId()));
                        Map<String, Integer> dayMap = latePickupOrders.get(dayIndex);
                        dayMap.put(machineName, dayMap.get(machineName) + 1); // increment counter of orders
                    }
                }
            }

            OrdersReport ordersReport = new OrdersReport(
                    orderOfRegion.getKey(),
                    String.valueOf(iTime.getReportsMonth()),
                    String.valueOf(iTime.getReportsYear()),
                    ekOrders,
                    latePickupOrders);

            byte[] ordersBytes;
            Response responseForSaveReport = new Response();
            try {
                ordersBytes = getSerializedObject(ordersReport);
            } catch (Exception e) {
                responseForSaveReport.setDescription("There was an error in serializing OrdersReport object");
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
                continue;
            }
            saveReportInDB(
                    responseForSaveReport, ReportType.ORDERS, Regions.valueOf(orderOfRegion.getKey()), ordersBytes);
            if (responseForSaveReport.getCode() != ResponseCode.OK) {
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
            }
        }

        return !isReportOfARegionFailed;
    }

    @Override
    public boolean generateUsersReport(Response response) {
        boolean isReportOfARegionFailed = false;
        // map that holds key: machineId, value: it's Region (as String)
        Map<Integer, String> regionByMachineId = new HashMap<>();
        // map that holds key: region, value: it's orders
        Map<String, List<Order>> ordersByRegion = new HashMap<>();
        List<Order> filteredByDateOrder = Server.mysqlController.getAllNonDeliveryOrders();
        if (filteredByDateOrder == null) {
            response.setDescription("failed in getAllNonDeliveryOrders");
            return false;
        }

        // init set of all machineIds from all orders
        Set<Integer> machineIdsSet = filteredByDateOrder.stream()
                .map(Order::getMachineId)
                .map(Integer::parseInt).collect(Collectors.toSet());

        // mapping all machines to their regions
        for (Integer integer : machineIdsSet) {
            Response responseToRegion = new Response();
            getRegionAndNameByMachineId(responseToRegion, integer);
            if (responseToRegion.getCode() != ResponseCode.OK) {
                response.setDescription("failed in getRegionByMachineId");
                return false;
            }
            String machineRegion = responseToRegion.getBody().get(0).toString();
            if (Regions.valueOf(machineRegion) == Regions.All)
                continue;
            regionByMachineId.put(integer, machineRegion);
        }

        // init all orders by region
        ordersByRegion.put(Regions.North.name(), new ArrayList<>());
        ordersByRegion.put(Regions.South.name(), new ArrayList<>());
        ordersByRegion.put(Regions.UAE.name(), new ArrayList<>());

        for(Order currentOrder : filteredByDateOrder) {
            Integer currentMachineId = Integer.parseInt(currentOrder.getMachineId());
            String region = regionByMachineId.get(currentMachineId);
            ordersByRegion.get(region).add(currentOrder);
        }
        // now we have the orders based on the region

        // ---- Generating Reports ----
        // Iterating over all regions and filling relevant data
        for (Map.Entry<String,List<Order>> orderOfRegion : ordersByRegion.entrySet()) {
            // map of all orders of clients that holds <key: clientId, value: amountOfOrders>
            Map<String, Integer> clientsOrders = new HashMap<>();
            // map of all orders of subscribers that holds <key: clientId, value: amountOfOrders>
            Map<String, Integer> subscribersOrders = new HashMap<>();

            // filling with actual values
            for (Order order : orderOfRegion.getValue()) {
                // checking of orderId starts with "0" or "1"
                if (order.getOrderId().startsWith("0")) {
                    String currentClientId = order.getCustomerId().toString();
                    if(!clientsOrders.containsKey(currentClientId))
                        clientsOrders.put(currentClientId, 0);
                    int newValue = clientsOrders.get(currentClientId) + 1;
                    clientsOrders.put(currentClientId, newValue);
                } else { // starts with "1"
                    String currentSubscriberId = order.getCustomerId().toString();
                    if(!subscribersOrders.containsKey(currentSubscriberId))
                        subscribersOrders.put(currentSubscriberId, 0);
                    int newValue = subscribersOrders.get(currentSubscriberId) + 1;
                    subscribersOrders.put(currentSubscriberId, newValue);
                }
            }

            UsersReport usersReport = new UsersReport(orderOfRegion.getKey(),
                    String.valueOf(iTime.getReportsMonth()),
                    String.valueOf(iTime.getReportsYear()),
                    clientsOrders,
                    subscribersOrders,
                    new ArrayList<>());

            // this map has for key: userId, and for value: amount of orders
            // map is organized from most to least orders
            Map<String, Integer> top3UserIdAndAmount = usersReport.getTop3UserIdAndAmount();
            List<String> top3Names = new LinkedList<>();
            for (String Id : top3UserIdAndAmount.keySet()) {
                Response userDataResponse = new Response();
                User currentUser = mysqlController.getUserDataById(userDataResponse, Integer.parseInt(Id));
                if (currentUser == null) {
                    isReportOfARegionFailed = true;
                    break;
                }
                top3Names.add(currentUser.getFirstName() + " " + currentUser.getLastName());
            }
            // if there is an error importing data of top3clients
            if (isReportOfARegionFailed)
                continue;

            usersReport.setTop3ClientNames(top3Names);

            Response responseForSaveReport = new Response();
            byte[] reportBytes;
            try {
                reportBytes = getSerializedObject(usersReport);
            } catch (Exception e) {
                responseForSaveReport.setDescription("There was an error in serializing UsersReport object");
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
                continue;
            }
            saveReportInDB(
                    responseForSaveReport, ReportType.USERS, Regions.valueOf(orderOfRegion.getKey()), reportBytes);
            if (responseForSaveReport.getCode() != ResponseCode.OK) {
                appendDescription(responseForSaveReport, response);
                isReportOfARegionFailed = true;
            }
        }

        return !isReportOfARegionFailed;
    }
}
