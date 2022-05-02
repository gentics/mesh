package com.gentics.mesh.context.impl;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.MailSendingContext;
import com.gentics.mesh.context.MicronodeMigrationContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BinaryDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.event.node.MailSendingCause;
import com.gentics.mesh.core.rest.event.node.MicroschemaMigrationCause;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;

/**
 * @see MicronodeMigrationContext
 */
public class MailSendingContextImpl extends AbstractInternalActionContext implements MailSendingContext {

	private MigrationStatusHandler status;

	private HibBranch branch;

	private HibMicroschemaVersion fromVersion;

	private HibMicroschemaVersion toVersion;

	private MailSendingCause cause;

	private HibProject project;

	private HibBinary hibBinary;

	private HibNode hibNode;

	private BinaryDao binaryDao;

	private String binaryName;

	private String binaryMimeType;

	private MultiMap parameters = MultiMap.caseInsensitiveMultiMap();

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	public HibProject getProject() {
		return project;
	}

	public void setProject(HibProject project) {
		this.project = project;
	}
	public HibBranch getBranch() {
		return branch;
	}

	public void setBranch(HibBranch branch) {
		this.branch = branch;
	}

	public void setFromVersion(HibMicroschemaVersion fromVersion) {
		this.fromVersion = fromVersion;
	}

	public HibNode getHibNode() {
		return hibNode;
	}

	public void setHibNode(HibNode hibNode) {
		this.hibNode = hibNode;
	}

	public HibBinary getHibBinary() {
		return hibBinary;
	}
	public void setHibBinary(HibBinary hibBinary) {
		this.hibBinary = hibBinary;
	}


	public void setToVersion(HibMicroschemaVersion toVersion) {
		this.toVersion = toVersion;
	}

	public MailSendingCause getCause() {
		return cause;
	}

	@Override
	public void validate() {

	}

	public void setCause(MailSendingCause cause) {
		this.cause = cause;
	}


	@Override
	public Map<String, Object> data() {
		return null;
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
	public String query() {
		return null;
	}

	@Override
	public void fail(Throwable cause) {

	}

	@Override
	public String getBodyAsString() {
		return null;
	}

	@Override
	public Locale getLocale() {
		return null;
	}

	@Override
	public void logout() {

	}

	@Override
	public void setUser(MeshAuthUser user) {

	}

	@Override
	public HibUser getUser() {
		return null;
	}

	@Override
	public MeshAuthUser getMeshAuthUser() {
		return null;
	}

	@Override
	public void send(HttpResponseStatus status) {

	}

	@Override
	public Set<FileUpload> getFileUploads() {
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
	public void setEtag(String entityTag, boolean isWeak) {

	}

	@Override
	public void setLocation(String basePath) {

	}

	@Override
	public boolean matches(String entityTag, boolean isWeak) {
		return false;
	}

	@Override
	public boolean isMigrationContext() {
		return false;
	}

	@Override
	public void setWebrootResponseType(String type) {

	}

	@Override
	public boolean isPurgeAllowed() {
		return false;
	}


	public BinaryDao getBinaryDao() {
		return binaryDao;
	}

	@Override
	public String getBinaryName() {
		return binaryName;
	}

	public void setBinaryName(String binaryName){
		this.binaryName = binaryName;
	}

	@Override
	public String getBinaryMimeType() {
		return binaryMimeType;
	}

	public void setBinaryMimeType(String binaryMimeType){
		this.binaryMimeType = binaryMimeType;
	}

	public void setBinaryDao(BinaryDao binaryDao) {
		this.binaryDao = binaryDao;
	}
}
