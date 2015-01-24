/*
 * Created on Apr 14, 2005
 * @author Jim Evans
 * @version $Id: XmlTextWriter.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.io.*;
import java.util.*;

/**
 * Really crude class to write XML output. 
 * Someplace, there is a stock package that could handle this better.
 */

public class XmlTextWriter {
    public XmlTextWriter(Writer writer) {
        this.writer = writer;
    }
    
    public void writeString(String text) throws IOException {
        writer.write(encodeString(text));
    }
    
    public void writeStartElement(String localName) throws IOException {
        writer.write("<" + localName + ">\n");
        stack.push(localName);
    }
    
    public void writeEndElement() throws IOException {
        writer.write("</" + (String)(stack.pop()) + ">\n");
    }
    
//  public void writeElementString(String localName, String value) throws IOException {
//      writeStartElement(localName);
//      if (value != null && value.length() > 0) {
//          writeString(value);
//      }
//      writeEndElement();
//  }

    public void writeElementString(String localName, String value) throws IOException {
        writer.write(" <" + localName + ">" + encodeString(value) + "</" + localName + ">\n");
    }
    
    public void writeOptionalElementString(String localName, String value) throws IOException {
        if (value == null || value.length() == 0)
            return;
        writeElementString(localName, value);
    }
    
    private Writer writer;
    private Stack stack = new Stack();
    
    private String encodeString(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '&':
                sb.append("&amp;");
                break;
            case '<':
                sb.append("&lt;");
                break;
            case '>':
                sb.append("&gt;");
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }
}
