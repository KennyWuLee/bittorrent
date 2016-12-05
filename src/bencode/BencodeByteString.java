package bencode;

import java.io.*;

public class BencodeByteString extends BencodeElem {
	public byte[] data;
	
	public String getValue() {
		try {
			return new String(data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.out.println("unsupported encoding");
		}
		return "";
	}
}
