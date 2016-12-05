package bencode;

import java.io.*;

public class BencodeParser {

	int index;
	InputStream in;
	
	public BencodeParser(InputStream in) {
		this.in = in;
		index = 0;
	}
	
	public BencodeElem readElement() throws IOException {
		char c = nextChar();
		if(Character.isDigit(c)) {
			return readString(c);
		} else if(c == 'i') { 
			return readIntegerWithoutI();
		} else if(c == 'l') {
			return readListWithoutL();
		} else if(c == 'd') {
			return readDictionaryWithoutD();
		} else if(c == 'e') {
			return new BencodeEnd();
		} else {
			throw new IOException();
		}
	}
	
	public BencodeByteString readString() throws IOException {
		char c = nextChar();
		if(! Character.isDigit(c))
			throw new IOException();
		return readString(c);
	}
	
	public BencodeByteString readString(char firstDigit) throws IOException {
		BencodeByteString bytestring = new BencodeByteString();
		bytestring.start = index - 1;
		StringBuilder sb = new StringBuilder();
		sb.append(firstDigit);
		char c = nextChar();
		while(Character.isDigit(c)) {
			sb.append(c);
			c = nextChar();
		}
		if(c != ':')
			throw new IOException();
		int n = Integer.parseInt(sb.toString());
		byte[] data = new byte[n];
		for(int i = 0; i < n; i++) {
			int j = in.read();
			index++;
			if(j == -1)
				throw new IOException();
			data[i] = (byte)j;
		}
		bytestring.data = data;
		bytestring.end = index;
		return bytestring;
	}

	public BencodeInteger readInteger() throws IOException {
		char c = nextChar();
		if(c != 'i')
			throw new IOException();
		return readIntegerWithoutI();
	}
	
	public BencodeInteger readIntegerWithoutI() throws IOException {
		BencodeInteger bint = new BencodeInteger();
		bint.start = index - 1;
		StringBuilder sb = new StringBuilder();
		char c = nextChar();
		while(c != 'e') {
			sb.append(c);
			c = nextChar();
		}
		bint.value = Integer.parseInt(sb.toString());
		bint.end = index;
		return bint;
	}
	
	public BencodeList readList() throws IOException {
		char c = nextChar();
		if(c != 'l')
			throw new IOException();
		return readListWithoutL();
	}
	
	public BencodeList readListWithoutL() throws IOException {
		BencodeList blist = new BencodeList();
		blist.start = index - 1;
		BencodeElem e;
		e = readElement();
		while(!(e instanceof BencodeEnd)) {
			blist.list.add(e);
			e = readElement();
		}
		blist.end = index;
		return blist;
	}
	
	public BencodeDictionary readDictionary() throws IOException {
		char c = nextChar();
		if(c != 'd')
			throw new IOException();
		return readDictionaryWithoutD();
	}
	
	public BencodeDictionary readDictionaryWithoutD() throws IOException {
		BencodeDictionary bdict = new BencodeDictionary();
		bdict.start = index - 1;
		BencodeElem e = readElement();
		while(! (e instanceof BencodeEnd)) {
			if(e instanceof BencodeByteString) {
				BencodeByteString key = (BencodeByteString) e;
				BencodeElem value = readElement();
				if(value instanceof BencodeEnd)
					throw new IOException();
				bdict.dict.put(key.getValue(), value);
				e = readElement();
			} else {
				throw new IOException();
			}
		}
		bdict.end = index;
		return bdict;
	}
	
	public char nextChar() throws IOException {
		int c = in.read();
		index++;
		if(c == -1)
			throw new EOFException();
		return (char) c;
	}
}
