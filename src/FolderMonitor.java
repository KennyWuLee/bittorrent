import java.io.*;
import java.util.HashSet;

public class FolderMonitor implements Runnable {

	String watchFolder;
	Main main;
	HashSet<String> files;

	public FolderMonitor(Main main, String watchfolder) {
		this.main = main;
		this.watchFolder = watchfolder;
		files = new HashSet<String>();
	}

	@Override
	public void run() {
		while(true) {
			File f = new File(watchFolder);
			for(File file : f.listFiles()) {
				if(! files.contains(file.getName()))  {
					files.add(file.getName());
					main.addTorrent(watchFolder + file.getName());
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				System.out.println("FolderMonitor: sleep interrupted");
				e.printStackTrace();
			}
		}
	}

}
