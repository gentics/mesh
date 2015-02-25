import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.test.AbstractCoreApiVerticleTest;
import com.gentics.cailun.verticle.admin.AdminVerticle;

public class AdminVerticleTest extends AbstractCoreApiVerticleTest {

	@Autowired
	private AdminVerticle adminVerticle;

	@Override
	public AbstractCoreApiVerticle getVerticle() {
		return adminVerticle;
	}

	@Test
	public void testLoadSimpleNavigation() {
		fail("Not yet implemented");
	}

}
