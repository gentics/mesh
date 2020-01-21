package com.gentics.mesh.core.ssl;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLTestClient {
	public static final String CLIENT_CERT_P12 = "src/test/resources/client-ssl/alice.p12";
	public static final String CLIENT_CERT_PEM = "src/test/resources/client-ssl/alice.pem";
	public static final String CLIENT_KEY_PEM = "src/test/resources/client-ssl/alice.key";
	public static final String CA_CERT = "src/test/resources/client-ssl/server.pem";
	public static final String FMT_TEST_URL = "https://localhost:%s/api/v1";

	public static void call(int port, boolean sendClientAuth) throws IOException {
		OkHttpClient client = initializeHttpClient(sendClientAuth);
		Request request = new Request.Builder().url(String.format(FMT_TEST_URL, port)).build();

		System.out.println("Performing request: " + request);

		Response response = client.newCall(request).execute();

		System.out.println("Received response: " + response);
	}

	private static OkHttpClient initializeHttpClient(boolean sendClientAuth) {
		KeyManager[] keyManagers = null;
		TrustManager[] trustManagers = null;

		if (sendClientAuth) {
			try {
				if ("pem".equals(System.getProperty("keyStoreType"))) {
					System.out.println("Loading private key from " + CLIENT_KEY_PEM);
					keyManagers = getKeyManagersPem("dummyPass".toCharArray());
				} else {
					System.out.println("Loading private key from " + CLIENT_CERT_P12);
					keyManagers = getKeyManagersPkcs12();
				}
			} catch (KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException | IOException | CertificateException e) {
				rethrow(e, "Could not create key managers");
			}
		} else {
			System.out.println("Not sending client certificate");
		}

		try {
			trustManagers = getTrustManagers();
		} catch (CertificateException | IOException | KeyStoreException | NoSuchAlgorithmException e) {
			rethrow(e, "Could not create trust managers");
		}

		SSLContext sslCtx = null;

		try {
			sslCtx = SSLContext.getInstance("TLS");
			sslCtx.init(keyManagers, trustManagers, null);
		} catch (NoSuchAlgorithmException | KeyManagementException e) {
			rethrow(e, "Could not create SSL context");
		}

		return new OkHttpClient().newBuilder()
			.hostnameVerifier((hostName, sslSession) -> true)
			.sslSocketFactory(sslCtx.getSocketFactory(), (X509TrustManager) trustManagers[0])
			.build();
	}

	private static KeyManager[] getKeyManagersPkcs12()
		throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance("PKCS12");
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		char[] keyStorePass = "12345".toCharArray();

		keyStore.load(new FileInputStream(CLIENT_CERT_P12), keyStorePass);
		keyManagerFactory.init(keyStore, keyStorePass);

		return keyManagerFactory.getKeyManagers();
	}

	private static KeyManager[] getKeyManagersPem(char[] keyStorePass)
		throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, IOException, CertificateException {
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate clientCert = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(CLIENT_CERT_PEM));
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		String principal = clientCert.getSubjectX500Principal().getName();

		try (PEMParser pemParser = new PEMParser(new FileReader(CLIENT_KEY_PEM))) {
			PrivateKey privateKey = new JcaPEMKeyConverter().getPrivateKey((PrivateKeyInfo) pemParser.readObject());
			keyStore.load(null);
			keyStore.setCertificateEntry(principal + "Cert", clientCert);
			keyStore.setKeyEntry(principal + "Key", privateKey, keyStorePass, new Certificate[] { clientCert });
			keyManagerFactory.init(keyStore, keyStorePass);
		}

		return keyManagerFactory.getKeyManagers();
	}

	private static TrustManager[] getTrustManagers() throws NoSuchAlgorithmException, KeyStoreException, IOException, CertificateException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		X509Certificate caCert = (X509Certificate) certificateFactory.generateCertificate(new FileInputStream(CA_CERT));

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
					return null;
				}
			}
		};
	}

	private static void rethrow(Throwable e, String msg) {
		System.err.println(msg + ": " + e.getMessage());

		throw new RuntimeException(e);
	}

}
