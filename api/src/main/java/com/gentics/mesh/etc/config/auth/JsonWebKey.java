package com.gentics.mesh.etc.config.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonObject;

/**
 * POJO for a JWK - Reference by https://tools.ietf.org/html/rfc7517
 */
public class JsonWebKey {

	public static ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(Include.NON_NULL, Include.ALWAYS));
	}

	/**
	 * The "kty" (key type) parameter identifies the cryptographic algorithm family used with the key, such as "RSA" or "EC".
	 */
	@JsonProperty("kty")
	private String type;

	/**
	 * The "use" (public key use) parameter identifies the intended use of the public key. The "use" parameter is employed to indicate whether a public key is
	 * used for encrypting data or verifying the signature on data.
	 */
	@JsonProperty("use")
	private String publicKeyUse;

	/**
	 * The "key_ops" (key operations) parameter identifies the operation(s) for which the key is intended to be used. The "key_ops" parameter is intended for
	 * use cases in which public, private, or symmetric keys may be present.
	 */
	@JsonProperty("key_ops")
	private String operations;

	/**
	 * The "alg" (algorithm) parameter identifies the algorithm intended for use with the key.
	 */
	@JsonProperty("alg")
	private String algorithm;

	/**
	 * The "kid" (key ID) parameter is used to match a specific key.
	 */
	@JsonProperty("kid")
	private String id;

	/**
	 * The "x5u" (X.509 URL) parameter is a URI [RFC3986] that refers to a resource for an X.509 public key certificate or certificate chain [RFC5280].
	 */
	@JsonProperty("x5u")
	private String x509Url;

	/**
	 * The "x5c" (X.509 certificate chain) parameter contains a chain of one or more PKIX certificates [RFC5280].
	 */
	@JsonProperty("x5c")
	private List<String> x509chain;

	/**
	 * The "x5t" (X.509 certificate SHA-1 thumbprint) parameter is a base64url-encoded SHA-1 thumbprint (a.k.a. digest) of the DER encoding of an X.509
	 * certificate [RFC5280].
	 */
	@JsonProperty("x5t")
	private String x509sha1thumbprint;

	/**
	 * 
	 * The "x5t#S256" (X.509 certificate SHA-256 thumbprint) parameter is a base64url-encoded SHA-256 thumbprint (a.k.a. digest) of the DER encoding of an X.509
	 * certificate [RFC5280].
	 */
	@JsonProperty("x5t#S256")
	private String x509sha512thumbprint;

	/**
	 * The value of the "keys" parameter is an array of JWK values.
	 */
	private List<JsonWebKey> keys = new ArrayList<>();

	// Parameters for Elliptic Curve Private Keys

	/**
	 * The "d" (ECC private key) parameter contains the Elliptic Curve private key value.
	 */
	private String d;

	// Parameters for RSA Keys

	/**
	 * The "e" (exponent) parameter contains the exponent value for the RSA public key.
	 */
	private String e;

	/**
	 * The "n" (modulus) parameter contains the modulus value for the RSA public key.
	 */
	private String n;

	// Parameters for RSA Private Keys

	/**
	 * The "x" (x coordinate) parameter contains the x coordinate for the Elliptic Curve point.
	 */
	private String x;

	/**
	 * The "y" (y coordinate) parameter contains the y coordinate for the Elliptic Curve point.
	 */
	private String y;

	/**
	 * The "crv" (curve) parameter identifies the cryptographic curve used with the key.
	 */
	private String crv;

	/**
	 * The "p" (first prime factor) parameter contains the first prime factor.
	 */
	private String p;

	/**
	 * The "q" (second prime factor) parameter contains the second prime factor.
	 */
	private String q;

	/**
	 * The "dp" (first factor CRT exponent) parameter contains the Chinese Remainder Theorem (CRT) exponent of the first factor.
	 */
	private String dp;

	/**
	 * 
	 * The "dq" (second factor CRT exponent) parameter contains the CRT exponent of the second factor.
	 */
	private String dq;

	/**
	 * The "qi" (first CRT coefficient) parameter contains the CRT coefficient of the second factor.
	 */
	private String qi;

	/**
	 * The "oth" (other primes info) parameter contains an array of information about any third and subsequent primes, should they exist.
	 */
	private String oth;

	/**
	 * The "r" (prime factor) parameter within an "oth" array member represents the value of a subsequent prime factor.
	 */
	private String r;

	/**
	 * The "t" (factor CRT coefficient) parameter within an "oth" array member represents the CRT coefficient of the corresponding prime factor.
	 */
	private String t;

	// Parameters for Symmetric Keys

	/**
	 * The "k" (key value) parameter contains the value of the symmetric (or other single-valued) key.
	 */
	private String k;

	public String getAlgorithm() {
		return algorithm;
	}

	public JsonWebKey setAlgorithm(String alg) {
		this.algorithm = alg;
		return this;
	}

	public String getType() {
		return type;
	}

	public JsonWebKey setType(String kty) {
		this.type = kty;
		return this;
	}

	public String getPublicKeyUse() {
		return publicKeyUse;
	}

	public JsonWebKey setPublicKeyUse(String use) {
		this.publicKeyUse = use;
		return this;
	}

	public String getOperations() {
		return operations;
	}

	public JsonWebKey setOperations(String keyOperations) {
		this.operations = keyOperations;
		return this;
	}

	public String getId() {
		return id;
	}

	public JsonWebKey setId(String kid) {
		this.id = kid;
		return this;
	}

	public List<String> getX509chain() {
		return x509chain;
	}

	public JsonWebKey setX509chain(List<String> x509chain) {
		this.x509chain = x509chain;
		return this;
	}

	public String getX509sha1thumbprint() {
		return x509sha1thumbprint;
	}

	public JsonWebKey setX509sha1thumbprint(String x509sha1thumbprint) {
		this.x509sha1thumbprint = x509sha1thumbprint;
		return this;
	}

	public String getX509sha512thumbprint() {
		return x509sha512thumbprint;
	}

	public JsonWebKey setX509sha512thumbprint(String x509sha512thumbprint) {
		this.x509sha512thumbprint = x509sha512thumbprint;
		return this;
	}

	public String getX509Url() {
		return x509Url;
	}

	public JsonWebKey setX509Url(String x509Url) {
		this.x509Url = x509Url;
		return this;
	}

	public List<JsonWebKey> getKeys() {
		return keys;
	}

	public JsonWebKey setKeys(List<JsonWebKey> keys) {
		this.keys = keys;
		return this;
	}

	public String getCrv() {
		return crv;
	}

	public void setCrv(String crv) {
		this.crv = crv;
	}

	public String getD() {
		return d;
	}

	public void setD(String d) {
		this.d = d;
	}

	public String getDp() {
		return dp;
	}

	public void setDp(String dp) {
		this.dp = dp;
	}

	public String getDq() {
		return dq;
	}

	public void setDq(String dq) {
		this.dq = dq;
	}

	public String getE() {
		return e;
	}

	public void setE(String e) {
		this.e = e;
	}

	public String getK() {
		return k;
	}

	public void setK(String k) {
		this.k = k;
	}

	public String getN() {
		return n;
	}

	public JsonWebKey setN(String n) {
		this.n = n;
		return this;
	}

	public String getOth() {
		return oth;
	}

	public JsonWebKey setOth(String oth) {
		this.oth = oth;
		return this;
	}

	public String getP() {
		return p;
	}

	public JsonWebKey setP(String p) {
		this.p = p;
		return this;
	}

	public String getQ() {
		return q;
	}

	public JsonWebKey setQ(String q) {
		this.q = q;
		return this;
	}

	public String getQi() {
		return qi;
	}

	public JsonWebKey setQi(String qi) {
		this.qi = qi;
		return this;
	}

	public String getR() {
		return r;
	}

	public JsonWebKey setR(String r) {
		this.r = r;
		return this;
	}

	public String getT() {
		return t;
	}

	public JsonWebKey setT(String t) {
		this.t = t;
		return this;
	}

	public String getX() {
		return x;
	}

	public JsonWebKey setX(String x) {
		this.x = x;
		return this;
	}

	public String getY() {
		return y;
	}

	public JsonWebKey setY(String y) {
		this.y = y;
		return this;
	}

	public JsonObject toJson() {
		try {
			return new JsonObject(mapper.writeValueAsString(this));
		} catch (Exception e) {
			throw new EncodeException("Failed to encode as JSON: " + e.getMessage());
		}
	}

	public void validate() {
		Objects.requireNonNull(type, "The type of the JWK is missing.");
	}

}
