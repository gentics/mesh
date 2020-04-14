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

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
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
		singleBranchProjects()
			.flatMap(project -> project.getNodeRoot().findAll().stream())
			.forEach(node -> {
				long count = nodeCount.get();
				if (count != 0 && count % 1000 == 0) {
					log.info("Checked {} nodes. Found and fixed {} broken nodes", count, fixedNodes.get());
				}
				node.getGraphFieldContainers(ContainerType.INITIAL).stream()
					.forEach(content -> {
						boolean mutated = fixNodeVersionOrder(node, content);
						if (mutated) {
							fixedNodes.incrementAndGet();
						}
					});
				nodeCount.incrementAndGet();
			});
		log.info("Checked {} nodes. Found and fixed {} broken nodes", nodeCount.get(), fixedNodes.get());
	}

	private boolean fixNodeVersionOrder(Node node, NodeGraphFieldContainer initialContent) {
		Set<VersionNumber> seenVersions = new HashSet<>();
		Set<Object> deletedVertexIds = new HashSet<>();
		TreeSet<NodeGraphFieldContainer> versions = new TreeSet<>(Comparator.comparing(NodeGraphFieldContainer::getVersion));
		NodeGraphFieldContainer highestVersion = null;
		NodeGraphFieldContainer currentContainer = initialContent;
		NodeGraphFieldContainer latestPublished = null;
		DummyBulkActionContext bac = new DummyBulkActionContext();
		boolean mutated = false;

		String branchUuid = initialContent.getBranches(ContainerType.INITIAL).iterator().next();
		String languageTag = initialContent.getLanguageTag();

		EdgeFrame originalDraftEdge = node.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, ContainerType.DRAFT);
		Optional<Object> originalDraftId = Optional.ofNullable(node.getGraphFieldContainer(languageTag, branchUuid, ContainerType.DRAFT))
			.map(ElementFrame::id);
		EdgeFrame originalPublishedEdge = node.getGraphFieldContainerEdgeFrame(languageTag, branchUuid, ContainerType.PUBLISHED);
		Optional<Object> originalPublishedId = Optional.ofNullable(node.getGraphFieldContainer(languageTag, branchUuid, ContainerType.PUBLISHED))
			.map(ElementFrame::id);

		while (currentContainer != null) {
			// Since we only look at single branch projects, there can at most only be one next version.
			NodeGraphFieldContainer nextContainer = Iterables.getFirst(currentContainer.getNextVersions(), null);
			VersionNumber currentVersion = currentContainer.getVersion();
			boolean isDuplicate = !seenVersions.add(currentVersion);
			if (isDuplicate) {
				deletedVertexIds.add(currentContainer.id());
				currentContainer.delete(bac, false);
				if (nextContainer != null) {
					// highestVersion can't be null because this if-branch is only visited if there is a duplicate
					highestVersion.setSingleLinkOutTo(nextContainer, HAS_VERSION);
				}
				mutated = true;
				currentContainer = nextContainer;
			} else {
				if (highestVersion != null && currentVersion.compareTo(highestVersion.getVersion()) < 0) {
					// Wrong order

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
		}

		// Reset draft and published edge

		if (originalDraftId.isPresent() && !originalDraftId.get().equals(highestVersion.id())) {
			setNewOutV(node, highestVersion, branchUuid, languageTag, ContainerType.DRAFT);
			// Deleting the edge would fail if it was already removed implicitly by removing one of the vertices
			if (!deletedVertexIds.contains(originalDraftId.get())) {
				originalDraftEdge.remove();
			}
			mutated = true;
		}

		if (originalPublishedId.isPresent() && latestPublished != null && !originalPublishedId.get().equals(latestPublished.id())) {
			setNewOutV(node, latestPublished, branchUuid, languageTag, ContainerType.PUBLISHED);
			// Deleting the edge would fail if it was already removed implicitly by removing one of the vertices
			if (!deletedVertexIds.contains(originalPublishedId.get())) {
				originalPublishedEdge.remove();
			}
			mutated = true;
		}
		Tx.get().commit();
		return mutated;
	}

	private GraphFieldContainerEdge setNewOutV(Node from, NodeGraphFieldContainer to, String branchUuid, String languageTag, ContainerType type) {
		GraphFieldContainerEdge newEdge = from.addFramedEdge(HAS_FIELD_CONTAINER, to, GraphFieldContainerEdgeImpl.class);
		newEdge.setBranchUuid(branchUuid);
		newEdge.setLanguageTag(languageTag);
		newEdge.setType(type);
		to.updateWebrootPathInfo(branchUuid, "node_conflicting_segmentfield_update");
		return newEdge;
	}

	private Stream<? extends Project> singleBranchProjects() {
		return boot.get().projectRoot().findAll().stream()
			.filter(this::hasSingleBranch);
	}

	private boolean hasSingleBranch(Project project) {
		long count = project.getBranchRoot().computeCount();
		if (count == 1) {
			return true;
		} else {
			log.info("Skipping project {{}} because it has {} branches", project.getName(), count);
			return false;
		}
	}
}
