import java.net.*;
import java.io.*;
import java.util.*;

public class PeerConnection implements Runnable {
	
	private final String PROTOCOL = "BitTorrent protocol";
	private TorrentHandler handler;
	private Config conf;
	private Socket socket;
	private BufferedInputStream in;
	private BufferedOutputStream out;
	private byte[] remotePeerId;
	private byte[] myPeerId;
	private Peer identifier;
	private boolean[] peerPieces;
	private TorrentProgress progress;
	private int seedingPiceIndex;
	private byte[] seedingPieceData;
	private boolean amInterested, amChocking, peerInterested, peerChocking;
	
	private Piece downloadingPiece;
	
	public PeerConnection(Socket s, Peer p, byte[] peerId, Config conf) throws IOException  {
		this.identifier = p;
		myPeerId = peerId;
		this.conf = conf;
		
		remotePeerId = new byte[20];
		seedingPiceIndex = -1;
		amInterested = false;
		amChocking = true;
		peerInterested = false;
		peerChocking = true;
		
		socket = s;
		in = new BufferedInputStream(socket.getInputStream());
		out = new BufferedOutputStream(socket.getOutputStream());
	}
	
	public void assignTorrent(TorrentHandler th) {
		this.handler = th;
		this.progress = th.getProgress();
		peerPieces = new boolean[handler.getNumberOfPieces()];
	}
	
	@Override
	public void run() {
		try {
			sendBitFiled();
			while(amInterested || isLeecher()) {
				if(amInterested && amChocking)
					sendUnchoke();
				else if(peerInterested && amChocking)
					sendUnchoke();
				else if(amInterested && ! peerChocking)
					downloadPiece();
				else
					recieveMessage();
			}
		} catch(EOFException e) {
			if(downloadingPiece != null)
				progress.setMissing(downloadingPiece.getIndex());
			log("EOF when reading from peer " + name());
			return;
		} catch(IOException e) {
			if(downloadingPiece != null)
				progress.setMissing(downloadingPiece.getIndex());
			log("IOError when reading from peer " + name());
			System.out.println(e.getMessage());
			e.printStackTrace();
			return;
		} catch (InvalidMessageException e) {
			if(downloadingPiece != null)
				progress.setMissing(downloadingPiece.getIndex());
			log("Invalid Message");
		}
		try {
			in.close();
			out.close();
			socket.close();
		} catch (IOException e) {
			log("Error closing the connection");
		}
	}
	
	public void sendHave(int i) throws IOException {
		byte[] len = Util.intToBigEndian(5, 4);
		out.write(len);
		out.write(4);
		byte[] piece = Util.intToBigEndian(i, 4);
		out.write(piece);
		out.flush();
		log("send have");
	}
	
	public boolean isLeecher() {
		for(boolean b : peerPieces)
			if(! b)
				return true;
		return false;
	}
	
	public void downloadPiece() throws IOException, InvalidMessageException {
		int i = progress.getAvailableMissingForDownload(peerPieces);
		if(i != -1) {
			downloadingPiece = new Piece(i, handler.getTotalBytes(), conf.blocksize, handler.getPieceSize());
			log("Downloading piece " + i);
			for(int j = 0; j < conf.requestQueueSize && downloadingPiece.queueSize() > 0; j++) {
				int[] a = downloadingPiece.popRequest();
				int blockindex = a[0];
				int length = a[1];
				sendRequest(downloadingPiece.getIndex(), blockindex, length);
			}
			while(! downloadingPiece.recievedAllBlocks()) {
				recieveMessage();
			}
			byte[] a = downloadingPiece.getData();
			handler.writePiece(downloadingPiece.getIndex(), a);
			downloadingPiece = null;
		}
	}
	
	public void updateInterested() throws IOException {
		int i = progress.getAvailableMissing(peerPieces);
		if(i != -1 && ! amInterested)
			sendInterested();
		else if(i == -1 && amInterested)
			sendUninterested();
	}
	
