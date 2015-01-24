/*
 * Created on Feb 2, 2005
 * @author Jim Evans
 * @version $Id: WhozzFile.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Read WhozzCalling messages from a text file, using the timestamps in the file.
 */
public class WhozzFile extends WhozzCalling {
    protected BufferedReader ins;
    public WhozzFile(String fileName) throws FileNotFoundException {
        ins = new BufferedReader(new FileReader(fileName));
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private static Date lastDate = new Date();

    private int lines;
    private BufferedReader cin;
    private int eofCount = 3;
    
    private String readLine() {
        if (lines == 0) {
            if (cin == null) {
                cin = new BufferedReader(new InputStreamReader(System.in));
            }
            System.out.print("Number of lines to process: ");
            try {
                String s = cin.readLine();
                if (s == null) {
                    lines = -1;
                } else if (s.length() > 0) {
                    lines = Integer.parseInt(s);
                } else {
                    lines = 1;
                }
            } catch (IOException e) {
            }
        }
        try {
            if (lines < 0) {
                return ins.readLine();
            }
            --lines;
            return ins.readLine();
        } catch (IOException e) {
            //CallerID.logMessage(ex);
        }
        return null;
    }
    /* (non-Javadoc)
     * @see LineData#readMessage()
     */
    public Message readMessage() {
        long ms = 0;
        while (CallerID.app.msgQueue.size() > 3) {
            try {
                Thread.sleep(lines >= 0 ? 10 : 1000);
            } catch (InterruptedException e1) {
            }
        }
        String s = readLine();
        if (s == null) {
            try {
                Thread.sleep(10000);
                if (--eofCount <= 0) {
                    CallerID.doShutdown();
                }
            } catch (InterruptedException e) {
            }
            return null;
        }
        if (s.length() >= 13) {
            if (s.substring(13, 18).equals("Date:")) {
                try {
                    SimpleDateFormat sdf2 = new SimpleDateFormat(
                    "EEE MMM dd HH:mm:ss zzz yyyy");
                    lastDate = sdf2.parse(s.substring(19));
                } catch (Exception ex) {
                    CallerID.logMessage(ex);
                }
            }
            Date d;
            try {
                d = sdf.parse(s.substring(0, 13));
                d.setMonth(lastDate.getMonth()); 
                d.setDate(lastDate.getDate()); 
                d.setYear(lastDate.getYear());
                ms = d.getTime();
                CallerID.app.msReference = ms;
            } catch (Exception ex) {
                return null;
                //CallerID.logMessage(ex);
            }
            //CallerID.logMessage(s);
            s = s.substring(13);
        }
        CallerID.logMessage(s);
        Message m = new Message(s);
        m.msTime = ms;
        return m;
        //return null;
    }

    /* (non-Javadoc)
     * @see LineData#close()
     */
    public void close() {
        try {
            if (ins != null)
                ins.close();
        } catch (IOException e) {
        }
        ins = null;
    }
}
