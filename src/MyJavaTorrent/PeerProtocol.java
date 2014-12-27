package MyJavaTorrent;

public class PeerProtocol {
    public static final int HANDSHAKE = -1;
    public static final int KEEP_ALIVE = 0;
    public static final int CHOKE = 1;
    public static final int UNCHOKE = 2;
    public static final int INTERESTED = 3;
    public static final int NOT_INTERESTED = 4;
    public static final int HAVE = 5;
    public static final int BITFIELD = 6;
    public static final int REQUEST = 7;
    public static final int PIECE = 8;
    public static final int CANCEL = 9;
    public static final int PORT = 10;
    public static final String[] MESSAGE_TYPE = {"Keep_Alive", "Choke", "Unchoke",
                                        "Interested", "Not_Interested", "Have",
                                        "Bitfield", "Request", "Piece",
                                        "Cancel", "Port"};

    public static final int BLOCK_SIZE = 16384;
    public static final byte[] BLOCK_SIZE_BYTES = Utils.intToByteArray(16384);
}
