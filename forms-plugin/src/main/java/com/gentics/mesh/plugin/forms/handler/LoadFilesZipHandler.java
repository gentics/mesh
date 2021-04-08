package com.gentics.mesh.plugin.forms.handler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.common.dagger.AdminClient;
import com.gentics.mesh.plugin.common.dagger.StorageDir;
import com.gentics.mesh.plugin.forms.FormsPlugin;
import com.gentics.mesh.plugin.forms.model.FileElement;
import com.gentics.mesh.plugin.forms.model.FormElement;
import com.gentics.mesh.plugin.forms.service.FormService;
import com.gentics.mesh.plugin.forms.service.SchemaService;
import com.gentics.mesh.plugin.forms.service.StatusService;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.util.UUIDUtil;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.google.common.net.HttpHeaders;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.http.HttpServerResponse;

@Singleton
public class LoadFilesZipHandler extends AbstractFormsHandler {
	private static final Pattern filenameWithNumPattern = Pattern.compile("(?<base>.*)( \\((?<num>[\\d+])\\))(?<ext>\\.[^\\.]+)?");
	private static final Pattern filenameWithoutNumPattern = Pattern.compile("(?<base>.*)(?<ext>\\.[^\\.]+)");

	private final MeshRestClient adminClient;
	private final FormService formService;
	private final File storageDir;
	private final StatusService statusService;

	/**
	 * Get the next filename to try
	 * 
	 * @param filename
	 *            filename
	 * @return next filename to try
	 */
	public static String getNextFilename(String filename) {
		Matcher m1 = filenameWithNumPattern.matcher(filename);
		if (m1.matches()) {
			String base = m1.group("base");
			String numGroup = m1.group("num");
			int num = numGroup != null ? Integer.parseInt(numGroup) + 1 : 1;
			String ext = m1.group("ext");
			if (ext == null) {
				ext = "";
			}
			return String.format("%s (%d)%s", base, num, ext);
		} else {
			Matcher m2 = filenameWithoutNumPattern.matcher(filename);
			if (m2.matches()) {
				String base = m2.group("base");
				String ext = m2.group("ext");
				if (ext == null) {
					ext = "";
				}
				return String.format("%s (%d)%s", base, 1, ext);
			} else {
				return String.format("%s (%d)", filename, 1);
			}
		}
	}

	@Inject
	public LoadFilesZipHandler(@AdminClient MeshRestClient adminClient, FormService formService, @StorageDir File storageDir, StatusService statusService) {
		this.adminClient = adminClient;
		this.formService = formService;
		this.storageDir = storageDir;
		this.statusService = statusService;
	}

	@Override
	public Completable handle(PluginContext context) {
		String projectName = context.project().getString("name");
		String uuid = context.pathParam("formUuid");

		return statusService.languageFallback(projectName)
			.flatMap(langFallback -> formService.load(projectName, uuid, langFallback))
			.flatMap(formService::toForm)
			.flatMapCompletable(form -> {
				Set<String> fileElementNames = form.flattened().filter(elem -> elem instanceof FileElement).map(FormElement::getName)
					.filter(Objects::nonNull).collect(Collectors.toSet());
				Set<String> fileFieldNames = form.flattened().filter(elem -> elem instanceof FileElement).map(FormElement::getGraphQLName)
					.filter(Objects::nonNull).collect(Collectors.toSet());

				Single<List<Downloadable>> singleDownloadables = null;
				if (fileElementNames.isEmpty()) {
					singleDownloadables = Single.just(Collections.emptyList());
				} else {
					singleDownloadables = getGraphQl(projectName, uuid, fileFieldNames)
						.flatMap(graphQlResponse -> extractDownloadables(projectName, fileElementNames, graphQlResponse));
				}

				return singleDownloadables
					.flatMap(downloadables -> zipBinaries(projectName, downloadables))
					.flatMapCompletable(file -> {
						context.response().putHeader(HttpHeaders.CONTENT_DISPOSITION, getContentDisposition(uuid, form.getName(), "zip"));
						return new HttpServerResponse(context.response()).rxSendFile(file.getAbsolutePath()).doFinally(file::delete);
					});
			});
	}

	/**
	 * Run the graphql query to find all data nodes with binaries
	 * 
	 * @param projectName
	 *            project name
	 * @param uuid
	 *            form uuid
	 * @param fileFieldNames
	 *            set of file field names
	 * @return single graphql response
	 */
	protected Single<GraphQLResponse> getGraphQl(String projectName, String uuid, Set<String> fileFieldNames) {
		Map<String, String> renderCtx = new HashMap<>();
		renderCtx.put("schemaName", SchemaService.getSchemaName(uuid));
		renderCtx.put("fieldNames", String.join("\n", fileFieldNames));

		String query;

		try {
			String rawQuery = IOUtils.toString(
				FormsPlugin.class.getResource("loadFormData.graphql"),
				"UTF-8");
			query = new Handlebars().compileInline(rawQuery).apply(Context.newContext(renderCtx));

			GraphQLRequest request = new GraphQLRequest()
				.setQuery(query)
				.setVariables(new JsonObject().put("uuid", uuid).put("perPage", Integer.MAX_VALUE).put("page", 1));

			return adminClient.graphql(projectName, request).toSingle();

		} catch (IOException e) {
			return Single.error(e);
		}
	}

