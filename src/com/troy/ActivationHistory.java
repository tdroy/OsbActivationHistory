package com.troy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.Set;
 
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;

import org.apache.commons.configuration.PropertiesConfiguration;

import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;
 
import com.bea.wli.config.Ref;
import com.bea.wli.config.mbeans.ConfigMBean;
import com.bea.wli.sb.management.configuration.ALSBConfigurationMBean;
import com.bea.wli.sb.management.configuration.SessionManagementMBean;

import dnl.utils.text.table.TextTable;

import com.bea.wli.config.task.ExecRecord;
 
public class ActivationHistory {
	
 static SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"); 
  
 static String debug = "OFF";
 static String hostname = "localhost";
 static int port = 7001;
 static String username = "weblogic";
 static String password = "Welcome1";
 static String startDate = null;
 static String endDate = null;
 
 static String[] hostnameArray = {"localhost"};
 static int[] portArray = {7001};
 static String[] usernameArray = {"weblogic"};
 static String[] passwordArray = {"Welcome1"};
 static String[] startDateArray = null;
 static String[] endDateArray = null;
 
 static String smtpUser = null;
 static String smtpPassword = null;
 static String smtpHost = null;
 static String smtpPort = null;
 static String smtpAuth = null;
 static String smtpTls = null;
 static String smtpFromAddress = null;
 static String smtpToAddress = null;
 static String smtpCcAddress = null;
 static String emailSubject = null;
 static String emailBody = "<html> <body>";
 

 
 public static void main(String[] args) throws Exception {
	  System.out.println("*****************************************************");
	  System.out.println("****          Troy Activation History            ****");
	  System.out.println("****  Host : Host IP                             ****");
	  System.out.println("****  Port : Port number                         ****");
	  System.out.println("****  User : WLS user                            ****");
	  System.out.println("****  Password : user password                   ****");
	  System.out.println("****  Start Time : dd-MM-yyyy HH:mm:ss (24Hrs)   ****");
	  System.out.println("****  End Time : dd-MM-yyyy HH:mm:ss (24Hrs)     ****");
	  System.out.println("****  If start/end time empty, then default is   ****");
	  System.out.println("****  Conside and 30days old and today.          ****");
	  System.out.println("*****************************************************");
	  
	  ActivationHistory activityHistory = new ActivationHistory();
	  activityHistory.loadProp();
	  
	  for (int i =0 ; i < hostnameArray.length ; i++)
		  {
		  	hostname = hostnameArray[i];
		  	port = portArray[i];
		  	username = usernameArray[i];
		  	password = passwordArray[i];
		  	startDate = startDateArray[i];
		  	endDate = endDateArray[i];
		  	
			//Check start and end Date if null then set start date as month previous and end date today.
			if (startDate.isEmpty())
			  { 
				    System.out.println(dateFormat.format(new Date()) + " No start date provided, setting a month ago.");
					Calendar calStartDate = Calendar.getInstance();
					calStartDate.add(Calendar.MONTH, -1);
					startDate = dateFormat.format(calStartDate.getTime()); 
			  }
			  
			if (endDate.isEmpty())
			  {
					Calendar calEndDate = Calendar.getInstance();
					endDate = dateFormat.format(calEndDate.getTime());
			  }
			
		  	activityHistory.getOsbActivationData();
		  }
	  	  
	  if (emailSubject.isEmpty())
		  emailSubject = "Troy CR Data "; 
	  
	  //Email subject append with month.
	  emailSubject += " " + new SimpleDateFormat("MMM").format(Calendar.getInstance().getTime()) + " " + new SimpleDateFormat("yyyy").format(Calendar.getInstance().getTime());
	  emailBody += "<br> <b>FMW OPS Team </b></body> </html>"; 

      EmailUtil email = new EmailUtil();
      email.sendEmail(smtpUser , smtpPassword , smtpHost , smtpPort , smtpAuth , smtpTls , smtpFromAddress , smtpToAddress , emailSubject, emailBody);	

 }
 
 public void loadProp()
 {
	 try
	 { 
		 Properties prop = new Properties();
		 PropertiesConfiguration config = new PropertiesConfiguration("config.properties");
		 InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("config.properties");
		 prop.load(inputStream);
		 
/*		 hostname = prop.getProperty("hostname");
		 port = Integer.parseInt(prop.getProperty("port")); 
		 username = prop.getProperty("username");
		 password = prop.getProperty("password");
		 startDate = prop.getProperty("startDate");
		 endDate = prop.getProperty("endDate");*/
		 
		 hostnameArray = config.getStringArray("hostname");
		 portArray = Arrays.stream(config.getStringArray("port")).mapToInt(Integer::parseInt).toArray();
		 usernameArray = config.getStringArray("username");
		 passwordArray = config.getStringArray("password");
		 startDateArray = config.getStringArray("startDate");
		 endDateArray = config.getStringArray("endDate");
		 
		 debug = prop.getProperty("debug");
		 smtpUser = prop.getProperty("smtpUser");
		 smtpPassword = prop.getProperty("smtpPassword");
		 smtpHost = prop.getProperty("smtpHost");
		 smtpPort = prop.getProperty("smtpPort");
		 smtpAuth = prop.getProperty("smtpAuth");
		 smtpTls = prop.getProperty("smtpTls");
		 smtpFromAddress = prop.getProperty("smtpFromAddress");
		 smtpToAddress = prop.getProperty("smtpToAddress");
		 smtpCcAddress = prop.getProperty("smtpCcAddress");
		 emailSubject = prop.getProperty("emailSubject");
		 
	 }
	 catch(Exception e)
	 {
		 System.out.println(dateFormat.format(new Date()) + "config.properties file not found in classpath or invalid configuration.");
		 e.printStackTrace();
	 }
	 
 }
 
