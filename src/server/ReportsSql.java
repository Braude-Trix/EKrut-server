package server;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.Response;
import serverModels.ProductInMachineHistory;

import java.util.List;

public class ReportsSql implements IReportsSql {
    @Override
    public Boolean checkIfReportsAreAlreadyCreated() {
        return null;
    }

    @Override
    public List<ProductInMachineHistory> getAllProductsHistory() {
        return null;
    }

    @Override
    public void getNameByProductId(Response response, Integer productId) {

    }

    @Override
    public void getRegionAndNameByMachineId(Response response, Integer machineId) {

    }

    @Override
    public boolean saveInventoryReportInDb(Response response, InventoryReport inventoryReport, Regions region, int machineId) {
        return false;
    }

    @Override
    public void saveReportInDB(Response response, ReportType type, Regions region, int machineId, byte[] blobBytes) {

    }

    @Override
    public void deleteLastMonthFromTable(Response response) {

    }

    @Override
    public void transferDataFromProductInMachineToHistory(Response response) {

    }
}
