/*
 * Created on Apr 26, 2005
 * @author Jim Evans
 * @version $Id: Refresh.java 31 2008-09-26 03:23:27Z root $
 */
package org.wrek.CallerID;

import java.io.IOException;
import org.w3c.dom.*;

/**
 * 
 */
public class Refresh {
	/*
	 * Serialized public properties
	 */
	public int line;
	
	public Refresh() { line = -1; }
	public Refresh(int line) { this.line = line; }

	public void xmlDeserialize(Element n) {
		String s = CallStatus.getInnerText(n, "Line");
		if (s != null && s.length() > 0)
			line = Integer.parseInt(s);
	}
	
    public void xmlSerialize(XmlTextWriter xout, String tagName) throws IOException {
        long t;
        if (tagName == null) tagName = "Refresh";
        xout.writeStartElement(tagName);
        if (line >= 0)
        	xout.writeOptionalElementString("Line", Integer.toString(line));
        xout.writeEndElement();
	}
}
