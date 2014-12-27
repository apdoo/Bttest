package MyJavaTorrent;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.swing.event.*;

public class DownloadTask extends Thread implements InListener, OutListener {
	private int state = DownloadState.IDLE;
	
	private byte[] fileInfoHash;
	private byte[] clientID;  //My id
	public Peer peer;
	//private String peer.toString();
	
	public byte[] bitField = null;
	private Piece downloadPiece = null;
	private int offset = 0;
	private EventListenerList listeners = new EventListenerList();
	private Socket peerConnection = null;
	private InputStream is = null;
	private OutputStream os = null;
	public MessageSender ms = null;
	public MessageReceiver mr = null;
	private long downloaded = 0;
	private long uploaded = 0;
	private long lastMessageTime = 0;
	private boolean initiate; //If true then send handshake message
	private boolean running = true; 
	
	private LinkedList<Integer> pendingRequest;
	
	
	public DownloadTask(Peer peer, byte[] fileInfoHash, byte[] clientID, boolean initiate, byte[] bitField, Socket s) {		
		this.fileInfoHash = fileInfoHash;
		this.clientID = clientID;
		this.initiate = initiate;
		//System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaa");
		this.bitField = bitField;
		this.pendingRequest = new LinkedList<Integer>(); 
		if (s == null) {
			this.peer = peer;
			
		}
		else {
			try {
				this.peerConnection = s;
				String peerIP = this.peerConnection.getInetAddress().getHostAddress();
				int peerPort = this.peerConnection.getPort();
				this.is = this.peerConnection.getInputStream();
				this.os = this.peerConnection.getOutputStream();
				this.peer = new Peer();
				this.peer.setIP(peerIP);
				this.peer.setPort(peerPort);
				
			}
			catch (IOException e) {
				
			}
		}
	}
	
	public synchronized Peer getPeer() {
		return this.peer;
	}
	
	
	
	public DownloadTask(Peer peer, byte[] fileInfoHash, byte[] clientID, boolean initiate, byte[] bitField) {
		this(peer, fileInfoHash, clientID, initiate, bitField, null);
	}
	
	public DownloadTask(Peer peer, byte[] fileInfoHash, byte[] clientID) {
		this(peer, fileInfoHash, clientID, true, null);
	}
	
	public void initConnection() throws UnknownHostException, IOException {
		if (this.peerConnection == null && !this.peer.isConnected()) {
			this.peerConnection = new Socket(this.peer.getIP(), this.peer.getPort());
			this.os = this.peerConnection.getOutputStream();
			this.is = this.peerConnection.getInputStream();
			this.peer.setConnected(true);		
		}
		this.ms = new MessageSender(this.os);
		this.ms.addOutListener(this);
		this.ms.start();
		this.mr = new MessageReceiver(this.is);
		this.mr.addInListener(this);
		this.mr.start();
		this.informAddTask(this.peer.toString(), this);
		if (this.initiate) {
			MessageHandshake msh = new MessageHandshake(this.fileInfoHash, this.clientID);		
			this.ms.addMessage(msh);
			this.changeState(DownloadState.WAIT_HANDSHAKE);
		}
		else 
			this.changeState(DownloadState.WAIT_DOWNLOAD);
		
	}
	
