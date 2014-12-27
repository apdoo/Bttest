package MyJavaTorrent;

import java.util.*;
import java.io.*;
import java.net.*;

public class DownloadManager implements ConnectionListenerInterface, DownloadListener, PeerUpdateListener {
	
	private TorrentFile torrent = null;
	private byte[] clientID;
	private int fileNumber = 0;
	private int pieceNumber;
	private long length = 0;  //Total length of the download files
	private long left = 0;
	private Piece[] pieceList;
	private BitSet completePiece;
	private BitSet requestPiece;
	
	private RandomAccessFile[] outputFile;
	private PeerUpdater pu = null;
	private ConnectionListener cl = null;
	
	private List unchokeList = new LinkedList();
	private LinkedHashMap<String, Peer> peerList = null;
	private LinkedHashMap<String, BitSet> peerHasPiece = null;
	private TreeMap<String, DownloadTask> fileTask = null;
	
	private LinkedHashMap unchokeMap = new LinkedHashMap<String, Integer>();
	//private long lastTrackerContact = 0;
	private long lastUnchoking = 0;
	private int optimisticUnchoke = 0;
	
	
	private final int interval = 10;
	
	public DownloadManager(TorrentFile torrent, byte[] clientID, String savePath) {		
		this.torrent = torrent;
		this.clientID = clientID;
		this.peerList = new LinkedHashMap<String, Peer>();
		this.fileTask = new TreeMap<String, DownloadTask>();
		this.peerHasPiece = new LinkedHashMap<String, BitSet>();
		this.pieceNumber = torrent.pieceHashBinary.size();
		this.pieceList = new Piece[pieceNumber];
		this.fileNumber = this.torrent.fileLength.size();
		this.completePiece = new BitSet(pieceNumber);
		this.requestPiece = new BitSet(pieceNumber);
		this.outputFile = new RandomAccessFile[this.fileNumber];		
		this.length = this.torrent.totalLength;
		this.left = this.length;
		this.checkTempFiles(savePath);
		int file = 0;
		int fileOffset = 0;
		for (int i = 0; i < this.pieceNumber; i++) {
			TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
			int pieceOffset = 0;
			do {
				tm.put(file, fileOffset);
				if (fileOffset + this.torrent.pieceLength - pieceOffset 
						>= (Integer)(torrent.fileLength.get(file)) 
						&& i != this.pieceNumber - 1) {
					pieceOffset += ((Integer)(torrent.fileLength.get(file))).intValue() - fileOffset;
					file++;
					fileOffset = 0;
					if (pieceOffset == this.torrent.pieceLength)
						break;
				}
				else {
					fileOffset += this.torrent.pieceLength - pieceOffset;
					break;
				}			
			}
			while (true);
			int pl = this.torrent.pieceLength;
			if (i == this.pieceNumber - 1) {
				pl = ((Long)(this.length % this.torrent.pieceLength)).intValue();
			}
			pieceList[i] = new Piece(i, pl, (byte[])torrent.pieceHashBinary.get(i), tm);
			//If the piece is complete then no need to download again
			//Note that the piece has already been in the file!
			//Those piece that are not downloaded completely should be downloaded again
			if (this.testPieceComplete(i)) {
				this.setPieceComplete(i, true);
				this.left -= this.pieceList[i].getLength();
			}
		}
		this.lastUnchoking = System.currentTimeMillis();
	}
	
	public boolean startListening(int minPort, int maxPort) {
		this.cl = new ConnectionListener();
		if (this.cl.connect(minPort, maxPort)) {
			this.cl.addConnectionListenerInterface(this);
			return true;
		}
		else {
			System.err.println("Can not start listening port.");
			return false;	
		}			
	}
	
	public void startTrackerUpdate() {
		this.pu = new PeerUpdater(this.clientID, this.torrent);
		this.pu.addPeerUpdateListener(this);
		this.pu.setListeningPort(this.cl.getConnectedPort());
		this.pu.setLeft(this.left);
		this.pu.start();
	}
	
