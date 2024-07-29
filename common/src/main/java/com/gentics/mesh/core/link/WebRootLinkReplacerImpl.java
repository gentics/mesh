package com.gentics.mesh.core.link;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingNodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.storage.S3BinaryStorage;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.VersionUtils;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.VersioningParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class will resolve mesh link placeholders.
 */
@Singleton
public class WebRootLinkReplacerImpl implements WebRootLinkReplacer {

	private static final String START_TAG = "{{mesh.link(";
	private static final String END_TAG = ")}}";

	private static final Logger log = LoggerFactory.getLogger(WebRootLinkReplacerImpl.class);

	private final MeshOptions options;

	private S3BinaryStorage s3BinaryStorage;

	@Inject
	public WebRootLinkReplacerImpl(MeshOptions options, S3BinaryStorage s3BinaryStorage) {
		this.options = options;
		this.s3BinaryStorage = s3BinaryStorage;
	}

	@Override
	public String replace(InternalActionContext ac, String branch, ContainerType edgeType, String content, LinkType type, String projectName,
		List<String> languageTags) {
		if (isEmpty(content) || type == LinkType.OFF || type == null) {
			return content;
		}

		List<ContentSegment> segments = new ArrayList<>();
		if (languageTags != null) {
			segments = tokenize(content, branch, languageTags.toArray(new String[languageTags.size()]));
		} else {
			segments = tokenize(content, branch);
		}

		StringBuilder renderedContent = new StringBuilder(content.length());
		segments.stream().forEachOrdered(seg -> renderedContent.append(seg.render(this, ac, edgeType, type, projectName)));

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
		HibNode node = Tx.get().nodeDao().findByUuidGlobal(uuid);
		String language;
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
				return VersionUtils.baseRoute(ac.getApiVersion()) + "/" + projectName + "/webroot/error/404";
			default:
				throw error(BAD_REQUEST, "Cannot render link with type " + type);
			}
		} else {
			if (languageTags == null || languageTags.length == 0) {
				String defaultLanguage = options.getDefaultLanguage();
				language = defaultLanguage;
			} else {
				language = languageTags[0];
			}
			ContentDao contentDao = Tx.get().contentDao();

			HibNodeFieldContainer nullableGraphFieldContainer = contentDao.getFieldContainer(node, language);
			Optional<HibNodeFieldContainer> maybeGraphFieldContainer = Optional.ofNullable(nullableGraphFieldContainer);

			Optional<S3HibBinary> maybeBinaryField = maybeGraphFieldContainer
				.flatMap(graphFieldContainer -> Optional.ofNullable(graphFieldContainer.getSchemaContainerVersion()))
				.flatMap(schemaContainerVersion -> Optional.ofNullable(schemaContainerVersion.getSchema()))
				.flatMap(schema -> Optional.ofNullable(schema.getFields()))
				.flatMap(fields -> fields.stream().filter(x -> x instanceof S3BinaryFieldSchema).findAny())
				.flatMap(s3binaryFieldSchema -> {
					String linkResolver = options.getS3Options().getLinkResolver();
					//if there is a S3 field and we can do the link resolving with S3 from the configuration then we should return the presigned URL
					if (Objects.isNull(linkResolver) || linkResolver.equals("s3")) {
						String fieldName = s3binaryFieldSchema.getName();
						return Optional.ofNullable(nullableGraphFieldContainer.getS3Binary(fieldName)).map(S3HibBinaryField::getBinary);
					} else {
						return Optional.empty();
					}
				});
			if (maybeBinaryField.isPresent()) {
				return s3BinaryStorage.createDownloadPresignedUrl(options.getS3Options().getBucket(), maybeBinaryField.get().getS3ObjectKey(), false).blockingGet().getPresignedUrl();
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
		NodeDao nodeDao = tx.nodeDao();

		languageTags = appendDefaultLanguageIfNotContained(languageTags);

		HibProject theirProject = node.getProject();

		HibBranch branch = tx.branchDao().findBranchOrLatest(theirProject, branchNameOrUuid);

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
			return VersionUtils.baseRoute(ac.getApiVersion()) + "/" + node.getProject().getName() + "/webroot" + path
				+ branchQueryParameter(branch);
		default:
			throw error(BAD_REQUEST, "Cannot render link with type " + type);
		}
	}

	@Override
	public Map<String, String> replaceMany(InternalActionContext ac, String branch, ContainerType edgeType,
			Set<String> contents, LinkType linkType, String projectName, String... languageTags) {
		if (edgeType == null) {
			edgeType = ContainerType.DRAFT;
		}
		final String[] finLanguageTags = appendDefaultLanguageIfNotContained(languageTags);

		CommonTx tx = CommonTx.get();
		PersistingNodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = tx.branchDao();

		// 1. tokenize all contents
		Map<String, List<ContentSegment>> segmentListsByContent = contents.stream()
				.collect(Collectors.toMap(Function.identity(), c -> tokenize(c, branch, finLanguageTags)));

		// 2. collect the list of target nodeUuids and map to the target branches
		Map<String, Set<String>> branchesPerNodeUuid = new HashMap<>();
		// also collect the language tags per nodeUuid
		Map<String, Set<String[]>> languageTagsPerNodeUuid = new HashMap<>();
		segmentListsByContent.values().stream().flatMap(list -> list.stream()).forEach(segment -> {
			segment.getTargetUuid().ifPresent(targetUuid -> {
				String targetBranch = segment.getBranch().orElse(branch);
				branchesPerNodeUuid.computeIfAbsent(targetUuid, key -> new HashSet<>()).add(targetBranch);

				String[] nodeLanguageTags = segment.getLanguageTags().orElse(finLanguageTags);
				languageTagsPerNodeUuid.computeIfAbsent(targetUuid, key -> new HashSet<>()).add(nodeLanguageTags);
			});
		});

		// 3. load the nodes by their UUIDs
		Map<String, ? extends HibNode> nodeMap = nodeDao.findByUuidGlobal(branchesPerNodeUuid.keySet()).stream().collect(Collectors.toMap(HibNode::getUuid, Function.identity()));

		// 4. load paths of the nodes per requested languages
		Map<HibBranch, Map<String[], Map<HibNode, String>>> pathsPerBranchAndNode = new HashMap<>();
		Map<String, HibBranch> branchPerUUids = new HashMap<>();
		for (Entry<String, Set<String>> entry : branchesPerNodeUuid.entrySet()) {
			String nodeUuid = entry.getKey();
			HibNode node = nodeMap.get(nodeUuid);
			if (node != null) {
				HibProject theirProject = node.getProject();

				for (String branchUuidOrName : entry.getValue()) {
					String key = theirProject.getUuid() + "-" + branchUuidOrName;
					HibBranch theirBranch = branchPerUUids.computeIfAbsent(key, unused -> branchDao.findBranchOrLatest(theirProject, branchUuidOrName));

					for (String[] nodeLanguageTags : languageTagsPerNodeUuid.get(nodeUuid)) {
						pathsPerBranchAndNode.computeIfAbsent(theirBranch, k -> new HashMap<>())
								.computeIfAbsent(nodeLanguageTags, k -> new HashMap<>()).put(node, null);
					}
				}
			}
		}
		for (Entry<HibBranch, Map<String[], Map<HibNode, String>>> entry : pathsPerBranchAndNode.entrySet()) {
			HibBranch currentBranch = entry.getKey();
			for (Entry<String[], Map<HibNode, String>> entry2 : entry.getValue().entrySet()) {
				String[] nodeLanguageTags = entry2.getKey();
				Set<HibNode> nodes = entry2.getValue().keySet();
				entry2.setValue(nodeDao.getPaths(nodes, currentBranch, ac, edgeType, nodeLanguageTags));
			}
		}

		// 5. adapt paths to link type
		switch (linkType) {
		case SHORT:
			HibProject txProject = tx.getProject(ac);
			HibBranch txBranch = tx.getBranch(ac);
			for (Entry<HibBranch, Map<String[], Map<HibNode, String>>> entry : pathsPerBranchAndNode.entrySet()) {
				HibBranch currentBranch = entry.getKey();
				Map<String[], Map<HibNode, String>> languageTagsMap = entry.getValue();
				for (Map<HibNode, String> pathMap : languageTagsMap.values()) {
					for (Entry<HibNode, String> entry2 : pathMap.entrySet()) {
						HibNode node = entry2.getKey();
						if (txProject == null || !txBranch.equals(currentBranch)) {
							String path = entry2.getValue();
							entry2.setValue(generateSchemeAuthorityForNode(node, currentBranch) + path);
						}
					}
				}
			}
			break;
		case MEDIUM:
			for (Entry<HibBranch, Map<String[], Map<HibNode, String>>> entry : pathsPerBranchAndNode.entrySet()) {
				Map<String[], Map<HibNode, String>> languageTagsMap = entry.getValue();
				for (Map<HibNode, String> pathMap : languageTagsMap.values()) {
					for (Entry<HibNode, String> entry2 : pathMap.entrySet()) {
						HibNode node = entry2.getKey();
						String path = entry2.getValue();
						entry2.setValue("/" + node.getProject().getName() + path);
					}
				}
			}
			break;
		case FULL:
			String baseRoute = VersionUtils.baseRoute(ac.getApiVersion()) + "/";
			for (Entry<HibBranch, Map<String[], Map<HibNode, String>>> entry : pathsPerBranchAndNode.entrySet()) {
				HibBranch currentBranch = entry.getKey();
				Map<String[], Map<HibNode, String>> languageTagsMap = entry.getValue();
				for (Map<HibNode, String> pathMap : languageTagsMap.values()) {
					for (Entry<HibNode, String> entry2 : pathMap.entrySet()) {
						HibNode node = entry2.getKey();
						String path = entry2.getValue();
						entry2.setValue(
								baseRoute + node.getProject().getName() + "/webroot" + path + branchQueryParameter(currentBranch));
					}
				}
			}
			break;
		default:
			throw error(BAD_REQUEST, "Cannot render link with type " + linkType);
		}

		// 6. reconstruct content with replaced links
		Map<String, String> result = new HashMap<>();
		for (String content : contents) {
			StringBuilder renderedContent = new StringBuilder(content.length());
			List<ContentSegment> segments = segmentListsByContent.get(content);
			segments.stream().forEachOrdered(seg -> {
				seg.getTargetUuid().ifPresentOrElse(nodeUuid -> {
					String currentBranch = seg.getBranch().orElse(branch);
					String[] nodeLanguageTags = seg.getLanguageTags().orElse(finLanguageTags);
					HibNode node = nodeMap.get(nodeUuid);
					String renderedSegment = null;
					if (node != null) {
						String key = node.getProject().getUuid() + "-" + currentBranch;
						HibBranch theirBranch = branchPerUUids.computeIfAbsent(key, unused -> branchDao.findBranchOrLatest(node.getProject(), currentBranch));
						renderedSegment = pathsPerBranchAndNode.getOrDefault(theirBranch, Collections.emptyMap()).getOrDefault(nodeLanguageTags, Collections.emptyMap()).getOrDefault(node, null);
					}
					if (StringUtils.isBlank(renderedSegment)) {
						switch (linkType) {
						case SHORT:
							renderedSegment = "/error/404";
							break;
						case MEDIUM:
							renderedSegment = "/" + projectName + "/error/404";
							break;
						case FULL:
							renderedSegment = VersionUtils.baseRoute(ac.getApiVersion()) + "/" + projectName + "/webroot/error/404";
							break;
						default:
							throw error(BAD_REQUEST, "Cannot render link with type " + linkType);
						}
					}
					renderedContent.append(renderedSegment);
				}, () -> {
					renderedContent.append(seg.toString());
				});
			});
			result.put(content, renderedContent.toString());
		}

		return result;
	}

	@Override
	public List<ContentSegment> tokenize(String content, String branch, String... languageTags) {
		if (isEmpty(content)) {
			return Collections.emptyList();
		}

		List<ContentSegment> segments = new ArrayList<>();
		int pos = 0;
		int lastPos = 0;
		int length = content.length();

		// 1. Tokenize the content
		while (lastPos < length) {
			pos = content.indexOf(START_TAG, lastPos);
			if (pos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(new StringContentSegment(content.substring(lastPos)));
				}
				break;
			}
			int endPos = content.indexOf(END_TAG, pos);
			if (endPos == -1) {
				// add last string segment
				if (lastPos < length) {
					segments.add(new StringContentSegment(content.substring(lastPos)));
				}
				break;
			}

			// add intermediate string segment
			if (lastPos < pos) {
				segments.add(new StringContentSegment(content.substring(lastPos, pos)));
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
				segments.add(new LinkContentSegment(linkArguments[0], branch, appendDefaultLanguageIfNotContained(linkArguments[1].trim())));
			} else if (languageTags.length > 0) {
				segments.add(new LinkContentSegment(linkArguments[0], branch, languageTags));
			} else {
				segments.add(new LinkContentSegment(linkArguments[0], branch));
			}

			lastPos = endPos + END_TAG.length();
		}
		return segments;
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

	/**
	 * Append the default language to the given language tags (if not already contained)
	 * @param languageTags language tags, may be empty or null
	 * @return array of language tags, never empty, never null
	 */
	private String[] appendDefaultLanguageIfNotContained(String... languageTags) {
		String defaultLanguage = options.getDefaultLanguage();
		if (languageTags == null) {
			return ArrayUtils.toArray(defaultLanguage);
		} else if (ArrayUtils.contains(languageTags, defaultLanguage)) {
			return languageTags;
		} else {
			return ArrayUtils.add(languageTags, defaultLanguage);
		}
	}
}
