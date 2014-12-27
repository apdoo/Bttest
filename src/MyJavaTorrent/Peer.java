package MyJavaTorrent;

import java.util.*;

public class Peer {
	private String id;
	private String ip;
	private int port;
	
	private boolean choked = true;  //The peer is choked by me
	private boolean interested = false;	//The peer think I am interesting
	private boolean choking = true; //The peer is choking me
	private boolean interesting = false; //I think the peer is interesting
	private boolean connected = false;
	
	private BitSet hasPiece;
	private int downloaded = 0;
	private int uploaded = 0;
	private int intervalDownloaded = 0;
	private float lastDownloadTime = 0;
	private int intervalUploaded = 0;
	private float lastUploadTime = 0;
	
	public Peer() {
		this.hasPiece = new BitSet();
	}
	
	public Peer(String id, String ip, int port) {
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.lastDownloadTime = System.currentTimeMillis();
		this.lastUploadTime = System.currentTimeMillis();
		this.hasPiece = new BitSet();
	}
	
	public void resetDownload() {
		this.intervalDownloaded = 0;
		this.lastDownloadTime = System.currentTimeMillis();
	}
	
	public void resetUpload() {
		this.intervalUploaded = 0;
		this.lastUploadTime = System.currentTimeMillis();
	}
	
	public float getDownloadRate(boolean reset) {
		if (reset) {
			float currentDownloadRate = this.intervalDownloaded;
			this.intervalDownloaded = 0;
			return currentDownloadRate;
		}
		else
			return this.intervalDownloaded;
	}
	
	public float getUploadRate(boolean reset) {
		if (reset) {
			float currentUploadRate = this.intervalUploaded;
			this.intervalUploaded = 0;
			return currentUploadRate;
		}
		else 
			return this.intervalUploaded;
	}
	
	public int getDownloaded() {
		return this.downloaded;
	}
	
	public int getUploaded() {
		return this.uploaded;
	}
	

	
	public String getID() {
		return this.id;
	}
	
	public String getIP() {
		return this.ip;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public BitSet getHasPiece() {
		return this.hasPiece;
	}
	
	public void setID(String id) {
		this.id = id;
	}
	
	public void setIP(String ip) {
		this.ip = ip;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public boolean isChoked() {
		return this.choked;
	}
	
	public boolean isInterested() {
		return this.interested;
	}
	
	public boolean isChoking() {
		return this.choking;
	}
	
	public boolean isInteresting() {
		return this.interesting;
	}
	
	public void setChoked(boolean c) {
		this.choked = c;
	}
	
	public void setInterested(boolean i) {
		this.interested = i;
	}
	
	public void setChoking(boolean c) {
		this.choking = c;
	}
	
	public void setInteresting(boolean i) {
		this.interesting = i;
	}
	
	public boolean isConnected() {
		return this.connected;
	}
	
	public void setConnected(boolean c) {
		this.connected = c;
	}
	
	
	public void setHasPiece(int pieceIndex, boolean has) {
		this.hasPiece.set(pieceIndex, has);		
	}
	
    public void setHasPiece(byte[] bitfield){
        boolean[] b = Utils.byteArray2BitArray(bitfield);
        for (int i = 0; i < b.length; i++)
            this.hasPiece.set(i,b[i]);
    }
	
	public boolean equals(Peer p) {
		if (this.id == p.getID() && this.ip == p.getIP() && this.port == p.getPort())
			return true;
		else
			return false;			
	}
	
	public String toString() {
		return (this.ip + ":" + this.port);
	}
	
	public void setDownloadRate(int download) {
		this.intervalDownloaded += download;
		this.downloaded += download;
	}
	
	public void setUploadRate(int upload) {
		this.intervalUploaded += upload;
		this.uploaded += upload;
	}
}