	public void run() {
		try {
			this.initConnection();
			while (this.running) {
				synchronized (this) {
					this.wait();
				}
			}			
		}
		catch (UnknownHostException uhe) {
			this.informTaskComplete(this.peer.toString(), DownloadState.UNKNOWN_HOST);
		}
		catch (IOException ioe) {
			this.informTaskComplete(this.peer.toString(), DownloadState.CONNECTION_REFUSED);
		}
		catch (InterruptedException ie) {
			
		}		
	}
	public void messageReceived(Message m) {
		 if (m == null) {
			 this.informTaskComplete(this.peer.toString(), DownloadState.BAD_MESSAGE);
			 return;
		 }
		 this.lastMessageTime = System.currentTimeMillis();
		 if (m.getType() == PeerProtocol.HANDSHAKE) {			 
			 MessageHandshake mh = (MessageHandshake)m;
			 //System.out.println("Receive Handshake Message");
			 if (Utils.bytesCompare(mh.getFileInfoHash(), this.fileInfoHash)) {
				 if (!this.initiate) { //The peer request me to handshake
					 this.peer.setID(new String(mh.getPeerID()));	
					 //System.out.println("HandShake:"+this.peer.toString());
					 this.ms.addMessage(new MessageHandshake(this.fileInfoHash, this.clientID));
				 }
				 //Bitfield message should be immediately sent after receiving handshake
				 this.ms.addMessage(new MessageNormal(PeerProtocol.BITFIELD, this.bitField));
				 this.changeState(DownloadState.WAIT_DOWNLOAD); 
			 }
			 else {
				 this.informTaskComplete(peer.toString(), DownloadState.BAD_HANDSHAKE);
			 }
			 mh = null;
		 }
		 else {
			 MessageNormal mn = (MessageNormal)m;
			 switch (mn.getType()) {
			 	case PeerProtocol.KEEP_ALIVE:
			 		//System.out.println("Receive KEEP_ALIVE Message");
			 		break;
			 	case PeerProtocol.CHOKE:
			 		//System.out.println("Receive CHOKE Message");
			 		this.peer.setChoking(true);			 		
			 		break;
			 	case PeerProtocol.UNCHOKE:
			 		
			 		this.peer.setChoking(false);
			 		if (this.downloadPiece == null) {
			 			//System.out.println("UNCHOKE Message->DownloadReady");
			 		
			 			this.changeState(DownloadState.DOWNLOAD_READY);
			 		}
			 		else { 
			 			//System.out.println("UNCHOKE Message->Downloading");
			 			this.changeState(DownloadState.DOWNLOADING);
			 		}
			 		break;
			 	case PeerProtocol.INTERESTED: //The peer think I am interesting
			 		//System.out.println("Receive INTERESTED Message");
			 		this.peer.setInterested(true);
			 		break;
			 	case PeerProtocol.NOT_INTERESTED:
			 		//System.out.println("Receive NOT_INTERESTED Message");
			 		this.peer.setInterested(false);
			 		break;
			 	case PeerProtocol.HAVE:
			 		//System.out.println("Receive HAVE Message");
			 		this.peer.setHasPiece(Utils.byteArrayToInt(mn.getPayload()), true);
			 		this.informPeerHasPiece(this.peer.toString(), this.peer.getHasPiece());
			 		break;
			 	case PeerProtocol.BITFIELD:
			 		//System.out.println("Receive BITFIELD Message");
			 		this.peer.setHasPiece(mn.getPayload());
			 		this.informPeerHasPiece(this.peer.toString(), this.peer.getHasPiece());
			 		this.changeState(DownloadState.WAIT_UNCHOKE);
			 		break;
			 	case PeerProtocol.REQUEST:
			 		//System.out.println("Receive REQUEST Message");
			 		if (this.peer.isChoked()) {
			 			this.informTaskComplete(this.peer.toString(), DownloadState.BAD_MESSAGE);			 			
			 		}
			 		else {
			 			//request: <len=0013><id=6><index><begin><length>
			 			int requestIndex  = Utils.byteArrayToInt(Utils.makeArray(mn.getPayload(), 0, 4));
			 			int requestBegin  = Utils.byteArrayToInt(Utils.makeArray(mn.getPayload(), 4, 4));
			 			int requestLength = Utils.byteArrayToInt(Utils.makeArray(mn.getPayload(), 8, 4));
			 			this.informPeerRequest(peer.toString(), requestIndex, requestBegin, requestLength);
			 		}
			 		break;
			 	case PeerProtocol.PIECE:
			 		//System.out.println("Receive PIECE Message");
			 		//piece: <len=0009+X><id=7><index><begin><block>
			 		int pieceBegin = Utils.byteArrayToInt(Utils.makeArray(mn.getPayload(), 4, 4));
			 		byte[] pieceData = Utils.makeArray(mn.getPayload(), 8, mn.getPayload().length - 8);
			 		try {
			 			this.downloadPiece.setBlock(pieceBegin, pieceData);
			 		}
			 		catch (NullPointerException e) {
			 			
			 		}
			 		this.peer.setDownloadRate(pieceData.length);
			 		//Note that DownloadTask is only referred to one piece
			 		this.pendingRequest.remove(new Integer(pieceBegin));
			 		this.changeState(DownloadState.DOWNLOADING);
			 		break;
			 	case PeerProtocol.CANCEL:
			 		break;
			 	case PeerProtocol.PORT:
			 		break;
			 }
			 mn = null;
		 }
		 m = null;
	}
	public void keepAliveSent() {
		if (System.currentTimeMillis() - this.lastMessageTime > DownloadState.PEER_TIMEOUT) {
			this.clearPiece();
			this.informTaskComplete(this.peer.toString(), DownloadState.TIMEOUT);
			return;
		}
		this.informPeerReady(this.peer.toString());
		
	}
	public void connectionClosed() {
		this.clearPiece();
		this.informTaskComplete(this.peer.toString(), DownloadState.CONNECTION_REFUSED);
		
	}
	
	public synchronized void clearPiece() {
		if (downloadPiece != null) {
			this.informPieceRequest(downloadPiece.getIndex(), false);
			downloadPiece = null;
		}
	}
	
