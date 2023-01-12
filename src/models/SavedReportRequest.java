package models;

import java.io.Serializable;

public class SavedReportRequest implements Serializable {
    private final int year;
    private final int month;
    private final ReportType reportType;
    private final Regions region;
    private final Integer machineId;

    public SavedReportRequest(int year, int month, ReportType reportType, Regions region, Integer machineId) {
        this.year = year;
        this.month = month;
        this.reportType = reportType;
        this.region = region;
        this.machineId = machineId;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public ReportType getReportType() {
        return reportType;
    }

    public Regions getRegion() {
        return region;
    }

    public Integer getMachineId() {
        return machineId;
    }
}
