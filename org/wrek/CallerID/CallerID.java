/*
 * Created on Feb 2, 2005
 * @author Jim Evans
 * @version $Id: CallerID.java 31 2008-09-26 03:23:27Z root $
 *
 * Program to monitor the caller id data from a WhozzCalling box.
 * Writes data to a log file. Writes to a MySQL database.
 * Broadcasts to CallScreening client programs.
 */

package org.wrek.CallerID;
import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Main class for CallerID monitor program
 */
public class CallerID implements Runnable {
    /**
     * Flag to request shutdown of the program. 
     * May also require calls to notifyAll() or interrupt() to get it noticed.
     */
    public volatile static boolean shutdown = false;
    public static CallerID app = null;
    public static Properties properties; 
    public static long idleTimeout = 5 * 60 * 1000;

    private static int errorRate = -1;
    private static int errorCounter;

    private static void printUsage() {
        System.out.println("usage: java CallerID [options]");
        System.out.println("---");
        System.out.println("Options:");
        System.out.println("   -s {SOURCE}         Sets the caller id SOURCE (default is "
                + properties.getProperty("callSource") + ")");
        System.out.println("   -l {LOGFILE}        Sets the LOGFILE location (default is "
                + properties.getProperty("logFile") + ")");
        System.out.println("   -d {DATABASE}       Sets the DATABASE connection string (default is "
                + properties.getProperty("database") + ")");
        System.out.println("   -e {ERRORRRATE}     Sets the error rate (default is "
                + properties.getProperty("errorRate") + ")");
        System.out.println("   -c {CLIENT}         Sets the client protocol/port (default is "
                + properties.getProperty("client") + ")");
        System.out.println("   -t {TIMEOUT}        Sets the idle status timeout (default is "
                + properties.getProperty("timeout") + ")");
        System.out.println("\n");
        System.out.println("System is running: "
                + System.getProperty("os.name"));
        File f = new File("CallerID.properties");
        if (f.exists())
            System.out.println("Default properties loaded from CallerID.properties");
    }
    
    private static void parseArgs(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-s")) {
                properties.setProperty("callSource", args[++i]);
            } else if (args[i].equals("-l")) {
                properties.setProperty("logFile", args[++i]);
            } else if (args[i].equals("-d")) {
                properties.setProperty("database", args[++i]);
            } else if (args[i].equals("-e")) {
                properties.setProperty("errorRate", args[++i]);
            } else if (args[i].equals("-c")) {
                properties.setProperty("client", args[++i]);
            } else if (args[i].equals("-t")) {
                properties.setProperty("timeout", args[++i]);
            }
        }
    }
    
    private static void loadProperties() {
        Properties defProps = new Properties();
        defProps.setProperty("callSource", "tcp://130.207.137.73:3003");
        defProps.setProperty("database", "jdbc:mysql://localhost/callerid");
        defProps.setProperty("logFile", "'WC'yyMM'.log'");
        defProps.setProperty("errorRate", "-1");
        defProps.setProperty("client", "udp://255.255.255.255:9966");
        defProps.setProperty("timeout", "5:00");
        properties = new Properties(defProps);
        try {
            InputStream is = new FileInputStream("CallerID.properties");
            properties.load(is);
            is.close();
        } catch (IOException e) {
        }
    }
    
    public static void main(String[] args) {
        loadProperties();

        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("-help"))) {
            printUsage();
            return;
        }
        parseArgs(args);

        logFile = properties.getProperty("logFile");
        if (logFile.indexOf('\'') >= 0)
            sdfLogName = new SimpleDateFormat(logFile);

        errorRate = Integer.parseInt(properties.getProperty("errorRate"));
        try {
			SimpleDateFormat sdf = new SimpleDateFormat("m:s");
			SimpleTimeZone tz = new SimpleTimeZone(0, "GMT");
			tz.setRawOffset(0);
			sdf.setTimeZone(tz);
			idleTimeout = sdf.parse(properties.getProperty("timeout")).getTime();
		} catch (Exception e) {
			logMessage(e);
		}
        
        StringBuffer sb = new StringBuffer("CallerID:");
        for (int i = 0; i < args.length; i++) {
            sb.append(" " + args[i]);
        }
        logMessage(sb.toString());

        for (Enumeration e = properties.propertyNames(); e.hasMoreElements(); ) {
            String name = e.nextElement().toString();
            logMessage("  " + name + "=" + properties.getProperty(name)); 
        }

        app = new CallerID();
        app.run();
    }

    private static String logFile;
    /**
     * Queue of messages from WhozzCalling box or CallScreening client
     * Comment for <code>msgQueue</code>
     */
    public Queue msgQueue;

    public long msReference = 0;
    public long currentTimeMillis() {
        if (msReference != 0)
            return msReference;
        return System.currentTimeMillis();
    }

    private static Thread mainThread;
    public static void doShutdown() {
        shutdown = true;
        mainThread.interrupt();
    }
    
    public synchronized void run() {
        logMessage("CallerID startup.");
        
        mainThread = Thread.currentThread();
        errorCounter = errorRate;
        msgQueue = new Queue();
        LineMsg lm = new LineMsg();
        LineStatus ls = new LineStatus(4);
        Client cl = new Client(properties.getProperty("client"));
        cl.open();
        cl.start();
        lm.start();
        msgQueue.addLast(new Refresh());

        while (!shutdown) {
            try {
                Object msg = msgQueue.removeFirst();
                //System.out.println("Msg: " + msg.toString());
                if (msg instanceof WhozzCalling.Message) {
                    ls.processMessage((WhozzCalling.Message)msg);
                } else if (msg instanceof Refresh) {
                	ls.refresh((Refresh)msg);
//                } else if (msg instanceof CallScreen) {
                	//TODO Process CallScreen messages from client.                	
                }
            } catch (InterruptedException e) {
                break;
            }
            ls.sendUpdates(cl);
        }
        logMessage("CallerID shutdown.");
    }
    private static int lastDate;
    private static SimpleDateFormat sdfLogName;
    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    public static void logMessage(String msg) {
        try {
            PrintStream ps;
            Date dt = new Date();
            if (logFile.length() > 0 && !logFile.startsWith("console:")) {
                String fn;
                if (sdfLogName != null)
                    fn = sdfLogName.format(dt);
                else
                    fn = logFile;
                ps = new PrintStream(new FileOutputStream(fn, true));
            } else {
                logFile = "";
                ps = System.out;
            }
            if (dt.getDate() != lastDate) {
                ps.println(sdf.format(dt) + " Date: " + dt);
                lastDate = dt.getDate();
            }
            ps.println(sdf.format(dt) + " " + msg);
            if (logFile.length() > 0) {
                ps.close();
            }
        } catch (Exception ex) {
            System.err.println("logMessage: " + ex);
        }
    }

    public static void logMessage(Exception ex) {
        logMessage(ex.toString());
    }
    
    public static boolean makeError() {
        if (errorRate < 0) return false;
        if (--errorCounter > 0) return false;
        errorCounter = errorRate;
        return true;
    }
}
