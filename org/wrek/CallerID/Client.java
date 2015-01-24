/*
 * Created on Apr 8, 2005
 * @author Jim Evans
 * @version $Id: Client.java 31 2008-09-26 03:23:27Z root $
 */

package org.wrek.CallerID;

import java.io.*;
import java.net.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * Handle communication to the client program.
 * Currently only sends data via UDP.
 */
public class Client extends Thread {
    public String portName;
    public Client(String portName) {
        this.portName = portName;
    }

    public void open() {
        if (portName.startsWith("console:")) {
            outs = System.out;
            return;
        }
        try {
            URI uri = new URI(portName);
//            System.out.println("scheme = " + uri.getScheme());
//            System.out.println("host = " + uri.getHost());
//            System.out.println("port = " + uri.getPort());
            if (uri.getScheme().equalsIgnoreCase("udp")) {
                try {
                    sendSocket = new DatagramSocket();
                    sendPacket = new DatagramPacket(new byte[512], 512, InetAddress.getByName(uri.getHost()), uri.getPort());
                } catch (Exception e) {
                    CallerID.logMessage(e);
                }
            }
        } catch (URISyntaxException e) {
            CallerID.logMessage(e);
        }
    }
    
    public void send(String msg) {
        if (sendSocket != null) {
            try {
                sendPacket.setData(msg.getBytes());
                sendSocket.send(sendPacket);
            } catch (IOException e) {
                CallerID.logMessage(e);
            }
        } else if (outs != null) {
            outs.println(msg);
        }
    }
    
    public DatagramSocket sockServer;
    public DatagramPacket pktServer;
    
    public void run() {
        try {
            sockServer = new DatagramSocket(9967);
        } catch (SocketException e) {
        }
        pktServer = new DatagramPacket(new byte[4096], 4096);
        while (true) {
            try {
                sockServer.receive(pktServer);
            } catch (IOException e1) {
            }
            ByteArrayInputStream ins = new ByteArrayInputStream(pktServer.getData(), 0, pktServer.getLength());
            InputStreamReader isr = new InputStreamReader(ins);
            BufferedReader br = new BufferedReader(isr);
            StringBuffer sb = new StringBuffer();
            try {
                while (true) {
                    String s = br.readLine();
                    if (s == null) break;
                    sb.append(s); sb.append("\n");
                }
            } catch (IOException e4) {
                //e4.printStackTrace();
            }
            ins.reset();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = null;
            try {
                db = dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e2) {
            }
            try {
                Document doc = db.parse(ins);
                Element n = doc.getDocumentElement();
                if (n.getTagName() == "Refresh") {
                	Refresh msg = new Refresh();
                	msg.xmlDeserialize(n);
                    CallerID.app.msgQueue.addLast(msg);
                }
            } catch (SAXException e3) {
                // TODO Auto-generated catch block
                // e3.printStackTrace();
            } catch (IOException e3) {
                // TODO Auto-generated catch block
                // e3.printStackTrace();
            }
        }
    }
    
    private DatagramSocket sendSocket;
    private DatagramPacket sendPacket;
    private DatagramSocket recvSocket;
    private PrintStream outs;
}
