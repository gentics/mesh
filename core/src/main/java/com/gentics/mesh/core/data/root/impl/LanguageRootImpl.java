package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;

import java.util.Iterator;
import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Vertex;

import rx.Single;

public class LanguageRootImpl extends AbstractRootVertex<Language> implements LanguageRoot {

	public static void init(Database database) {
		database.addVertexType(LanguageRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_LANGUAGE);
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
	public void addLanguage(Language language) {
		addItem(language);
	}

	@Override
	public Language create(String languageName, String languageTag) {
		LanguageImpl language = getGraph().addFramedVertex(LanguageImpl.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		addLanguage(language);
		return language;
	}

	@Override
	public Single<Language> create(InternalActionContext rc) {
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
		Database db = MeshCore.get().database();
		Iterator<Vertex> it = db.getVertices(LanguageImpl.class, new String[] { LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY },
				new Object[] { languageTag });
		if (it.hasNext()) {
			//TODO check whether the language was assigned to this root node?
			//return out(HAS_LANGUAGE).has(LanguageImpl.class).has("languageTag", languageTag).nextOrDefaultExplicit(LanguageImpl.class, null);
			FramedGraph graph = Database.getThreadLocalGraph();
			return graph.frameElementExplicit(it.next(), LanguageImpl.class);
		} else {
			return null;
		}
	}

	/**
	 * The tag language is currently fixed to english since we only want to store tags based on a single language. The idea is that tags will be localizable in
	 * the future.
	 */
	@Override
	public Language getTagDefaultLanguage() {
		return findByLanguageTag(TagImpl.DEFAULT_TAG_LANGUAGE_TAG);
	}

	@Override
	public Single<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		throw new NotImplementedException();
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		throw new NotImplementedException("The language root should never be deleted.");
	}

}
