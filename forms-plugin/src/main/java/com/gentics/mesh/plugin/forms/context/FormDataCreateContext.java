package com.gentics.mesh.plugin.forms.context;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.gentics.mesh.plugin.forms.model.schema.FormData;
import com.gentics.mesh.plugin.forms.model.schema.FormDataProperty;
import com.gentics.mesh.plugin.forms.service.FormService;

import io.reactivex.Single;
import io.vertx.core.MultiMap;
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
	private MultiMap parsedPostedData;

	/**
	 * Get a Single that emits the instance or an error if validation on any of the parameters fails
	 * @param pluginContext plugin context
	 * @param handleCookies true to handle cookies
	 * @return single
	 */
	public static Single<FormDataCreateContext> getCreate(PluginContext pluginContext, boolean handleCookies, boolean publish) {
		Single<String> formUuidSingle = ParameterUtil.getUuidFromPath(pluginContext, "formUuid");
		Single<String> languageSingle = ParameterUtil.getFirstLanguage(pluginContext, "")
				.flatMap(lang -> lang.isEmpty()
						? Single.error(new MeshPluginException("lang query parameter is required", 400))
						: Single.just(lang));

		return Single.zip(formUuidSingle, languageSingle,
				(formUuid, language) -> new FormDataCreateContext(pluginContext, handleCookies, publish, formUuid, language));
	}

	protected FormDataCreateContext(PluginContext pluginContext, boolean handleCookies, boolean publish, String formUuid, String language) {
		this.context = pluginContext;
		this.handleCookies = handleCookies;
		this.publish = publish;
		this.formUuid = formUuid;
		this.language = language;
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
	 * Returns the form field data as a flat {@link MultiMap} (field ID → value), with
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
	 * @return MultiMap of field ID → string value(s); empty map if {@code formdata} is absent or unparseable
	 */
	public MultiMap getPostedData() {
		if (parsedPostedData == null) {
			String formdataJson = null;
			String contentType = context.request().getHeader(HttpHeaders.CONTENT_TYPE);
			if (Strings.CI.startsWith(contentType, HttpConstants.APPLICATION_JSON)) {
				formdataJson = context.body().asString();
			} else {
				formdataJson = context.request().formAttributes().get("formdata");
			}

			if (formdataJson == null || formdataJson.isBlank()) {
				parsedPostedData = MultiMap.caseInsensitiveMultiMap();
			} else {
				try {
					FormData formData = OBJECT_MAPPER.readValue(formdataJson, FormData.class);
					MultiMap result = MultiMap.caseInsensitiveMultiMap();
					if (formData.getProperties() != null) {
						for (Map.Entry<String, FormDataProperty> entry : formData.getProperties().entrySet()) {
							String key = entry.getKey();
							FormDataProperty prop = entry.getValue();
							List<Object> values = prop.getValues();
							if (values != null && !values.isEmpty()) {
								for (Object v : values) {
									String sv = extractStringValue(v);
									if (sv != null) {
										result.add(key, sv);
									}
								}
							} else {
								String sv = extractStringValue(prop.getValue());
								if (sv != null) {
									result.add(key, sv);
								}
							}
						}
					}
					parsedPostedData = result;
				} catch (Exception e) {
					log.warn("Could not parse formdata JSON", e);
					parsedPostedData = MultiMap.caseInsensitiveMultiMap();
				}
			}
		}
		return FormService.adminMailLabelToValue(getForm(), parsedPostedData, language);
	}

	/**
	 * Extracts a storable string from a property value.
	 * Returns the string directly, or — if the value is a possible-value object ({@code {identifier, displayName}}) —
	 * returns its {@code identifier}. Returns {@code null} for binary metadata objects and other non-string types.
	 */
	private static String extractStringValue(Object v) {
		if (v instanceof String) {
			return (String) v;
		}
		if (v instanceof Map) {
			Object id = ((Map<?, ?>) v).get("identifier");
			if (id instanceof String) {
				return (String) id;
			}
		}
		return null;
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
}
