/*
 * Created on Feb 2, 2005
 * @author Jim Evans
 * @version $Id: LineMsg.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.io.FileNotFoundException;

/**
 * Thread to connect to the WhozzCalling box, wait for messages,
 * and add them to the CallerID.msgQueue.
 */
public class LineMsg extends Thread {
    private WhozzCalling whozz = null;
    public void run() {
        String source = CallerID.properties.getProperty("callSource");
        if (source.startsWith("file://")) {
            try {
                whozz = new WhozzFile(source.substring(7));
            } catch (FileNotFoundException e) {
            }
        } else if (source.startsWith("tcp://")) {
            whozz = new WhozzTCP(source.substring(6));
        } else {
            CallerID.logMessage("Unknown source connection type: " + source);
            System.err.println("Unknown source connection type: " + source);
            System.exit(1);
        }
        int count = 0;
        try {
            while (!CallerID.shutdown) {
                WhozzCalling.Message msg = null;
                if (whozz != null)
                    msg = whozz.readMessage();
                if (msg != null) {
                    CallerID.app.msgQueue.addLast(msg);
                }
            }
        } catch (Exception e) {
        }
        if (whozz != null)
            whozz.close();
    }
}
