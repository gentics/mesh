package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.plugin.forms.config.ProxyOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.util.RxUtil;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Captcha Element. Implementation uses FriendlyCaptcha
 *
 * @TODO: Uses the same code as the ReCaptcha Element, we should rewrite this to use a utility class or an abstract class
 */
public class FriendlyCaptchaElement extends FormElement {
	protected final static String TITLE_KEY = "error.validation.captcha.title";
	protected final static String MESSAGE_KEY = "error.validation.captcha";
	protected final static List<String> FAST_FAILER_SOLUTIONS = Arrays.asList(".FETCHING", ".UNSTARTED", ".UNFINISHED");

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
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient) {
		String solution = postedData.get(this.getConfiguration().getFriendlyCaptcha().getSolutionKey());

		return isValid(solution, httpClient).toMaybe()
				.flatMap(valid -> valid 
						? RxUtil.emptyMaybe()
						: Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY)).setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY))));
	}

	/**
	 * Check validity of the captcha response
	 *
	 * @param captchaSolution captcha response
	 * @param httpClient 
	 * @return async true iff the captcha response is valid
	 */
	protected Single<Boolean> isValid(String captchaSolution, OkHttpClient httpClient) {
		log.debug("Trying to validate captchaResponse: {}", captchaSolution);

		if (StringUtils.isBlank(captchaSolution)) {
			log.debug("Solution is empty");
			return Single.just(false);
		} else if(FAST_FAILER_SOLUTIONS.contains(captchaSolution)) {
			// Values that are per definition false and so we don't need to ask the endpoint
			log.debug("Solution is a fast failer!");
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

			// prepare request
			FormBody formBody = new FormBody.Builder()
					.addEncoded("secret", this.getConfiguration().getFriendlyCaptcha().getSecretKey())
					.addEncoded("sitekey", this.getConfiguration().getFriendlyCaptcha().getSiteKey())
					.addEncoded("solution", captchaSolution)
					.build();

			Request request = new Request.Builder()
					.url(this.getConfiguration().getFriendlyCaptcha().getVerificationUrl())
					.post(formBody)
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.build();

			// send request
			Call call = httpClient.newCall(request);
			return Single.<Response>create(sub -> {
				call.enqueue(new Callback() {
					@Override
					public void onFailure(Call call, IOException e) {
						if (!sub.isDisposed()) {
							// TODO FIXME shouldn't this be actually thrown through Rx?
							//sub.onError(new IOException(String.format("I/O Error in %s %s : %s (%s)", HttpMethod.POST, apiUrl, e.getClass().getSimpleName(), e.getLocalizedMessage()), e));
							log.error("OkHttp POST to ReCaptcha failed.", e);
							sub.onSuccess(new Response.Builder().code(500).message(e.getMessage()).build());
						}
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException {
						if (!sub.isDisposed()) {
							sub.onSuccess(response);
						} else {
							response.close();
						}
					}
				});
			}).map(response -> {
				int status = response.code();

				log.debug("Response status: {}", status);

				if (status >= 200 && status < 300) {
					ResponseBody responseEntity = response.body();
					if (responseEntity != null) {
						// https://developers.google.com/recaptcha/docs/verify#api-response
						String body = responseEntity.string();
						JsonObject json = new JsonObject(body);

						if (log.isDebugEnabled()) {
							log.debug("Response:\n{}", json.encodePrettily());
						}
						return json.getBoolean("success", false);
					} else if(this.getConfiguration().getFriendlyCaptcha().isAcceptOnErrorStatus()) {
						// With this configuration the recommendation from  here: https://docs.friendlycaptcha.com/#/verification_api?id=verification-best-practices
						// can be activated
						//log.debug("AcceptOnErrorActive therefor this request is considered correct");
						log.warn("Unexpected response status: {} of verification request", status);
						return true;
					} else {
						throw new IllegalStateException(String.format("Unexpected response status: %d of verification request", status));
					}
				} else {
					throw new IllegalStateException(String.format("Unexpected response status: %d of verification request", status));
				}
			}).onErrorResumeNext(e -> {
				log.error("Error while requesting " + this.getConfiguration().getFriendlyCaptcha().getSolutionKey(), e);
				return Single.just(false);
			});
		}
	}
}
