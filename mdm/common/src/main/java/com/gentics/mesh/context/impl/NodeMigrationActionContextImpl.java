package com.gentics.mesh.context.impl;

import java.util.*;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.NodeMigrationUser;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.etc.config.HttpServerConfig;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.ext.web.FileUpload;

/**
 * Action context implementation which will be used within the node migration.
 */
public class NodeMigrationActionContextImpl extends AbstractInternalActionContext implements NodeMigrationActionContext {

	private Map<String, Object> data;

	private Set<ConflictWarning> conflicts = new HashSet<>();

	private MultiMap parameters = MultiMap.caseInsensitiveMultiMap();

	private String body;

	private String query;

	private HibProject project;

	private HibBranch branch;

	private SchemaMigrationCause cause;

	private HibSchemaVersion fromContainerVersion;

	private HibSchemaVersion toContainerVersion;

	private MigrationStatusHandler status;

	private HttpServerConfig httpServerConfig;

	public NodeMigrationActionContextImpl() {
		this.httpServerConfig = Tx.maybeGet().map(tx -> tx.data().options().getHttpServerOptions()).orElse(null);
	}

	@Override
	public HibBranch getBranch() {
		return branch;
	}

	@Override
	protected HttpServerConfig getHttpServerConfig() {
		return httpServerConfig;
	}

	/**
	 * Set the {@link HttpServerConfig}.
	 * 
	 * @param config
	 */
	public void setHttpServerConfig(HttpServerConfig config) {
		this.httpServerConfig = config;
	}

	/**
	 * Set the body.
	 *
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Set the query.
	 *
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public Map<String, Object> data() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	public String getBodyAsString() {
		return body;
	}

	@Override
	public void setUser(MeshAuthUser user) {

	}

	@Override
	public HibProject getProject() {
		return project;
	}

	/**
	 * Set the project
	 *
	 * @param project
	 */
	public void setProject(HibProject project) {
		this.project = project;
	}

	public void setBranch(HibBranch branch) {
		this.branch = branch;
	}

	@Override
	public HibUser getUser() {
		return new NodeMigrationUser();
	}

	@Override
	public MeshAuthUser getMeshAuthUser() {
		return null;
	}

	@Override
	public List<FileUpload> getFileUploads() {

		return null;
	}

	@Override
	public MultiMap requestHeaders() {

		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {

	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public MultiMap getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.add(name, value);
	}

	@Override
	public void send(String body, HttpResponseStatus status, String contentType) {

	}

	@Override
	public void send(HttpResponseStatus status) {

	}

	@Override
	public void fail(Throwable cause) {

	}

	@Override
	public Locale getLocale() {

		return null;
	}

	@Override
	public void logout() {

	}

	@Override
	public void setEtag(String entityTag, boolean isWeak) {

	}

	@Override
	public void setLocation(String location) {

	}

	@Override
	public boolean matches(String etag, boolean isWeak) {
		return false;
	}

	@Override
	public boolean isMigrationContext() {
		return true;
	}

	@Override
	public void setWebrootResponseType(String type) {
		// Not supported
	}

	/**
	 * Add the encountered conflict info to the context.
	 *
	 * @param info
	 */
	public void addConflictInfo(ConflictWarning info) {
		conflicts.add(info);
	}

	/**
	 * Get the set of encountered conflicts.
	 *
	 * @return
	 */
	public Set<ConflictWarning> getConflicts() {
		return conflicts;
	}

	@Override
	public SchemaMigrationCause getCause() {
		return cause;
	}

	public void setCause(SchemaMigrationCause cause) {
		this.cause = cause;
	}

	@Override
	public HibSchemaVersion getFromVersion() {
		return fromContainerVersion;
	}

	public void setFromVersion(HibSchemaVersion fromContainerVersion) {
		this.fromContainerVersion = fromContainerVersion;
	}

	@Override
	public HibSchemaVersion getToVersion() {
		return toContainerVersion;
	}

	public void setToVersion(HibSchemaVersion toContainerVersion) {
		this.toContainerVersion = toContainerVersion;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(fromContainerVersion, "The source schema reference is missing in the context.");
		Objects.requireNonNull(toContainerVersion, "The target schema reference is missing in the context.");
	}

	@Override
	public boolean isPurgeAllowed() {
		// The purge operation is not allowed during schema migrations. Instead the purge will be executed after containers have been migrated.
		return false;
	}

}
