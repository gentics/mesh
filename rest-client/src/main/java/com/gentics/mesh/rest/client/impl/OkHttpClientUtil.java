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

public final class OkHttpClientUtil {

	private static final Logger log = LoggerFactory.getLogger(OkHttpClientUtil.class);

	private OkHttpClientUtil() {
	}

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

		if (config.getTrustedCA() != null) {
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

	}

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
			keyStore.load(null);
			keyStore.setCertificateEntry(principal + "Cert", clientCert);
			keyStore.setKeyEntry(principal + "Key", privateKey, randomKeyStorePass, new Certificate[] { clientCert });
			keyManagerFactory.init(keyStore, randomKeyStorePass);
		}

		return keyManagerFactory.getKeyManagers();
	}

	private static TrustManager[] getTrustManagers(MeshRestClientConfig config)
		throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
		byte[] caCertData = config.getTrustedCA();
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(caCertData));

		keyStore.load(null);
		keyStore.setCertificateEntry(caCert.getSubjectX500Principal().getName(), caCert);
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
