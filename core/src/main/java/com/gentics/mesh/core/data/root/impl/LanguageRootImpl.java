package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Iterator;
import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.event.EventQueueBatch;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Vertex;

/**
 * @see LanguageRoot
 */
public class LanguageRootImpl extends AbstractRootVertex<Language> implements LanguageRoot {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(LanguageRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_LANGUAGE).withInOut());
		// TODO add unique index
	}

	@Override
	public Class<LanguageImpl> getPersistanceClass() {
		return LanguageImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_LANGUAGE;
	}

	@Override
	public long globalCount() {
		return db().count(LanguageImpl.class);
	}

	@Override
	public void addLanguage(Language language) {
		addItem(language);
	}

	@Override
	public Language create(String languageName, String languageTag, String uuid) {
		Language language = getGraph().addFramedVertex(LanguageImpl.class);
		if (uuid != null) {
			language.setUuid(uuid);
		}
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		addLanguage(language);
		return language;
	}

	@Override
	public Language create(InternalActionContext rc, EventQueueBatch batch, String uuid) {
		throw new NotImplementedException("Languages can be created using REST");
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	@Override
	public Language findByLanguageTag(String languageTag) {
		Iterator<Vertex> it = db().getVertices(LanguageImpl.class, new String[] { LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY },
				new Object[] { languageTag });
		if (it.hasNext()) {
			//TODO check whether the language was assigned to this root node?
			//return out(HAS_LANGUAGE).has(LanguageImpl.class).has("languageTag", languageTag).nextOrDefaultExplicit(LanguageImpl.class, null);
			FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
			return graph.frameElementExplicit(it.next(), LanguageImpl.class);
		} else {
			return null;
		}
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		throw error(BAD_REQUEST, "Languages are not accessible");
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The language root should never be deleted.");
	}

}
