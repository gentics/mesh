package com.gentics.mesh.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.io.FileExistsException;

/**
 * Helper for keystore operations.
 */
public final class KeyStoreHelper {

	/**
	 * Create a keystore for the given path and store various keys in it which are needed for JWT.
	 * 
	 * @param keystorePath
	 * @param keystorePassword
	 * @throws NoSuchAlgorithmException
	 *             Thrown if the HmacSHA256 algorithm could not be found
	 * @throws KeyStoreException
	 * @throws IOException
	 * @throws CertificateException
	 */
	public static void gen(String keystorePath, String keystorePassword)
			throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		Objects.requireNonNull(keystorePassword, "The keystore password must be specified.");
		File keystoreFile = new File(keystorePath);
		if (keystoreFile.exists()) {
			throw new FileExistsException(keystoreFile);
		} else {
			if (keystoreFile.getParentFile() != null) {
				keystoreFile.getParentFile().mkdirs();
			}
			keystoreFile.createNewFile();
		}

		KeyGenerator keygen = KeyGenerator.getInstance("HmacSHA256");
		SecretKey key = keygen.generateKey();

		KeyStore keystore = KeyStore.getInstance("jceks");
		keystore.load(null, null);

		// This call throws an exception
		keystore.setKeyEntry("HS256", key, keystorePassword.toCharArray(), null);
		try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
			keystore.store(fos, keystorePassword.toCharArray());
			fos.flush();
		}

		// SecretKey keyRetrieved = (SecretKey) keystore.getKey("theKey", keystorePassword.toCharArray());
		// System.out.println(keyRetrieved.getAlgorithm());
	}

}
