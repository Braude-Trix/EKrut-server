package logic;

import gui.ServerGui;
import models.Response;
import models.ResponseCode;
import server.Server;

import java.util.Calendar;
import java.util.Objects;


/**
 * A class that contains a thread that runs every hour 
 * and checks if the month is over - produces new reports if so
 *
 */
public class EndOfMonthTask implements Runnable {
    public static final String REPORTS_ALREADY_EXISTS = "Reports are already exists";
    public static final String NEW_REPORTS_CREATED = "Generating All Reports has done successfully";
    private int lastCollectedMonth = -1;
    private final int HOUR_IN_MILLIS = 60 * 60 * 1000;

    /**
     * Creating all reports on the end of each month
     * checks if last month is current month, if true, thread sleeps for 1 hour.
     * else trying to create monthly reports
     * This task is executed every 1 hour until the thread is interrupted.
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Calendar calendar = Calendar.getInstance();
                int currentMonth = calendar.get(Calendar.MONTH) + 1;
                if (lastCollectedMonth != currentMonth) {
                    ServerGui.serverGui.printToConsole("[Reports] Month is over, start creating reports");
                    boolean created_successfully = createAllReports();
                    if (created_successfully)
                        lastCollectedMonth = currentMonth;
                    // else => will retry next hour
                } else {
                    ServerGui.serverGui.printToConsole("[Reports] Didn't pass a month, doesn't creating reports");
                }
                Thread.sleep(HOUR_IN_MILLIS);
            } catch (InterruptedException e) {
                ServerGui.serverGui.printToConsole(e.getMessage(), true);
                System.out.println(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean createAllReports() {
        Response response = new Response();
        try {
            Server.server_instance.mysqlController.generateAllReports(response);
        } catch (Exception e) {
            ServerGui.serverGui.printToConsole(e.getMessage(), true);
            ServerGui.serverGui.printToConsole("[Reports] Will retry in the next hour", true);
            return false;
        }
        if (response.getCode() == ResponseCode.OK) {
            if (Objects.equals(response.getDescription(), REPORTS_ALREADY_EXISTS) ||
                    Objects.equals(response.getDescription(), NEW_REPORTS_CREATED)) {
                ServerGui.serverGui.printToConsole("[Reports] " + response.getDescription());
            }
            return true;
        } else {
            String msg = "[Reports] Some error occurred while trying to create reports, please try again manually";
            ServerGui.serverGui.printToConsole(msg, true);
            ServerGui.serverGui.printToConsole(response.getDescription(), true);
            return false;
        }
    }
}
