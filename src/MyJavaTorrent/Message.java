package MyJavaTorrent;

abstract public class Message {
	protected int type;
	private int priority;
	
	public Message() {
		
	}	

	public Message(int type, int priority) {
		this.type = type;
		this.priority = priority;
	}
	
	public Message(int type) {
		this.type = type;
		this.priority = 0;
	}
	
	public int getPriority() {
		return this.priority;
	}
	
	public int getType() {
		return this.type;
	}
	
	abstract public byte[] generateMessage();
}
