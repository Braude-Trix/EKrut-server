package server;

public interface ITime {
    int getDaysInMonthOfReport();
    boolean isDayInDate(int day, String dbDate);
    boolean isInCurrentMonth(String dbDate);
    int extractYear(String yearByFormat);
    int extractMonth(String monthByFormat);
    int extractDay(String dayByFormat);
    int getReportsYear();
    int getReportsMonth();
    int getCurrentMonth();
    int getCurrentDay();
}
