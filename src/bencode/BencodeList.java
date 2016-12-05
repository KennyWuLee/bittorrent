package bencode;

import java.util.*;

public class BencodeList extends BencodeElem {
	public ArrayList<BencodeElem> list;
	
	public BencodeList() {
		list = new ArrayList<BencodeElem>();
	}
}
