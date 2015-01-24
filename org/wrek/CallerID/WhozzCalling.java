/*
 * Created on Feb 2, 2005
 * @author Jim Evans
 * @version $Id: WhozzCalling.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Abstract class for handling messages from a WhozzCalling box.
 */
public abstract class WhozzCalling {
    public abstract void close();
    /**
     * Read a message from the WhozzCalling box. Block if there is no message available.
     * @return a WhozzCalling.Message object
     */
    public abstract Message readMessage();
    
    private static String getField(String s, int start, int length) {
        int end = start + length;
        if (start >= s.length())
            return "";
        if (end >= s.length())
            return s.substring(start).trim();
        return s.substring(start, end).trim();
    }

    /**
     * A parsed message from the WhozzCalling box.
     * @author jle
     */
    public class Message {
        public long msTime;
        public int type;
        public int line;
        public Date callStart;
        public String from;
        public String caller;
        public boolean start;
        public String text;

        /*
         * Typical Messages: 
         * ..........1.........2.........3.........4.........5.........6..
         * 012345678901234567890123456789012345678901234567890123456789012 
         * $01 I S 0276 G A3 09/26 11:28 AM 708-980-7710   Mike Sandman En 
         * $10 I E 0011 G A1 01/08 11:12 AM 4043030680     0#*10459 
         * $10 I S 0000 B A0 01/08 02:39 PM No-CallerID 
         * $10 F             01/08 14:30:55 
         * $10 N             01/08 14:30:56 
         * $10 R             01/08 14:33:41 
         * $10 O S 0000 G 00 01/08 11:13 AM 456 
         * $10 O E 0006 G 00 01/08 11:13 AM 456 
         * $03 r             01/08 15:32:39 
         * $03 V61           00/00 00:00:04 
         * $02 L             00/00 00:00:34
         */
        public Message(String msg) {
            text = msg;
            int i = msg.indexOf('$');
            if (i > 0)
                msg = msg.substring(i);
            if (msg.length() < 32) {
                //CallerID.logMessage("Message too short " + msg.length());
                return;
            }
            if (msg.charAt(0) != '$') {
                //CallerID.logMessage("Message does not start with $");
                return;
            }
            line = Integer.parseInt(msg.substring(1, 3));
            line = ((line - 1) & 0x3) + 1;
            type = msg.charAt(4);
            start = (msg.charAt(6) == 'S');

            try {
                SimpleDateFormat df;
                if ("RFNHVLr".indexOf(type) >= 0) {
                    df = new SimpleDateFormat("MM/dd hh:mm:ss");
                } else {
                    df = new SimpleDateFormat("MM/dd hh:mm a");
                }
                ParsePosition pp = new ParsePosition(18);
                callStart = df.parse(msg, pp);
                Calendar cal = new GregorianCalendar();
                int year = cal.get(Calendar.YEAR);
                cal.setTime(callStart);
                if (cal.get(Calendar.YEAR) == 1970)
                    cal.set(Calendar.YEAR, year);
                callStart = cal.getTime();
            } catch (Exception ex) {
            }

            from = getField(msg, 33, 14);
            caller = getField(msg, 48, 15);
        }

        public String toString() {
            if (type == 0) return text;
            return line + ", " + (char) type + ", " + callStart + ", " + from + ", " + caller
                    + ", " + start;
        }
    }
}
