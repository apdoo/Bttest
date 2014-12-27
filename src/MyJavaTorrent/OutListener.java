package MyJavaTorrent;

import java.util.EventListener;

public interface OutListener extends EventListener {
	public void connectionClosed();
	public void keepAliveSent();
}
