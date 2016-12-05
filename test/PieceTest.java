import static org.junit.Assert.*;

import java.awt.geom.QuadCurve2D;
import java.io.*;

import org.junit.Test;

public class PieceTest {

	@Test
	public void piece() {
		try {
			TorrentFile t = new TorrentFile(new File("test/resources/2986c844cdbcc75e6cf4689b61b8c4b362719e34.torrent"));
			Config conf = new Config();
			int piecesize = (int) t.getPieceSize();
			int blocksize = conf.blocksize;
			int totalsize = (int) t.getSizeInBytes();
			System.out.println("piece size " + piecesize);
			System.out.println("blocksize " + blocksize);
			System.out.println("total size " + totalsize);
			System.out.println("number of pieces " + (totalsize + piecesize - 1 )/ piecesize);
			
			Piece p = new Piece(0, totalsize, blocksize, piecesize);
			while(p.queueSize() > 0) {
				int[] a = p.popRequest();
				System.out.println("block[" + a[0] + "]    length: " + a[1]);
			}
			
			p = new Piece(1519, totalsize, blocksize, piecesize);
			while(p.queueSize() > 0) {
				int[] a = p.popRequest();
				System.out.println("block[" + a[0] + "]    length: " + a[1]);
			}
			
		} catch (IOException e) {
			System.out.println("IOError");
			e.printStackTrace();
		}
		
	}

	@Test
	public void piece2() {
		try {
			TorrentFile t = new TorrentFile(new File("test/resources/good.txt.torrent"));
			Config conf = new Config();
			int piecesize = (int) t.getPieceSize();
			int blocksize = conf.blocksize;
			int totalsize = (int) t.getSizeInBytes();
			System.out.println("piece size " + piecesize);
			System.out.println("blocksize " + blocksize);
			System.out.println("total size " + totalsize);
			System.out.println("number of pieces " + (totalsize + piecesize - 1 )/ piecesize);
			
			Piece p = new Piece(0, totalsize, blocksize, piecesize);
			while(p.queueSize() > 0) {
				int[] a = p.popRequest();
				System.out.println("block[" + a[0] + "]    length: " + a[1]);
			}
			
		} catch (IOException e) {
			System.out.println("IOError");
			e.printStackTrace();
		}
		
	}
}
