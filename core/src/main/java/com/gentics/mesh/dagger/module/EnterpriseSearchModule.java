package com.gentics.mesh.dagger.module;

import javax.inject.Singleton;

import com.gentics.mesh.contentrag.chunking.ChunkingOptions;
import com.gentics.mesh.contentrag.meshsync.RagChunkEventHandler;
import com.gentics.mesh.contentrag.meshsync.RagChunkIndexHandler;
import com.gentics.mesh.contentrag.meshsync.RagChunkWriter;
import com.gentics.mesh.contentrag.meshsync.RagFieldSelector;
import com.gentics.mesh.contentrag.meshsync.RagSourceContentBuilder;
import com.gentics.mesh.contentrag.meshsync.RagSyncConfig;
import com.gentics.mesh.contentrag.meshsync.RagSyncOptions;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.EventHandler;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoSet;

/**
 * Bindet den RAG-Chunk-Sync in die Suche ein.
 *
 * <p>Die beiden Handler haengen sich ueber die {@code @Multibinds}-Erweiterungspunkte aus
 * {@code CommonBindModule} an {@code MainEventHandler} und {@code IndexHandlerRegistry}. Die
 * Handler stammen aus {@code content-rag/mesh-sync}; eingebunden wird das Modul ueber
 * {@code CommonModule}.
 */
@Module
public abstract class EnterpriseSearchModule {

	/**
	 * @param handler Delta-Pfad des RAG-Index
	 * @return Beitrag zum Set der zusaetzlichen Event-Handler
	 */
	@Binds
	@IntoSet
	abstract EventHandler ragChunkEventHandler(RagChunkEventHandler handler);

	/**
	 * @param handler Voll-Sync und Aufraeumen des RAG-Index
	 * @return Beitrag zum Set der zusaetzlichen Index-Handler
	 */
	@Binds
	@IntoSet
	abstract IndexHandler<?> ragChunkIndexHandler(RagChunkIndexHandler handler);

	/**
	 * Betriebsparameter aus Umgebung und System-Properties. Vorgabe: aus.
	 *
	 * @return Parameter des RAG-Syncs
	 */
	@Provides
	@Singleton
	public static RagSyncOptions ragSyncOptions() {
		return RagSyncConfig.syncOptions();
	}

	/**
	 * @return Chunk-Groesse und Ueberlappung
	 */
	@Provides
	@Singleton
	public static ChunkingOptions ragChunkingOptions() {
		return RagSyncConfig.chunkingOptions();
	}

	/**
	 * @param options Betriebsparameter
	 * @return Feldauswahl mit Cache je Schemaversion
	 */
	@Provides
	@Singleton
	public static RagFieldSelector ragFieldSelector(RagSyncOptions options) {
		return new RagFieldSelector(options);
	}

	/**
	 * @param selector Feldauswahl
	 * @return Aufbau des Quellinhalts aus einem Node-Container
	 */
	@Provides
	@Singleton
	public static RagSourceContentBuilder ragSourceContentBuilder(RagFieldSelector selector) {
		return new RagSourceContentBuilder(selector, RagSyncConfig.sourceUrlTemplate());
	}

	/**
	 * @param helper          Request-Factory von Mesh
	 * @param searchProvider  Zugriff auf Client und Praefix
	 * @param compliance      Elasticsearch-Kompatibilitaetsmodus
	 * @param builder         Aufbau des Quellinhalts
	 * @param chunkingOptions Chunk-Parameter
	 * @return gemeinsame Schreiblogik beider Handler
	 */
	@Provides
	@Singleton
	public static RagChunkWriter ragChunkWriter(MeshHelper helper, SearchProvider searchProvider,
			Compliance compliance, RagSourceContentBuilder builder, ChunkingOptions chunkingOptions) {
		return new RagChunkWriter(helper, searchProvider, compliance, builder, chunkingOptions);
	}
}
