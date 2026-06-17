package com.gentics.mesh.plugin.forms.context;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.util.ParameterUtil;
import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.model.FormtypeConfigsModel;
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.TranslationsService;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.FileUpload;

/**
 * This context holds everything that the form data create handlers need.
 */
public class FormDataCreateContext {
	private static final Logger log = LoggerFactory.getLogger(FormDataCreateContext.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String CTX_KEY_FORM = "FDS_Form";
	private static final String CTX_KEY_FORMNODE = "FDS_FormNode";
	private static final String CTX_KEY_FORMDATANODE = "FDS_FormDataNode";

	private final PluginContext context;
	private final Set<FileUpload> fileUploads;
	private final boolean handleCookies;
	private final boolean publish;
	private final String formUuid;
	private final String language; // Language of the request sent from the frontend
	private final Map<String, String> translations;
	private PostedData parsedPostedData;

	/**
	 * Get a Single that emits the instance or an error if validation on any of the parameters fails
	 * @param formtypeConfigs formtype configs
	 * @param pluginContext plugin context
	 * @param handleCookies true to handle cookies
	 * @param publish whether to publish or not
	 * @param translationsService translation service
	 * @return single
	 */
	public static Single<FormDataCreateContext> getCreate(FormtypeConfigsModel formtypeConfigs, PluginContext pluginContext, boolean handleCookies, boolean publish, TranslationsService translationsService) {
		return ParameterUtil.getUuidFromPath(pluginContext, "formUuid")
			.flatMap(formUuid -> ParameterUtil.getFirstLanguage(pluginContext, "")
				.flatMap(lang -> StringUtils.isBlank(lang)
					? Single.error(new MeshPluginException("lang query parameter is required", HttpResponseStatus.BAD_REQUEST.code()))
					: Single.just(lang))
				.flatMap(lang -> translationsService.getTranslations(formtypeConfigs, pluginContext.projectName(), formUuid, lang)
					.map(translations -> new FormDataCreateContext(pluginContext, handleCookies, publish, formUuid, lang, translations))));
	}

	protected FormDataCreateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String language, Map<String, String> translations) {
		this.context = pluginContext;
		this.handleCookies = handleCookies;
		this.publish = publish;
		this.formUuid = formUuid;
		this.language = language;
		this.translations = Collections.unmodifiableMap(translations);
		fileUploads = new HashSet<>(context.fileUploads());
		// remove empty uploads
		fileUploads.removeIf(up -> up.size() == 0);
	}

	public String getProjectName() {
		return context.project().getString("name");
	}

	public String getFormUuid() {
		return formUuid;
	}

	/**
	 * Returns the form field data as a flat {@link Map} (field ID → value), with
	 * {@link com.gentics.mesh.plugin.forms.model.AdminMailElement} identifiers already reverse-mapped
	 * to real email addresses.
	 *
	 * <p>The raw JSON is either parsed lazily from the {@code formdata} multipart part sent by FormGen or
	 * from the request body, when the request header Content-Type: application/json is found.
	 * Parsing is cached; the AdminMailElement reverse-mapping is applied on every call so it
	 * always reflects the current form (set via {@link #setForm(Form)}).</p>
	 *
	 * <p>Each property in {@link FormData#getProperties()} is mapped as follows during parsing:</p>
	 * <ul>
	 *   <li><b>String value/values</b> — stored as-is.</li>
	 *   <li><b>Possible-value objects</b> ({@code {identifier, displayName}}) — only the
	 *       {@code identifier} is stored. For {@code AdminMailElement}, FormGen sends
	 *       {@code identifier = displayName}; the reverse-mapping restores the real email address.</li>
	 *   <li><b>Binary metadata objects</b> and other non-string types — skipped; binary fields
	 *       are handled separately via file uploads.</li>
	 * </ul>
	 *
	 * @return Map of field ID → string value(s); empty map if {@code formdata} is absent or unparseable
	 */
	public PostedData getPostedData() {
		if (parsedPostedData == null) {
			String formdataJson = null;
			String contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE);
			if (Strings.CI.startsWith(contentType, HttpConstants.APPLICATION_JSON)) {
				formdataJson = context.body().asString();
			} else {
				formdataJson = context.request().formAttributes().get("formdata");
			}

			if (formdataJson == null || formdataJson.isBlank()) {
				parsedPostedData = new PostedData(getForm());
			} else {
				try {
					FormData formData = OBJECT_MAPPER.readValue(formdataJson, FormData.class);
					parsedPostedData = Optional.ofNullable(formData.getProperties()).map(props -> new PostedData(getForm(), props)).orElseGet(() -> new PostedData(getForm()));

					Set<String> elementKeys = getForm().getElementsInsideProperties().filter(FormElement::isActive).map(FormElement::getKey).collect(Collectors.toSet());

					parsedPostedData.keySet().retainAll(elementKeys);

				} catch (Exception e) {
					log.warn("Could not parse formdata JSON", e);
					parsedPostedData = new PostedData(getForm());
				}
			}
			parsedPostedData = FormService.adminMailLabelToValue(getForm(), parsedPostedData, language);
		}
		return parsedPostedData;
	}

	public String getReferer() {
		return context.request().getHeader("Referer");
	}

	public Set<FileUpload> getFileUploads() {
		return fileUploads;
	}

	public PluginContext getContext() {
		return context;
	}

	public Form getForm() {
		return context.get(CTX_KEY_FORM);
	}

	public FormDataCreateContext setForm(Form form) {
		context.put(CTX_KEY_FORM, form);
		return this;
	}

	public NodeResponse getFormDataNode() {
		return context.get(CTX_KEY_FORMDATANODE);
	}

	public FormDataCreateContext setFormDataNode(NodeResponse formDataNode) {
		context.put(CTX_KEY_FORMDATANODE, formDataNode);
		return this;
	}

	public NodeResponse getFormNode() {
		return context.get(CTX_KEY_FORMNODE);
	}

	public FormDataCreateContext setFormNode(NodeResponse formNode) {
		context.put(CTX_KEY_FORMNODE, formNode);
		return this;
	}

	public boolean isPublish() {
		return publish;
	}

	public boolean handleCookies() {
		return handleCookies;
	}

	public String getLanguage() {
		return language;
	}

	public Map<String, String> getTranslations() {
		return translations;
	}
}
