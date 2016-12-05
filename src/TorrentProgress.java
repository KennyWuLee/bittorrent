import java.util.HashSet;

public class TorrentProgress {

	HashSet<Integer> missing, downloading, downloaded;
	int pieces;
	
	public TorrentProgress(int pieces) {
		missing = new HashSet<Integer>();
		downloading = new HashSet<Integer>();
		downloaded = new HashSet<Integer>();
		this.pieces = pieces;
		for(int i = 0; i < pieces; i++)
			missing.add(i);
	}
	
	public synchronized boolean isMissing(int index) {
		return missing.contains(index);
	}
	

	public synchronized boolean isDowloading(int index) {
		return downloading.contains(index);
	}
	

	public synchronized boolean isDownloaded(int index) {
		return downloaded.contains(index);
	}
	
	public synchronized void setMissing(int index) {
		if(downloading.contains(index))
			downloading.remove(index);
		if(downloaded.contains(index))
			downloaded.remove(index);
		missing.add(index);
	}
	
	public synchronized void setDownloading(int index) {
		if(downloaded.contains(index))
			downloaded.remove(index);
		if(missing.contains(index))
			missing.remove(index);
		downloading.add(index);
	}
	
	public synchronized void setDownloaded(int index) {
		if(downloading.contains(index))
			downloading.remove(index);
		if(missing.contains(index))
			missing.remove(index);
		downloaded.add(index);
	}
	
	public synchronized int piecesDownloading() {
		return downloading.size();
	}
	
	public synchronized int piecesMissing() {
		return missing.size();
	}
	
	public synchronized int piecesDownloaded() {
		return downloaded.size();
	}
	
	public synchronized int getAvailableMissing(boolean[] peerPieces) {
		for(int i = 0; i < peerPieces.length; i++)
			if(missing.contains(i))
				return i;
		return -1;
	}

	public synchronized int getAvailableMissingForDownload(boolean[] peerPieces) {
		int piece = getAvailableMissing(peerPieces);
			if(piece != -1)
				setDownloading(piece);
		return piece;
	}
}
