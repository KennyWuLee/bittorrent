import org.junit.Test;

public class FolderTest {
	@Test
	public void main() {
		Main m = new Main();
		FolderMonitor mon = new FolderMonitor(m, "watch");
		Thread t = new Thread(mon);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			System.out.println("interrupted waiting for main");
			e.printStackTrace();
		}
	}
}
