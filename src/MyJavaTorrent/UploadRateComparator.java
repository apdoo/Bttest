package MyJavaTorrent;

import java.util.*;

public class UploadRateComparator implements Comparator {
	public int compare(Object a, Object b) {
		float rateA = ((Peer)a).getUploadRate(false);
		float rateB = ((Peer)b).getUploadRate(false);
		if (rateA > rateB)
			return -1;
		else if (rateA < rateB)
			return 1;
		return 0;
	}
}