 // This is JMX method -=-=-=-=-
 public void getOsbActivationData() 
	{
	 	JMXConnector conn = null;
	 	String sessionName = null;
	 	SessionManagementMBean sm = null;
		try 
		{
		  System.out.println(dateFormat.format(new Date()) + " Connecting to " + hostname + ":" + port + " ...");  
		  JMXServiceURL serviceURL = new JMXServiceURL("t3", hostname, port, "/jndi/" + DomainRuntimeServiceMBean.MBEANSERVER_JNDI_NAME);
	 
		  Hashtable<String, String> h = new Hashtable<String, String>();
		 
		  h.put(Context.SECURITY_PRINCIPAL, username);
		  h.put(Context.SECURITY_CREDENTIALS, password);
		  h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
		 
		  conn = JMXConnectorFactory.connect(serviceURL, h);

 	      System.out.println(dateFormat.format(new Date()) + " JMX connection established to " + hostname + ":" + port + " as " + username);
		 
		   // get mbean connection
		  MBeanServerConnection mbconn = conn.getMBeanServerConnection(); 
		 
		   // Get SessionmanagementMBean
		  sm = JMX.newMBeanProxy(mbconn, ObjectName.getInstance(SessionManagementMBean.OBJECT_NAME), SessionManagementMBean.class);
		 
		   // Create a session
		  sessionName = "ServiceBus";           
		  sm.createSession(sessionName);
		 
		  // Get the configuration MBean for the session, do stuff, and then discard the session.
		  ConfigMBean configMBean = 
		     JMX.newMBeanProxy(
		       mbconn, 
		       ObjectName.getInstance("com.bea.wli.config:Name=" + ConfigMBean.NAME + "." + sessionName + ",Type=" + ConfigMBean.TYPE), 
		       ConfigMBean.class
		     );
		
		  List<ExecRecord> recordList = configMBean.getExecRecords(dateFormat.parse(startDate), dateFormat.parse(endDate), null, null, null, null);
		  Iterator itr = recordList.iterator();
		    
		  String[] columnNames = {"ACTIVATION","DESCRIPTION", "EXECUTION TIME", "USER", "STATUS"};
		  String[][] row = new String[recordList.size()][5];
		  int i = 0;
		    
		  while(itr.hasNext()) {
		    	ExecRecord record = (ExecRecord) itr.next(); 
		    	//row[i][0] = "{" + record.getDescription() + "," + record.getDescription().getUserDescription() + "," + dateFormate.format(record.getExecTime()) + "," + record.getUser() + "," + record.getStatus() + "}";
		    	row[i][0] = record.getDescription().toString();
		    	row[i][1] = record.getDescription().getUserDescription();
		    	row[i][2] = dateFormat.format(record.getExecTime());
		    	row[i][3] = record.getUser();
		    	row[i][4] = record.getStatus().name();
		    	i++;
		    }
		  
		  Object[][] data = row;
	    
	      TextTable tt = new TextTable(columnNames, data);
	      // this adds the numbering on the left
	      //tt.setAddRowNumbering(true);
	      // sort by the first column
	      tt.setSort(2);
	      tt.printTable();    
	      
	      emailBody += "<br> <b> Domain name : " + mbconn.getDefaultDomain() + " (" + startDate + " to " + endDate + ")" + " </b> <br> ";
	
	      //Adding table headers.
	      emailBody += "<table style='border:2px solid black'> <tr>";
	      for (int j =0 ; j< columnNames.length ; j++)
			{
				emailBody += "<th>" + columnNames[j] + "</th>";
			}
	      emailBody += "</tr>";
	    
	      for (int j =0 ; j < row.length ; j++)
			{
	    	    if (j % 2 == 0)
	    	    	emailBody += "<tr bgcolor=#eee>";
	    	    else 
	    	    	emailBody += "<tr>";
				//Adding row data
				for (int k = 0 ; k < columnNames.length; k++)
					{
							emailBody += "<td>" + row[j][k] + "</td>";
					}
				emailBody += "</tr>";
			}
			
		  emailBody += "</table>";    
	 }
		catch(Exception e)
		{
			System.out.println(dateFormat.format(new Date()) + " Error while JMX connection. Domain AdminServer may not running or invalid configuration.");
			if (debug.equalsIgnoreCase("ON"))
				e.printStackTrace();
		}
		finally {
			try
			{
				// use activateSession to commit session changes instead
				sm.discardSession(sessionName);
				conn.close();
				System.out.println(dateFormat.format(new Date()) + " Closed JMX connection.");
			} catch(Exception e)
			{
				System.out.println(dateFormat.format(new Date()) + " Error closing connection.");
				if (debug.equalsIgnoreCase("ON"))
					e.printStackTrace();
			}
		}

	}
 
}

