package server;

import models.InventoryReport;
import models.ProductInMachine;
import models.Regions;
import models.ReportType;
import models.Response;
import models.ResponseCode;
import models.StatusInMachine;
import serverModels.ProductInMachineHistory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import gui.ServerGui;

public class ReportsSql implements IReportsSql {
	public static Connection conn = mysqlController.conn;
	
	private final static String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
	
	
    @Override
    public Boolean checkIfReportsAreAlreadyCreated() {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM saved_reports WHERE year_creation = ? AND month_creation = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, getReportsYear());
            stmt.setInt(2, getReportsMonth());
            rs = stmt.executeQuery();
            boolean hasReport = rs.next();
            rs.close();
            return hasReport;
        } catch (SQLException e) {
            return null;
        }
    }
    
    /**
     * @return The year of report to be created
     */
    private int getReportsYear() {
        int currentMonth = getCurrentMonth();
        int currentYear = Year.now().getValue();

        if (currentMonth == 1) {
            return currentYear - 1;
        }
        return currentYear;
    }

    /**
     * @return The month of report to be created
     */
    private int getReportsMonth() {
        int currentMonth = getCurrentMonth();

        if (currentMonth == 1) {
            return 12;
        }
        return currentMonth - 1;
    }
    
    private int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    private int getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }

    @Override
    public List<ProductInMachineHistory> getAllProductsHistory() {
        PreparedStatement stmt;
        ResultSet rs;
        List<ProductInMachineHistory> productInMachineHistories = new ArrayList<>();

        String query = "SELECT * FROM product_in_machine_history WHERE updated_month = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, getReportsMonth());
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
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
            return null;
        }
        return productInMachineHistories;
    }

    @Override
    public void getNameByProductId(Response response, Integer productId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM products WHERE productId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("productName"));
            }
            mysqlController.editResponse(response, ResponseCode.OK, "Successfully get name by product id", res);
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR,
                    "There was an error while searching for name of product", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    @Override
    public void getRegionAndNameByMachineId(Response response, Integer machineId) {
        List<Object> res = new ArrayList<>();
        ResultSet rs;
        PreparedStatement stmt;
        String query = "SELECT * FROM machine WHERE machineId = ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, machineId);
            rs = stmt.executeQuery();
            if(rs.next()) {
                res.add(rs.getString("region"));
                res.add(rs.getString("machineName"));
            }
            mysqlController.editResponse(response, ResponseCode.OK, "Successfully get region by machine id", res);
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR, "Error loading data (DB)", null);
            e.printStackTrace();
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
    }

    @Override
    public boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, int machineId) {
    	byte[] inventoryBytes;
        Response responseForSaveReport = new Response();
        try {
            inventoryBytes = mysqlController.getSerializedObject(inventoryReport);
        } catch (Exception e) {
            responseForSaveReport.setDescription("There was an error in serializing InventoryReport object");
            mysqlController.appendDescription(responseForSaveReport, response);
            return false;
        }
        saveReportInDB(responseForSaveReport, ReportType.INVENTORY,
                region, machineId, inventoryBytes);
        if (responseForSaveReport.getCode() != ResponseCode.OK) {
        	mysqlController.appendDescription(responseForSaveReport, response);
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
             stmt = conn.prepareStatement(query);
             stmt.setInt(1, getReportsYear());
             stmt.setInt(2, getReportsMonth());
             stmt.setString(3, type.name());
             stmt.setString(4, region.name());
             stmt.setInt(5, machineId);
             stmt.setBytes(6, blobBytes);

             stmt.executeUpdate();
             String msg = String.format("Successfully saved report %s of region %s", type.name(), region.name());
             if (machineId != -1)
                 msg = String.format("%s of machine %s", msg, machineId);
             mysqlController.editResponse(response, ResponseCode.OK, msg, null);
             ServerGui.serverGui.printToConsole(msg);
         } catch (SQLException e) {
             String msg = String.format("There was an error in saving report %s of region %s", type.name(), region.name());
             if (machineId != -1)
                 msg = String.format("%s of machine %s", msg, machineId);
             mysqlController.editResponse(response, ResponseCode.DB_ERROR, msg, null);
             ServerGui.serverGui.printToConsole(msg);
             e.printStackTrace();
             ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
         }
    }

    @Override
    public void deleteLastMonthFromTable(Response response) {
        int currentMonth = getCurrentMonth();
        PreparedStatement stmt;
        String query = "DELETE FROM product_in_machine_history WHERE updated_month != ?";
        try {
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentMonth);
            stmt.executeUpdate();
        } catch (SQLException e) {
        	mysqlController.editResponse(response, ResponseCode.DB_ERROR,
                    "[Report] Failed to delete previous month's inventory data", null);
            System.out.println(e.getMessage());
            ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }

        mysqlController.editResponse(response, ResponseCode.OK,
                "[Report] Successfully deleted previous month's inventory data", null);
    }

    @Override
    public void transferDataFromProductInMachineToHistory(Response response) {
        PreparedStatement stmt;
        ResultSet rs;

        String query = "SELECT * FROM productinmachine";
        try {
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
                ProductInMachine productInMachine = new ProductInMachine(
                        Integer.toString(rs.getInt("machineId")),
                        Integer.toString(rs.getInt("productId")),
                        StatusInMachine.valueOf(rs.getString("statusInMachine")),
                        rs.getInt("amountInMachine"));
                mysqlController.insertProductInMachineToHistory(response, productInMachine);
                if (response.getCode() != ResponseCode.OK)
                    break;
            }
            rs.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            if (response.getCode() != ResponseCode.OK)
                ServerGui.serverGui.printToConsole(response.getDescription(), true);
            else
                ServerGui.serverGui.printToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
        }
        mysqlController.editResponse(response, ResponseCode.OK, "[Report] Successfully inserting all of current month's inventory data to table", null);
    }
}
