package MyJavaTorrent;

import java.io.*;
import javax.swing.event.*;

public class MessageReceiver extends Thread {
	private boolean running = true;	
	private InputStream is = null;
	private DataInputStream dis = null;
	private EventListenerList listeners = new EventListenerList();
	private boolean handshakeFlag = false;

	
	public MessageReceiver(InputStream is) {
		//this.peerID = peerID;
		this.is = is;
		this.dis = new DataInputStream(is);
	}
	
	//Since length in the message is variable, it is convenient to use DataInputStream rather than InputStream.read() 
	private int readData(byte[] data) { 
		try {
			this.dis.readFully(data);
		}
		catch (IOException e) {
			return -1;
		}
		return data.length;
	}
	
	public void addInListener(InListener listener) {
		listeners.add(InListener.class, listener);
	}
	
	public void removeInListener(InListener listener) {
		listeners.remove(InListener.class, listener);
	}
	
	public InListener[] getInListener() {
		return listeners.getListeners(InListener.class);
	}
	
	protected void informMessageReceived(Message m) {
		for (InListener listener : getInListener()) {
			listener.messageReceived(m);
		}
	}
	
	
	public void run() {		
		int messageID;
		byte[] handshakeLength = new byte[1];
		byte[] normalLength = new byte[4];
		int normalLengthInt;
		byte[] protocol = new byte[19];
		byte[] reserved = new byte[8];
		byte[] fileInfoHash = new byte[20];
		byte[] peerID = new byte[20];
		byte[] payload;
		MessageHandshake mh = new MessageHandshake();
		MessageNormal mn = new MessageNormal();
		int r = 0;
		int i;
		while (this.running) {
			try {
				if (!handshakeFlag) {
					r = this.readData(handshakeLength);
					if (r <= 0) 
						mh = null;
					else {
						for (i = 0; i < 19; i++)
							protocol[i] = (byte)is.read();
						for (i = 0; i < 8; i++)
							reserved[i] = (byte)is.read();
						for (i = 0; i < 20; i++)
							fileInfoHash[i] = (byte)is.read();
						for (i = 0; i < 20; i++)
							peerID[i] = (byte)is.read();
						mh.setParameters(handshakeLength, protocol, reserved, fileInfoHash, peerID);
					}
				}
				else {
					r = this.readData(normalLength);
					if (r <= 0)
						mn = null;
					else {
						normalLengthInt = Utils.byteArrayToInt(normalLength);
						if (normalLengthInt == 0) //Keep Alive Message
							mn.setParameters(PeerProtocol.KEEP_ALIVE);
						else {
							messageID = is.read();
							if (messageID == -1) {
								System.err.println("Receive Message ID error.");
								mn = null;
							}
							else {
								if (normalLengthInt == 1)
									mn.setParameters(messageID + 1);
								else {
									normalLengthInt--;
									payload = new byte[normalLengthInt];
									if (this.readData(payload) > 0)
										mn.setParameters(messageID + 1, payload);
									payload = null;
								}	
							}
								
						}
					}
				}
			}
			catch (IOException ioe) {				
				this.informMessageReceived(null);
				return;
			}
			/*catch (NullPointerException npe) {
				this.informMessageReceived(null);
				return;
			}*/
			catch (Exception e) {
				System.err.println("Message Receive Error.");
				System.err.println(e.getMessage());
				this.informMessageReceived(null);
				return;
			}		
			if (!this.handshakeFlag) {
				this.handshakeFlag = true;
				this.informMessageReceived(mh);				
			}
			else
				this.informMessageReceived(mn);
		}
		try {
			this.dis.close();
			this.dis = null;
		}
		catch (Exception e) {
			
		}
	}
	public void stopThread() {
		this.running = false;
	}
	
	
	
	
}
