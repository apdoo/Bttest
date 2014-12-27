package MyJavaTorrent;

public class DownloadFile {
	public DownloadFile(){
		try {
			TorrentProcessor tp = new TorrentProcessor();
//			if (args.length < 2) {
//				System.err.println("Parameters of Torrent File and Save Path should be added.");
//				System.exit(1);
//			}
//			TorrentFile t = tp.getTorrentFile(tp.analyzeTorrent(args[0]));
            System.out.println("start..");
            TorrentFile t = tp.getTorrentFile(tp.analyzeTorrent("F:\\vedios\\sis\\t.torrent"));//bt文件路径
            System.out.println(t.toString());
            if (t != null) {
//				DownloadManager dm = new DownloadManager(t, Utils.generateID(), args[1]);
				DownloadManager dm = new DownloadManager(t, Utils.generateID(), "F:\\vedios\\sis\\");//下载文件路径
				dm.startListening(6881, 6889);
				dm.startTrackerUpdate();
				dm.blockUntilComplete();
				dm.stopTrackerUpdate();
				dm.closeTempFiles();
				
			}
			else {
				System.err.println("Analyze torrent file failed.");
				System.exit(1);
			}
		}
		catch (Exception e) {
			System.err.println("Processing torrent file failed.");
			System.exit(1);
		}
			
		
	}
	public static void main(String args[]){
		new DownloadFile();
	}
}