	public void blockUntilComplete() {
		byte[] b = new byte[0];
		while (true) {
			try {
				synchronized(b) {
					b.wait(interval * 1000);
					this.unchokePeers(); //avoid fabiriation
					b.notifyAll();
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			if (this.isComplete()) {
				System.out.println("Download Complete, Sharing..., Ctrl+C to terminate.");
			}
		}
	}
	public void stopTrackerUpdate() {
		this.pu.stopConnectTracker();
	}
	
	public void closeTempFiles() {
		for (int i = 0; i < this.outputFile.length; i++) {
			try {
				this.outputFile[i].close();
			}
			catch (Exception e) {
				System.err.println("Close Temp File Error.");
				e.printStackTrace();
			}
		}
	}
	
	public synchronized int checkTempFiles(String savePath) {
		String sp = savePath;
		if (this.fileNumber > 1)
			sp += this.torrent.saveFileName + "/";
		new File(sp).mkdirs();
		for (int i = 0; i < this.fileNumber; i++) {
			File f = new File(sp + ((String)(this.torrent.fileName.get(i))));
			try {
				this.outputFile[i] = new RandomAccessFile(f,"rw");
				this.outputFile[i].setLength((Integer)this.torrent.fileLength.get(i));
			}
			catch (IOException e) {
				System.err.println("Create temp file failed.");
				e.printStackTrace();				
			}			
		}	
		
		return 0;
	}
	
	public boolean testPieceComplete(int piece) {
		boolean c = false;
		this.pieceList[piece].setBlock(0, this.getPieceFromFiles(piece));
		c = this.pieceList[piece].compareSha1();
		this.pieceList[piece].clearData();
		return c;		
	}
	
	public synchronized byte[] getPieceFromFiles(int piece) {
		byte[] data = new byte[this.pieceList[piece].getLength()];
		int leftData = data.length;
		for (Iterator it = this.pieceList[piece].getFilesOffset().keySet().iterator(); it.hasNext();) {
			try {
				Integer file = (Integer)(it.next());
				//Get the remaining length from the current position to the end of the file.
				//This is only considered in the last piece of the file
				int l = ((Integer)this.torrent.fileLength.get(file.intValue())).intValue()
				      - ((Integer)(this.pieceList[piece].getFilesOffset().get(file))).intValue();
				//Seek the current position in the outputfile
				this.outputFile[file.intValue()].seek(((Integer)(this.pieceList[piece].getFilesOffset().get(file))).intValue());
				if (l < leftData) 
					this.outputFile[file.intValue()].read(data, data.length - leftData, l);
				else
					this.outputFile[file.intValue()].read(data, data.length - leftData, leftData);
				//Only in the piece that cross two files will cause leftData
				//to still be bigger than l.  Generally, the for loops only one time 
				leftData -= l;
			}
			catch (IOException e) {
				System.err.println("DownloadManager.getPieceFromFiles error.");
			}
		}
		return data;
	}
	
	public synchronized void savePiece(int piece) {
		byte[] data = this.pieceList[piece].data();
		int leftData = data.length;
		for (Iterator it = this.pieceList[piece].getFilesOffset().keySet().iterator(); it.hasNext();) {
			try {
				Integer file = (Integer)(it.next());
				int l = ((Integer)this.torrent.fileLength.get(file.intValue())).intValue()
			      - ((Integer)(this.pieceList[piece].getFilesOffset().get(file))).intValue();
				this.outputFile[file.intValue()].seek(((Integer)(this.pieceList[piece].getFilesOffset().get(file))).intValue());
				if (l < leftData) 
					this.outputFile[file.intValue()].write(data, data.length - leftData, l);
				else
					this.outputFile[file.intValue()].write(data, data.length - leftData, leftData);
				leftData -= l;				
			}
			catch (IOException e) {
				System.err.println("DownloadManager.savePiece error.");
			}
		}
		data = null;
		this.pieceList[piece].clearData();		
	}
	
	public synchronized byte[] getPieceBlock(int piece, int begin, int length) {
		//Get a segment in the piece
		return Utils.makeArray(this.getPieceFromFiles(piece), begin, length);
	}
	
	private synchronized void unchokePeers() {
		synchronized (this.fileTask) {
			int notInterested = 0;
			int downloaders = 0;
			int choked = 0;
			this.unchokeMap.clear();
			//peerList saves the peer information, gives it to List<Peer>
			List<Peer> lp = new LinkedList<Peer>(this.peerList.values());
			if (!this.isComplete())
				//Sort the Peer by comparing download rate			
				Collections.sort(lp, new DownloadRateComparator());
			else
				Collections.sort(lp, new UploadRateComparator());
			for (Iterator it = lp.iterator(); it.hasNext();) {
				Peer p = (Peer)it.next();
				if (p.getDownloadRate(false) > 0)
					System.out.println(p + " rate: " + p.getDownloadRate(true) / (1024 * interval) + "Kb/s");
				DownloadTask dt = this.fileTask.get(p.toString());
				if (dt != null && downloaders < 5) {  
					//Peer that is not interested, or have higher download rate get unchoke
					if (!p.isInterested()) {
						this.unchokeMap.put(p.toString(), p);
						if (p.isChoked())
							dt.ms.addMessage(new MessageNormal(PeerProtocol.UNCHOKE));
						p.setChoked(false);
						while(this.unchokeList.remove(p));
						notInterested++;
					}
					else if (p.isChoked()) {
						this.unchokeMap.put(p.toString(), p);
						dt.ms.addMessage(new MessageNormal(PeerProtocol.UNCHOKE));
						p.setChoked(false);
						while(this.unchokeList.remove(p));
						downloaders++;
					}
				}
				else {
					if (!p.isChoked()) {
						dt.ms.addMessage(new MessageNormal(PeerProtocol.CHOKE));
						p.setChoked(true);
					}
					if (!this.unchokeList.contains(p))
						this.unchokeList.add(p);
					choked++;
				}
				p = null;
				dt = null;
			}
		}
		this.lastUnchoking = System.currentTimeMillis();
		if (optimisticUnchoke == 3) {
			this.optimisticUnchoke();
			optimisticUnchoke = 0;
		}
		else optimisticUnchoke++;
	}
	
	public synchronized void optimisticUnchoke() {
		if (!this.unchokeList.isEmpty()) {
			Peer p = null;
			do {
				p = (Peer)this.unchokeList.remove(0);
				synchronized (this.fileTask) {
					DownloadTask dt = this.fileTask.get(p.toString());
					if (dt != null) {
						dt.ms.addMessage(new MessageNormal(PeerProtocol.UNCHOKE));
						p.setChoked(false);
						this.unchokeMap.put(p.toString(), p);
						System.out.println(p + " optimistically unchoked.");
					}
					else p = null;
					dt = null;
				}
			}
			while ((p == null) && (!this.unchokeList.isEmpty()));
			p = null;			
		}
	}
	                                             
	public synchronized void connectionAccepted(Socket s) {
		synchronized (this.fileTask) {
			String peerID = s.getInetAddress().getHostAddress() + ":" + s.getPort();
			if (!this.fileTask.containsKey(peerID)) {
				//**
				Peer p = new Peer();
				p.setID(peerID);
				p.setIP(s.getInetAddress().getHostAddress());
				p.setPort(s.getPort());
				//p = null;
				//Since connection accepted, handshake has already done, so initiate = false
				DownloadTask dt = new DownloadTask(p, this.torrent.infoHashBinary,this.clientID,false,this.getBitField(),s);
				dt.addDownloadListener(this);
				this.fileTask.put(dt.getPeer().toString(), dt);
				this.peerList.put(dt.getPeer().toString(), dt.getPeer());
				dt.start();
			}
		}
	}
	
	public synchronized void addTask(String peerID, DownloadTask dt) {
		synchronized (this.fileTask) {
			this.fileTask.put(peerID, dt);
		}
	}
	
	public synchronized void peerRequest(String peerID, int piece, int begin, int length) {
		if (this.isPieceComplete(piece)) { //Can upload
			DownloadTask dt = this.fileTask.get(peerID);
			if (dt != null) {
				dt.ms.addMessage(new MessageNormal(PeerProtocol.PIECE, Utils.concat(Utils.intToByteArray(piece), Utils.concat(Utils.intToByteArray(begin), this.getPieceBlock(piece, begin, length)))));
				dt.peer.setUploadRate(length);
			}
			dt = null;
			this.pu.updateParameters(0, length, "");
		}
		else {
			try {
				this.fileTask.get(peerID).stopThread();
			}
			catch (Exception e) {
				
			}
			this.fileTask.remove(peerID);
			this.peerList.remove(peerID);
			this.unchokeMap.remove(peerID);			
		}
	}
	
	public synchronized void peerReady(String peerID) {
		if (System.currentTimeMillis() - this.lastUnchoking > interval * 1000) 
			this.unchokePeers();
		int pieceToRequest = this.choosePiece(peerID);
		if (pieceToRequest != -1) {
			this.fileTask.get(peerID).requestPiece(this.pieceList[pieceToRequest]);
		}
	}
	
	
	
	private synchronized int choosePiece(String peerID) {
		synchronized (this.completePiece) {
			int index = 0;
			ArrayList<Integer> pieceToRequest = new ArrayList<Integer>(this.pieceNumber);
			for (int i = 0; i < this.pieceNumber; i++) {
				//Note that if there is only a few pieces left, then the request will be sent together
				if ((!this.isPieceRequested(i) || (this.completePiece.cardinality() >= this.pieceNumber - 5))
						&& (!this.isPieceComplete(i)) && this.peerHasPiece.get(peerID) != null) {
					if (this.peerHasPiece.get(peerID).get(i))
						pieceToRequest.add(i);
				}				
			}
			if (pieceToRequest.size() > 0) {
				Random r = new Random(System.currentTimeMillis());
				index = pieceToRequest.get(r.nextInt(pieceToRequest.size()));
				this.setPieceRequested(index, true);
				return index;
			}
			return -1;
		}
	}
	public synchronized void peerHasPiece(String peerID, BitSet hasPiece) {
		this.peerHasPiece.put(peerID, hasPiece);
		//Get the Bitset that I do not have but the peer has 
		BitSet interest = (BitSet)(hasPiece.clone());
		interest.andNot(this.completePiece);		
		DownloadTask dt = this.fileTask.get(peerID);
		if (dt != null) {
			if (interest.cardinality() > 0 && !dt.peer.isInteresting()) {
				dt.ms.addMessage(new MessageNormal(PeerProtocol.INTERESTED,2));
				dt.peer.setInteresting(true);
			}
		}
		dt = null;
	}

	public synchronized void taskComplete(String peerID, int reason) {
		switch (reason) {
		case DownloadState.BAD_MESSAGE:			
			break;
		case DownloadState.BAD_HANDSHAKE:
			break;
		case DownloadState.CONNECTION_REFUSED:
			break;
		case DownloadState.UNKNOWN_HOST:
			break;
		
		}
		this.peerHasPiece.remove(peerID);
		this.fileTask.remove(peerID);
		this.peerList.remove(peerID);
		
	}
	public synchronized void pieceRequested(int piece, boolean requested) {
		this.setPieceRequested(piece, requested);
	}
	public synchronized void pieceCompleted(String peerID, int piece, boolean complete) {
		synchronized (this.requestPiece) {
			this.requestPiece.clear(piece);
		}
		synchronized (this.completePiece) {
			if (complete && !this.isPieceComplete(piece)) {
				//System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaa");
				pu.updateParameters(this.torrent.pieceLength, 0, "");
				this.completePiece.set(piece, complete);
				float process = (float)(100.0) * (float)(this.completePiece.cardinality()) / (float)(this.pieceNumber);
				for (Iterator it = this.fileTask.keySet().iterator(); it.hasNext();) 
					try {
						this.fileTask.get(it.next()).ms.addMessage(new MessageNormal(PeerProtocol.HAVE, 1, Utils.intToByteArray(piece)));
					}
					catch (NullPointerException e){
						
					}
				System.out.println("Piece Completed By " + peerID + " : " + piece + " (Total percent = " + process + "% )");
				this.savePiece(piece);					
				
			}
			else if (!complete) {
				System.out.println("Piece " + piece + " Sha1 hash does not match");
			}
			if (this.completePiece.cardinality() == this.pieceNumber) {
				System.out.println("Download Completed!");
				this.notify();
			}
		}
	}
	

	public synchronized void updatePeerList(LinkedHashMap list) {
		synchronized (this.fileTask) {
			Set keyset = list.keySet();
			for (Iterator it = keyset.iterator(); it.hasNext();) {
				String key = (String)it.next();
				if (!this.fileTask.containsKey(key)) {
					Peer p = (Peer)list.get(key);
					this.peerList.put(p.toString(), p);
					DownloadTask dt = new DownloadTask(p, this.torrent.infoHashBinary, this.clientID, true, this.getBitField());
					dt.addDownloadListener(this);
					dt.start();
				}
			}
		}
		System.out.println("Peer List updated, found " + list.size() + " peers.");
	}
	
	public void updateFailed(int error, String message) {
		System.err.println(message);
		//System.err.flush();
	}
	
	
	
	public synchronized void setPieceComplete(int piece, boolean complete) {
		synchronized (this.completePiece) {
			this.completePiece.set(piece, complete);
		}
	}
	
	public synchronized void setPieceRequested(int piece, boolean request) {
		synchronized (this.requestPiece) {
			this.requestPiece.set(piece, request);
		}
	}

	
	public synchronized boolean isComplete() {
		synchronized (this.completePiece) {
			return (this.completePiece.cardinality() == this.pieceNumber);
			//cardinality: returns the number of bits set to true
		}
	}
	
	public synchronized boolean isPieceComplete(int piece) {
		synchronized (this.completePiece) {
			return this.completePiece.get(piece);
		}
	}
	
	public synchronized boolean isPieceRequested(int piece) {
		synchronized (this.requestPiece) {
			return this.requestPiece.get(piece);
		}
	}
	
    public byte[] getBitField() {
        int l = (int) Math.ceil((double)this.pieceNumber / 8.0);
        byte[] bitfield = new byte[l];
        for (int i = 0; i < this.pieceNumber; i++)
            if (this.completePiece.get(i)) {
                bitfield[i / 8] |= 1 << (7 - i % 8);
            }
        return bitfield;
    }
	
}
