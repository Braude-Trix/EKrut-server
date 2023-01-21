package server;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.Response;
import serverModels.ProductInMachineHistory;

import java.sql.Connection;
import java.util.List;

public interface IReportsSql {
    Boolean checkIfReportsAreAlreadyCreated();

    List<ProductInMachineHistory> getAllProductsHistory();

    String getNameByProductId(Response response, Integer productId);

    List<String> getRegionAndNameByMachineId(Response response, Integer machineId);

    Boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, Integer machineId);

    void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes);

    Boolean deleteLastMonthFromTable(Response response);

    Boolean transferDataFromProductInMachineToHistory(Response response);

    boolean generateOrdersReport(Response response);

    boolean generateUsersReport(Response response);
}
