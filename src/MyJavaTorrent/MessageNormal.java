package MyJavaTorrent;

public class MessageNormal extends Message {
	private byte[] length = new byte[4];
	private byte[] messageID = new byte[1];
	private byte[] payload;
	
	//Type of the Message
	//0: keep-alive: <len=0000>
	//1: choke: <len=0001><id=0>
	//2: unchoke: <len=0001><id=1>
	//3: interested: <len=0001><id=2>
	//4: not interested: <len=0001><id=3>
	//5: have: <len=0005><id=4><piece index>
	//6: bitfield: <len=0001+X><id=5><bitfield>
	//7: request: <len=0013><id=6><index><begin><length>
	//8: piece: <len=0009+X><id=7><index><begin><block>
	//9: cancel: <len=0013><id=8><index><begin><length>
	//10: port: <len=0003><id=9><listen-port>
	
	public MessageNormal() {
		super();
	}
	
	public MessageNormal(int type) {
		this(type,0);
	}
	
	public MessageNormal(int type, int priority) {
		super(type,priority);
		this.setParameters(type);		
	}
	
	public MessageNormal(int type, byte[] payload) {
		this(type,0,payload);
	}
	
	public MessageNormal(int type, int priority, byte[] payload) {
		super(type,priority);
		this.setParameters(type, payload);
	}	
	
	public void setParameters(int type) {
		this.type = type;
        switch (type) {
        case 0:
            this.length = new byte[] {0, 0, 0, 0};
            break;
        case 1:
            this.length = new byte[] {0, 0, 0, 1};
            this.messageID[0] = 0;
            break;
        case 2:
            this.length = new byte[] {0, 0, 0, 1};
            this.messageID[0] = 1;
            break;
        case 3:
            this.length = new byte[] {0, 0, 0, 1};
            this.messageID[0] = 2;
            break;
        case 4:
            this.length = new byte[] {0, 0, 0, 1};
            this.messageID[0] = 3;
            break;
        }
	}
	
	public void setParameters(int type, byte[] payload) {
        this.type = type;
        switch (type) {
        case 5:
            this.length = new byte[] {0, 0, 0, 5};
            this.messageID[0] = 4;
            this.payload = payload;
            break;
        case 6:
            this.length = Utils.intToByteArray(1 + payload.length);
            this.messageID[0] = 5;
            this.payload = payload;
            break;
        case 7:
            this.length = new byte[] {0, 0, 0, 13};
            this.messageID[0] = 6;
            this.payload = payload;
            break;
        case 8:
            this.length = Utils.intToByteArray(1 + payload.length);
            this.messageID[0] = 7;
            this.payload = payload;
            break;
        case 9:
            this.length = new byte[] {0, 0, 0, 13};
            this.messageID[0] = 8;
            this.payload = payload;
            break;
        case 10:
            this.length = new byte[] {0, 0, 0, 3};
            this.messageID[0] = 9;
            this.payload = payload;
            break;
        }		
	}
	
	public byte[] getLength() {
		return this.length;
	}
	
	public byte[] getMessageID() {
		return this.messageID;
	}
	
	public byte[] getPayload() {
		return this.payload;
	}
	
	public void setLength(byte[] length) {
		this.length = length;
	}
	
	public void setMessageID(int id) {
		this.messageID[0] = (byte)id;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	
	public byte[] generateMessage() {
		if (this.type == 0)
			return this.length;
		if (this.type <= 4)
			return Utils.concat(this.length, this.messageID);
		return Utils.concat(this.length, Utils.concat(this.messageID,this.payload));
	}
}
