import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class UtilTest {

	@Test
	public void bigEndianTest() {
		int num = 60337;
		byte[] data = Util.intToBigEndian(num, 4);
		System.out.println(Arrays.toString(data));
		int result = (int) Util.bigEndianToInt(data);
		System.out.println(result);
		assertEquals(num, result);
	}
	
	@Test
	public void bigEndianTest2() {
		int num = 1;
		byte[] data = Util.intToBigEndian(num, 4);
		System.out.println(Arrays.toString(data));
	}
}
