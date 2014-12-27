package MyJavaTorrent;

import java.io.*;
import java.util.concurrent.*;
import javax.swing.event.*;

public class MessageSender extends Thread {
	private boolean running = true; //Thread runs flag
	private OutputStream os = null; //write to the peer connection outputstream
	private LinkedBlockingQueue<Message> outMessage = null;
	//A linked queue which represents the message ready to send
	private EventListenerList listeners = new EventListenerList();
	private int keepAliveInterval = 60000;
	//Generally keepAlive message is sent in interval of two minutes.
	
	public MessageSender(OutputStream os) {
		this.os = os;
		this.outMessage = new LinkedBlockingQueue<Message>();
	}
	
	public void addOutListener(OutListener listener) {
		listeners.add(OutListener.class, listener);
	}
	
	public void removeOutListener(OutListener listener) {
		listeners.remove(OutListener.class, listener);
	}
	
	public OutListener[] getOutListener() {
		return listeners.getListeners(OutListener.class);
	}
	
	protected void informConnectionClosed() { //inform downloadtask that the connection is closed
		for (OutListener listener : getOutListener()) {
			listener.connectionClosed();
		}
	}
	
	protected void informKeepAliveSent() {
		for (OutListener listener : getOutListener()) {
			listener.keepAliveSent();
		}
	}
	
	public synchronized void addMessage(Message m) {
		this.outMessage.add(m);		
	}
	
	public void run() {
		Message m = null;
		byte[] keepAliveMessage = new MessageNormal(PeerProtocol.KEEP_ALIVE).generateMessage();
		try {
			while (this.running) {
				if (this.outMessage != null && this.os != null)
					//poll is considered with only one socket, thus one download task
					m = outMessage.poll(this.keepAliveInterval, TimeUnit.MILLISECONDS);
					if (m != null) {
						os.write(m.generateMessage());
						m = null;
					}
					else if (this.running) {  //Since not synchronized, it is possible that the thread is set to stop running.
						os.write(keepAliveMessage);
						this.informKeepAliveSent();
					}
			}
		}
		catch (IOException ioe) {
			this.informConnectionClosed();
		}
		catch (InterruptedException ie) {
			
		}
		catch (Exception e) {
			this.informConnectionClosed();
		}
		
		
		if (this.outMessage != null)
			this.outMessage.clear();
		this.outMessage = null;
		try {
			this.os.close();
			this.os = null;
			this.notify();
		}
		catch (Exception e) {
			
		}
	}
	
	public void stopThread() {
		this.running = false;
	}
	
	
	
	
	
	
}
