/*
 * Created on Apr 8, 2005
 * @author Jim Evans
 * @version $Id: WhozzTCP.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;
import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Get WhozzCalling message from the box using TCP connection to the Lantronix terminal server.
 */
public class WhozzTCP extends WhozzCalling {
    public WhozzTCP(String portName) {
        this.portName = portName;
    }

    public void open() throws IOException {
        String[] sa = portName.split(":");
        socket = new Socket(sa[0], Integer.parseInt(sa[1]));
        socket.setTcpNoDelay(true);
        ins = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        outs = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        state = 0;
        connected = false;
    }

    public void close() {
        connected = false;
        if (outs != null) {
            try {
                outs.close();
            } catch (IOException ex) {
            }
            outs = null;
        }
        if (ins != null) {
            try {
                ins.close();
            } catch (IOException ex) {
            }
            ins = null;
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
            }
            socket = null;
        }
    }

    /* (non-Javadoc)
     * @see WhozzCalling#readMessage()
     */
    public Message readMessage() {
        while (true) {
            if (!connected)
                connect();
            try {
                String s;
                long ms = 0;
                s = ins.readLine();
                ms = System.currentTimeMillis();
                CallerID.logMessage(s);
                Message m = new Message(s);
                m.msTime = ms;
                return m;
            } catch (InterruptedIOException ex) {
                System.out.println(ex);
                if (++timeouts > 5) {
                    timeouts = 0;
                    connected = false;
                    state = 2;
                }
            } catch (IOException ex) {
                CallerID.logMessage(ex);
                connected = false;
                state = 0;
            }
        }
    }

    private Socket socket;
    private BufferedReader ins = null;
    private BufferedWriter outs = null;
    private String portName;
    private int timeouts;
    private String options = "EcXUdAsobkt";
    private boolean connected = false;
    private int state = 0;
    
    private void connect() {
        long delay = 0;
        int connectTries = 0;

        while (true) {
            switch (state) {
            case 0 : // Close and clean up any mess
                if (socket != null && connectTries <= 1)
                    CallerID.logMessage("Disconnect " + socket.toString());
                close();
                state = 1;
                break;
            case 1 : // Try to open the connection
                try {
                    open();
                    if (connectTries++ == 0)
                        CallerID.logMessage("Connected to "
                                + socket.getRemoteSocketAddress());
                    socket.setSoTimeout(10000);
                    state = 2;
                    delay = 0;
                } catch (IOException ex) {
                    if (connectTries++ == 0)
                        CallerID.logMessage("Failed to connected to " + portName);
                    CallerID.logMessage(ex);
                    delay = 10000;
                }
                break;
            case 2 : // Send a ping
                try {
                    int ch;
                    outs.write("@");
                    outs.flush();
                    do {
                        ch = ins.read();
                        System.out.print((char) ch);
                    } while (ch != '#');
                    System.out.println("");
                    state = 3;
                } catch (SocketTimeoutException ex) {
                    System.out.println(ex);
                    if (connectTries <= 1)
                        CallerID.logMessage(ex);
                    state = 0;
                    delay = 1000;
                } catch (IOException ex) {
                    CallerID.logMessage(ex);
                    state = 0;
                    delay = 1000;
                }
                break;
            case 3 : // Get the version and configuration string
                try {
                    outs.write("V");
                    outs.flush();
                    String version = ins.readLine();
                    CallerID.logMessage(version);
                    if (adjustOptions(version.substring(5, 16))) {
                        socket.setSoTimeout(5 * 60 * 1000);
                        state = 4;
                    } else {
                        delay = 100;
                    }
                } catch (IOException ex) {
                    CallerID.logMessage(ex);
                    state = 0;
                    delay = 1000;
                }
                break;
            case 4 : // Yea! we are connected
                connected = true;
                return;
            }
            try {
                if (delay > 0)
                    Thread.sleep(delay);
            } catch (Exception ex) {
                CallerID.logMessage(ex);
            }
        }
    }

    private boolean adjustOptions(String v) throws IOException {
        if (options.equals(v))
            return true;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (options.indexOf(c) < 0) {
                if (Character.isLowerCase(c))
                    c = Character.toUpperCase(c);
                else
                    c = Character.toLowerCase(c);
                if (options.indexOf(c) >= 0) {
                    outs.write(c);
                    outs.flush();
                    CallerID.logMessage("Set option " + c);
                    return false;
                }
            }
        }
        return true;
    }
}
