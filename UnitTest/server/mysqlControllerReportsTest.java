package server;

import static logic.EndOfMonthTask.NEW_REPORTS_CREATED;
import static logic.EndOfMonthTask.REPORTS_ALREADY_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;

import models.InventoryReport;
import models.Regions;
import models.ReportType;
import models.SavedReportRequest;
import models.StatusInMachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gui.IServerGui;
import models.Response;
import models.ResponseCode;
import serverModels.ProductInMachineHistory;
import serverModels.ServerConf;

class mysqlControllerReportsTest {
	private static final ReportType REPORT_TYPE = ReportType.INVENTORY;
	private static final Regions REGION = Regions.North;
	private static ITime timesMock;
	private Connection connMock;
	private PreparedStatement stmtMock;
	private IServerGui serverGuiMock;
	private IReportsSql reportsSqlMock;

	private mysqlController mySql;
	private ServerConf conf;
	private Response res;
	
    private static final String EXECUTE_UPDATE_ERROR_MSG = "An error occurred when trying to executeUpdate in SQL, " +
            "please check your sql connection configuration in server panel";
    private static final String REPORT_FOUND_MSG = "Report data was fetched successfully";
    private static final String REPORT_NOT_FOUND_MSG = "Couldn't find report for the specified parameters";
    private static final String REPORT_ERROR_MSG = "There was an error while trying to fetch report data";
    private static final String ERROR_IN_CHECKING_MSG = "Failed while checking if reports are exists";
    private static final String ERROR_WHILE_INVENTORY_GEN_MSG = "There was an error while trying to generate all reports";

	private static final int YEAR = 2022;
    private static final int MONTH = 12;
	private static final int DAYS_IM_MONTH = 31;

