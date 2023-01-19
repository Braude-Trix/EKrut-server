package models;

import java.io.Serializable;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SavedReportRequest that = (SavedReportRequest) o;
        return year == that.year && month == that.month && reportType == that.reportType && region == that.region && Objects.equals(machineId, that.machineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(year, month, reportType, region, machineId);
    }
}
