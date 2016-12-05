import java.io.*;
import java.net.*;
import java.util.*;

import bencode.*;

public class TrackerConnect {
	
	private int interval;
	private String trackerId;
	private String announce;
	private boolean firstReq;
	private Config conf;
	
	public TrackerConnect(String announce, Config conf) {
		this.announce = announce;
		this.conf = conf;
		firstReq = true;
	}
	
	public void completed(TorrentHandler handler, byte[] peerId) throws IOException {
		trackerRequest(handler, peerId, "completed");
	}
	
	public void stopped(TorrentHandler handler, byte[] peerId) throws IOException {
		trackerRequest(handler, peerId, "stopped");
	}
	
	public LinkedList<Peer> getPeers(TorrentHandler handler, byte[] peerId) throws IOException {
		if(firstReq) {
			firstReq = false;
			return trackerRequest(handler, peerId, "started");
		}
		return trackerRequest(handler, peerId, "");
	}
	
	public LinkedList<Peer> trackerRequest(TorrentHandler handler, byte[] peerId, String event) throws IOException {
		String s = constructURL(event, handler, peerId);
		URL url = new URL(s);
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();
		connection.setRequestMethod("GET");
		InputStream in = connection.getInputStream();
		BencodeParser parser = new BencodeParser(in);
		BencodeDictionary dict = parser.readDictionary();
		update(dict);
		return extractPeers(dict);
	}
	
	private String constructURL(String event, TorrentHandler handler, byte[] peerId) {
		StringBuilder urlBuilder = new StringBuilder();
		String announce = handler.getAnnounce();
		String hash = percentEncode(handler.getInfoHash());
		String pId = percentEncode(peerId);
		long left = handler.getTotalBytes() - handler.getDownloaded();
		urlBuilder.append(announce);
		urlBuilder.append("?");
		urlBuilder.append("info_hash=");
		urlBuilder.append(hash);
		urlBuilder.append("&");
		urlBuilder.append("peer_id=");
		urlBuilder.append(pId);
		urlBuilder.append("&");
		urlBuilder.append("port=");
		urlBuilder.append(conf.port);
		urlBuilder.append("&");
		urlBuilder.append("uploaded=");
		urlBuilder.append(handler.getUpladed());
		urlBuilder.append("&");
		urlBuilder.append("downloaded=");
		urlBuilder.append(handler.getDownloaded());
		urlBuilder.append("&");
		urlBuilder.append("left=");
		urlBuilder.append(left);
		urlBuilder.append("&");
		urlBuilder.append("compact=1");
		urlBuilder.append("&");
		urlBuilder.append("no_peer_id=1");
		urlBuilder.append("&");
		if(event != "") {
			urlBuilder.append("event=");
			urlBuilder.append(event);
		}
		if(trackerId != null) {
			urlBuilder.append("&");
			urlBuilder.append("trackerid=");
			urlBuilder.append(trackerId);
		}
		//TODO more
		return urlBuilder.toString();
	}
	
	private LinkedList<Peer> extractPeers(BencodeDictionary bdict) {
		LinkedList<Peer> list = new LinkedList<Peer>();
		if(bdict.dict.containsKey("peers")) {
			byte[] peers = ((BencodeByteString) bdict.dict.get("peers")).data;
			for(int i = 0; i < peers.length / 6; i++) {
				try {
					Peer p = new Peer();
					byte[] ip = new byte[4];
					for(int j = 0; j < ip.length; j++)
						ip[j] = peers[i*6+j];
					p.address = InetAddress.getByAddress(ip);
					byte[] port = new byte[2];
					for(int j = 0; j < port.length; j++)
						port[j] = peers[i*6+4+j];
					p.port = (int) Util.bigEndianToInt(port);
					list.add(p);
				} catch (UnknownHostException e) {
					System.out.println("UnknownHostException");
					e.printStackTrace();
				}
			}
		}
		return list;
	}
	
	private void update(BencodeDictionary bdict) throws UnknownHostException {
		if(bdict.dict.containsKey("trackerid")) {
			BencodeByteString id = (BencodeByteString) bdict.dict.get("trackerid");
			trackerId = id.getValue();
		}
		if(bdict.dict.containsKey("interval"))
			interval = ((BencodeInteger) bdict.dict.get("interval")).value;
	}
	
	private String percentEncode(byte[] data) {
		StringBuilder sb = new StringBuilder();
		for(byte b : data) {
			if(Character.isAlphabetic(b) || Character.isDigit(b) || 
					b == '.' || b == '-' || b == '_' || b == '~' ||b == '$' || b == '+' || 
					b == '!' || b == '*' || b == '\'' || b == '(' || b == ')' || b == ',') {
				sb.append((char)b);
			} else {
				sb.append("%" + String.format("%02x", b));
			}
		}
		return sb.toString();
	}
}