	/**
	 * Extract the downloadables from the graphql response
	 * 
	 * @param projectName
	 *            project name
	 * @param fileElementNames
	 *            set of file element names
	 * @param response
	 *            graphql response
	 * @return single list of downloadables
	 * @throws FileNotFoundException
	 */
	protected Single<List<Downloadable>> extractDownloadables(String projectName, Set<String> fileElementNames, GraphQLResponse response)
		throws FileNotFoundException {
		List<Downloadable> downloadables = new ArrayList<>();
		JsonObject data = response.getData();
		if (data == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonObject node = data.getJsonObject("node");
		if (node == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonObject children = node.getJsonObject("children");
		if (children == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		JsonArray elements = children.getJsonArray("elements");
		if (elements == null) {
			return Single.error(new Exception("GraphQl response contained no data"));
		}
		for (int i = 0; i < elements.size(); i++) {
			JsonObject element = elements.getJsonObject(i);
			String uuid = element.getString("uuid");
			JsonObject fields = element.getJsonObject("fields");
			for (String name : fileElementNames) {
				JsonObject fieldValue = fields.getJsonObject(name);
				if (fieldValue != null) {
					downloadables.add(new Downloadable(uuid, name));
				}
			}
		}

		return Single.just(downloadables);
	}

	/**
	 * Download all binaries from the list of downloadables and add to a zip file
	 * 
	 * @param projectName
	 *            project name
	 * @param downloadables
	 *            list of downloadables
	 * @return single zip file
	 * @throws FileNotFoundException
	 */
	protected Single<File> zipBinaries(String projectName, List<Downloadable> downloadables) throws FileNotFoundException {
		File zipFile = new File(storageDir, UUIDUtil.randomUUID() + ".zip");
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

		if (downloadables.isEmpty()) {
			try {
				out.close();
				return Single.just(zipFile);
			} catch (IOException e) {
				return Single.error(e);
			}
		}

		return Observable.fromIterable(downloadables).flatMap(d -> {
			return adminClient.downloadBinaryField(projectName, d.uuid, "en", d.fieldName).toObservable().flatMap(download -> {
				addZipEntryWithUniquename(out, download.getFilename());
				try (InputStream ins = download.getStream()) {
					IOUtils.copy(ins, out);
				}
				out.closeEntry();
				out.flush();
				return Observable.just(zipFile);
			});
		}, 1).doOnError(t -> {
			out.close();
			zipFile.delete();
		}).doOnComplete(() -> out.close()).lastOrError();
	}

	/**
	 * Add a new ZipEntry to the ZipOutputStream instance. First try the given filename, but if that leads to a ZipException (duplicate entry), try making the
	 * filename unique by calling {@link #getNextFilename(String)}
	 * 
	 * @param out
	 *            zip output stream
	 * @param filename
	 *            proposed filename
	 * @throws IOException
	 */
	protected void addZipEntryWithUniquename(ZipOutputStream out, String filename) throws IOException {
		boolean tryAgain = false;
		do {
			tryAgain = false;
			try {
				out.putNextEntry(new ZipEntry(filename));
			} catch (ZipException e) {
				if (e.getMessage() != null && e.getMessage().startsWith("duplicate entry")) {
					// try again
					tryAgain = true;
					filename = getNextFilename(filename);
				} else {
					throw e;
				}
			}
		} while (tryAgain);
	}

	/**
	 * Get the value for the <code>Content-Dispostion</code> HTTP header.
	 *
	 * The method constructs a filename from the form name and the current date and time.
	 *
	 * @param formName
	 *            The name of the form
	 * @param extension
	 *            The file extension to be used (if it does not start with a '.', it will be prepended)
	 *
	 * @return A Content-Disposition value with a filename made up from the form name and the current date
	 */
	protected String getContentDisposition(String uuid, String formName, String extension) {
		if (!extension.startsWith(".")) {
			extension = "." + extension;
		}

		return "attachment; filename=forms-files_"
			+ (formName == null || formName.isEmpty() ? uuid : formName)
			+ "_"
			+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmm"))
			+ extension;
	}

	protected class Downloadable {
		String uuid;
		String fieldName;

		public Downloadable(String uuid, String fieldName) {
			this.uuid = uuid;
			this.fieldName = fieldName;
		}
	}
}
