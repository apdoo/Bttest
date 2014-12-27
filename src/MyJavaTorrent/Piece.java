package MyJavaTorrent;

import java.util.*;

public class Piece {
	byte[] sha1;
	
	private TreeMap<Integer, Integer> filesOffset;  //I am at which file?  I am at what position of the file?
	private TreeMap<Integer, byte[]> pieceBlock;
	//Note that because we need the map to be continuous, 
	//so using TreeMap not HashMap
	private int index;
	private int length;
	public Piece(int index, int length, byte[] sha1, TreeMap<Integer, Integer> tm) {
		this.index = index;
		this.length = length;
		this.sha1 = sha1;
		this.pieceBlock = new TreeMap<Integer, byte[]>();
		if (tm == null)
			this.filesOffset = new TreeMap<Integer, Integer>();
		else
			this.filesOffset = tm;		
	}
	
	public synchronized int getIndex() {
		return this.index;
	}
	
	public synchronized int getLength() {
		return this.length;
	}
	
	public TreeMap getFilesOffset() {
		return this.filesOffset;
	}
	
	public synchronized void setBlock(int offset, byte[] data) {
		this.pieceBlock.put(offset, data);
	}
	
	public void clearData() {
		this.pieceBlock.clear();
	}
	
    public synchronized byte[] data(){
        byte[] pieceData = new byte[0];
        for(Iterator it = this.pieceBlock.keySet().iterator(); it.hasNext();)
            pieceData = Utils.concat(pieceData, this.pieceBlock.get(it.next()));
        return pieceData;
    }

    public synchronized boolean compareSha1(){
    	return Utils.bytesCompare(Utils.hash(this.data()), this.sha1);
    	//return Utils.byteArrayToByteString(Utils.hash(this.data())).matches(Utils.byteArrayToByteString(this.sha1));    	
    }
}
