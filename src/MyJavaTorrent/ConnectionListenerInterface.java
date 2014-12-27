package MyJavaTorrent;

import java.util.EventListener;
import java.net.Socket;

public interface ConnectionListenerInterface extends EventListener {
	public void connectionAccepted(Socket s);

}
