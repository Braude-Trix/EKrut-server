package server;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ReportsSqlTest.class, 
	mysqlControllerReportsTest.class})
public class ReportsTestSuit {

}
