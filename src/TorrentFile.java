import java.io.*;
import java.security.*;
import java.util.*;
import bencode.*;

public class TorrentFile {
	
	private HashMap<String, BencodeElem> dict;
	private File f;
	private byte[] infohash;
	
	public TorrentFile(File f) throws IOException {
		this.f = f;
		parse(f);
		calculateInfoHash();
	}
	
	public String getAnnounce() { 
		return ((BencodeByteString)dict.get("announce")).getValue();
	}
	
	public boolean hasMultipleFiles() {
		BencodeDictionary info = (BencodeDictionary)dict.get("info");
		return info.dict.containsKey("files");
	}
	
	public byte[] getInfoHash() {
		return infohash;
	}
	
	public String getInfoHashAsString() {
		return Util.byteArrayToHex(infohash);
	}
	
	public int getPieceSize() {
		BencodeDictionary info = (BencodeDictionary) dict.get("info");
		BencodeInteger i = (BencodeInteger) info.dict.get("piece length");
		return i.value;
	}
	
	public long getSizeInBytes() {
		if(! hasMultipleFiles()) {
			BencodeDictionary info = (BencodeDictionary)dict.get("info");
			BencodeInteger length = (BencodeInteger) info.dict.get("length");
			return length.value;
		} else {
			//TODO
			return 0;
		}
	}
	
	private void calculateInfoHash() throws IOException {
		int start = dict.get("info").start;
		int end = dict.get("info").end;
		RandomAccessFile raf = new RandomAccessFile(f, "r");
		byte[] data = new byte[end - start];
		raf.seek(start);
		raf.read(data);
		infohash = Util.sha1(data);
		raf.close();
	}
	
	private void parse(File f) throws IOException {
		FileInputStream in = new FileInputStream(f);
		BencodeParser parser = new BencodeParser(in);
		dict = parser.readDictionary().dict;
		in.close();
	}
	
	public byte[] getHash(int piece) {
		BencodeDictionary info = (BencodeDictionary) dict.get("info");
		BencodeByteString s = (BencodeByteString) info.dict.get("pieces");
		byte[] pieces = s.data;
		byte[] hash = new byte[20];
		for(int i = 0; i < hash.length; i++)
			hash[i] = pieces[piece * 20 + i];
		return hash;
	}
	
	public String getFilename() {
		if(! hasMultipleFiles()) {
			BencodeDictionary info = (BencodeDictionary) dict.get("info");
			BencodeByteString name = (BencodeByteString) info.dict.get("name");
			return name.getValue();
		} else {
			System.out.println("Error: multiple file torrent");
			return null;
		}
	}
}