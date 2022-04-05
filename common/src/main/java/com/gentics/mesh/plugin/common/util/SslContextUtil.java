package com.gentics.mesh.plugin.common.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Utility methods to setup SSL contexts which use client certificates.
 */
public class SslContextUtil {

	/**
	 * Initialize a SSL context with the given key and trust managers.
	 *
	 * @param keyManagers The key managers to use
	 * @param trustManagers The trust managers to use
	 * @return A SSL context initialized with the specified key an trust managers
	 */
	public static SSLContext getSslContext(KeyManager[] keyManagers, TrustManager[] trustManagers) throws
			KeyManagementException,
			NoSuchAlgorithmException {
		SSLContext sslCtx = SSLContext.getInstance("TLS");

		sslCtx.init(keyManagers, trustManagers, null);

		return sslCtx;

	}

	/**
	 * Initialize a SSL context with the given key and trust managers, and
	 * return its socket factory.
	 *
	 * @see #getSslContext(KeyManager[], TrustManager[])
	 * @param keyManagers The key managers to use
	 * @param trustManagers The trust managers to use
	 * @return The socket factory of the initialized SSL context
	 */
	public static SSLSocketFactory getSslSocketFactory(KeyManager[] keyManagers, TrustManager[] trustManagers) throws
			KeyManagementException,
			NoSuchAlgorithmException {
		return getSslContext(keyManagers, trustManagers).getSocketFactory();
	}

	/**
	 * Get the keymanagers from the keystore from the given input stream.
	 *
	 * <p>
	 *     The input certificate must be in PKCS #12 format.
	 * </p>
	 *
	 * <p>
	 *     <strong>WARNING:</strong> the input stream is consumed by this method.
	 * </p>
	 *
	 * @param cert The certificate to extract the key managers from
	 * @param keyStorePass The password for the certificate
	 * @return The keymanagers from the given keystore
	 */
	public static KeyManager[] getKeyManagersPkcs12(InputStream cert, char[] keyStorePass) throws
			KeyStoreException,
			NoSuchAlgorithmException,
			UnrecoverableKeyException,
			IOException,
			CertificateException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		keyManagerFactory.init(getPkcs12KeyStore(cert, keyStorePass), keyStorePass);

		return keyManagerFactory.getKeyManagers();
	}

	/**
	 * Get the keymanagers from the given keystore.
	 *
	 * @param keyStore The keystore to create the keymanagers from
	 * @param keyStorePass The keystore passphrase
	 * @return The keymanagers created from the given keystore
	 */
	public static KeyManager[] getKeyManagersFromKeystore(KeyStore keyStore, char[] keyStorePass) throws
			NoSuchAlgorithmException,
			UnrecoverableKeyException,
			KeyStoreException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		keyManagerFactory.init(keyStore, keyStorePass);

		return keyManagerFactory.getKeyManagers();
	}

	/**
	 * Create a {@link KeyStore} from the given PKCS12 certificate.
	 *
	 * <p>
	 *     <strong>WARNING:</strong> the input stream is consumed by this method.
	 * </p>
	 *
	 * @param cert The certificate to extract the key managers from
	 * @param keyStorePass The password for the certificate
	 * @return The KeyStore created from the certificate
	 */
	public static KeyStore getPkcs12KeyStore(InputStream cert, char[] keyStorePass) throws
			CertificateException,
			IOException,
			NoSuchAlgorithmException,
			KeyStoreException {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");

		keyStore.load(cert, keyStorePass);

		return keyStore;
	}

	/**
	 * Get the keymanagers from the keystore from the given input stream.
	 *
	 * <p>
	 *     The input certificate and key must be in PEM format.
	 * </p>
	 *
	 * @param cert The certificate to extract the key managers from
	 * @param keyStorePass The password for the certificate
	 * @return The keymanagers from the given keystore
	 */
	public static KeyManager[] getKeyManagersPem(InputStream cert, InputStream key, char[] keyStorePass) throws
			KeyStoreException,
			NoSuchAlgorithmException,
			UnrecoverableKeyException,
			IOException,
			CertificateException {
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate clientCert = (X509Certificate) certificateFactory.generateCertificate(cert);
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		String principal = clientCert.getSubjectX500Principal().getName();

		PEMParser pemParser = new PEMParser(new InputStreamReader(key));
		PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemParser.readObject());

		keyStore.load(null);
		keyStore.setCertificateEntry(principal, clientCert);
		keyStore.setKeyEntry(principal, privateKey, keyStorePass, new Certificate[] { clientCert });
		keyManagerFactory.init(keyStore, keyStorePass);

		return keyManagerFactory.getKeyManagers();
	}

	/**
	 * Get the trust managers from the keystore from the given input stream.
	 *
	 * <p>
	 *     The input certificate must be in PEM format.
	 * </p>
	 *
	 * @param caCert The trusted certificates
	 *
	 * @return The trust managers from the given keystore
	 */
	public static TrustManager[] getTrustManagers(InputStream caCert) throws
			NoSuchAlgorithmException,
			KeyStoreException,
			IOException,
			CertificateException {
		TrustManagerFactory trustManagerFactory =
			TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		X509Certificate x509Cert = getX509Certificate(caCert);

		keyStore.load(null);
		keyStore.setCertificateEntry(x509Cert.getSubjectX500Principal().getName(), x509Cert);
		trustManagerFactory.init(keyStore);

		return trustManagerFactory.getTrustManagers();
	}

	/**
	 * Create a X.509 certificate from the given input.
	 *
	 * <p>
	 *     The input certificate must be in PEM format.
	 * </p>
	 *
	 * @param cert The certificate to load
	 * @return The X.509 certificate instance created from the input
	 */
	public static X509Certificate getX509Certificate(InputStream cert) throws CertificateException {
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

		return (X509Certificate) certificateFactory.generateCertificate(cert);
	}

	/**
	 * Get a dummy trust manager that will accept all connections.
	 *
	 * <p>
	 *     <strong>NOTE</strong> that this should only be used for testing
	 *     purposes.
	 * </p>
	 *
	 * @return A dummy trust manager that will accept all connections
	 */
	public static TrustManager[] getDummyTrustManager() {
		return new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			}
		};
	}
}
