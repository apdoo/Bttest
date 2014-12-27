package MyJavaTorrent;

import java.util.*; 

public interface DownloadListener extends EventListener {
	public void addTask(String peerID, DownloadTask dt);
	public void peerRequest(String peerID, int piece, int begin, int length);
	public void peerReady(String peerID);
	public void peerHasPiece(String peerID, BitSet hasPiece);

	public void taskComplete(String peerID, int reason);
	public void pieceRequested(int piece, boolean requested);
	public void pieceCompleted(String peerID, int piece, boolean complete);
}