	public synchronized void requestPiece(Piece p) {
		/*if (p == null) {	
			System.err.println("Piece Null Error.");
		}*/
		synchronized (this) {
			this.downloadPiece = p;
			if (this.state == DownloadState.DOWNLOAD_READY) 
				this.changeState(DownloadState.DOWNLOADING);
		}
		
	}
	

	public synchronized void addDownloadListener(DownloadListener listener) { //This should be synchronized since multiple downloadtask may happen on a single piece
		listeners.add(DownloadListener.class, listener);
	}
	
	public synchronized void removeDownloadListener(DownloadListener listener) {
		listeners.remove(DownloadListener.class, listener);
	}
	
	public synchronized DownloadListener[] getDownloadListener() {
		return listeners.getListeners(DownloadListener.class);
	}
	public synchronized void informAddTask(String peerID, DownloadTask dt) {
		for (DownloadListener listener : getDownloadListener()) {
			listener.addTask(peerID, dt);
		}
			
	}
	
	public synchronized void informTaskComplete(String peerID, int reason) {
		this.stopThread();
		for (DownloadListener listener : getDownloadListener()) {
			listener.taskComplete(peerID, reason);
		}
	}
	
	//inform the Download Manager that the piece is requested or not
	//When the piece finish downloading, the piece request is cancelled.
	public synchronized void informPieceRequest(int piece, boolean requested) {
		for (DownloadListener listener : getDownloadListener()) {
			listener.pieceRequested(piece, requested);
		}
	}
	
	public synchronized void informPieceComplete(int piece, boolean complete) {
		for (DownloadListener listener : getDownloadListener()) {
			listener.pieceCompleted(this.peer.toString(), piece, complete);
		}
	}
	
	public synchronized void informPeerReady(String peerID) {
		for (DownloadListener listener : getDownloadListener())
			listener.peerReady(peerID);
	}
	
	public synchronized void informPeerHasPiece(String peerID, BitSet hasPiece) {
		for (DownloadListener listener : getDownloadListener())
			listener.peerHasPiece(peerID, hasPiece);
	}
	
	public synchronized void informPeerRequest(String peerID, int piece, int begin, int length) {
		for (DownloadListener listener : getDownloadListener()) {
			listener.peerRequest(peerID, piece, begin, length);
		}
	}
	
	private synchronized void changeState(int newState) {
		int oldState = this.state;
		this.state = newState;
		switch (newState) {
			case DownloadState.WAIT_BLOCK:
			//System.out.println("ChangeStateToWaitForBlock");
			//Send more requests, because a single request needs time to response
				if (offset < downloadPiece.getLength() && this.pendingRequest.size() < 5)
					this.changeState(DownloadState.DOWNLOADING);
				break;		
			case DownloadState.DOWNLOAD_READY:
				//System.out.println("ChangeStateToDownloadReady");
				this.informPeerReady(peer.toString());
				break;
			case DownloadState.DOWNLOADING:
				//System.out.println("PendingRequestSize:"+this.pendingRequest.size());
				if (offset >= downloadPiece.getLength()) {
					if (this.pendingRequest.size() == 0) {
				
					//System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaa");
						int pieceIndex = downloadPiece.getIndex();
						offset = 0;					
						this.informPieceComplete(pieceIndex, downloadPiece.compareSha1());									
						
						this.clearPiece();
						this.changeState(DownloadState.DOWNLOAD_READY);
					}
				}
				else if (downloadPiece != null && !this.peer.isChoking()) { //Send Request to download
					//System.out.println("Peer choking");
					byte[] pieceIndex = Utils.intToByteArray(downloadPiece.getIndex());
					byte[] begin = Utils.intToByteArray(offset);
					int length = downloadPiece.getLength() - offset;
					if (length > PeerProtocol.BLOCK_SIZE)
						length = PeerProtocol.BLOCK_SIZE; 
					//A single request can no be too large
					//Download Request gives more priority					
					MessageNormal mn = new MessageNormal(PeerProtocol.REQUEST, 2, Utils.concat(pieceIndex, Utils.concat(begin, Utils.intToByteArray(length)))); 
					ms.addMessage(mn);
					//System.out.println("offset:"+offset);
					this.pendingRequest.add(new Integer(offset));
					offset += PeerProtocol.BLOCK_SIZE;
					this.changeState(DownloadState.WAIT_BLOCK);					
				}
				break;
					
			
		}
	}
	
	public synchronized void stopThread() {
		this.changeState(DownloadState.IDLE);
		this.running = false;
		synchronized (this) {
			if (this.ms != null) {
				this.ms.stopThread();
				this.ms = null;
			}
			if (this.mr != null) {
				this.mr.stopThread();
				this.ms = null;
			}
			try {
				this.peerConnection.close();
				this.is.close();
				this.os.close();
			}
			catch (Exception e) {				
				
			}
			this.peerConnection = null;
			this.notifyAll();
		}
	}
	
	
	
	
}
