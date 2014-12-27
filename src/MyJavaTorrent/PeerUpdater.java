package MyJavaTorrent;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.event.*;

public class PeerUpdater extends Thread {
	private LinkedHashMap<String,Peer> peerList;
	private TorrentFile torrent;
	private byte[] id;
	
	private long downloaded = 0;
	private long uploaded = 0;
	private long left = 0;
	private int listeningPort = 6881;
	private int key = 2160; //This key can be random 
		
	private int interval = 150;
	private int minInterval = 0;
	
	private boolean firstConnect = true;
	private boolean finishConnect = false;
	
	public String event = "&event=started"; //The initial state is start
	private EventListenerList listeners = new EventListenerList();
	

	public PeerUpdater(byte[] id, TorrentFile torrent) {
		this.id = id;
		this.torrent = torrent;
		this.left = torrent.totalLength;
		peerList = new LinkedHashMap();
		this.setDaemon(true);
	}
	
	public int getInterval() {
		return this.interval;
	}
	
	public int getMinInterval() {
		return this.minInterval;
	}
	
	public long getDownloaded() {
		return this.downloaded;
	}
	
	public long getUploaded() {
		return this.uploaded;
	}
	
	public long getLeft() {
		return this.left;
	}
	
	public String getEvent() {
		return this.event;
	}
	
	public LinkedHashMap getpeerList() {
		return this.peerList;
	}
	
	public void setListeningPort(int listeningPort) {
		this.listeningPort = listeningPort;
	}
	
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	public void setMinInterval(int minInterval) {
		this.minInterval = minInterval;
	}
	
	public void setDownloaded(long downloaded) {
		this.downloaded = downloaded;
	}
	
	public void setUploaded(long uploaded) {
		this.uploaded = uploaded;
	}
	
	public void setLeft(long left) {
		this.left = left;
	}
	
	public void setEvent(String event) {
		this.event = event;
	}
	
	public void addPeerUpdateListener(PeerUpdateListener pul) {
		listeners.add(PeerUpdateListener.class, pul);
	}
	
	public void removePeerUpdateListener(PeerUpdateListener pul) {
		listeners.remove(PeerUpdateListener.class, pul);
	}
	
	public PeerUpdateListener[] getPeerUpdateListeners() {
		return listeners.getListeners(PeerUpdateListener.class);
	}
	
	
	public synchronized void updateParameters(int downloaded, int uploaded, String event) {
		synchronized(this) {
			this.downloaded += downloaded;
			this.uploaded += uploaded;
			this.left -= downloaded;
			this.event = event;
		}
	}
	
	public synchronized Map connectTracker(byte[] id, TorrentFile t, long downloaded, long uploaded, long left, String event) {
		try {
			String s = t.announceURL + "?info_hash="
			         + t.infoHashURL + "&peer_id="
			         + Utils.byteArrayToURLString(id) + "&port="
			         + this.listeningPort + "&downloaded="
			         + downloaded + "&uploaded="
			         + uploaded + "&left="
			         + left + "&key="
			         + key + "&numwant=100&compact=1"
			         + event;
			URL connectURL = new URL(s);
			System.out.println("Connect Tracker.  URL = " + s);
			URLConnection uc = connectURL.openConnection();
			InputStream is = uc.getInputStream();
			BufferedInputStream bis = new BufferedInputStream(is);
			Map m = Decoder.decode(bis);
			System.out.println(m);
			bis.close();
			is.close();
			return m;
		}
		catch (MalformedURLException mue){
			this.informUpdateError(2, "Tracker URL is not valid.");			
		}	
		catch (UnknownHostException uhe) {
			this.informUpdateError(3, "Tracker not available, Retrying...");		
		}
		catch (IOException ioe) {
			this.informUpdateError(4, "Can not connect Tracker, Retrying...");
		}
		catch (Exception e) {
			this.informUpdateError(5, "Internal Error");
		}
		return null;
	}
	
	public void stopConnectTracker() {
		this.event = "&event=stopped";
		this.finishConnect = true;
		this.connectTracker(this.id, this.torrent, this.downloaded, this.uploaded, this.left, this.event);
	}
	
	public synchronized LinkedHashMap<String, Peer> processResponse(Map m) {
		
		if (m == null)
			return null;
		if (m.containsKey("failure reason")) {
			String errorMessage = "Tracker returns the following error message: ";
			errorMessage += new String((byte[])m.get("failure reason"));
			this.informUpdateError(1, errorMessage);
			return null;
		}
		if (m.containsKey("warning message")) {
			String warningMessage = "Tracker returns the following warning message: ";
			warningMessage += new String((byte[])m.get("warning message"));
			System.out.println(warningMessage);
		}
		int trackerInterval = ((Long)m.get("interval")).intValue();
		if (trackerInterval < this.interval) 
			this.interval = trackerInterval;
		else
			this.interval *= 2;
		Object peers = m.get("peers");
		ArrayList peerList = new ArrayList();
		LinkedHashMap<String, Peer> lhm = new LinkedHashMap();
		if (peers instanceof List) {
			peerList.addAll((List)peers);
			if (peerList != null && peerList.size() > 0) {
				for (int i = 0; i < peerList.size(); i++) {
					String peerID = new String((byte[])((Map)(peerList.get(i))).get("peer_id"));
					String ipAddress = new String((byte[])((Map)(peerList.get(i))).get("ip"));
					int port = ((Long)((Map)(peerList.get(i))).get("port")).intValue();
					Peer p = new Peer(peerID, ipAddress, port);
					lhm.put(p.toString(), p);
				}
			}
		}
		else if (peers instanceof byte[]) {
            byte[] p = ((byte[]) peers);
            for (int i = 0; i < p.length; i += 6) {
                Peer peer = new Peer();
                peer.setIP(Utils.byteToUnsignedInt(p[i]) + "." +
                           Utils.byteToUnsignedInt(p[i + 1]) + "." +
                           Utils.byteToUnsignedInt(p[i + 2]) + "." +
                           Utils.byteToUnsignedInt(p[i + 3]));
                peer.setPort(Utils.byteArrayToInt(Utils.makeArray(p,
                        i + 4, 2)));
                lhm.put(peer.toString(), peer);
            }
		}
		//System.out.println("lhmsize:"+lhm.size());
		return lhm;
	}
	
	public void run() {
		int tryTimes = 0;
		byte[] b = new byte[0];
		while (!finishConnect) {
			tryTimes++;
			System.out.println("New Connection Try: "+tryTimes);
			peerList = processResponse(connectTracker(id, torrent, downloaded, uploaded, left, event));
			if (peerList != null) {
				if (firstConnect) {
					event = "";
					firstConnect = false;
				}
				tryTimes = 0;
				//System.out.println("peerListsize:"+peerList.size());
				informUpdatePeerList(peerList);
				try {
					synchronized(b) {
						b.wait(interval * 1000);
					}
				}
				catch (InterruptedException e) {
					
				}				
			}
			else {
				try {
					synchronized(b) {
						b.wait(1500);
					}					
				}
				catch (InterruptedException e) {
					
				}
			}
		}
		
	}
	
	
	protected void informUpdatePeerList(LinkedHashMap l) {
		for (PeerUpdateListener listener : getPeerUpdateListeners()) {
			listener.updatePeerList(l);
		}
	}
	
    protected void informUpdateError(int error, String message) {
        for (PeerUpdateListener listener : getPeerUpdateListeners()) {
            listener.updateFailed(error, message);
        }
    	
    }
	
	
}
