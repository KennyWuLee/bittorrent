import static org.junit.Assert.*;

import java.io.*;
import java.util.Arrays;

import org.junit.Test;

public class MainTest {

	@Test
	public void main() {
		//String filename = "test/resources/good.txt.torrent";
		String filename = "test/resources/2986c844cdbcc75e6cf4689b61b8c4b362719e34.torrent";
		//String filename = "test/resources/ubuntu-16.10-desktop-amd64.iso.torrent";
		Main m = new Main();
		Thread t = new Thread(m);
		t.start();
		m.addTorrent(filename);
		try {
			t.join();
		} catch (InterruptedException e) {
			System.out.println("interrupted waiting for main");
			e.printStackTrace();
		}
	}
}
