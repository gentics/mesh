package com.gentics.mesh.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Utility to handle SSL / Keystore related operations.
 */
public final class SSLUtil {

	public static final String KEYSTORE_PASSWORD = "changeit";

	private SSLUtil() {
	}

	/**
	 * Add the given cert to the default keystore.
	 * 
	 * @param certPath
	 */
	public static void updateKeyStore(String certPath) {
		try {
			char[] passphrase = KEYSTORE_PASSWORD.toCharArray();
			InputStream certIn = ClassLoader.class.getResourceAsStream(certPath);

			final char sep = File.separatorChar;
			File dir = new File(System.getProperty("java.home") + sep + "lib" + sep + "security");
			File file = new File(dir, "cacerts");
			InputStream localCertIn = new FileInputStream(file);

			KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(localCertIn, passphrase);
			if (keystore.containsAlias("myAlias")) {
				certIn.close();
				localCertIn.close();
				return;
			}
			localCertIn.close();

			BufferedInputStream bis = new BufferedInputStream(certIn);
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			while (bis.available() > 0) {
				Certificate cert = cf.generateCertificate(bis);
				keystore.setCertificateEntry("myAlias", cert);
			}

			certIn.close();

			OutputStream out = new FileOutputStream(file);
			keystore.store(out, passphrase);
			out.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}
}
