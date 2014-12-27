package MyJavaTorrent;

import java.util.*;

public class TorrentFile {
	public String announceURL;	
	public String createdBy;
	public String comment;
	public String encoding;
	public String saveFileName;
	public long createDate;
	public int pieceLength;
	public ArrayList announceURLList;
	
	public ArrayList fileName;
	public ArrayList fileLength;
	public long totalLength;
	public byte[] infoHashBinary;	
	public String infoHashURL;
	
	public ArrayList pieceHashBinary;
	public ArrayList pieceHashHex;
	public ArrayList pieceHashURL;
	
	public TorrentFile() {
		super();
		announceURL = new String();
		createdBy = new String();
		comment = new String();
		encoding = new String();
		saveFileName = new String();
		createDate = -1;
		pieceLength = -1;
		announceURLList = new ArrayList();
		
		fileName = new ArrayList(); //May have multiple files
		fileLength = new ArrayList();
		totalLength = -1;
		infoHashBinary = new byte[20];
		infoHashURL = new String();
		
		
		pieceHashBinary = new ArrayList();
		pieceHashURL = new ArrayList();
		pieceHashHex = new ArrayList();
		
	}

    @Override
    public String toString() {
        return "TorrentFile{" +
                "announceURL='" + announceURL + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", comment='" + comment + '\'' +
                ", encoding='" + encoding + '\'' +
                ", saveFileName='" + saveFileName + '\'' +
                ", createDate=" + createDate +
                ", pieceLength=" + pieceLength +
                ", announceURLList=" + announceURLList +
                ", fileName=" + fileName +
                ", fileLength=" + fileLength +
                ", totalLength=" + totalLength +
                ", infoHashBinary=" + infoHashBinary +
                ", infoHashURL='" + infoHashURL + '\'' +
                ", pieceHashBinary=" + pieceHashBinary +
                ", pieceHashHex=" + pieceHashHex +
                ", pieceHashURL=" + pieceHashURL +
                '}';
    }
}
