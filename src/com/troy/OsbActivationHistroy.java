package com.troy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
 
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.Context;
 
import weblogic.management.mbeanservers.domainruntime.DomainRuntimeServiceMBean;
 
import com.bea.wli.config.Ref;
import com.bea.wli.config.mbeans.ConfigMBean;
import com.bea.wli.sb.management.configuration.ALSBConfigurationMBean;
import com.bea.wli.sb.management.configuration.SessionManagementMBean;

import dnl.utils.text.table.TextTable;

import com.bea.wli.config.task.ExecRecord;
 
public class OsbActivationHistroy {
  
 static String hostname = "localhost";
 static int port = 7001;
 static String username = "weblogic";
 static String password = "Welcome1";
 static String startDate = null;
 static String endDate = null;
 
 public static void main(String[] args) throws Exception {
	  System.out.println("*****************************************************");
	  System.out.println("****  Troy Activation History                    ****");
	  System.out.println("****  Host : Host IP                             ****");
	  System.out.println("****  Port : Port number                         ****");
	  System.out.println("****  User : WLS user                            ****");
	  System.out.println("****  Password : user password                   ****");
	  System.out.println("****  Start Time : dd-MM-yyyy HH:mm:ss (24Hrs)   ****");
	  System.out.println("****  End Time : dd-MM-yyyy HH:mm:ss (24Hrs)   ******");
	  System.out.println("*****************************************************");
	  
	  SimpleDateFormat dateFormate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	  
	  BufferedReader keyRead = new BufferedReader(new InputStreamReader(System.in));
	  
	  System.out.println("Enter Host : ");
	  hostname = keyRead.readLine();
	  
	  System.out.println("Enter Port : ");
	  port = Integer.parseInt(keyRead.readLine());
	  
	  System.out.println("Enter User : ");
	  username = keyRead.readLine();
	  
	  System.out.println("Enter Password : ");
	  password = keyRead.readLine();
	  
	  System.out.println("Enter Start Date (dd-MM-yyyy HH:mm:ss): ");
	  startDate = keyRead.readLine();
	  
	  System.out.println("Enter End Date (dd-MM-yyyy HH:mm:ss): ");
	  endDate = keyRead.readLine();
	  
	  //Check start and end Date if null then set start date as month previous and end date today.
	  if (startDate.isEmpty())
	  { 
		    System.out.println("No start date provided, setting a month ago.");
			Calendar calStartDate = Calendar.getInstance();
			calStartDate.add(Calendar.MONTH, -1);
			startDate = dateFormate.format(calStartDate.getTime());
	  }
	  
	  if (endDate.isEmpty())
	  {
			Calendar calEndDate = Calendar.getInstance();
			endDate = dateFormate.format(calEndDate.getTime());
	  }
	  
	  OsbActivationHistroy listProjects = new OsbActivationHistroy();
	  listProjects.display();
 }
 
 public void display() throws Exception {
	 
  SimpleDateFormat dateFormate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");  
 
  JMXServiceURL serviceURL = new JMXServiceURL("t3", hostname, port, "/jndi/" + DomainRuntimeServiceMBean.MBEANSERVER_JNDI_NAME);
 
  Hashtable<String, String> h = new Hashtable<String, String>();
 
  h.put(Context.SECURITY_PRINCIPAL, username);
  h.put(Context.SECURITY_CREDENTIALS, password);
  h.put(JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES, "weblogic.management.remote");
 
  JMXConnector conn = JMXConnectorFactory.connect(serviceURL, h);
  System.out.println("\nconnected");   
 
  try {
   System.out.println("Opened JMX connection to " + hostname + ":" + port + " as " + username);
 
   // get mbean connection
   MBeanServerConnection mbconn = conn.getMBeanServerConnection(); 
 
   // Get SessionmanagementMBean
   SessionManagementMBean sm = 
    JMX.newMBeanProxy(mbconn, ObjectName.getInstance(SessionManagementMBean.OBJECT_NAME), SessionManagementMBean.class);
 
   // Create a session
   String sessionName = "ServiceBus";           
   sm.createSession(sessionName);
 
   // Get the configuration MBean for the session, do stuff, and then discard the session.
   try
   {
    //System.out.println("Session exists? : " + sm.sessionExists(sessionName));
 
    ConfigMBean configMBean = 
     JMX.newMBeanProxy(
       mbconn, 
       ObjectName.getInstance("com.bea.wli.config:Name=" + ConfigMBean.NAME + "." + sessionName + ",Type=" + ConfigMBean.TYPE), 
       ConfigMBean.class
     );

    List<ExecRecord> recordList = configMBean.getExecRecords(dateFormate.parse(startDate), dateFormate.parse(endDate), null, null, null, null);
    Iterator itr = recordList.iterator();
    
    String[] columnNames = {"ACTIVATION","DESCRIPTION", "EXECUTION TIME", "USER", "STATUS"};
    String[][] row = new String[recordList.size()][5];
    int i = 0;
    
    while(itr.hasNext()) {
    	ExecRecord record = (ExecRecord) itr.next(); 
    	//row[i][0] = "{" + record.getDescription() + "," + record.getDescription().getUserDescription() + "," + dateFormate.format(record.getExecTime()) + "," + record.getUser() + "," + record.getStatus() + "}";
    	row[i][0] = record.getDescription().toString();
    	row[i][1] = record.getDescription().getUserDescription();
    	row[i][2] = dateFormate.format(record.getExecTime());
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
    
    String emailSubject = "CR Data domain : " + mbconn.getDefaultDomain() + " (<" + startDate + "> to <" + endDate + ">)"; 
    String emailBody = "<html> <body> <table border = '1'>";
    
    //Adding table headers.
    emailBody += "<tr>";
    for (int j =0 ; j< columnNames.length ; j++)
    {
    	emailBody += "<th>" + columnNames[j] + "</th>";
    }
    emailBody += "</tr>";
    
    for (int j =0 ; j < row.length ; j++)
    {
    	emailBody += "<tr>";
    	//Adding row data
    	for (int k = 0 ; k < columnNames.length; k++)
    	{
    		emailBody += "<th>" + row[j][k] + "</th>";
    	}
    	emailBody += "</tr>";
    }
    emailBody += "</table> </body> </html>";
    
    EmailUtil email = new EmailUtil();
    email.sendEmail("troy.avaya@gmail.com", "Avaya_buddy1", "smtp.gmail.com", "587", "true", "true", "troy.avaya@gmail.com", "tanmoy.duttaroy@gmail.com", emailSubject, emailBody);

   }
   finally
   {
    // use activateSession to commit session changes instead
    sm.discardSession(sessionName);
   }
  } finally {
   conn.close();
   System.out.println("\nClosed JMX connection");
  }      
 }
}
