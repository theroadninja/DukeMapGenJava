package trn;
import org.junit.Assert;
import org.junit.Test;


public class MainTests {

	@Test
	public void testPlumbing(){
		Assert.assertTrue(Main.DOSPATH != "");
	}
}
