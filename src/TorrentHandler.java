import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class TorrentHandler {
	
	private TorrentFile torrentfile;
	private Config conf;
	private int numberOfPieces;
	private int pieceSize;
	private long totalBytes;
	private HashSet<Peer> activePeers;
	private HashSet<Peer> inactivePeers;
	private HashSet<Peer> failedPeers;
	private TorrentProgress progress;
	private HashMap<Peer, PeerConnection> connectedPeers;
	private byte[] peerId;
	//TODO 
	//multiple files
	private RandomAccessFile downloadFile;
	
	public TorrentHandler(TorrentFile f, Config conf, byte[] peerId) {
		this.peerId = peerId;
		torrentfile = f;
		this.conf = conf;
		totalBytes = torrentfile.getSizeInBytes();
		pieceSize = torrentfile.getPieceSize();
		//round up by 1
		numberOfPieces = (int) ((totalBytes + pieceSize - 1) / pieceSize);
		activePeers = new HashSet<Peer>();
		inactivePeers = new HashSet<Peer>();
		failedPeers = new HashSet<Peer>();
		progress = new TorrentProgress(numberOfPieces);
		connectedPeers = new HashMap<Peer, PeerConnection>();
		readDownloadFile();
	}
	
	public void readDownloadFile() {
		try {
			File file = new File(conf.downloadDir + torrentfile.getFilename());
			downloadFile = new RandomAccessFile(file, "rw");
			byte[] piece = new byte[pieceSize];
			int b = 0;
			int count = 0;
			System.out.println("checking file");
			for(int i = 0; i < numberOfPieces && b != -1; i++) {
				b = downloadFile.read(piece);
				if(checkHash(i, piece)) {
					count++;
					progress.setDownloaded(i);
				}
			}
			System.out.println((100.0 * count) / numberOfPieces + "% done");
		} catch (FileNotFoundException e) {
			System.out.println("download file not found");
		} catch (IOException e) {
			System.out.println("Error reading from downloadfile");
		}
	}
	
	public PeerConnection addPeer() {
		PeerConnection connection = null;
		Peer p = null;
		Iterator<Peer> it = getInactivePeers().iterator();
		LinkedList<Peer> failed = new LinkedList<Peer>();
		while(it.hasNext() && connection == null) {
			p = it.next();
			if((connection = connectToPeer(p)) == null)
				failed.add(p);
		}
		//cant modify the inactive peers while iterating
		for(Peer peer : failed)
			failPeer(peer);
		if(connection != null && p != null) {
			activatePeer(p);
			connectedPeers.put(p, connection);
		}
		return connection;
	}
	
	public PeerConnection connectToPeer(Peer p) {
		System.out.println("trying to connect to peer" + p.address + ":" + p.port);
		try {
			String ip = p.address.getHostAddress();
			String localIp = InetAddress.getLocalHost().getHostAddress();
			if((ip.equals(localIp) || ip.equals("127.0.0.1")) && p.port == conf.port) {
				System.out.println("ignoring myself");
				return null;
			}
		} catch (UnknownHostException e1) {
			System.out.println("unkown host");
		}
		Socket s;
		try {
			//s = new Socket(p.address, p.port, InetAddress.getLocalHost(), conf.port);
			s = new Socket(p.address, p.port);
			try {
				PeerConnection con = new PeerConnection(s, p, peerId, conf);
				con.assignTorrent(this);
				con.sendHandshake();
				byte[] info_hash = con.recieveHandshake();
				if(! Arrays.equals(info_hash, torrentfile.getInfoHash()))
					throw new IOException();
				System.out.println("succesfully connected to peer " + p.address + ":" + p.port);
				return con;
			} catch (IOException e) {
				System.out.println("failed to handshake with peer: " + p.address + ":" + p.port);
				return null;
			}
		}
		catch (IOException e) {
			System.out.println("failed to open connection to peer: " + p.address + ":" + p.port);
			System.out.println(e.getMessage());
			return null;
		}
		
	}
	
	public int numOfPeers() {
		int sum = 0;
		sum += activePeers.size();
		sum += inactivePeers.size();
		sum += failedPeers.size();
		return sum;
	}
	
	public void deactivatePeer(Peer p) {
		if(activePeers.contains(p))
			activePeers.remove(p);
		if(connectedPeers.containsKey(p))
			connectedPeers.remove(p);
		inactivePeers.add(p);
	}
	
	public void activatePeer(Peer p) {
		if(inactivePeers.contains(p))
			inactivePeers.remove(p);
		activePeers.add(p);
	}
	
	public void failPeer(Peer p) {
		if(inactivePeers.contains(p))
			inactivePeers.remove(p);
		failedPeers.add(p);
	}
	
	public void addPeers(List<Peer> list) {
		for(Peer p : list)
			if(! activePeers.contains(p) && ! failedPeers.contains(p))
				inactivePeers.add(p);
	}
	
	public Set<Peer> getInactivePeers() {
		return inactivePeers;
	}
	
	public byte[] getInfoHash() {
		return torrentfile.getInfoHash();
	}
	
	public String getInfoHashAsString() {
		return torrentfile.getInfoHashAsString();
	}
	
	public String getAnnounce() {
		return torrentfile.getAnnounce();
	}
	
	public long getTotalBytes() {
		return totalBytes;
	}
	
	public long getUpladed() {
		//TODO 0 for now
		return 0;
	}
	
	public long getDownloaded() {
		return progress.piecesDownloaded() * pieceSize;
	}
	
	public int getNumberOfPieces() {
		return numberOfPieces;
	}
	
	public long getPieceSize() {
		return pieceSize;
	}
	
	public synchronized void writePiece(int index, byte[] data) {
		try {
			System.out.println("wrinting piece " + index);
			if(checkHash(index, data)) {
				progress.setDownloaded(index);
				downloadFile.seek(index * pieceSize);
				downloadFile.write(data);
				sendHave(index);
			} else {
				System.out.println("invalid hash for piece " + index);
				progress.setMissing(index);
			}
		} catch(IOException e) {
			System.out.println("Error writing to download file");
		}
	}
	
	public boolean checkHash(int index, byte[] data) {
		byte[] recievedHash = Util.sha1(data);
		byte[] expectedHash = torrentfile.getHash(index);
		return Arrays.equals(recievedHash, expectedHash);
	}
	
	public synchronized byte[] getPiece(int index) {
		try {
			int size = (int) Math.min(pieceSize, totalBytes - index * pieceSize);
			byte[] data = new byte[size];
			downloadFile.seek(index * pieceSize);
			downloadFile.read(data);
			return data;
			
		} catch (IOException e) {
			System.out.println("Error reading from file");
			return (new byte[pieceSize]);
		}
	}
	
	public boolean isDownloading() {
		int remaningPieces = progress.piecesDownloading() + progress.piecesMissing();
		return remaningPieces > 0;
	}
	
	public void sendHave(int index) {
		try {
		for(PeerConnection con : connectedPeers.values())
			con.sendHave(index);
		} catch (IOException e) {
			System.out.println("Error sending have");
		}
	}
	
	public TorrentProgress getProgress() {
		return progress;
	}
}
