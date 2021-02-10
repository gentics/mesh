package com.gentics.mesh.core.link;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.OrientDBNodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.VersionHandlerImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.VersioningParameters;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * This class will resolve mesh link placeholders.
 */
@Singleton
public class WebRootLinkReplacerImpl implements WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	private static final Logger log = LoggerFactory.getLogger(WebRootLinkReplacerImpl.class);

	private final BootstrapInitializer boot;

	private final MeshOptions options;

	@Inject
	public WebRootLinkReplacerImpl(BootstrapInitializer boot, MeshOptions options) {
		this.boot = boot;
		this.options = options;
	}

	@Override
	public String replace(InternalActionContext ac, String branch, ContainerType edgeType, String content, LinkType type, String projectName,
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
			if (linkArguments.length == 3) {
				// Branch in link argument always comes last (third argument)
				branch = linkArguments[2].trim();
			}
			if (linkArguments.length >= 2) {
				segments.add(resolve(ac, branch, edgeType, linkArguments[0], type, projectName, linkArguments[1].trim()));
			} else if (languageTags != null) {
				segments.add(resolve(ac, branch, edgeType, linkArguments[0], type, projectName,
					languageTags.toArray(new String[languageTags.size()])));
			} else {
				segments.add(resolve(ac, branch, edgeType, linkArguments[0], type, projectName));
			}

			lastPos = endPos + END_TAG.length();
		}

		// 3.: Buildup the new content
		StringBuilder renderedContent = new StringBuilder(length);
		segments.stream().forEachOrdered(obs -> renderedContent.append(obs));

		return renderedContent.toString();
	}

	@Override
	public String resolve(InternalActionContext ac, String branch, ContainerType edgeType, String uuid, LinkType type, String projectName,
		String... languageTags) {
		return resolve(ac, branch, edgeType, uuid, type, projectName, false, languageTags);
	}

	@Override
	public String resolve(InternalActionContext ac, String branch, ContainerType edgeType, String uuid, LinkType type, String projectName,
		boolean forceAbsolute,
		String... languageTags) {
		// Get rid of additional whitespaces
		uuid = uuid.trim();
		Node node = boot.meshRoot().findNodeByUuid(uuid);

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
				return VersionHandlerImpl.baseRoute(ac.getApiVersion()) + "/" + projectName + "/webroot/error/404";
			default:
				throw error(BAD_REQUEST, "Cannot render link with type " + type);
			}
		}
		return resolve(ac, branch, edgeType, node, type, forceAbsolute, languageTags);
	}

	@Override
	public String resolve(InternalActionContext ac, String branchNameOrUuid, ContainerType edgeType, HibNode node, LinkType type,
		String... languageTags) {
		return resolve(ac, branchNameOrUuid, edgeType, node, type, false, languageTags);
	}

	@Override
	public String resolve(InternalActionContext ac, String branchNameOrUuid, ContainerType edgeType, HibNode node, LinkType type,
		boolean forceAbsolute,
		String... languageTags) {
		Tx tx = Tx.get();
		OrientDBNodeDao nodeDao = (OrientDBNodeDao) tx.nodeDao();

		String defaultLanguage = options.getDefaultLanguage();
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

		HibProject theirProject = node.getProject();

		HibBranch branch = theirProject.findBranchOrLatest(branchNameOrUuid);

		// edge type defaults to DRAFT
		if (edgeType == null) {
			edgeType = ContainerType.DRAFT;
		}
		if (log.isDebugEnabled()) {
			log.debug("Resolving link to " + node.getUuid() + " in language " + Arrays.toString(languageTags) + " with type " + type.name());
		}

		String path = nodeDao.getPath(node, ac, branch.getUuid(), edgeType, languageTags);
		if (path == null) {
			path = "/error/404";
		}
		switch (type) {
		case SHORT:
			// We also try to append the scheme and authority part of the uri for foreign nodes.
			// Otherwise that part will be empty and thus the link relative.
			if (!forceAbsolute && tx.getProject(ac) != null && tx.getBranch(ac).equals(branch)) {
				return path;
			} else {
				return generateSchemeAuthorityForNode(node, branch) + path;
			}
		case MEDIUM:
			return "/" + node.getProject().getName() + path;
		case FULL:
			return VersionHandlerImpl.baseRoute(ac.getApiVersion()) + "/" + node.getProject().getName() + "/webroot" + path
				+ branchQueryParameter(branch);
		default:
			throw error(BAD_REQUEST, "Cannot render link with type " + type);
		}
	}

	/**
	 * Return the URL prefix for the given node. The latest branch of the node's project will be used to fetch the needed information.
	 *
	 * @param node
	 * @param branch
	 *            branch
	 * @return scheme and authority or empty string if the branch of the node does not supply the needed information
	 */
	private String generateSchemeAuthorityForNode(HibNode node, HibBranch branch) {
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
	 * Returns the query parameter for the given branch. This is the query parameter that is necessary to get the node in the correct branch. If the given
	 * branch is the latest branch, no query parameter is necessary and thus an empty string is returned.
	 *
	 * @param branch
	 *            The branch to generate the query parameter for.
	 * @return Example: "?branch=test1"
	 */
	private String branchQueryParameter(HibBranch branch) {
		if (branch.isLatest()) {
			return "";
		}
		return String.format("?%s=%s", VersioningParameters.BRANCH_QUERY_PARAM_KEY, branch.getName());
	}
}
