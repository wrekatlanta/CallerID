/*
 * Created on Apr 14, 2005
 * @author Jim Evans
 * @version $Id: CallStatus.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Maintain information about call status.
 * Used by both the server and the client.
 * Includes XML serialize/deserialize.
 */
public class CallStatus {
    /*
     * Serialized public properties
     */
    public int line;
    public int recordID;
    public long wait;
    public long duration;
    public String handle;
    public String notes;
    public String number;
    public String shortName;
    public String fullName;
    
    public final static int IDLE = 0;
    public final static int ANSWERED = 1;
//    public final static int OUTBOUND = 2;
    public final static int RING = 3;
    public int progress;
    
    public final static int OK = 1;
    public final static int WARNING = 2;
    public final static int BLOCKED = 3;
    public int status;
    public int screened;

    /*
     * Non-serialized public properties
     */
    public final static int UNKNOWN = 0;
    public final static int INBOUND = 1;
    public final static int OUTBOUND = 2;
    public int direction;

    public boolean modified;
    public long msRing;
    public long msAnswered;
    public long msIdle;
    public boolean needLookup;

    public CallStatus(int n) {
        line = n;
        clear();
    }

    public void clear() {
        msAnswered = 0;
        wait = -1;
        duration = -1;
        recordID = 0;
        direction = UNKNOWN;
        progress = IDLE;
        status = UNKNOWN;
        screened = UNKNOWN;
        handle = null;
        notes = null;
        number = null;
        shortName = null;
        fullName = null;
    }

    public static String getInnerText(Node n) {
        StringBuffer sb = new StringBuffer();
        for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling()) {
            switch (child.getNodeType()) {
                case Node.TEXT_NODE:
                case Node.CDATA_SECTION_NODE:
                    sb.append(child.getNodeValue());
                    break;
            }
        }
        return sb.toString();
    }

    public static String getInnerText(Element node, String tagname) {
        NodeList nl = node.getElementsByTagName(tagname);
        if (nl == null || nl.getLength() == 0) return null;
        return getInnerText(nl.item(0));
    }
    
    public void xmlDeserialize(Element n) {
        line = Integer.parseInt(getInnerText(n, "Line"));
        String s = getInnerText(n, "Progress");
        for (int i = 0; i < progressNames.length; i++)
            if (s.compareToIgnoreCase(progressNames[i]) == 0) {
                progress = i;
                break;
            }
        number = getInnerText(n, "Number");
        shortName = getInnerText(n, "ShortName");
        s = getInnerText(n, "Wait");
        if (s != null && s.length() > 0)
            wait = Long.parseLong(s);
        s = getInnerText(n, "Duration");
        if (s != null && s.length() > 0)
            duration = Long.parseLong(s);

        s = getInnerText(n, "Status");
        if (s != null)
            for (int i = 0; i < statusNames.length; i++)
                if (s.compareToIgnoreCase(statusNames[i]) == 0) {
                    status = i;
                    break;
                }
        fullName = getInnerText(n, "FullName");

        s = getInnerText(n, "Screened");
        if (s != null)
            for (int i = 0; i < statusNames.length; i++)
                if (s.compareToIgnoreCase(statusNames[i]) == 0) {
                    screened = i;
                    break;
                }
        handle = getInnerText(n, "Handle");
        notes = getInnerText(n, "Notes");
    }

    public final static String[] progressNames = {"Idle", "Answered", "Outbound", "Ring"};
    public final static String[] statusNames = {"Unknown", "OK", "Warning", "Blocked"};

    public void xmlSerialize(XmlTextWriter xout, String tagName) throws IOException {
        long t;
        if (tagName == null) tagName = "CallStatus";
        xout.writeStartElement(tagName);
        xout.writeElementString("Line", Integer.toString(line));
        xout.writeElementString("ID", Integer.toString(recordID));
        xout.writeElementString("Progress", progressNames[progress]);
        xout.writeOptionalElementString("Number", number);
        xout.writeOptionalElementString("ShortName", shortName);
        switch (progress) {
        case IDLE:
            if (wait >= 0)
                xout.writeElementString("Wait", Long.toString(wait));
            if (duration >= 0)
                xout.writeElementString("Duration", Long.toString(duration));
            break;
        case ANSWERED:
            xout.writeElementString("Wait", Long.toString(wait));
            t = CallerID.app.currentTimeMillis() - msAnswered;
            xout.writeElementString("Duration", Long.toString(t));
            break;
        case OUTBOUND:
            t = CallerID.app.currentTimeMillis() - this.msAnswered;
            xout.writeElementString("Duration", Long.toString(t));
            break;
        case RING:
            t = CallerID.app.currentTimeMillis() - msRing;
            xout.writeElementString("Wait", Long.toString(t));
            break;
        }
        
        if (status != 0)
            xout.writeOptionalElementString("Status", statusNames[status]);
        xout.writeOptionalElementString("FullName", fullName);

        if (screened != 0)
            xout.writeOptionalElementString("Screened", statusNames[screened]);
        xout.writeOptionalElementString("Handle", handle);
        xout.writeOptionalElementString("Notes", notes);
        xout.writeEndElement();
    }
}

