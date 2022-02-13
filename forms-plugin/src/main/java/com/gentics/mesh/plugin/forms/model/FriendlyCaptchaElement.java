package com.gentics.mesh.plugin.forms.model;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.config.ProxyOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Captcha Element. Implementation uses FriendlyCaptcha
 *
 * @TODO: Uses the same code as the ReCaptcha Element, we should rewrite this to use a utility class or an abstract class
 */
public class FriendlyCaptchaElement extends FormElement {
	protected final static String TITLE_KEY = "error.validation.captcha.title";

	protected final static String MESSAGE_KEY = "error.validation.captcha";

	public FriendlyCaptchaElement() {
	}

	protected FriendlyCaptchaElement(FriendlyCaptchaElement other, boolean deep) {
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
		return new FriendlyCaptchaElement(this, deep);
	}

	@Override
	public ValidationError validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode) {
		String solution = postedData.get(this.getConfiguration().getFriendlyCaptcha().getSolutionKey());

		if (isValid(solution)) {
			return null;
		} else {
			return new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY))
					.setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY));
		}
	}

	/**
	 * Check validity of the captcha response
	 *
	 * @param captchaSolution captcha response
	 * @return true iff the captcha response is valid
	 */
	protected boolean isValid(String captchaSolution) {
		log.debug("Trying to validate captchaResponse: {}", captchaSolution);
		List<String> fastFailerSolutions = new ArrayList<String>() {{
			add(".FETCHING");
			add(".UNSTARTED");
			add(".UNFINISHED");
		}};

		if (StringUtils.isBlank(captchaSolution)) {
			log.debug("Solution is empty");
			return false;
		} else if(fastFailerSolutions.contains(captchaSolution)) {
			// Values that are per definition false and so we don't need to ask the endpoint
			log.debug("Solution is a fast failer!");
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
				HttpPost request = new HttpPost(this.getConfiguration().getFriendlyCaptcha().getVerificationUrl());

				List<NameValuePair> form = new ArrayList<>();
				form.add(new BasicNameValuePair("secret", this.getConfiguration().getFriendlyCaptcha().getSecretKey()));
				form.add(new BasicNameValuePair("sitekey", this.getConfiguration().getFriendlyCaptcha().getSiteKey()));
				form.add(new BasicNameValuePair("solution", captchaSolution));
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
					} else if(this.getConfiguration().getFriendlyCaptcha().isAcceptOnErrorStatus()) {
						// With this configuration the recommendation from  here: https://docs.friendlycaptcha.com/#/verification_api?id=verification-best-practices
						// can be activated
						log.debug("AcceptOnErrorActive therefor this request is considered correct");
						log.error("Unexpected response status: %d of verification request", status);
						return true;
					} else {
						throw new ClientProtocolException(String.format("Unexpected response status: %d of verification request", status));
					}
				});
			} catch (IOException e) {
				log.error("Error while verifying " + this.getConfiguration().getFriendlyCaptcha().getSolutionKey(), e);
				return false;
			}
		}
	}
}
