package MyJavaTorrent;

import java.util.EventListener;

public interface InListener extends EventListener {
	public void messageReceived(Message m);
}
