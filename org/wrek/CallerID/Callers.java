/*
 * Created on Apr 27, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.wrek.CallerID;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author jle
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Callers {
    public boolean connected = false;
    
    private String connString;
    private Connection conn;
    private PreparedStatement find;
    
    public Callers(String connString) {
        this.connString = connString;
    }
    
    public void close() {
        if (find != null) {
            try {
                find.close();
            } catch (Exception ex) {
            }
            find = null;
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception ex) {
            }
            conn = null;
        }
        connected = false;
    }

    public boolean open() {
        close();
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(connString);
			find = conn.prepareStatement("SELECT FullName, Status FROM callers WHERE Number=?");
			CallerID.logMessage("Connected to database at " + connString);
			connected = true;
		} catch (Exception ex) {
			CallerID.logMessage(ex);
			close();
		}
		return connected;
    }

    public boolean lookupCaller(CallStatus cs) {
		if (!connected)
			open();
		try {
			String num = cs.number;
			if (num.startsWith("9")) num = num.substring(1);
			if (num.startsWith("1")) num = num.substring(1);
			find.setString(1, num);
			ResultSet rs = find.executeQuery();
			if (rs.first()) {
				cs.fullName = rs.getString(1);
				String s = rs.getString(2);
		        if (s != null)
		            for (int i = 0; i < CallStatus.statusNames.length; i++)
		                if (s.compareToIgnoreCase(CallStatus.statusNames[i]) == 0) {
		                    cs.status = i;
		                    break;
		                }
				cs.modified = true;
				return true;
			}
		} catch (SQLException e) {
			CallerID.logMessage(e);
			close();
		}
		return false;
	}
}
