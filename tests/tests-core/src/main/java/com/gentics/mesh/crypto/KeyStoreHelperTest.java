package com.gentics.mesh.crypto;

import org.junit.Test;

public class KeyStoreHelperTest {

	@Test
	public void testKeyStore() throws Exception {
		KeyStoreHelper.gen("hmac_store.jceks", "password");
	}
}
