package MyJavaTorrent;

import java.io.*;
import java.util.*;



public class Decoder {
	public byte[] fileInfoHash;
	
	public Decoder() {
		
	}

	public static Map decode(byte[] byteArray) throws IOException {
		return new Decoder().decodeByteArray(byteArray);
	}
	public static Map decode(BufferedInputStream bis) throws IOException {
		return new Decoder().decodeBufferedInputStream(bis);
	}

	public Map decodeByteArray(byte[] byteArray) throws IOException {
		return decodeByteArrayInputStream(new ByteArrayInputStream(byteArray));
	}

	
	public Map decodeBufferedInputStream(BufferedInputStream bis) throws IOException {
		Object result = decodeInputStream(bis);
		if (result == null) {
			throw (new IOException("Decode Buffered Input Stream error"));
		}
		return (Map)result;
	}
	
	private Map decodeByteArrayInputStream(ByteArrayInputStream bais) throws IOException {
		Object result = decodeInputStream(bais);
		if (result == null) {
			throw (new IOException("Decode Buffered Input Stream error"));
		}
		return (Map)result;	

	}
	
	private Object decodeInputStream(InputStream is) throws IOException {
		if (!is.markSupported()) {
			throw new IOException("InputStream must support mark.");
		}
		is.mark(Integer.MAX_VALUE);
		int currentByte = is.read();
		switch (currentByte) {
		case 'i':
			return new Long(getInteger(is,'e'));
			
		case 'e':			
		case -1:
			return null;			
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':		
			is.reset();
			return getByteArray(is);
		case 'd':
			Map tempMap = new HashMap();
			byte[] byteArray = null;
			
			while (true){
				byteArray = (byte[])decodeInputStream(is);
				if (byteArray == null) break;
				Object value = decodeInputStream(is);
				String key = new String(byteArray);
				tempMap.put(key, value);
			}			
			//currentByte = is.read();
			return tempMap;
		case 'l':
			ArrayList al = new ArrayList();
			Object listValue = null;
			while (true) {
				listValue = decodeInputStream(is);
				if (listValue == null) break;
				al.add(listValue);
			}
			//currentByte = is.read();
			return al;
		default:
			int len = is.available();
			if (len > 255) len = 255; 
			byte[] b = new byte[len];
			is.read(b);
			throw (new IOException("BenDecoder.decodeInputStream(InputStream is) error at " + currentByte + b));			
		}

	}
	private long getInteger(InputStream is, char endChar) throws IOException {
		
		StringBuffer s = new StringBuffer();
		int currentByte = is.read();
		//currentByte >= 0 means end of the InputStream
		//currentByte == endChar means the integer is ended,
		//the end char might be ":" or "e".
		while (currentByte >= 0 && currentByte != endChar) {
			s.append((char)currentByte);
			currentByte = is.read();
		}
		if (currentByte < 0) 
			return -1;
		else 
			return Long.parseLong(s.toString());
	}
	private byte[] getByteArray(InputStream is) throws IOException {
		//need implementation
		int byteLength = (int)getInteger(is,':');
		if (byteLength < 0) return null;
		byte[] byteArray = new byte[byteLength];
		int countLength = 0;
		int readLength = 0;
		while (countLength < byteLength) {
			readLength = is.read(byteArray, countLength, byteLength - countLength);
			if (readLength <= 0) break;
			countLength += readLength;
		}
		return byteArray;
	}
	
	
}
