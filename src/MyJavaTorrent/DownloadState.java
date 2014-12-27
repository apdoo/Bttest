package MyJavaTorrent;

public class DownloadState {
	public static final int IDLE = 0;
	public static final int WAIT_HANDSHAKE = 1;
	public static final int WAIT_UNCHOKE = 2;
	public static final int WAIT_DOWNLOAD = 3;
	public static final int DOWNLOAD_READY = 4;
	public static final int DOWNLOADING = 5;
	public static final int WAIT_BLOCK = 6;
	
	public static final int TASK_COMPLETED = 0;
	public static final int UNKNOWN_HOST = 1;
	public static final int BAD_HANDSHAKE = 2;
	public static final int CONNECTION_REFUSED = 3;
	public static final int BAD_MESSAGE = 4;
	public static final int TIMEOUT = 5;
	
	public static final int PEER_TIMEOUT = 240000;
	
	
}
