package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.jpa.AvailableHints;

import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.QueryHint;

/**
 * Entity representing the child/parent relationship for nodes in branches.
 * Implemented as a closure table (https://www.slideshare.net/billkarwin/sql-antipatterns-strike-back?src=embed slide 68)
 * to speed up hierarchical queries (e.g. breadcrumbs query).
 * An alternative solution would be to use Common Table Expression but this is not supported by all databases and JPA.
 *
 */
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "node_branch_parent")
@NamedQueries(
        value = {
                @NamedQuery(
                        name = "nodeBranchParent.findChildrenByNode",
                        query = "select distinct n.child from node_branch_parent n " +
                                "where n.nodeParent = :node " +
                                "and n.distance = 1"
                ),
                @NamedQuery(
                        name = "nodeBranchParent.findChildrenByNodeAndBranch",
                        query = "select n.child from node_branch_parent n " +
                                "where n.id.nodeParentUuid = :node " +
                                "and n.id.branchParentUuid = :branch " +
                                "and n.distance = 1",
                        hints = {@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true")}
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findChildrenByNodesAndBranch",
                        query = "select n.nodeParent, n.child from node_branch_parent n " +
                                "where n.id.nodeParentUuid in :nodesUuids " +
                                "and n.id.branchParentUuid = :branch " +
                                "and n.distance = 1"
                ),
                @NamedQuery(
                        name = "nodeBranchParent.findParentNodeByNodeAndBranch",
                        query = "select n.nodeParent from node_branch_parent n " +
                                "where n.id.childUuid = :node " +
                                "and n.id.branchParentUuid = :branch " +
                                "and n.distance = 1",
                        hints = {@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true")}
                ),
                @NamedQuery(
                        name = "nodeBranchParent.findParentNodeUuidByNodeAndBranch",
                        query = "select n.nodeParent.dbUuid from node_branch_parent n " +
                                "where n.id.childUuid = :node " +
                                "and n.id.branchParentUuid = :branch " +
                                "and n.distance = 1",
                        hints = {@QueryHint(name = AvailableHints.HINT_CACHEABLE, value = "true")}
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findParentNodesByNodesAndBranch",
                        query = "select n.child, n.nodeParent from node_branch_parent n " +
                                "where n.id.childUuid in :nodesUuids " +
                                "and n.id.branchParentUuid = :branch " +
                                "and n.distance = 1"
                ),
                @NamedQuery(
                        // https://www.percona.com/blog/2011/02/14/moving-subtrees-in-closure-table/
                        name = "nodeBranchParents.disconnectParentSubTree",
                        query = "delete from node_branch_parent m " +
                                "where m.child.dbUuid in :children " +
                                "and m.nodeParent.dbUuid not in :children " +
                                "and m.branchParent = :branch"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findAncestors",
                        query = "select p.child, p.nodeParent from node_branch_parent p " +
                                "where p.child.dbUuid in :nodeUuids " +
                                "and p.branchParent.dbUuid = :branchUuid " +
                                "order by p.distance desc"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findDescendants",
                        query = "select distinct(p.child) from node_branch_parent p " +
                                "where p.nodeParent = :node "
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findDescendantsInBranch",
                        query = "select distinct(p.child) from node_branch_parent p " +
                                "where p.nodeParent = :node and p.branchParent = :branch"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.deleteParentsInBranch",
                        query = "delete from node_branch_parent m " +
                                "where m.id.childUuid = :node " +
                                "and m.id.branchParentUuid = :branch"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.findBreadcrumbs",
                        query = "select n.nodeParent from node_branch_parent n " +
                               "where n.child = :node " +
                               "and n.branchParent = :branch " +
                               "order by n.distance desc"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.deleteAllByNode",
                        query = "delete from node_branch_parent m " +
                                "where m.child = :node or m.nodeParent = :node"
                ),
                @NamedQuery(
                        name = "nodeBranchParents.deleteAllByProject",
                        query = "delete from node_branch_parent nbp " +
                                " where nbp.child.dbUuid in (select n.dbUuid from node n where n.project = :project) " +
                                " or nbp.nodeParent.dbUuid in (select n.dbUuid from node n where n.project = :project) "
                ),
                @NamedQuery(
                        name = "nodeBranchParents.deleteAllByNodeUuids",
                        query = "delete from node_branch_parent nbp " +
                                " where nbp.child.dbUuid in :nodesUuid " +
                                " or nbp.nodeParent.dbUuid in :nodesUuid "
                ),
                @NamedQuery(
                        name = "nodeBranchParents.deleteAllByNodeUuidsAndBranch",
                        query = "delete from node_branch_parent nbp " +
                                " where nbp.branchParent.dbUuid = :branchUuid " +
                                " and (nbp.child.dbUuid in :nodesUuid " +
                                " or nbp.nodeParent.dbUuid in :nodesUuid )"
                ),
        }
)
public class HibBranchNodeParent implements Serializable {

	private static final long serialVersionUID = -791188336672832004L;

    // we have to use a native query since hql does not support bulk insert for entities with a composite id
    // see https://stackoverflow.com/questions/6220594/why-this-hql-is-not-valid
    // this syntax should be understandable by all databases we are supporting
	public static final String INSERT_IN_CLOSURE_TABLE_SQL = "insert into mesh_node_branch_parent(nodeparent_dbuuid, child_dbuuid, branchparent_dbuuid, distance) " +
            "select p.nodeparent_dbuuid, c.child_dbuuid, p.branchparent_dbuuid, p.distance + c.distance + 1 " +
            "from mesh_node_branch_parent p, mesh_node_branch_parent c " +
            "where p.child_dbuuid = :parentNode and p.branchparent_dbuuid = :branch and c.nodeparent_dbuuid = :node and c.branchparent_dbuuid = :branch";

    public static final String BULK_INSERT_IN_CLOSURE_TABLE_SQL = "insert into mesh_node_branch_parent(nodeparent_dbuuid, child_dbuuid, branchparent_dbuuid, distance) " +
            "select p.nodeparent_dbuuid, p.child_dbuuid, :newBranchUuid, p.distance " +
            "from mesh_node_branch_parent p " +
            "where p.child_dbuuid in :nodeUuids and p.branchparent_dbuuid = :oldBranchUuid";

	@EmbeddedId
    private HibBranchNodeParentId id;

    @ManyToOne
    @MapsId("childUuid")
    private HibNodeImpl child;

    @ManyToOne
    @MapsId("nodeParentUuid")
    private HibNodeImpl nodeParent;

    @ManyToOne
    @MapsId("branchParentUuid")
    private HibBranchImpl branchParent;

    private int distance;

    public HibBranchNodeParent() {}

    public HibBranchNodeParent(HibNode node, HibNode parent, HibBranch branch, int distance) {
        child = (HibNodeImpl) node;
        nodeParent = (HibNodeImpl) parent;
        branchParent = (HibBranchImpl) branch;
        id = new HibBranchNodeParentId(child.getDbUuid(), nodeParent.getDbUuid(), branchParent.getDbUuid());
        this.distance = distance;
    }

    public HibBranchNodeParentId getId() {
        return id;
    }

    public void setId(HibBranchNodeParentId id) {
        this.id = id;
    }

    public HibNode getChild() {
        return child;
    }

    public void setChild(HibNode child) {
        this.child = (HibNodeImpl) child;
    }

    public HibNode getNodeParent() {
        return nodeParent;
    }

    public void setNodeParent(HibNode nodeParent) {
        this.nodeParent = (HibNodeImpl) nodeParent;
    }

    public HibBranch getBranchParent() {
        return branchParent;
    }

    public void setBranchParent(HibBranch branchParent) {
        this.branchParent = (HibBranchImpl) branchParent;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibBranchNodeParent that = (HibBranchNodeParent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
