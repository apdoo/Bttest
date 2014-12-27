package MyJavaTorrent;

import java.io.*;
import java.util.*;
import java.nio.*;
import java.net.URL;


public class TorrentProcessor {
	private TorrentFile torrent;
	
    private byte[] fileInfoHash;
	
	public TorrentProcessor() {
		this.torrent = new TorrentFile();
	}
	
	public TorrentProcessor(TorrentFile torrent) {
		this.torrent = torrent;
	}
	
	public Map analyzeTorrent(String fileName) {
        System.out.println("filename:"+fileName);
        return this.analyzeTorrent(new File(fileName));
	}
	
	public Map analyzeTorrent(File f) {
		try {
        	IOManager iom = new IOManager();
            System.out.println("analyzeTorrent");
            byte[] b = iom.readBytes(f);
            System.out.println(b.length);
            System.out.println("fileInfoHash:"+iom.fileInfoHash.length);
        	System.out.println("fileInfoHash:"+Utils.hash(iom.fileInfoHash));
        	this.fileInfoHash = iom.fileInfoHash;
        	System.out.println("file:"+new String(this.fileInfoHash));
            return Decoder.decode(b);
		}
		catch (IOException e){}
		return null;
	}
	
	public TorrentFile getTorrentFile(Map m) {
		if (m == null)
			return null;
		if (m.containsKey("announce"))
			this.torrent.announceURL = new String((byte[])m.get("announce"));		
		else
			return null;
		if (m.containsKey("announce-list"))
			this.torrent.announceURLList = new ArrayList((ArrayList)m.get("announce-list"));
		if (m.containsKey("created by"))
			this.torrent.createdBy = new String((byte[])m.get("created by"));
		if (m.containsKey("comment"))
			this.torrent.comment = new String((byte[])m.get("comment"));
		if (m.containsKey("encoding"))
			this.torrent.encoding = new String((byte[])m.get("encoding"));
		if (m.containsKey("creation date"))
			this.torrent.createDate = (Long)m.get("creation date");
		if (m.containsKey("info")) {
			Map info = (Map)m.get("info");
			//this.fileInfoHash = Encoder.encode(this.fileInfoHash);			
			this.torrent.infoHashBinary = Utils.hash(this.fileInfoHash);
			this.torrent.infoHashURL = Utils.byteArrayToURLString(this.torrent.infoHashBinary);
			if (info.containsKey("name"))  //Optional
				this.torrent.saveFileName = new String((byte[])info.get("name"));
			if (info.containsKey("piece length"))
				this.torrent.pieceLength = ((Long)info.get("piece length")).intValue();
			else return null;
			if (info.containsKey("pieces")) {
				byte[] piecesHash = (byte[])info.get("pieces");
				int piecesHashLength = piecesHash.length; 
				if (piecesHashLength % 20 != 0) return null;
				for (int i = 0; i < piecesHashLength / 20; i++) {
					byte[] byteArray = new byte[20];
					int offset = i * 20;
					for (int j = offset; j < offset + 20; j++) 
						byteArray[j - offset] = piecesHash[j];
					this.torrent.pieceHashBinary.add(byteArray);
				}
			}
			else return null;
			if (!info.containsKey("files")) {
				int fileLength = ((Long)info.get("length")).intValue();
				this.torrent.fileLength.add(fileLength);
				this.torrent.totalLength = fileLength;
				this.torrent.fileName.add(new String((byte[])info.get("name")));
			}
			else {
				List filesList = (List)info.get("files");
				this.torrent.totalLength = 0;
				for (int i = 0; i < filesList.size(); i++) {
					int fileLength = ((Long)((Map)filesList.get(i)).get("length")).intValue();
					this.torrent.fileLength.add(fileLength);
					this.torrent.totalLength += fileLength;
					List path = (List)((Map)filesList.get(i)).get("path");
					String filePath = new String();
					for (int j = 0; j < path.size(); j++) 
						filePath += new String((byte[])path.get(j));
					this.torrent.fileName.add(filePath);
				}
			}					
		}
		else return null;
		return this.torrent;
	}
	public void generatePieceHashes() {
		this.generatePieceHashes(this.torrent);
	}
	public void generatePieceHashes(TorrentFile torrent) {
		//implement me
	}
	public TorrentFile getTorrent() {
		return this.torrent;
	}
	
	
	
}
