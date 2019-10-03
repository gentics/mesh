package com.gentics.mesh.plugin.formgenerator.model;

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
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.formgenerator.config.ProxyOptions;
import com.gentics.mesh.portal.i18n.I18nHelper;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;

/**
 * ReCaptcha Element. Implementation uses ReCaptcha
 */
public class ReCaptchaElement extends FormElement {
	/**
	 * URL for verification
	 */
	protected final static String VERIFICATION_URL = "https://www.google.com/recaptcha/api/siteverify";

	/**
	 * Key of the posted captcha response
	 */
	protected final static String RESPONSE_KEY = "g-recaptcha-response";

	protected final static String TITLE_KEY = "error.validation.captcha.title";

	protected final static String MESSAGE_KEY = "error.validation.captcha";

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
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		String captchaResponse = postedData.get(RESPONSE_KEY);

		if (StringUtils.isEmpty(captchaResponse)) {
			captchaResponse = postedData.get(getName());
		}

		if (isValid(captchaResponse)) {
			return null;
		} else {
			return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY))
				.setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY));
		}
	}

	/**
	 * Check validity of the captcha response
	 * 
	 * @param captchaResponse
	 *            captcha response
	 * @return true iff the captcha response is valid
	 */
	protected boolean isValid(String captchaResponse) {
		log.debug("Trying to validate captchaResponse: {}", captchaResponse);

		if (StringUtils.isBlank(captchaResponse)) {
			return false;
		} else {

			ProxyOptions proxyOptions = getConfiguration().getProxyOptions();

			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
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

			try (CloseableHttpClient client = clientBuilder.build()) {
				HttpPost request = new HttpPost(VERIFICATION_URL);

				List<NameValuePair> form = new ArrayList<>();
				form.add(new BasicNameValuePair("secret", getConfiguration().getReCaptcha().getSecretKey()));
				form.add(new BasicNameValuePair("response", captchaResponse));
				request.setEntity(new UrlEncodedFormEntity(form));

				if (log.isDebugEnabled()) {
					log.debug("Sending request: {}\n{}", request, request.getEntity());
				}

				return client.execute(request, response -> {
					int status = response.getStatusLine().getStatusCode();

					log.debug("Response status: {}", status);

					if (status >= 200 && status < 300) {
						HttpEntity responseEntity = response.getEntity();
						if (responseEntity != null) {
							JsonObject json = new JsonObject(EntityUtils.toString(responseEntity));

							if (log.isDebugEnabled()) {
								log.debug("Response:\n{}", json.encodePrettily());
							}

							return json.getBoolean("success", false);
						} else {
							throw new ClientProtocolException("Verification request did not return entity");
						}
					} else {
						throw new ClientProtocolException(String.format("Unexpected response status: %d of verification request", status));
					}
				});
			} catch (IOException e) {
				log.error("Error while verifying " + RESPONSE_KEY, e);
				return false;
			}
		}
	}
}
