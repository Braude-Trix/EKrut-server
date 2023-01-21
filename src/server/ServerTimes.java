package server;

import java.time.Year;
import java.time.YearMonth;
import java.util.Calendar;

public class ServerTimes implements ITime {
    @Override
    public int getDaysInMonthOfReport() {
        // Get the number of days in that month
        YearMonth yearMonthObject = YearMonth.of(getReportsYear(), getReportsMonth());
        return yearMonthObject.lengthOfMonth();
    }

    /**
     * @return true if day of dbDate is equal to specified day
     */
    @Override
    public boolean isDayInDate(int day, String dbDate) {
        return day == extractDay(dbDate);
    }

    @Override
    public boolean isInCurrentMonth(String dbDate) {
        return getReportsYear() == extractYear(dbDate) && getReportsMonth() == extractMonth(dbDate);
    }

    @Override
    public int extractYear(String yearByFormat) {
        // according to the format utils.StyleConstants.DATE_FORMAT
        return Integer.parseInt(yearByFormat.split("-")[2]);
    }

    @Override
    public int extractMonth(String monthByFormat) {
        // according to the format utils.StyleConstants.DATE_FORMAT
        String month = monthByFormat.split("-")[1];
        if (month.startsWith("0"))
            month = month.substring(1);
        return Integer.parseInt(month);
    }

    @Override
    public int extractDay(String dayByFormat) {
        // according to the format utils.StyleConstants.DATE_FORMAT
        String month = dayByFormat.split("-")[0];
        if (month.startsWith("0"))
            month = month.substring(1);
        return Integer.parseInt(month);
    }

    /**
     * @return The year of report to be created
     */
    @Override
    public int getReportsYear() {
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
    @Override
    public int getReportsMonth() {
        int currentMonth = getCurrentMonth();

        if (currentMonth == 1) {
            return 12;
        }
        return currentMonth - 1;
    }

    @Override
    public int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    @Override
    public int getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    }
}
