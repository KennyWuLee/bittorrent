import java.io.*;
import java.net.*;

public class Listener implements Runnable {
	
	Config conf;
	byte[] peerId;
	Main main;
	
	public Listener(Config conf, byte[] peerId, Main main) {
		this.conf = conf;
		this.peerId = peerId;
		this.main = main;
	}

	@Override
	public void run() {
	    try {
			ServerSocket serverSocket = new ServerSocket(conf.port);
			while(true) {
				Socket clientSocket = serverSocket.accept();
				Peer p = new Peer();
				p.address = clientSocket.getInetAddress();
				p.port = clientSocket.getPort();
				System.out.println("client connected from " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
				PeerConnection pc = new PeerConnection(clientSocket, p, peerId, conf);
				main.addConnection(p, pc);
			}
		} catch (IOException e) {
			System.out.println("unable to listen on port");
		}
	}	
}
