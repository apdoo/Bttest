package MyJavaTorrent;

import java.io.*;

public class IOManager {
	BufferedReader br;
	BufferedWriter bw;
    public byte[] fileInfoHash;
    boolean findInfoFlag = false;
	public IOManager() {
		
	}
	public static boolean save(byte[] data, String fileName) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		fos.write(data, 0, data.length);
		fos.flush();
		fos.close();
		return true;	
		
	}
	public static void save(InputStream is, String fileName) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileName);
		int r;
		while (true) {
			r = is.read();
			if (r == -1) break;
			fos.write(r);
			fos.flush();
		}
		fos.close();
	}

	public byte[] readBytes(File f) {
        System.out.println("IOManager readBytes.."+f.getAbsoluteFile());
        long fileSize = 0;
		byte[] fileByte = null;
		try {
			FileInputStream fis = new FileInputStream(f);
			fileSize = f.length();
            System.out.println("fileSize"+fileSize);
            fileByte = new byte[(int)fileSize];
            System.out.println("fileByte"+fileByte.length);
            int filePointer = 0;
			int bytesRead = 0;
			while (filePointer < fileByte.length) {
				bytesRead = fis.read(fileByte, filePointer, fileByte.length - filePointer);
				if (bytesRead < 0) break;
				filePointer += bytesRead;
			}
            System.out.println("filePointer"+filePointer);
            byte[] tempArray = null;
            for (int i = 0; i < filePointer - 3; i++) {
            	if (findInfoFlag) break;
            	tempArray = Utils.makeArray(fileByte, i, 4);
            	if (Utils.bytesCompare(tempArray, "info".getBytes())) {
                	System.out.println("Find info");
                	findInfoFlag = true;
                	int j = i + 4;
                	fileInfoHash = new byte[0];
                	boolean dflag = true;
                	//boolean keyOrValue = true; //1: key, 0: value
                	while (true) {
                		if (dflag == false) break;
                		switch (fileByte[j]) {
                		case 'd':
                			fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);
                			dflag = true;
                			j++;
                			break;
                		case 'e':
                			fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);
                			dflag = false;
                			j++;
                			break;
                		case 'i':
                			fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);
                			while (fileByte[j] != 'e') {
                				j++;
                				fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);                				
                			}
                			j++;
                			//keyOrValue = !keyOrValue;
                			break;
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
                			String tempIntString = new String();
                			byte[] tempIntByte;
                			while (fileByte[j] != ':') {
                				tempIntByte = new byte[0];
                				tempIntByte = Utils.concat(tempIntByte, fileByte[j]);                				
                				tempIntString += new String(tempIntByte);
                				fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);
                				j++;                			
                			}
                			fileInfoHash = Utils.concat(fileInfoHash, fileByte[j]);
                			j++;
                			int tempInt = Integer.parseInt(tempIntString);
                			for (int k = 0; k < tempInt; k++) {
                				fileInfoHash = Utils.concat(fileInfoHash, fileByte[j + k]);                					
                			}
                			j = j + tempInt;                			
                			break;	
                		}                	
                	}
                }
            }
			fis.close();
			
		}
		catch (FileNotFoundException e) {
			System.err.println("IOManager.readBytes FileNotFoundException Error : "+f.getName());
			return null;
		}
		catch (IOException e) {
			System.err.println("IOManager.readBytes IOException Error : "+f.getName());
		}
        System.out.println("return"+fileByte.length);
        return fileByte;
	}
	
}
