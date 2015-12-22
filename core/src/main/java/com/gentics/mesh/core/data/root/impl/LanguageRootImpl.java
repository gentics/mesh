package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LANGUAGE;

import java.util.Iterator;
import java.util.Stack;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Vertex;

import rx.Observable;

public class LanguageRootImpl extends AbstractRootVertex<Language>implements LanguageRoot {

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_LANGUAGE);
		database.addVertexType(LanguageRootImpl.class);
		// TODO add unique index
	}

	@Override
	protected Class<LanguageImpl> getPersistanceClass() {
		return LanguageImpl.class;
	}

	@Override
	protected String getRootLabel() {
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
	public Observable<Language> create(InternalActionContext rc) {
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
		Database db = MeshSpringConfiguration.getInstance().database();
		Iterator<Vertex> it = db.getVertices(LanguageImpl.class, new String[] { LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY }, new Object[] {languageTag});
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
	public Observable<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		throw new NotImplementedException();
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The language root should never be deleted.");
	}

}
