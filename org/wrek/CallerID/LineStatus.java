/*
 * Created on Feb 3, 2005
 * @author Jim Evans
 * @version $Id: LineStatus.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;

import java.io.*;
import java.util.*;

/**
 * Process the messages from the WhozzCalling box and update the CallStatus info.
 */
public class LineStatus {
    public LineStatus(int lines) {
        lineInfo = new CallStatus[lines + 1];
        for (int l = 1; l <= lines; l++) {
            lineInfo[l] = new CallStatus(l);
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
        	public void run() {
    			idleTimeout();
        	}
        }, 1000, 1000);
        threadLookup = new Thread() {
        	public synchronized void run() {
        		while (!CallerID.shutdown) {
        			try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
        			lookup();
        		}
        	}
        };
        threadLookup.start();
    }

    public Calls calls = new Calls(CallerID.properties.getProperty("database"));
    public Callers callers = new Callers(CallerID.properties.getProperty("database"));
    
    public void processMessage(WhozzCalling.Message msg) {
        int line = msg.line;
        CallStatus li = lineInfo[line];
        switch (msg.type) {
        case 'R':   // Ringing
            li.clear();
            li.direction = CallStatus.INBOUND;
            li.progress = CallStatus.RING;
            li.modified = true;
            li.msRing = msg.msTime;
            break;
        case 'I':   // Inbound call
            li.modified = true;
            li.direction = CallStatus.INBOUND;
            if (msg.start) {
                // Inbound call start
                li.msAnswered = 0;
                li.number = msg.from;
                li.shortName = msg.caller;
                li.fullName = null;
                li.needLookup = true;
                synchronized (threadLookup) {
                	threadLookup.notify();
                }
                calls.startCall(li);
            } else {
                // Inbound call end
                if (li.msAnswered > 0)
                    li.duration = msg.msTime - li.msAnswered;
                else
                    li.duration = -1;

                // Caller gave up waiting
                if (li.wait == -1)
                    li.wait = msg.msTime - li.msRing;
                li.progress = CallStatus.IDLE;
                calls.endCall(li);
                li.direction = CallStatus.UNKNOWN;
                li.recordID = 0;
                li.msIdle = System.currentTimeMillis();
            }
            break;
        case 'F':   // Off hook
            li.modified = true;
            //li.progress = CallStatus.OFFHOOK;
            if (li.direction == CallStatus.INBOUND) {
                // Call answered
                li.progress = CallStatus.ANSWERED;
                li.wait = msg.msTime - li.msRing;
                li.msAnswered = msg.msTime;
            } else {
                li.progress = CallStatus.OUTBOUND;
                li.wait = 0;
                li.msRing = msg.msTime;
                li.msAnswered = msg.msTime;
                li.number = null;
                li.shortName = null;
                li.fullName = null;
            }
            break;
        case 'N': // On hook
            li.modified = true;
            if (li.msAnswered > 0)
                li.duration = msg.msTime - li.msAnswered;
            else
                li.duration = -1;
            li.progress = CallStatus.IDLE;
            li.msIdle = System.currentTimeMillis();
            break;
        case 'O':   // Outbound call
            li.modified = true;
            li.direction = CallStatus.OUTBOUND;
            if (msg.start) {
                // Outbound call start
                if (li.msRing == 0)
                    li.msRing = msg.msTime;
                li.msAnswered = msg.msTime;
                li.number = msg.from;
                li.shortName = msg.caller;
                li.fullName = null;
                li.needLookup = true;
                synchronized (threadLookup) {
                	threadLookup.notify();
                }
                calls.startCall(li);
            } else {
                // Outbound call end
                if (li.recordID > 0) {
                    li.wait = -1;
                    li.duration = msg.msTime - li.msAnswered;
                    li.shortName = msg.caller;
                    li.progress = CallStatus.IDLE;
                    calls.endCall(li);
                    li.shortName = null;        // For security reasons
                    li.needLookup = false;
                    li.direction = CallStatus.UNKNOWN;
                    li.msIdle = System.currentTimeMillis();
                }
            }
        }
    }

    public void refresh(Refresh r) {
		if (r.line < 0) {
			for (int i = 1; i < lineInfo.length; i++) {
				lineInfo[i].modified = true;
			}
		} else if (r.line < lineInfo.length){
			if (lineInfo[r.line] != null)
				lineInfo[r.line].modified = true;
		}
	}

    public void lookup() {
		for (int i = 1; i < lineInfo.length; i++) {
			CallStatus cs = lineInfo[i];
			if (cs.needLookup) {
				if (cs.shortName != null && callers.lookupCaller(cs)) {
					cs.modified = true;
					CallerID.app.msgQueue.addLast(new Refresh(i));
				}
				cs.needLookup = false;
			}
		}
	}

    public void sendUpdates(Client client) {
        for (int i = 1; i < lineInfo.length; i++) {
            CallStatus li = lineInfo[i];
            if (!li.modified) continue;
            li.modified = false;
            StringWriter sw = new StringWriter();
            XmlTextWriter xml = new XmlTextWriter(sw);
            try {
                li.xmlSerialize(xml, null);
                String s = sw.toString();
                //System.out.println(s.length() + ": " + s);
                client.send(s);
            } catch (IOException e) {
                CallerID.logMessage(e);
            }
        }
    }
    
    private CallStatus[] lineInfo;

    private Timer timer;
    
    private void idleTimeout() {
        long t = System.currentTimeMillis();
        for (int i = 1; i < lineInfo.length; i++) {
            CallStatus cs = lineInfo[i];
            if (cs.msIdle > 0)
                if (t > (cs.msIdle + CallerID.idleTimeout)) {
                    if (cs.progress == CallStatus.IDLE) {
                        cs.clear();
                        cs.modified = true;
                        CallerID.app.msgQueue.addLast(new Refresh(i));
                    }
                    cs.msIdle = 0;  
                }
        }
    }
    
    private Thread threadLookup;
}
