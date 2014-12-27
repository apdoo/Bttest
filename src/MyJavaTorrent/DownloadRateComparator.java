package MyJavaTorrent;

import java.util.*;

public class DownloadRateComparator implements Comparator {
	public int compare(Object a, Object b) {
		float rateA = ((Peer)a).getDownloadRate(false);
		float rateB = ((Peer)b).getDownloadRate(false);
		if (rateA > rateB)
			return -1;
		else if (rateA < rateB)
			return 1;
		return 0;
	}
}
