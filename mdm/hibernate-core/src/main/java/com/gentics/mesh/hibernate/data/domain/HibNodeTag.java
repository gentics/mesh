package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

import org.hibernate.annotations.Target;

import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.tag.Tag;

/**
 * Node-Tag edge.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "node_tag")
public class HibNodeTag implements Serializable {

    private static final long serialVersionUID = 9161649253483595823L;

	@EmbeddedId
    private HibNodeTagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("nodeUUID")
    @Target(HibNodeImpl.class)
    private Node node;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("tagUUID")
    @Target(HibTagImpl.class)
    private Tag tag;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("branchUUID")
    @Target(HibBranchImpl.class)
    private Branch branch;

    public HibNodeTag() {}

    public HibNodeTag(HibNodeImpl node, HibTagImpl tag, HibBranchImpl branch) {
        this.node = node;
        this.tag = tag;
        this.branch = branch;
        id = new HibNodeTagId(node, tag, branch);
    }

    public HibNodeTagId getId() {
        return id;
    }

    public void setId(HibNodeTagId id) {
        this.id = id;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HibNodeTag that = (HibNodeTag) o;
        return Objects.equals(id, that.id) && Objects.equals(node, that.node) && Objects.equals(tag, that.tag) && Objects.equals(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, node, tag, branch);
    }
}
