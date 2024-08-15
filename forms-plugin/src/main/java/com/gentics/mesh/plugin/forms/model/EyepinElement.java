package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.Locale;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.forms.config.EyepinOptions;
import com.gentics.mesh.plugin.forms.i18n.I18nHelper;
import com.gentics.mesh.plugin.forms.util.RxUtil;
import com.gentics.mesh.util.StreamUtil;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Eyepin newsletter registration element
 */
@Reportable
public class EyepinElement extends FormElement {

	@JsonProperty
	private String value;

	@JsonProperty
	private String customerId;

	public EyepinElement() {
	}

	protected EyepinElement(EyepinElement other, boolean deep) {
		super(other, deep);

		this.value = other.value;
		this.customerId = other.customerId;
	}

	public String getCustomerId() {
		return customerId;
	}

	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	protected FormElement clone(boolean deep) {
		return new EyepinElement(this, deep);
	}

	@Override
	public Maybe<ValidationError> validate(MultiMap postedData, Set<FileUpload> fileUploads, NodeResponse formDataNode, Vertx rxVertx, OkHttpClient httpClient) {
		String value = getSingleFormData(postedData);

		ValidationError error = new ValidationError();
		if (StringUtils.isNotBlank(value)) {
			// eyepin element was checked, thus user mail must exist and have a value too
			boolean containsUserMail = postedData.entries().stream().anyMatch(v -> v.getKey().startsWith("usermail"));

			if (!containsUserMail) {
				error.setName(this.getName());
				error.setTitle(this.getLabel());
				error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.eyepin.missing-usermail"));

				log.debug("Form has no usermail element thus eyepin element cannot be used.");

				return Maybe.just(error);
			} else {
				postedData.forEach(e -> {
					if (e.getKey().startsWith("usermail")) {
						if (StringUtils.isBlank(e.getValue())) {
							// user mail is not valid
							error.setName(this.getName());
							error.setTitle(this.getLabel());
							error.setMessage(I18nHelper.get(Locale.GERMAN, "error.validation.eyepin.empty-usermail"));

							log.debug("Usermail element has no value thus eyepin element cannot be used.");
						}
					}
				});
			}
		}
		return RxUtil.fromCallableIf(() -> StringUtils.isNotBlank(error.getName()), () -> error);
	}

	/**
	 * Performs submit action
	 */
	@JsonIgnore
	public Completable performSubmit(Form form, MultiMap postedData, Vertx rxVertx, OkHttpClient httpClient) {
		log.debug("Form submitted: Contains eyepin element.");

		String value = getSingleFormData(postedData);

		if (StringUtils.isNotBlank(value)) {
			// insert each user mail to eyepin addresses
			return Single.zip(
					StreamUtil.toIterable(form.flattened().filter(e -> e instanceof UsermailElement).map(e -> (UsermailElement) e)
						.map(element -> {
							String userMail = element.getSingleFormData(postedData);
	
							if (StringUtils.isBlank(userMail)) {
								// TODO: return Warning
								log.debug("User mail is empty. Skip inserting address to eyepin.");
								return Completable.complete();
							}
							// add user mail to Eyepin addresses
							return insertAddressToEyepin(userMail, httpClient);
						// Map to some random value, only the event is what we need
						}).map(c -> c.toSingleDefault(1))),
					// Collecting all the completed events, send a new one
					allProcessed -> Completable.complete())
				.flatMapCompletable(c -> c);
		}
		return Completable.complete();
	}

	/**
	 * Send a request to eyepin to insert the user email to the address list of the
	 * customer id(=Mandant) that is configured in the form element.
	 * 
	 * https://docs.eyepin.com/api/address#address-insert
	 * 
	 * @param userMail user mail that should be added to addresses in eyepin
	 * @return 
	 */
	public Completable insertAddressToEyepin(String userMail, OkHttpClient httpClient) {
		if (StringUtils.isBlank(userMail)) {
			log.debug("User mail is empty.");
		} else {
			log.debug("Trying to insert address to Eyepin: {}", userMail);

			EyepinOptions eyepinOptions = getConfiguration().getEyepin();
			String password = eyepinOptions.getPassword();
			String username = eyepinOptions.getUsername();
			String apiUrl = eyepinOptions.getApiUrl();

			String customerId = this.getCustomerId();

			log.debug("Eyepin customer id: " + customerId);
			if (!StringUtils.isBlank(customerId)) {
				
				// prepare request
				MediaType mediaType = MediaType.parse("application/xml");
				String credential = Credentials.basic(username, password);

				RequestBody body = RequestBody.create(mediaType,
						"<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<addressinsert key=\"Email\" save-empty-fields=\"0\" overrule-blacklist=\"0\" customerid=\""
								+ customerId + "\">\n\t<email>" + userMail
								+ "</email>\n\t<status>1</status>\n  <handleoptin>1</handleoptin>\n</addressinsert>");
				Request request = new Request.Builder().url(apiUrl).post(body).addHeader("Content-Type", "text/xml")
						.addHeader("Authorization", credential).build();

				// send request
				Call call = httpClient.newCall(request);
				return Single.<Response>create(sub -> {
					call.enqueue(new Callback() {
						@Override
						public void onFailure(Call call, IOException e) {
							if (!sub.isDisposed()) {
								// TODO FIXME shouldn't this be actually thrown through Rx?
								//sub.onError(new IOException(String.format("I/O Error in %s %s : %s (%s)", HttpMethod.POST, apiUrl, e.getClass().getSimpleName(), e.getLocalizedMessage()), e));
								log.error("OkHttp POST to eyepin failed.", e);
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
				}).flatMapCompletable(response -> {
					int responseCode = response.code();

					if (responseCode == 200) {
						handleResponse(response);
					} else {
						log.error("OkHttp POST to eyepin failed with code " + responseCode + ": " + response.message());
					}
					return Completable.complete();
				});
			} else {
				log.warn("Eyepin element has no customer id set. Address not inserted.");
			}			
		}
		return Completable.complete();
	}

	/**
	 * Parse the eyepin response and log its response code and description.
	 * 
	 * @param respones XML response from eyepin
	 */
	private void handleResponse(Response response) {

		// check response content
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			ResponseBody responseBody = response.body();

			String xmlString = responseBody.string();
			log.debug("OkHttp POST to eyepin - response body: " + xmlString);

			db = factory.newDocumentBuilder();

			InputSource inStream = new InputSource();
			inStream.setCharacterStream(new StringReader(xmlString));
			Document doc = db.parse(inStream);

			NodeList nl = doc.getElementsByTagName("response");

			String responseBodyCode = "";
			String responseBodyDescription = "";
			for (int i = 0; i < nl.getLength(); i++) {
				if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {

					NodeList childNodes = nl.item(i).getChildNodes();
					for (int j = 0; j < childNodes.getLength(); j++) {
						if (childNodes.item(j).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
							String nodeName = childNodes.item(j).getNodeName();
							String nodeTextContent = childNodes.item(j).getTextContent();
							switch (nodeName) {
							case "code":
								responseBodyCode = nodeTextContent;
								break;
							case "description":
								responseBodyDescription = nodeTextContent;
								break;
							default:
								break;
							}
						}
					}
				}
			}
			if (responseBodyCode.equals("2000")) {
				log.debug("Eyepin response - code: " + responseBodyCode);
				log.debug("Eyepin response - description: " + responseBodyDescription);
			} else {
				log.error("Eyepin response - code: " + responseBodyCode);
				log.error("Eyepin response - description: " + responseBodyDescription);
			}
		} catch (Throwable e) {
			log.error("Eyepin response processing error", e);
		}
	}
}
