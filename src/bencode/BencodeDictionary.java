package bencode;

import java.util.*;

public class BencodeDictionary extends BencodeElem {
	public HashMap<String, BencodeElem> dict;
	
	public BencodeDictionary() {
		dict = new HashMap<String, BencodeElem>();
	}
}
