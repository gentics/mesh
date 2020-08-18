package com.gentics.mesh.changelog.highlevel.change;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.collect.Iterables;
import com.syncleus.ferma.EdgeFrame;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Fixes the order of node versions and removes duplicate versions. This was caused by a bug described in these issues:
 * <ul>
 *     <li><a href="https://github.com/gentics/mesh/issues/979">https://github.com/gentics/mesh/issues/979</a></li>
 *     <li><a href="https://github.com/gentics/mesh/issues/1014">https://github.com/gentics/mesh/issues/1014</a></li>
 *     <li><a href="https://github.com/gentics/mesh/issues/1025">https://github.com/gentics/mesh/issues/1025</a></li>
 * </ul>
 *
 * This only fixes nodes in projects with single branches, because this is the simplest case to implement.
 */
@Singleton
public class FixNodeVersionOrder extends AbstractHighLevelChange {
	public static final Logger log = LoggerFactory.getLogger(FixNodeVersionOrder.class);

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public FixNodeVersionOrder(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	@Override
	public String getUuid() {
		return "38F37731BDBB4FAFBEA5A5D19C435E11";
	}

	@Override
	public String getName() {
		return "FixNodeVersionOrder";
	}

	@Override
	public String getDescription() {
		return "Fixes the order of node versions and removes duplicate versions (single branch only)";
	}

	@Override
	public void apply() {
		AtomicLong nodeCount = new AtomicLong();
		AtomicLong fixedNodes = new AtomicLong();
		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		singleBranchProjects()
			.flatMap(project -> project.getNodeRoot().findAll().stream())
			.forEach(node -> {
				long count = nodeCount.get();
				if (count != 0 && count % 1000 == 0) {
					log.info("Checked {} nodes. Found and fixed {} broken nodes", count, fixedNodes.get());
				}
				node.getGraphFieldContainers(ContainerType.INITIAL).stream()
					.forEach(content -> {
						boolean mutated = fixNodeVersionOrder(context, node, content);
						if (mutated) {
							fixedNodes.incrementAndGet();
						}
					});
				nodeCount.incrementAndGet();
			});
		log.info("Checked {} nodes. Found and fixed {} broken nodes", nodeCount.get(), fixedNodes.get());
		context.getConflicts()
			.forEach(conflict -> log.info("Encountered conflict for node {" + conflict.getNodeUuid() + "} which was automatically resolved."));
	}

	private boolean fixNodeVersionOrder(NodeMigrationActionContext context, Node node, NodeGraphFieldContainer initialContent) {
		ContentDaoWrapper contentDao = boot.get().contentDao();
		Set<VersionNumber> seenVersions = new HashSet<>();
		TreeSet<NodeGraphFieldContainer> versions = new TreeSet<>(Comparator.comparing(NodeGraphFieldContainer::getVersion));
		NodeGraphFieldContainer highestVersion = null;
		NodeGraphFieldContainer currentContainer = initialContent;
		NodeGraphFieldContainer latestPublished = null;
		boolean mutated = false;

		String branchUuid = initialContent.getBranches(ContainerType.INITIAL).iterator().next();
		String languageTag = initialContent.getLanguageTag();

		EdgeFrame originalDraftEdge = node.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, ContainerType.DRAFT);
		Optional<Object> originalDraftId = Optional.ofNullable(contentDao.getGraphFieldContainer(node, languageTag, branchUuid, ContainerType.DRAFT))
			.map(ElementFrame::id);
		EdgeFrame originalPublishedEdge = node.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, ContainerType.PUBLISHED);
		Optional<Object> originalPublishedId = Optional.ofNullable(contentDao.getGraphFieldContainer(node, languageTag, branchUuid, ContainerType.PUBLISHED))
			.map(ElementFrame::id);

		if (log.isDebugEnabled()) {
			log.debug("Fixing node {}-{}", node.getUuid(), languageTag);
		}

		while (currentContainer != null) {
			// Since we only look at single branch projects, there can at most only be one next version.
			NodeGraphFieldContainer nextContainer = Iterables.getFirst(currentContainer.getNextVersions(), null);
			VersionNumber currentVersion = currentContainer.getVersion();
			boolean isDuplicate = !seenVersions.add(currentVersion);
			if (isDuplicate) {
				currentVersion = generateUniqueVersion(seenVersions, currentVersion);
				currentContainer.setVersion(currentVersion);
				seenVersions.add(currentVersion);
				mutated = true;
			}
			if (highestVersion != null && currentVersion.compareTo(highestVersion.getVersion()) < 0) {
				// This means that currentVersion is in the wrong order and needs to be moved.

				NodeGraphFieldContainer lower = versions.lower(currentContainer);
				// There must always be a higher version
				NodeGraphFieldContainer higher = versions.higher(currentContainer);
				higher.unlinkIn(null, HAS_VERSION);
				currentContainer.unlinkIn(null, HAS_VERSION);
				currentContainer.unlinkOut(null, HAS_VERSION);
				currentContainer.setNextVersion(higher);
				if (lower != null) {
					lower.unlinkOut(null, HAS_VERSION);
					lower.setNextVersion(currentContainer);
				}
				mutated = true;
			}
			versions.add(currentContainer);
			if (currentVersion.getMinor() == 0 && (latestPublished == null || latestPublished.getVersion().getMajor() < currentVersion.getMajor())) {
				latestPublished = currentContainer;
			}

			highestVersion = versions.last();
			currentContainer = nextContainer;
		}

		// Reset draft and published edge

		if (originalDraftId.isPresent() && !originalDraftId.get().equals(highestVersion.id())) {
			originalDraftEdge.remove();
			setNewOutV(context, node, highestVersion, branchUuid, languageTag, ContainerType.DRAFT);
			mutated = true;
		}

		if (originalPublishedId.isPresent() && latestPublished != null && !originalPublishedId.get().equals(latestPublished.id())) {
			originalPublishedEdge.remove();
			setNewOutV(context, node, latestPublished, branchUuid, languageTag, ContainerType.PUBLISHED);
			mutated = true;
		}
		Tx.get().commit();
		if (mutated && log.isDebugEnabled()) {
			log.debug("The node was broken and has been fixed.");
		}
		return mutated;
	}

	/**
	 * Increases the minor version until a unique version has been found.
	 * @param seenVersions
	 * @param version
	 * @return
	 */
	private VersionNumber generateUniqueVersion(Set<VersionNumber> seenVersions, VersionNumber version) {
		while (seenVersions.contains(version)) {
			version = version.nextDraft();
		}
		return version;
	}

	private GraphFieldContainerEdge setNewOutV(NodeMigrationActionContext context, Node from, NodeGraphFieldContainer to, String branchUuid, String languageTag, ContainerType type) {
		GraphFieldContainerEdge newEdge = from.addFramedEdge(HAS_FIELD_CONTAINER, to, GraphFieldContainerEdgeImpl.class);
		newEdge.setBranchUuid(branchUuid);
		newEdge.setLanguageTag(languageTag);
		newEdge.setType(type);
		to.updateWebrootPathInfo(context, branchUuid, "node_conflicting_segmentfield_update");
		return newEdge;
	}

	private Stream<? extends HibProject> singleBranchProjects() {
		return Tx.get().data().projectDao().findAll().stream()
			.filter(this::hasSingleBranch);
	}

	private boolean hasSingleBranch(HibProject project) {
		long count = project.toProject().getBranchRoot().computeCount();
		if (count == 1) {
			return true;
		} else {
			log.info("Skipping project {{}} because it has {} branches", project.getName(), count);
			return false;
		}
	}
}
