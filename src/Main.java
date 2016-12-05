import java.io.*;
import java.util.*;

import com.google.gson.Gson;

public class Main implements Runnable {
	
	private Config conf;
	private byte[] peerId;
	private Map<String, TorrentHandler> torrents;
	private HashMap<String, TrackerConnect> trackers;
	private HashSet<Thread> activeConnections;
	private HashMap<Thread, PeerConnection> peers;
	private Thread listenerThread;
	private Thread folderMonitorThread;

	
	public static void main(String[] args) {
		Main m = new Main();
		Thread t = new Thread(m);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			System.out.println("interrupted waiting for main");
			e.printStackTrace();
		}
	}
	
	public Main() {
		conf = readConfig();
		generatePeerID();
		torrents = new HashMap<String, TorrentHandler>();
		trackers = new HashMap<String, TrackerConnect>();
		activeConnections = new HashSet<Thread>();
		peers = new HashMap<Thread, PeerConnection>();
	}
	
	@Override
	public void run() {
		launchListener();
		lauchFolderMonitor();
		while(true) {
			removeDeadPeers();
			
			if(activeConnections.size() < conf.maxPeers)
				activatePeer();
		}
	}
	
	public void lauchFolderMonitor() {
		folderMonitorThread = new Thread(new FolderMonitor(this, conf.watchDir));
		folderMonitorThread.start();	
	}
	
	public synchronized void removeDeadPeers() {
		LinkedList<Thread> deadPeers = new LinkedList<Thread>();
		for(Thread t : activeConnections)
			if(! t.isAlive())
				deadPeers.add(t);
		for(Thread t : deadPeers)
			deavtivatePeer(t);
	}
	
	public Config readConfig() {
		Gson gson = new Gson();
		Config conf;
		try {
			FileReader fr = new FileReader("config.json");
			conf = gson.fromJson(fr, Config.class);
			return conf;
		} catch (FileNotFoundException e) {
			try {
				conf = new Config();
				FileWriter fw = new FileWriter("config.json");
				fw.write(gson.toJson(conf));
				fw.close();
				return conf;
			} catch (IOException e1) {
				System.out.println("error writing config file");
			}
		}
		return new Config();
	}
	
	public void launchListener() {
		listenerThread = new Thread(new Listener(conf, peerId, this));
		listenerThread.start();
	}
	
	private synchronized void activatePeer() {
		Iterator<TorrentHandler> torrentHandlerIt = torrents.values().iterator();
		TorrentHandler th = null;
		PeerConnection connection = null;
		/*
		 * Go through torrents in no particular order
		 * would be better to prefer torrents with few peers
		 */
		while(torrentHandlerIt.hasNext() && connection == null) {
			th = torrentHandlerIt.next();
			if(th.isDownloading()) {
				/*
				 * This is not ideal size it will keep hitting the tracker if 
				 * the torrent has no peers.
				 * This could be checked using the tracker's scrape feature.
				 */
				if(th.numOfPeers() == 0)
					getPeersFromTracker(th);
				connection = th.addPeer();
			}
		}
		if(connection != null && th != null) {
			lauchPeerThread(connection, th);
		}
	}
	
	public void lauchPeerThread(PeerConnection con, TorrentHandler th) {
		Thread t = new Thread(con);
		activeConnections.add(t);
		peers.put(t, con);
		th.activatePeer(con.getPeer());
		t.start();
	}
	
	public void getPeersFromTracker(TorrentHandler handler) {
		String announce = handler.getAnnounce();
		if(! trackers.containsKey(announce)) {
			TrackerConnect tcon = new TrackerConnect(announce, conf);
			trackers.put(announce, tcon);
		}
		List<Peer> list;
		try {
			list = trackers.get(announce).getPeers(handler, peerId);
		} catch (IOException e) {
			System.out.println("couldn't connect to tracker");
			return;
		}
		handler.addPeers(list);
		System.out.println("tracker gave peers:");
		for(Peer p : list)
			System.out.println(p.address + ":" + p.port);
	}
	
	public synchronized void addConnection(Peer p, PeerConnection con) {
		try {
			byte[] info_hash = con.recieveHandshake();
			String torrent = Util.byteArrayToHex(info_hash);
			if(! torrents.containsKey(torrent)) {
				System.out.println("wrong info hash from client " + p.address + ":" + p.port);
				return;
			}
			TorrentHandler th = torrents.get(torrent);
			con.assignTorrent(th);
			con.sendHandshake();
			lauchPeerThread(con, th);
		} catch (IOException e) {
			System.out.println("error handshaking with new client " + p.address + ":" + p.port);
		}
	}
	
	public synchronized void addTorrent(String filename) {
		TorrentFile f;
		try {
			f = new TorrentFile(new File(filename));
		} catch (IOException e) {
			System.out.println("couldn't read torrent file");
			return;
		}
		if(! torrents.containsKey(f.getInfoHashAsString())) {
			TorrentHandler handler = new TorrentHandler(f, conf, peerId);
			torrents.put(f.getInfoHashAsString(), handler);
			getPeersFromTracker(handler);
			System.out.println("added torrent: " + handler.getInfoHashAsString());
		}
	}
	
	private synchronized void deavtivatePeer(Thread t) {
		activeConnections.remove(t);
		PeerConnection con = peers.remove(t);
		con.getHandler().deactivatePeer(con.getPeer());
		System.out.println("removed inactive peer: " + con.name());
	}

	private void generatePeerID() {
		peerId = new byte[20];
		int i = 0;
		while(i < conf.clientIdent.length()) {
			peerId[i] = (byte) conf.clientIdent.charAt(i);
			i++;
		}
		Random rand = new Random();
		while(i < peerId.length) {
			peerId[i] = (byte) (rand.nextInt('z' - 'a') + 'a');
			i++;
		}
	}
}
