package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.config.ProxyOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;

/**
 * Captcha Element. Implementation uses ReCaptcha
 */
public class ReCaptchaElement extends FormElement {
	/**
	 * URL for verification
	 */
	public final static String VERIFICATION_URL = "https://www.google.com/recaptcha/api/siteverify";

	/**
	 * Key of the posted captcha response
	 */
	protected final static String RESPONSE_KEY = "g-recaptcha-response";

	protected final static String TITLE_KEY = "error.validation.captcha.title";

	protected final static String MESSAGE_KEY = "error.validation.captcha";

	public ReCaptchaElement() {
	}

	protected ReCaptchaElement(ReCaptchaElement other, boolean deep) {
		super(other, deep);
	}

	@Override
	public void addFieldSchema(FieldSchemaContainer container) {
	}

	@Override
	public String getSingleFormData(MultiMap postedData) {
		return null;
	}

	@Override
	public List<String> getMultiFormData(MultiMap postedData) {
		return null;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new ReCaptchaElement(this, deep);
	}

	@Override
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx) {
		String captchaResponse = postedData.get(RESPONSE_KEY);

		if (StringUtils.isEmpty(captchaResponse)) {
			captchaResponse = postedData.get(getName());
		}
		return isValid(captchaResponse).toMaybe()
				.flatMap(valid -> valid 
						? RxUtil.emptyMaybe()
						: Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY)).setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY))));
	}

	/**
	 * Check validity of the captcha response
	 *
	 * @param captchaResponse
	 *            captcha response
	 * @return true iff the captcha response is valid
	 */
	protected Single<Boolean> isValid(String captchaResponse) {
		log.debug("Trying to validate captchaResponse: {}", captchaResponse);

		if (StringUtils.isBlank(captchaResponse)) {
			return Single.just(false);
		} else {

			ProxyOptions proxyOptions = getConfiguration().getProxyOptions();

			HttpAsyncClientBuilder clientBuilder = HttpAsyncClientBuilder.create();
			clientBuilder.useSystemProperties();

			if (proxyOptions != null && proxyOptions.isActive()) {
				log.debug("Using proxy:\n{}", proxyOptions);

				clientBuilder.setProxy(new HttpHost(proxyOptions.getHost(), Integer.parseInt(proxyOptions.getPort())));

				if (!StringUtils.isBlank(proxyOptions.getUser()) && !StringUtils.isBlank(proxyOptions.getPassword())) {
					CredentialsProvider credsProvider = new BasicCredentialsProvider();
					credsProvider.setCredentials(
						new AuthScope(proxyOptions.getHost(), Integer.parseInt(proxyOptions.getPort())),
						new UsernamePasswordCredentials(proxyOptions.getUser(), proxyOptions.getPassword()));

					clientBuilder.setDefaultCredentialsProvider(credsProvider);
					clientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
				}
			}

			try (CloseableHttpAsyncClient client = clientBuilder.build()) {
				String url = getConfiguration().getReCaptcha().getVerificationUrl();
				if (StringUtils.isBlank(url)) {
					url = VERIFICATION_URL;
				}
				HttpPost request = new HttpPost(url);

				List<NameValuePair> form = new ArrayList<>();
				form.add(new BasicNameValuePair("secret", getConfiguration().getReCaptcha().getSecretKey()));
				form.add(new BasicNameValuePair("response", captchaResponse));
				request.setEntity(new UrlEncodedFormEntity(form));

				if (log.isDebugEnabled()) {
					log.debug("Sending request: {}\n{}", request, request.getEntity());
				}

				return Single.fromFuture(client.execute(request, null)).map(response -> {
					int status = response.getStatusLine().getStatusCode();

					log.debug("Response status: {}", status);

					if (status >= 200 && status < 300) {
						HttpEntity responseEntity = response.getEntity();
						if (responseEntity != null) {
							// https://developers.google.com/recaptcha/docs/verify#api-response
							String body = EntityUtils.toString(responseEntity);
							JsonObject json = new JsonObject(body);

							boolean success = json.getBoolean("success", false);
							boolean logError = false;

							if (!success) {
								List<String> errorCodes = JsonPath
										.parse(body,
												Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build())
										.read("$.error-codes");

								if (errorCodes != null) {
									for (String code : errorCodes) {
										switch (code) {
										case "missing-input-secret":
										case "invalid-input-secret":
										case "bad-request":
											// these are considered "abnormal" error codes (e.g. the input secret is missing or invalid or the request is invalid)
											logError = true;
											break;
										case "missing-input-response":
										case "invalid-input-response":
										case "timeout-or-duplicate":
											// these are considered "normal" error codes (e.g. the user did not fill out the recaptcha or filling out took too long)
											break;
										default:
											break;
										}
									}
								}
							}

							if (log.isDebugEnabled()) {
								log.debug("Recaptcha validation {}. Response:\n{}", success ? "succeeded" : "failed", json.encodePrettily());
							}
							if (!success && logError) {
								log.error("Recaptcha validation failed. Response:\n{}", json.encodePrettily());
							}
							return success;
						} else {
							throw new ClientProtocolException("Verification request did not return entity");
						}
					} else {
						throw new ClientProtocolException(String.format("Unexpected response status: %d of verification request", status));
					}
				});
			} catch (IOException e) {
				log.error("Error while verifying " + RESPONSE_KEY, e);
				return Single.just(false);
			}
		}
	}
}
