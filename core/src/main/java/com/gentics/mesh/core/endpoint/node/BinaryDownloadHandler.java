package com.gentics.mesh.core.endpoint.node;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.ext.web.RoutingContext;

/**
 * Handler for binary download requests.
 */
@Singleton
public class BinaryDownloadHandler extends AbstractHandler {

	private final MeshOptions options;
	private final BinaryFieldResponseHandler binaryFieldResponseHandler;
	private final Database db;

	@Inject
	public BinaryDownloadHandler(MeshOptions options, Database db, BinaryFieldResponseHandler binaryFieldResponseHandler) {
		this.options = options;
		this.db = db;
		this.binaryFieldResponseHandler = binaryFieldResponseHandler;
	}

	/**
	 * Handle the binary download request.
	 * 
	 * @param rc
	 *            The routing context for the request.
	 * @param uuid
	 * @param fieldName
	 */
	public void handleReadBinaryField(RoutingContext rc, String uuid, String fieldName) {
		InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
		db.tx(tx -> {
			HibProject project = tx.getProject(ac);
			HibNode node = tx.nodeDao().loadObjectByUuid(project, ac, uuid, READ_PUBLISHED_PERM);
			// Language language = boot.get().languageRoot().findByLanguageTag(languageTag);
			// if (language == null) {
			// throw error(NOT_FOUND, "error_language_not_found", languageTag);
			// }

			HibBranch branch = tx.getBranch(ac, node.getProject());
			NodeGraphFieldContainer fieldContainer = tx.contentDao().findVersion(node, ac.getNodeParameters().getLanguageList(options),
				branch.getUuid(),
				ac.getVersioningParameters().getVersion());
			if (fieldContainer == null) {
				throw error(NOT_FOUND, "object_not_found_for_version", ac.getVersioningParameters().getVersion());
			}
			BinaryGraphField field = fieldContainer.getBinary(fieldName);
			if (field == null) {
				throw error(NOT_FOUND, "error_binaryfield_not_found_with_name", fieldName);
			}
			binaryFieldResponseHandler.handle(rc, field);
		});
	}
}
