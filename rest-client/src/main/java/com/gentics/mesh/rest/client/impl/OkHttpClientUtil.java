package com.gentics.mesh.rest.client.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;

/**
 * Utility for the OkHttp client.
 */
public final class OkHttpClientUtil {

	private static final Logger log = LoggerFactory.getLogger(OkHttpClientUtil.class);

	private OkHttpClientUtil() {
	}

	/**
	 * Create a new client using the {@link MeshRestClientConfig} as source for various settiings.
	 * 
	 * @param config
	 * @return
	 */
	public static OkHttpClient createClient(MeshRestClientConfig config) {

		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequestsPerHost(64);

		// We need a long timeout per default since some requests take a long time. For all tests a 1 minute timeout works fine.
		Builder builder = new OkHttpClient.Builder()
			.callTimeout(Duration.ofMinutes(1))
			.connectTimeout(Duration.ofMinutes(1))
			.writeTimeout(Duration.ofMinutes(1))
			.readTimeout(Duration.ofMinutes(1))
			.dispatcher(dispatcher);

		initializeHttpClient(builder, config);
		return builder.build();
	}

	/**
	 * Initialize a given client builder with the client config.
	 * 
	 * @param builder
	 * @param config
	 */
	private static void initializeHttpClient(Builder builder, MeshRestClientConfig config) {
		KeyManager[] keyManagers = null;
		TrustManager[] trustManagers = null;

		byte[] clientKey = config.getClientKey();
		byte[] clientCert = config.getClientCert();

		// Only create a key manager if the keys/certs have been specified.
		if (clientKey != null && clientCert != null) {
			try {
				keyManagers = getKeyManagersPem(config);
			} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | IOException | CertificateException e) {
				rethrow(e, "Could not create key managers");
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Not sending client certificate");
			}
		}

		if (config.getTrustedCAs() != null && !config.getTrustedCAs().isEmpty()) {
			try {
				trustManagers = getTrustManagers(config);
			} catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
				rethrow(e, "Could not create trust managers");
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("No trusted CA found. Trusting all CA's");
			}
			trustManagers = getDummyTrustManager();
		}

		SSLContext sslCtx = null;
		try {
			log.debug("Creating SSL context");
			sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(keyManagers, trustManagers, null);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			rethrow(e, "Could not create SSL context");
		}

		if (!config.isVerifyHostnames()) {
			log.debug("Disabling hostname verification");
			builder.hostnameVerifier((hostName, sslSession) -> true);
		}
		if (sslCtx != null) {
			builder.sslSocketFactory(sslCtx.getSocketFactory(), (X509TrustManager) trustManagers[0]);
		}

		if (config.getProtocolVersion() != null) {
			switch (config.getProtocolVersion()) {
			case DEFAULT:
				// Follows enclosed OkHttpClient::DEFAULT_PROTOCOLS.
				builder.protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1));
				break;
			case HTTP_1_1:
				builder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
				break;
			case HTTP_2:
				// Caution: OkHttpClient does not support standalone h2 protocol, so in the case of SSL connection will fall back to Protocol.HTTP_2 + Protocol.HTTP_1_1.
				builder.protocols(Collections.singletonList(Protocol.H2_PRIOR_KNOWLEDGE));
				break;
			}
		}
	}

	/**
	 * Return the keymanagers to be used for client cert authentication for the REST client.
	 * 
	 * @param config
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws UnrecoverableKeyException
	 * @throws IOException
	 * @throws CertificateException
	 */
	private static KeyManager[] getKeyManagersPem(MeshRestClientConfig config)
		throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException {
		// The keystore pass is only used to access the in-memory keystore thus a random PW is sufficient
		char[] randomKeyStorePass = UUIDUtil.randomUUID().toCharArray();
		byte[] clientKeyData = config.getClientKey();
		byte[] clientCertData = config.getClientCert();
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate clientCert = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(clientCertData));
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		String principal = clientCert.getSubjectX500Principal().getName();

		Reader targetReader = new InputStreamReader(new ByteArrayInputStream(clientKeyData));
		try (PEMParser pemParser = new PEMParser(targetReader)) {
			log.debug("Read client key PEM file");
			PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemParser.readObject());
			// Load the keystore with no loading params
			keyStore.load(null);
			keyStore.setCertificateEntry(principal + "Cert", clientCert);
			keyStore.setKeyEntry(principal + "Key", privateKey, randomKeyStorePass, new Certificate[] { clientCert });
			keyManagerFactory.init(keyStore, randomKeyStorePass);
		}

		return keyManagerFactory.getKeyManagers();
	}

	private static TrustManager[] getTrustManagers(MeshRestClientConfig config)
		throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

		// Load the keystore with no loading params
		keyStore.load(null);
		for (byte[] caCertData : config.getTrustedCAs()) {
			X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(caCertData));
			keyStore.setCertificateEntry(caCert.getSubjectX500Principal().getName(), caCert);
		}

		trustManagerFactory.init(keyStore);
		return trustManagerFactory.getTrustManagers();
	}

	private static TrustManager[] getDummyTrustManager() {
		return new TrustManager[] {
			new X509TrustManager() {
				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			}
		};
	}

	private static void rethrow(Throwable e, String msg) {
		log.error(msg + ": " + e.getMessage());
		throw new RuntimeException(e);
	}

}
