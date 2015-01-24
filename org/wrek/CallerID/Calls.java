/*
 * Created on Feb 3, 2005
 * @author Jim Evans
 * @version $Id: Calls.java 31 2008-09-26 03:23:27Z root $
 *
 */

package org.wrek.CallerID;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.*;

/**
 * Class to handle accessing the Call table in the MySQL database.
 */
public class Calls {
    public boolean connected = false;
    
    private String connString;
    private Connection conn;
    private PreparedStatement insert;
    private PreparedStatement updateIn;
    private PreparedStatement updateOut;
    private PrintStream fileDB;
    
    public Calls(String connString) {
        this.connString = connString;
    }
    
    public void close() {
        if (insert != null) {
            try {
                insert.close();
            } catch (Exception ex) {
            }
            insert = null;
        }
        if (updateIn != null) {
            try {
                updateIn.close();
            } catch (Exception ex) {
            }
            updateIn = null;
        }
        if (updateOut != null) {
            try {
                updateOut.close();
            } catch (Exception ex) {
            }
            updateOut = null;
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ex) {
            }
            conn = null;
        }
        if (fileDB != null) {
            fileDB.close();
            fileDB = null;
        }
        connected = false;
    }

    public boolean open() {
        close();
        try {
            if (connString.startsWith("console:")) {
                fileDB = System.out;
            } else if (connString.startsWith("file://")) {
                fileDB = new PrintStream(new FileOutputStream(connString.substring(7)), true);
            } else {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                conn = DriverManager.getConnection(connString);
                insert = conn.prepareStatement(
                        "INSERT INTO calls (StartTime, Number, ShortName, Line, Direction) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                updateIn = conn.prepareStatement("UPDATE calls SET Duration=?, Wait=? WHERE id=?");
                updateOut = conn.prepareStatement("UPDATE calls SET Duration=?, ShortName=? WHERE id=?");
            }
            CallerID.logMessage("Connected to database at " + connString);
            connected = true;
        } catch (Exception ex) {
            CallerID.logMessage(ex);
            close();
        }
        return connected;
    }
    private int lastRecordID = 0;
    public void startCall(CallStatus li) {
        if (!connected)
            open();
        if (insert != null) {
            ResultSet rs;
            int rows;
            for (int retry = 2; retry > 0; retry--) {
                try {
                    insert.setTimestamp(1, new Timestamp(li.msRing));
                    insert.setString(2, li.number);
                    insert.setString(3, li.shortName);
                    insert.setInt(4, li.line);
                    insert.setString(5, li.direction == CallStatus.INBOUND ? "In" : "Out");
                    if (CallerID.makeError()) {
                        conn.close();
                    }
                    rows = insert.executeUpdate();
                    if (rows != 1)
                        System.out.println("Rows Inserted = " + rows);
                    rs = insert.getGeneratedKeys();
                    rs.next();
                    li.recordID = rs.getInt(1);
                    break;
                } catch (SQLException ex) {
                    CallerID.logMessage(ex);
                    open();
                }
            }
        } else {
            li.recordID = ++lastRecordID;
            fileDB.print("INSERT INTO calls (");
            fileDB.print(new Timestamp(li.msRing).toString());
            fileDB.print(", " + li.number);
            fileDB.print(", '" + li.shortName);
            fileDB.print("', " + li.line);
            fileDB.print(", " + ((li.direction == CallStatus.INBOUND) ? "In" : "Out"));
            fileDB.println(") as " + li.recordID);
        }
    }
    
    public void endCall(CallStatus li) {
        if (li.recordID <= 0)
            return;
        if (!connected)
            open();
        if (updateIn != null) {
            for (int retry = 2; retry > 0; retry--) {
                try {
                    int rows;
                    if (li.direction == CallStatus.INBOUND) {
                        updateIn.setInt(1, (int) (li.duration / 1000));
                        updateIn.setInt(2, (int) (li.wait / 1000));
                        updateIn.setInt(3, li.recordID);
                        if (CallerID.makeError()) {
                            conn.close();
                        }
                        rows = updateIn.executeUpdate();
                    } else {
                        updateOut.setInt(1, (int) (li.duration / 1000));
                        updateOut.setString(2, li.shortName);
                        updateOut.setInt(3, li.recordID);
                        if (CallerID.makeError()) {
                            conn.close();
                        }
                        rows = updateOut.executeUpdate();
                    }
                    if (rows != 1)
                        System.out.println("Rows Updated = " + rows);
                    break;
                } catch (SQLException ex) {
                    CallerID.logMessage(ex);
                    open();
                }
            }
        } else {
            if (li.direction == CallStatus.INBOUND) {
                fileDB.print("UPDATE calls SET Duration=");
                fileDB.print(li.duration / 1000);
                fileDB.print(", Wait=");
                fileDB.print(li.wait / 1000);
                fileDB.print(" WHERE ID=");
                fileDB.println(li.recordID);
            } else {
                fileDB.print("UPDATE calls SET Duration=");
                fileDB.print(li.duration / 1000);
                fileDB.print(", ShortName='");
                fileDB.print(li.shortName);
                fileDB.print("' WHERE ID=");
                fileDB.println(li.recordID);
            }
        }
        li.recordID = 0;
    }
}
