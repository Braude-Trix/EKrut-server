package server;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.Response;
import serverModels.ProductInMachineHistory;

import java.util.List;

public interface IReportsSql {
    Boolean checkIfReportsAreAlreadyCreated();
    List<ProductInMachineHistory> getAllProductsHistory();
    void getNameByProductId(Response response, Integer productId);
    void getRegionAndNameByMachineId(Response response, Integer machineId);
    boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, int machineId);
    void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes);
    void deleteLastMonthFromTable(Response response);
    void transferDataFromProductInMachineToHistory(Response response);
}
