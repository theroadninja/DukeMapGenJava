package trn;
import org.junit.Assert;
import org.junit.Test;


public class MainTests {

	@Test
	public void testPlumbing(){
		Assert.assertEquals("hello world", trn.Main.HELLO);
	}
}