	private static final String INVALID_MACHINE_ID = "222";
    private static final String REGION_OF_INV_MACHINE_ID = Regions.All.name();
    private static final String MACHINE_ID = "123";
    private static final String REGION_OF_MACHINE_ID = REGION.name();
    private static final String PRODUCT_ID = "999";
    private static final int AMOUNT = 5;
    private static final int DAY = 1;
    private static final String MACHINE_NAME = "EM Building braude";
    private static final String PRODUCT_NAME = "Bamba"; 
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_ABOVE =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_BELOW =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));
    private static final List<ProductInMachineHistory> FILTERED_BY_DATE_PRODUCTS_UNAVAILABLE =
    		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
    				new ProductInMachineHistory(INVALID_MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY));

    private InventoryReport getReportObject(boolean isBelow, boolean isUnavailable) {
    	List<Map<String, Integer>> dailyInventory = new ArrayList<>();
    	List<Integer> belowThresholdAmount = new ArrayList<>();
    	List<Integer> unavailableAmount = new ArrayList<>();
		int belowAmount = isBelow ? 1 : 0;
		int unAmount = isUnavailable ? 1 : 0;

		for (int i = 0; i < DAYS_IM_MONTH; i++) {
			dailyInventory.add(new HashMap<>()); // init hashmap
			dailyInventory.get(i).put(PRODUCT_NAME, AMOUNT);
			belowThresholdAmount.add(belowAmount);
			unavailableAmount.add(unAmount);
		}
    	return new InventoryReport(MACHINE_NAME, String.valueOf(MONTH), String.valueOf(YEAR),
				dailyInventory, belowThresholdAmount, unavailableAmount);
    }
    
    private InventoryReport getReportObject(boolean isBelowOnDay1, boolean isUnavailableOnDay1, 
    		boolean isBelowOnDay2, boolean isUnavailableOnDay2) {
    	List<Map<String, Integer>> dailyInventory = new ArrayList<>();
    	List<Integer> belowThresholdAmount = new ArrayList<>();
    	List<Integer> unavailableAmount = new ArrayList<>();
		int belowAmountOnDay1 = isBelowOnDay1 ? 1 : 0;
		int unAmountOnDay1 = isUnavailableOnDay1 ? 1 : 0;
		int belowAmountOnDay2 = isBelowOnDay2 ? 1 : 0;
		int unAmountOnDay2 = isUnavailableOnDay2 ? 1 : 0;

		for (int i = 0; i < DAYS_IM_MONTH; i++) {
			dailyInventory.add(new HashMap<>()); // init hashmap
			dailyInventory.get(i).put(PRODUCT_NAME, AMOUNT);
			if (i == 0) {
				belowThresholdAmount.add(belowAmountOnDay1);
				unavailableAmount.add(unAmountOnDay1);
			} else {
				belowThresholdAmount.add(belowAmountOnDay2);
				unavailableAmount.add(unAmountOnDay2);
			}
		}
    	return new InventoryReport(MACHINE_NAME, String.valueOf(MONTH), String.valueOf(YEAR),
				dailyInventory, belowThresholdAmount, unavailableAmount);
    }
    
	private void initMocksForSuccessfulCreate() {
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID))))
				.thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.transferDataFromProductInMachineToHistory(any())).thenReturn(Boolean.TRUE);
	}

	@SuppressWarnings("unchecked")
	private void setMockQueryAsError() throws SQLException {
		mySql.conn = connMock;
		when(connMock.prepareStatement(anyString())).thenReturn(stmtMock);
		when(stmtMock.executeQuery()).thenThrow(SQLException.class);
	}
	
	@BeforeAll
	static void beforeAll() {
		timesMock = mock(ITime.class);
		when(timesMock.getReportsYear()).thenReturn(YEAR);
		when(timesMock.getDaysInMonthOfReport()).thenReturn(DAYS_IM_MONTH);
		when(timesMock.getReportsMonth()).thenReturn(MONTH);
	}
   
	@BeforeEach
	void setUp() throws Exception {
		connMock = mock(Connection.class);
		stmtMock = mock(PreparedStatement.class);
		serverGuiMock = mock(IServerGui.class);
		reportsSqlMock = mock(IReportsSql.class);
		when(reportsSqlMock.generateOrdersReport(any())).thenReturn(true);
		when(reportsSqlMock.generateUsersReport(any())).thenReturn(true);
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(false);

		//Opening a mysql connection:
		conf = Server.getDefaultServerConf();
		mySql = new mysqlController(conf, serverGuiMock, reportsSqlMock, timesMock);
		res = new Response();
	}

	// Functionality: successiding in getting a report of a specific Request
	// input data: a request for a report (YEAR 2022, MONTH 12, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 2)
	// expected result: the Response isn't null and its code and description are valid
	@Test
	void getReportSuccessfully() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNotNull(res.getBody().get(0));
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), REPORT_FOUND_MSG);
	}
	
	// Functionality: failing in getting a report of a specific Request
	// input data: a request for a report (YEAR 1970, MONTH 1, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 1)
	// expected result: fail - we check in DB and wont find report with that date -> respons is INVALID_DATA 
	@Test
	void getReportNotFoundInvalidYear() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 12, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}


	@Test
	void getReportNotFoundInvalidMonth() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 13, REPORT_TYPE, REGION, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	@Test
	void getReportNotFoundInvalidType() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, null, REGION, 2);

		try {
			mySql.getReport(res, expectedRequestBody);
			fail();
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	@Test
	void getReportNotFoundInvalidRegion() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, null, 2);

		try {
			mySql.getReport(res, expectedRequestBody);
			fail();
		} catch (NullPointerException e) {
			assertTrue(true);
		}
	}

	@Test
	void getReportNotFoundRegionAll() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, Regions.All, 2);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	@Test
	void getReportNotFoundInvalidMachine() {
		SavedReportRequest expectedRequestBody = new SavedReportRequest(2022, 12, REPORT_TYPE, REGION, 99999);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.INVALID_DATA);
		assertEquals(res.getDescription(), REPORT_NOT_FOUND_MSG);
	}

	
	// Functionality: failing in getting a report of a specific Request, when we'll look
	// in DB with our mock we will throw an SQLException to get another failed response
	// input data: a request for a report (YEAR 1970, MONTH 1, 
	// TYPE ReportType.INVENTORY, REGION Regions.North, 1)
	// expected result: after using our mock we thorw SQLException, thats edit our response
	// to hold ResponseCode.DB_ERROR, REPORT_ERROR_MSG and null
	@Test
	void getReportThrowsSqlException() throws SQLException {
		setMockQueryAsError();
		SavedReportRequest expectedRequestBody = new SavedReportRequest(1970, 1, REPORT_TYPE, REGION, 1);

		mySql.getReport(res, expectedRequestBody);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), REPORT_ERROR_MSG);
		verify(serverGuiMock).setPrintToConsole(EXECUTE_UPDATE_ERROR_MSG, true);
	}

	@Test
	void generateAllReportsCheckIsCreatedError() throws SQLException {
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(null);

		mySql.generateAllReports(res);

		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_IN_CHECKING_MSG);
	}
	
	@Test
	void generateAllReportsCheckIsCreatedTrue() {
		when(reportsSqlMock.checkIfReportsAreAlreadyCreated()).thenReturn(true);

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), REPORTS_ALREADY_EXISTS);
	}

	@Test
	void getAllProductsHistoryFailed() {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}

	@Test
	void getNameByProductIdFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	@Test
	void getRegionAndNameByMachineIdFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(null);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	@Test
	void saveInventoryReportInDbFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.FALSE);

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	@Test
	void deleteLastMonthFromTableFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.FALSE);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	@Test
	void transferDataFailed() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		when(reportsSqlMock.getNameByProductId(any(), any())).thenReturn(PRODUCT_NAME);
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), any())).thenReturn(Arrays.asList(REGION_OF_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.getRegionAndNameByMachineId(any(), eq(Integer.valueOf(INVALID_MACHINE_ID)))).thenReturn(Arrays.asList(REGION_OF_INV_MACHINE_ID, MACHINE_NAME));
		when(reportsSqlMock.saveInventoryReportInDb(any(), any(), any(), any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.deleteLastMonthFromTable(any())).thenReturn(Boolean.TRUE);
		when(reportsSqlMock.transferDataFromProductInMachineToHistory(any())).thenReturn(Boolean.FALSE);
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.DB_ERROR);
		assertEquals(res.getDescription(), ERROR_WHILE_INVENTORY_GEN_MSG);
		verify(serverGuiMock, times(2)).setPrintToConsole(anyString(), eq(true));
	}
	
	@Test
	void successfulCreateInventoryReportOfAbove() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_ABOVE);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfBelow() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_BELOW);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfUnavailable() throws SQLException {
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(FILTERED_BY_DATE_PRODUCTS_UNAVAILABLE);
		initMocksForSuccessfulCreate();
		
		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	@Test
	void successfulCreateInventoryReportOfSameStatusBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> sameStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(sameStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	@Test
	void successfulCreateInventoryReportOfAboveToBelowBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false, true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfAboveToUnavailableBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(false, false, true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfBelowToUnavailableBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false, true, true)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfBelowToAboveBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, false, false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}

	@Test
	void successfulCreateInventoryReportOfUnavailableToBelowBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Below, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true, true, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
	
	@Test
	void successfulCreateInventoryReportOfUnavailableToAboveBetweenDays() throws SQLException {
	    List<ProductInMachineHistory> diffStatusBetweenDays =
		Arrays.asList(new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Not_Available, AMOUNT, MONTH, DAY),
				new ProductInMachineHistory(MACHINE_ID, PRODUCT_ID, StatusInMachine.Above, AMOUNT, MONTH, DAY+1));
		when(reportsSqlMock.getAllProductsHistory()).thenReturn(diffStatusBetweenDays);
		initMocksForSuccessfulCreate();

		mySql.generateAllReports(res);
		
		assertNull(res.getBody());
		assertEquals(res.getCode(), ResponseCode.OK);
		assertEquals(res.getDescription(), NEW_REPORTS_CREATED);
		verify(reportsSqlMock).saveInventoryReportInDb(any(), eq(getReportObject(true, true, false, false)), eq(REGION), eq(Integer.parseInt(MACHINE_ID)));
		verify(reportsSqlMock).deleteLastMonthFromTable(any());
		verify(reportsSqlMock).transferDataFromProductInMachineToHistory(any());
	}
}
