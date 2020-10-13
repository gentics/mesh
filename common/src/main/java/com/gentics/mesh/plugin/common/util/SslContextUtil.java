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
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		keyStore.load(cert, keyStorePass);
		keyManagerFactory.init(keyStore, keyStorePass);

		return keyManagerFactory.getKeyManagers();
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
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate x509Cert = (X509Certificate) certificateFactory.generateCertificate(caCert);

		keyStore.load(null);
		keyStore.setCertificateEntry(x509Cert.getSubjectX500Principal().getName(), x509Cert);
		trustManagerFactory.init(keyStore);

		return trustManagerFactory.getTrustManagers();
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
