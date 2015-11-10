package com.gentics.mesh.etc.config;

/**
 * JWT Authentication options POJO
 * @author philippguertler
 */
public class JWTAuthenticationOptions {
	
	public static final long DEFAULT_TOKEN_EXPIRATION_TIME = 60 * 60; //1 hour
	
	public static final String DEFAULT_KEYSTORE_PATH = "keystore.jceks";
	
	private long tokenExpirationTime = DEFAULT_TOKEN_EXPIRATION_TIME;
	
	private String signatureSecret;
	
	private String keystorePath = DEFAULT_KEYSTORE_PATH;

	/**
	 * Gets the time after which an authentication token should expire.
	 * @return The expiration time in seconds
	 */
	public long getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	/**
	 * Sets the time after which an authentication token should expire.
	 * @param tokenExpirationTime The expiration time in seconds
	 */
	public void setTokenExpirationTime(long tokenExpirationTime) {
		this.tokenExpirationTime = tokenExpirationTime;
	}

	/**
	 * Gets the secret passphrase which is used when singing the authentication token 
	 * @return
	 */
	public String getSignatureSecret() {
		return signatureSecret;
	}

	/**
	 * Sets the secret passphrase which is used when singing the authentication token 
	 * @return
	 */
	public void setSignatureSecret(String signatureSecret) {
		this.signatureSecret = signatureSecret;
	}

	/**
	 * Gets the path to the keystore file
	 * @return
	 */
	public String getKeystorePath() {
		return keystorePath;
	}
	
	/**
	 * Sets the path to the keystore file
	 * @return
	 */
	public void setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
	}
}
