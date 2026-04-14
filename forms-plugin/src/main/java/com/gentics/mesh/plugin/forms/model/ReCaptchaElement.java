package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Captcha Element. Implementation uses ReCaptcha
 */
@FormElementType("recaptcha")
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
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient) {
		String captchaResponse = postedData.get(RESPONSE_KEY);

		if (StringUtils.isEmpty(captchaResponse)) {
			captchaResponse = postedData.get(getName());
		}
		return isValid(captchaResponse, httpClient).toMaybe()
				.flatMap(valid -> valid 
						? RxUtil.emptyMaybe()
						: Maybe.just(new ValidationError().setName(getName()).setTitle(I18nHelper.get(Locale.GERMAN, TITLE_KEY)).setMessage(I18nHelper.get(Locale.GERMAN, MESSAGE_KEY)).setPreventStoring(true)));
		}

	/**
	 * Check validity of the captcha response
	 *
	 * @param captchaResponse
	 *            captcha response
	 * @param httpClient 
	 * @return true iff the captcha response is valid
	 */
	protected Single<Boolean> isValid(String captchaResponse, OkHttpClient httpClient) {
		log.debug("Trying to validate captchaResponse: {}", captchaResponse);

		if (StringUtils.isBlank(captchaResponse)) {
			return Single.just(false);
		} else {
			String url = getConfiguration().getReCaptcha().getVerificationUrl();
			if (StringUtils.isBlank(url)) {
				url = VERIFICATION_URL;
			}

			var secret = getConfiguration().getReCaptcha().getSecretKey();

			if (StringUtils.isBlank(secret)) {
				log.error("ReCaptcha secret key is missing, validation of reCaptcha element {} will always fail.", getName());

				return Single.just(false);
			}

			// prepare request
			FormBody formBody = new FormBody.Builder()
					.add("secret", secret)
					.add("response", captchaResponse)
					.build();

			Request request = new Request.Builder().url(url).post(formBody).build();

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
						throw new IllegalStateException("Verification request did not return entity");
					}
				} else {
					throw new IllegalStateException(String.format("Unexpected response status: %d of verification request", status));
				}
			}).onErrorResumeNext(e -> {
				log.error("Error while requesting " + RESPONSE_KEY, e);
				return Single.just(false);
			});
		}
	}
}
