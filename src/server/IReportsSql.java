package server;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.Response;
import serverModels.ProductInMachineHistory;

import java.sql.Connection;
import java.util.List;

public interface IReportsSql {
    Boolean checkIfReportsAreAlreadyCreated(); // V
    List<ProductInMachineHistory> getAllProductsHistory(); // V
    void getNameByProductId(Response response, Integer productId); // V
    void getRegionAndNameByMachineId(Response response, Integer machineId); // V
    boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, int machineId); // V
    void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes); // V
    void deleteLastMonthFromTable(Response response); // V
    void transferDataFromProductInMachineToHistory(Response response); // V
}
