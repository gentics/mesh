package com.gentics.mesh.core.link;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.parameter.VersioningParameters;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.router.APIRouter;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class will resolve mesh link placeholders.
 */
@Singleton
public class WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	private static final Logger log = LoggerFactory.getLogger(WebRootLinkReplacer.class);

	@Inject
	public BootstrapInitializer boot;

	@Inject
	public WebRootLinkReplacer() {
	}

	/**
	 * Replace the links in the content.
	 * 
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param edgeType
	 *            edge type
	 * @param content
	 *            content containing links to replace
	 * @param type
	 *            replacing type
	 * @param projectName
	 *            project name (used for 404 links)
	 * @param languageTags
	 *            optional language tags
	 * @return content with links (probably) replaced
	 */
	public String replace(InternalActionContext ac, String branchUuid, ContainerType edgeType, String content, LinkType type, String projectName,
		List<String> languageTags) {
		if (isEmpty(content) || type == LinkType.OFF || type == null) {
			return content;
		}

		List<String> segments = new ArrayList<>();
		int pos = 0;
		int lastPos = 0;
		int length = content.length();

		// 1. Tokenize the content
		while (lastPos < length) {
			pos = content.indexOf(START_TAG, lastPos);
			if (pos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(content.substring(lastPos));
				}
				break;
			}
			int endPos = content.indexOf(END_TAG, pos);
			if (endPos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(content.substring(lastPos));
				}
				break;
			}

			// add intermediate string segment
			if (lastPos < pos) {
				segments.add(content.substring(lastPos, pos));
			}

			// 2. Parse the link and invoke resolving
			String link = content.substring(pos + START_TAG.length(), endPos);
			// Strip away the quotes. We only care about the argument values
			// double quotes may be escaped
			link = link.replaceAll("\\\\\"", "");
			link = link.replaceAll("'", "");
			link = link.replaceAll("\"", "");
			String[] linkArguments = link.split(",");
			if (linkArguments.length == 2) {
				segments.add(resolve(ac, branchUuid, edgeType, linkArguments[0], type, projectName, linkArguments[1].trim()));
			} else if (languageTags != null) {
				segments.add(resolve(ac, branchUuid, edgeType, linkArguments[0], type, projectName,
					languageTags.toArray(new String[languageTags.size()])));
			} else {
				segments.add(resolve(ac, branchUuid, edgeType, linkArguments[0], type, projectName));
			}

			lastPos = endPos + END_TAG.length();
		}

		// 3.: Buildup the new content
		StringBuilder renderedContent = new StringBuilder(length);
		segments.stream().forEachOrdered(obs -> renderedContent.append(obs));

		return renderedContent.toString();
	}

	/**
	 * Resolve the link to the node with uuid (in the given language) into an observable
	 * 
	 * @param ac
	 * @param branchUuid
	 *            branch Uuid
	 * @param edgeType
	 *            edge type
	 * @param uuid
	 *            target uuid
	 * @param type
	 *            link type
	 * @param projectName
	 *            project name (which is used for 404 links)
	 * @param languageTags
	 *            optional language tags
	 * @return observable of the rendered link
	 */
	public String resolve(InternalActionContext ac, String branchUuid, ContainerType edgeType, String uuid, LinkType type, String projectName,
		String... languageTags) {
		// Get rid of additional whitespaces
		uuid = uuid.trim();
		Node node = boot.meshRoot().getNodeRoot().findByUuid(uuid);

		// check for null
		if (node == null) {
			if (log.isDebugEnabled()) {
				log.debug("Could not resolve link to '" + uuid + "', target node could not be found");
			}
			switch (type) {
			case SHORT:
				return "/error/404";
			case MEDIUM:
				return "/" + projectName + "/error/404";
			case FULL:
				return APIRouter.API_MOUNTPOINT + "/" + projectName + "/webroot/error/404";
			default:
				throw error(BAD_REQUEST, "Cannot render link with type " + type);
			}
		}
		return resolve(ac, branchUuid, edgeType, node, type, languageTags);
	}

	/**
	 * Resolve the link to the given node.
	 * 
	 * @param ac
	 * @param branchUuid
	 *            Branch UUID which will be used to render the path to the linked node. This uuid will only be used when rendering nodes of the same project.
	 *            Otherwise the latest branch of the node's project will be used.
	 * @param edgeType
	 *            edge type
	 * @param node
	 *            target node
	 * @param type
	 *            link type
	 * @param languageTags
	 *            target language
	 * @return observable of the rendered link
	 */
	public String resolve(InternalActionContext ac, String branchUuid, ContainerType edgeType, Node node, LinkType type, String... languageTags) {
		String defaultLanguage = Mesh.mesh().getOptions().getDefaultLanguage();
		if (languageTags == null || languageTags.length == 0) {
			languageTags = new String[] { defaultLanguage };

			if (log.isDebugEnabled()) {
				log.debug("Fallback to default language " + defaultLanguage);
			}
		} else {
			// In other cases add the default language to the list
			List<String> languageTagList = new ArrayList<String>(Arrays.asList(languageTags));
			languageTagList.add(defaultLanguage);
			languageTags = languageTagList.toArray(new String[languageTagList.size()]);
		}

		// We need to reset the given branchUuid if the node is not part of the currently active project.
		// In that case the latest branch of the foreign node project will be used.
		Project ourProject = ac.getProject();
		Project theirProject = node.getProject();
		if (ourProject != null && !ourProject.equals(theirProject)) {
			branchUuid = null;
		}
		// if no branch given, take the latest branch of the project
		if (branchUuid == null) {
			branchUuid = theirProject.getLatestBranch().getUuid();
		}
		// edge type defaults to DRAFT
		if (edgeType == null) {
			edgeType = ContainerType.DRAFT;
		}
		if (log.isDebugEnabled()) {
			log.debug("Resolving link to " + node.getUuid() + " in language " + Arrays.toString(languageTags) + " with type " + type.name());
		}

		String path = node.getPath(ac, branchUuid, edgeType, languageTags);
		if (path == null) {
			path = "/error/404";
		}
		switch (type) {
		case SHORT:
			// We also try to append the scheme and authority part of the uri for foreign nodes.
			// Otherwise that part will be empty and thus the link relative.
			return generateSchemeAuthorityForNode(node, theirProject.getBranchRoot().findByUuid(branchUuid)) + path;
		case MEDIUM:
			return "/" + node.getProject().getName() + path;
		case FULL:
			return APIRouter.API_MOUNTPOINT + "/" + node.getProject().getName() + "/webroot" + path + branchQueryParameter(theirProject.getBranchRoot().findByUuid(branchUuid));
		default:
			throw error(BAD_REQUEST, "Cannot render link with type " + type);
		}
	}

	/**
	 * Return the URL prefix for the given node. The latest branch of the node's project will be used to fetch the needed information.
	 *
	 * @param node
	 * @param branch branch
	 * @return scheme and authority or empty string if the branch of the node does not supply the needed information
	 */
	private String generateSchemeAuthorityForNode(Node node, Branch branch) {
		String hostname = branch.getHostname();
		if (StringUtils.isEmpty(hostname)) {
			// Fallback to urls without authority/scheme
			return "";
		}
		boolean isSSL = BooleanUtils.toBoolean(branch.getSsl());
		StringBuffer buffer = new StringBuffer();
		if (isSSL) {
			buffer.append("https://");
		} else {
			buffer.append("http://");
		}
		buffer.append(branch.getHostname());
		return buffer.toString();
	}

	/**
	 * Returns the query parameter for the given branch. This is the query parameter that is necessary to get the
	 * node in the correct branch.
	 * If the given branch is the latest branch, no query parameter is necessary and thus an empty string is returned.
	 *
	 * @param branch The branch to generate the query parameter for.
	 * @return Example: "?branch=test1"
	 */
	private String branchQueryParameter(Branch branch) {
		if (branch.isLatest()) {
			return "";
		}
		return String.format("?%s=%s", VersioningParameters.BRANCH_QUERY_PARAM_KEY, branch.getName());
	}
}
