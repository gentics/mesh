package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
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
	public final static String TITLE_KEY = "error.validation.captcha.title";
	public final static String MESSAGE_KEY = "error.validation.captcha";
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
				.flatMap(maybeError -> maybeError
						.map(error -> Maybe.just(new ValidationError()
							.setName(getName())
							.setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY)).setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY) + " " + error)))
						.orElse(RxUtil.emptyMaybe()));
	}

	/**
	 * Check validity of the captcha response
	 *
	 * @param captchaSolution captcha response
	 * @param httpClient 
	 * @return async optional error message, if the captcha is not valid
	 */
	protected Single<Optional<String>> isValid(String captchaSolution, OkHttpClient httpClient) {
		log.debug("Trying to validate captchaResponse: {}", captchaSolution);

		if (StringUtils.isBlank(captchaSolution)) {
			return Single.just(Optional.of("Solution is empty"));
		} else if(FAST_FAILER_SOLUTIONS.contains(captchaSolution)) {
			// Values that are per definition false and so we don't need to ask the endpoint
			return Single.just(Optional.of("Unacceptable solution"));
		} else {
			// prepare request
			FormBody formBody = new FormBody.Builder()
					.add("secret", this.getConfiguration().getFriendlyCaptcha().getSecretKey())
					.add("sitekey", this.getConfiguration().getFriendlyCaptcha().getSiteKey())
					.add("solution", captchaSolution)
					.build();

			Request request = new Request.Builder()
					.url(this.getConfiguration().getFriendlyCaptcha().getVerificationUrl())
					.post(formBody)
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
						boolean valid = json.getBoolean("success", false);
						if (valid) {
							return Optional.<String>empty();
						} else {
							return Optional.of(json.getString("details", "Unknown failure"));
						}
					} else if(this.getConfiguration().getFriendlyCaptcha().isAcceptOnErrorStatus()) {
						// With this configuration the recommendation from  here: https://docs.friendlycaptcha.com/#/verification_api?id=verification-best-practices
						// can be activated
						//log.debug("AcceptOnErrorActive therefore this request is considered correct");
						log.warn("Unexpected response status: {} of verification request", status);
						return Optional.<String>empty();
					} else {
						throw new IllegalStateException(String.format("Unexpected response status: %d of verification request", status));
					}
				} else {
					throw new IllegalStateException(String.format("Unexpected response status: %d of verification request", status));
				}
			}).onErrorResumeNext(e -> {
				log.error("Error while requesting " + this.getConfiguration().getFriendlyCaptcha().getSolutionKey(), e);
				return Single.just(Optional.of("Captcha validation request error"));
			});
		}
	}
}
