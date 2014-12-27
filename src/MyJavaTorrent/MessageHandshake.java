package MyJavaTorrent;

public class MessageHandshake extends Message {
	private byte[] pstrlen = new byte[1];
	private byte[] protocol = new byte[19];
	private byte[] reserved = new byte[8];
	private byte[] fileInfoHash = new byte[20];
	private byte[] peerID = new byte[20];
	
	public MessageHandshake() {
		super(-1,0);
	}
	
	public MessageHandshake(byte[] fileInfoHash, byte[] peerID) {
		super(-1,0);
		this.pstrlen = new byte[]{19};
		this.protocol = "BitTorrent protocol".getBytes();
		this.reserved = new byte[]{0,0,0,0,0,0,0,0};
		this.fileInfoHash = fileInfoHash;
		this.peerID = peerID;
	}
	
	/*public MessageHandshake(byte[] pstrlen, byte[] protocol, byte[] reserved, byte[] fileInfohash, byte[] peerID) {
		super(-1,0);
		this.pstrlen = pstrlen;
		this.protocol = protocol;
		this.reserved = reserved;
		this.fileInfoHash = fileInfoHash;
		this.peerID = peerID;
	}*/
	
	public byte getLength() {
		return pstrlen[0];
	}
	
	public byte[] getProtocol() {
		return this.protocol;
	}
	
	public byte[] getReserved() {
		return this.reserved;
	}
	
	public byte[] getFileInfoHash() {
		return this.fileInfoHash;		
	}
	
	public byte[] getPeerID() {
		return this.peerID;
	}
	
	public void setParameters(byte[] pstrlen, byte[] protocol, byte[] reserved, byte[] fileInfoHash, byte[] peerID) {
		this.pstrlen = pstrlen;
		this.protocol = protocol;
		this.reserved = reserved;
		this.fileInfoHash = fileInfoHash;
		this.peerID = peerID;
	}	
	
	public byte[] generateMessage() {
		return Utils.concat(this.pstrlen, Utils.concat(this.protocol, Utils.concat(this.reserved, Utils.concat(this.fileInfoHash, this.peerID))));
	}
}
