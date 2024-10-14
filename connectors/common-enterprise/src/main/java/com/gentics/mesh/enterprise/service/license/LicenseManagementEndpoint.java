package com.gentics.mesh.enterprise.service.license;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON;
import static com.gentics.mesh.http.HttpConstants.APPLICATION_OCTET_STREAM;
import static com.gentics.mesh.http.HttpConstants.MULTIPART_FORM_DATA;
import static com.gentics.mesh.util.MimeTypeUtils.DEFAULT_BINARY_MIME_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.io.File;
import java.util.Collections;
import java.util.Optional;

import com.gentics.license.LicenseStatus;
import com.gentics.mesh.auth.MeshAuthChain;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.LocalConfigApi;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.rest.InternalEndpointRoute;
import com.gentics.mesh.router.route.AbstractInternalEndpoint;
import com.gentics.mesh.util.MimeTypeUtils;
import com.gentics.mesh.util.VertxUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.file.CopyOptions;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;

public class LicenseManagementEndpoint extends AbstractInternalEndpoint {

	private static final String UPLOAD_QUERY_PARAM_FORCE = "force";
	public static final String ENDPOINT_ROOT = "admin/license";
	public static final String UPLOAD_FORM_FIELD = "license";

	private final LicenseService licenseService;
	private final HibernateMeshOptions options;

	public LicenseManagementEndpoint(LicenseService licenseService, MeshAuthChain chain, LocalConfigApi localConfigApi,
			Database db, HibernateMeshOptions options) {
		super(ENDPOINT_ROOT, chain, localConfigApi, db, options);
		this.licenseService = licenseService;
		this.options = options;
	}

	@Override
	public void registerEndPoints() {
		secureAll();

		InternalEndpointRoute licenseInfoEndpoint = createRoute();
		licenseInfoEndpoint.path("/");
		licenseInfoEndpoint.method(GET);
		licenseInfoEndpoint.description("Get current license info.");
		licenseInfoEndpoint.produces(APPLICATION_JSON);
		licenseInfoEndpoint.exampleResponse(OK, new LicenseResponse().setFeatures(Collections.singleton("Mesh SQL")).setStatus("VALID").setLicenseKey("ABCD-****-****-****-EFGH"), "License info response.");
		licenseInfoEndpoint
			.blockingHandler(rc -> HandlerUtilities.requiresAdminRole(rc, options, db), isOrderedBlockingHandlers())
			.handler(rc -> {
				InternalActionContext ac = wrap(rc);
				sendLicenseInfo(ac);
			});

		InternalEndpointRoute licenseFileEndpoint = createRoute();
		licenseFileEndpoint.path("/file");
		licenseFileEndpoint.method(GET);
		licenseFileEndpoint.description("Get current license file.");
		licenseFileEndpoint.produces(APPLICATION_OCTET_STREAM);
		licenseFileEndpoint
			.blockingHandler(rc -> HandlerUtilities.requiresAdminRole(rc, options, db), isOrderedBlockingHandlers())
			.blockingHandler(rc -> {
				InternalActionContext ac = wrap(rc);
				Optional<File> maybeLicenseFile = licenseService.getLicenseFile();
				maybeLicenseFile.ifPresentOrElse(
						file -> {
							HttpServerResponse response = rc.response();

							response.putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()));
							response.putHeader(HttpHeaders.CONTENT_TYPE, MimeTypeUtils.getMimeTypeForFilename(file.getName()).orElse(DEFAULT_BINARY_MIME_TYPE));
							response.putHeader(HttpHeaders.CACHE_CONTROL, "must-revalidate");
							response.putHeader(MeshHeaders.WEBROOT_RESPONSE_TYPE, "binary");
							// Set to IDENTITY to avoid gzip compression
							response.putHeader(HttpHeaders.CONTENT_ENCODING, HttpHeaders.IDENTITY);
		
							VertxUtil.addContentDispositionHeader(response, file.getName(), "inline");		
							response.sendFile(file.getAbsolutePath());
						}, 
						failNoLicense(ac));
			}, false);

		InternalEndpointRoute uploadLicenseEndpoint = createRoute();
		uploadLicenseEndpoint.path("/file");
		uploadLicenseEndpoint.method(POST);
		uploadLicenseEndpoint.addQueryParameter(UPLOAD_QUERY_PARAM_FORCE, "Force updating license even if its validation failed", "true");
		uploadLicenseEndpoint.description("Update current license file.");
		uploadLicenseEndpoint.consumes(MULTIPART_FORM_DATA);
		uploadLicenseEndpoint.produces(APPLICATION_JSON);
		uploadLicenseEndpoint.exampleResponse(OK, new LicenseResponse().setFeatures(Collections.singleton("Mesh SQL")).setStatus("VALID").setLicenseKey("ABCD-****-****-****-EFGH"), "License info response.");
		uploadLicenseEndpoint
			.blockingHandler(rc -> HandlerUtilities.requiresAdminRole(rc, options, db), isOrderedBlockingHandlers())
			.blockingHandler(rc -> {
				InternalActionContext ac = wrap(rc);
				ac.getFileUploads().stream().findAny().ifPresentOrElse(
						upload -> {
							VertxUtil.validateFileUpload(upload, UPLOAD_FORM_FIELD, options);
							String uploadFilePath = upload.uploadedFileName();
							FileSystem fs = rc.vertx().fileSystem();
							CopyOptions copyOptions = new CopyOptions().setReplaceExisting(true);
							licenseService.getLicenseFile()
								.filter(file -> file.length() > 0)
								.map(file -> fs.copy(file.getAbsolutePath(), file.getAbsolutePath() + ".bak", copyOptions))
								.orElse(Future.succeededFuture())
								.onComplete(unused -> fs.copy(uploadFilePath, options.getLicenseKeyPath(), copyOptions, done -> {
									LicenseStatus status = licenseService.reset();
									// We allow update only VALID status by default.
									// We allow update with any other than VALID or INVALID status only under the `force` flag set.
									// We allow no update with the INVALID status, since it can mean that the uploaded file is not even a license.
									if (status == LicenseStatus.VALID || (status != LicenseStatus.INVALID && rc.queryParam(UPLOAD_QUERY_PARAM_FORCE).stream().map(Boolean::parseBoolean).allMatch(Boolean::booleanValue))) {
										sendLicenseInfo(ac);
									} else {
										fs.copy(options.getLicenseKeyPath() + ".bak", options.getLicenseKeyPath(), copyOptions)
											.onComplete(unused1 -> ac.send(new GenericMessageResponse("Invalid license!"), HttpResponseStatus.BAD_REQUEST), ac::fail);
									}
								}), ac::fail);
							
						}, 
						() -> ac.send(new GenericMessageResponse("No license upload found."), HttpResponseStatus.BAD_REQUEST));
			}, false);
	}

	protected void sendLicenseInfo(InternalActionContext ac) {
		Optional.ofNullable(licenseService.getLicenseInfo()).ifPresentOrElse(
				license -> ac.send(license, OK), 
				failNoLicense(ac));
	}

	@Override
	public String getDescription() {
		return "License management endpoint";
	}

	private final Runnable failNoLicense(InternalActionContext ac) {
		return () -> ac.send(new GenericMessageResponse("No license found."), HttpResponseStatus.NOT_FOUND);
	}
}
