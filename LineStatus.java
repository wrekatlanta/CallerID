//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.sql.Timestamp;

//import Calls.LineInfo;

/*
 * Created on Feb 3, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author jle
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class LineStatus {
    public LineStatus(int lines) {
        lineInfo = new LineInfo[lines + 1];
        for (int l = 1; l <= lines; l++) {
            lineInfo[l] = new LineInfo(l);
        }
    }

    public Calls calls = new Calls(CallerID.properties.getProperty("database"));
    
    public void processMessage(WhozzCalling.Message msg) {
        int line = msg.line;
        LineInfo li = lineInfo[line];
        switch (msg.type) {
        case 'R':	// Ringing
            li.clear();
            li.direction = LineInfo.INBOUND;
            li.progress = LineInfo.RING;
            li.modified = true;
            li.msRing = msg.msTime;
            break;
        case 'I':	// Inbound call
            li.modified = true;
            li.direction = LineInfo.INBOUND;
            if (msg.start) {
                // Inbound call start
                li.msAnswered = 0;
                li.number = msg.from;
                li.shortName = msg.caller;
                calls.startCall(li);
            } else {
                // Inbound call end
                if (li.msAnswered > 0)
                    li.duration = msg.msTime - li.msAnswered;
                else
                    li.duration = 0;

                // Caller gave up waiting
                if (li.wait == -1)
                    li.wait = msg.msTime - li.msRing;
                li.progress = LineInfo.ONHOOK;
                calls.endCall(li);
                li.recordID = 0;
            }
            break;
        case 'F':	// Off hook
            li.modified = true;
            li.progress = LineInfo.OFFHOOK;
            if (li.direction == LineInfo.INBOUND) {
                // Call answered
                li.wait = msg.msTime - li.msRing;
                li.msAnswered = msg.msTime;
            } else {
                li.wait = 0;
                li.msRing = msg.msTime;
            }
            break;
        case 'N':	// On hook
            li.modified = true;
            li.progress = LineInfo.ONHOOK;
            break;
        case 'O':	// Outbound call
            li.modified = true;
            li.direction = LineInfo.OUTBOUND;
            if (msg.start) {
                // Outbound call start
                if (li.msRing == 0)
                    li.msRing = msg.msTime;
                li.msAnswered = msg.msTime;
                li.number = msg.from;
                li.shortName = msg.caller;
                calls.startCall(li);
            } else {
                // Outbound call end
                if (li.recordID > 0) {
                    li.duration = msg.msTime - li.msAnswered;
                    li.shortName = msg.caller;
                    li.progress = LineInfo.ONHOOK;
                    calls.endCall(li);
                }
            }
        }
    }

    public void sendUpdates() {
        for (int i = 1; i < lineInfo.length; i++) {
            LineInfo li = lineInfo[i];
            if (!li.modified) continue;
            li.modified = false;
            String s = li.fmtXML();
            System.out.println(s.length() + ": " + s);
        }
    }
    
    private LineInfo[] lineInfo;

    private static void writeElement(StringBuffer sb, String name, String value) {
        if (value == null) return;
        sb.append(" <" + name + ">");
        sb.append(value);
        sb.append("</" + name + ">\n");
    }
    
    public final static String[] directionNames = {"Unknown", "In", "Out"};
    public final static String[] progressNames = {"OnHook", "OffHook", "Ring"};

    public class LineInfo {
        public int line;
        public boolean modified;
        public long msRing;
        public long msAnswered;
        public int recordID;
        public long wait;
        public long duration;
        public String handle;
        public String notes;
        public String number;
        public String shortName;
        public String fullName;
        
        public final static int UNKNOWN = 0;
        public final static int INBOUND = 1;
        public final static int OUTBOUND = 2;
        public int direction;

        public final static int ONHOOK = 0;
        public final static int OFFHOOK = 1;
        public final static int RING = 2;
        public int progress;
        
        public LineInfo(int n) {
            line = n;
            clear();
        }

        public void clear() {
            msAnswered = 0;
            wait = -1;
            recordID = 0;
            direction = UNKNOWN;
            progress = ONHOOK;
            handle = null;
            notes = null;
            number = null;
            shortName = null;
            fullName = null;
        }
        
        public String fmtXML() {
            StringBuffer sb = new StringBuffer();
            sb.append("<CallStatus>\n");
            writeElement(sb, "Line", Integer.toString(line));
            writeElement(sb, "ID", Integer.toString(recordID));
            writeElement(sb, "Progress", progressNames[progress]);
            writeElement(sb, "Direction", directionNames[direction]);
            writeElement(sb, "Number", number);
            writeElement(sb, "ShortName", shortName);
            sb.append("</CallStatus>");
            return sb.toString();
        }
    }
}