	public synchronized void sendChoke() throws IOException {
		byte[] len = Util.intToBigEndian(1, 4);
		out.write(len);
		out.write(0);
		amChocking = true;
		out.flush();
		log("send choke");
	}
	
	public synchronized void sendUnchoke() throws IOException {
		byte[] len = Util.intToBigEndian(1, 4);
		out.write(len);
		out.write(1);
		amChocking = false;
		out.flush();
		log("send unchoke");
	}
	
	public synchronized void sendInterested() throws IOException {
		byte[] len = Util.intToBigEndian(1, 4);
		out.write(len);
		out.write(2);
		amInterested = true;
		out.flush();
		log("send interested");
	}
	
	public synchronized void sendUninterested() throws IOException {
		byte[] len = Util.intToBigEndian(1, 4);
		out.write(len);
		out.write(3);
		amInterested = false;
		out.flush();
		log("send uninterested");
	}
	
	public synchronized void sendBitFiled() throws IOException {
		int x = (handler.getNumberOfPieces() + 7) / 8;
		byte[] len = Util.intToBigEndian(1 + x, 4);
		out.write(len);
		out.write(5);
		byte[] bitfield = new byte[x];
		for(int i = 0; i < x; i++)
			bitfield[i] = 0;
		for(int i = 0; i < handler.getNumberOfPieces(); i++)
			if(progress.isDownloaded(i))
				bitfield[i/8] |= 1 << (8 - 1 - i%8);
		out.write(bitfield);
		out.flush();
		log("send bitfield " + Arrays.toString(bitfield));
	}
	
	public synchronized void sendRequest(int index, int block, int length) throws IOException {
		byte[] len = Util.intToBigEndian(13, 4);
		out.write(len);
		out.write(6);
		int begin = conf.blocksize * block;
		byte[] indexBytes = Util.intToBigEndian(index, 4);
		byte[] beginBytes = Util.intToBigEndian(begin, 4);
		byte[] lengthBytes = Util.intToBigEndian(length, 4);
		out.write(indexBytes);
		out.write(beginBytes);
		out.write(lengthBytes);
		out.flush();
		//to noisy
		//log("send request: index: " + index + " begin: " + begin + " length: " + length);
	}
	
	public synchronized void sendPiece(int index, int begin, byte[] block) throws IOException {
		byte[] len = Util.intToBigEndian(9 + block.length, 4);
		out.write(len);
		out.write(7);
		byte[] indexBytes = Util.intToBigEndian(index, 4);
		byte[] beginBytes = Util.intToBigEndian(begin, 4);
		out.write(indexBytes);
		out.write(beginBytes);
		out.write(block);
		out.flush();
		//to noisy
		//log("send piece: index: " + index + " begin: " + begin);
	}
	
	public TorrentHandler getHandler() {
		return handler;
	}
	
	public synchronized void sendHandshake() throws IOException {
		out.write((byte)19);
		String pstr = PROTOCOL;
		out.write(pstr.getBytes());
		for(int i = 0; i < 8; i++)
			out.write(0);
		out.write(handler.getInfoHash());
		out.write(myPeerId);
		out.flush();
	}
	
	public byte[] recieveHandshake() throws IOException {
		int length = read();
		byte[] pstr = new byte[length];
		byte[] info_hash = new byte[20];
		byte[] peer_id = new byte[20];
		read(pstr);
		String s = new String(pstr, "UTF-8");
		if(! s.equals(PROTOCOL)) {
			throw new IOException("protocol wrong");
		}
		in.skip(8);
		read(info_hash);
		//if(! Arrays.equals(info_hash, handler.getInfoHash()))
		//	throw new IOException("infohash wrong");
		read(peer_id);
		remotePeerId = peer_id;
		return info_hash;
	}
	
	public void processRequest(int index, int begin, int length) throws IOException {
		if(seedingPiceIndex != index) {
			seedingPieceData = handler.getPiece(index);
			seedingPiceIndex = index;
		}
		byte[] block = new byte[length];
		for(int i = 0; i < block.length; i++)
			block[i] = seedingPieceData[begin+i];
		sendPiece(index, begin, block);
	}
	
