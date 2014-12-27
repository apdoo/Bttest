package MyJavaTorrent;

import java.util.*;

public interface PeerUpdateListener extends EventListener {
	public void updatePeerList(LinkedHashMap peerList);
	public void updateFailed(int error, String message);
}
