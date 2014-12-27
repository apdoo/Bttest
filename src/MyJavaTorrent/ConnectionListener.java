package MyJavaTorrent;

import java.io.*;
import java.net.*;
import javax.swing.event.*;

public class ConnectionListener extends Thread {
	private int minPort = -1;
	private int maxPort = -1;
	private int connectedPort = -1;
	private ServerSocket ss = null;
	private EventListenerList listeners = new EventListenerList();
	
	
	public ConnectionListener() {
		
	}
	
	public ConnectionListener(int minPort, int maxPort) {
		this.minPort = minPort;
		this.maxPort = maxPort;
	}
	
	public int getMinPort() {
		return this.minPort;
	}
	
	public int getMaxPort() {
		return this.maxPort;
	}
	
	public int getConnectedPort() {
		return this.connectedPort;
	}
	
	public void setMinPort(int minPort) {
		this.minPort = minPort;		
	}
	
	public void setMaxPort(int maxPort) {
		this.maxPort = maxPort;
	}
	
	public boolean connect() {
		if (this.minPort == -1 || this.maxPort == -1)
			return false;
		else
			return this.connect(this.minPort, this.maxPort);
	}
	
	public boolean connect(int minPort, int maxPort) {
		this.minPort = minPort;
		this.maxPort = maxPort;
		for (int i = minPort; i <= maxPort; i++) {
			try {
				this.ss = new ServerSocket(i);
				this.connectedPort = i;
				this.setDaemon(true);
				this.start();
				return true;				
			}
			catch (IOException e) {
				
			}			
		}
		return false;
	}
	
	public void run() {
		byte[] b = new byte[0];
		try {
			while (true) {
				this.informConnectionAccepted(ss.accept());
				sleep(1000);				
			}
		}
		catch (InterruptedException ie) {
			
		}
		catch (IOException ioe) {
			System.err.println("ConnectionListener error.");
		}
		 
	}
	
	public void addConnectionListenerInterface(ConnectionListenerInterface cli) {
		listeners.add(ConnectionListenerInterface.class, cli);
	}
	
	public void removeConnectionListenerInterface(ConnectionListenerInterface cli) {
		listeners.remove(ConnectionListenerInterface.class, cli);
	}
	
	public ConnectionListenerInterface[] getConnectionListenerInterface() {
		return listeners.getListeners(ConnectionListenerInterface.class);
	}
	
	protected void informConnectionAccepted(Socket s) {
        for (ConnectionListenerInterface listener : getConnectionListenerInterface()) {
            listener.connectionAccepted(s);
        }
	}
	
}