	public void recieveMessage() throws IOException, InvalidMessageException {
		byte[] number = new byte[4]; 
		read(number);
		int len = (int) Util.bigEndianToInt(number);
		if(len == 0) {
			//keepalive
			//log("recived keepalive");
		} else {
			int messageId = read();
			switch(messageId) {
			case 0:
				//choke
				log("recived choke");
				peerChocking = true;
				break;
			case 1:
				//unchoke
				log("recived unchoke");
				peerChocking = false;
				break;
			case 2:
				//interested
				log("recived interested");
				peerInterested = true;
				break;
			case 3:
				//not interested
				log("recived not interested");
				peerInterested = false;
				break;
			case 4:
				//have
				log("recived have");
				read(number);
				int piece_index = (int) Util.bigEndianToInt(number);
				setHaveBit(piece_index);
				updateInterested();
				break;
			case 5:
				//bitfield
				log("recived bitfield");
				byte[] bitfield = new byte[len - 1];
				read(bitfield);
				log("bit field: " + Arrays.toString(bitfield));
				setHaveBits(bitfield);
				updateInterested();
				break;
			case 6:
				//request
				{
					read(number);
					int index = (int) Util.bigEndianToInt(number);
					read(number);
					int begin = (int) Util.bigEndianToInt(number);
					read(number);
					int length = (int) Util.bigEndianToInt(number);
					//to noisy
					//log("recived request: index: " + index + " begin: " + begin + " length: " + length);
					processRequest(index, begin, length);
				}
				break;
			case 7:
				//piece
				{
					read(number);
					int index = (int) Util.bigEndianToInt(number);
					read(number);
					int begin = (int) Util.bigEndianToInt(number);
					byte[] data = new byte[len - 9];
					read(data);
					//to noisy
					//log("recieved piece: index: " + index + " begin " + begin);
					if(index == downloadingPiece.getIndex()) {
						downloadingPiece.putBlock(begin, data);
						if(downloadingPiece.queueSize() > 0) {
							int[] a = downloadingPiece.popRequest();
							sendRequest(downloadingPiece.getIndex(), a[0], a[1]);
						}
					}
				}
				break;
			//last two are not implemented
			case 8:
				//cancel
				log("recived cancel");
				System.out.println("invalid message cancel");
				throw new InvalidMessageException();
				//break;
			case 9:
				//port
				log("recived port");
				System.out.println("invalid message port");
				throw new InvalidMessageException();
				//break;
			default:
				System.out.println("invalid message default");
				throw new InvalidMessageException();
			}
		}
	}
	
	public Peer getPeer() {
		return identifier;
	}
	
	public String name() {
		return socket.getInetAddress() + ":" + socket.getPort();
	}
	
	private boolean hasBit(int index) {
		return peerPieces[index];
	}
	
	private void setHaveBit(int index) throws InvalidMessageException {
		if(index >= 0 && index < handler.getNumberOfPieces()) {
			peerPieces[index] = true;
		} else {
			throw new InvalidMessageException();
		}
	}
	
	private void setHaveBits(byte[] field) throws InvalidMessageException {
		if(field.length == (handler.getNumberOfPieces() + 7) / 8) {
			for(int i = 0; i < peerPieces.length; i++)
				peerPieces[i] = (field[i/8] & (1 << (8 - 1 - i%8))) > 0;
		} else {
			throw new InvalidMessageException();
		}
	}
	
	private byte read() throws IOException {
		int i = in.read();
		if(i == -1)
			throw new EOFException();
		return (byte)i;
	}
	
	private void read(byte[] data) throws IOException {
		//TODO
		//add a timeout
		int i = in.read(data);
		if(i == -1)
			throw new EOFException();
	}
	
	private void log(String msg) {
		System.out.println("Peer " + name() + " - " + msg);
	}
}