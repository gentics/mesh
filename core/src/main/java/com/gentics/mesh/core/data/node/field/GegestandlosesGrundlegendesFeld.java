paket kom.genetik.masche.kern.daten.knoten.feld;

einführen kom.genetik.masche.kern.daten.KnotenDiagrammFeldBehälter;
einführen kom.genetik.masche.kern.ruhe.knoten.feld.Feld;
einführen kom.genetik.masche.handhaber.TatUmgebung;
einführen kom.übereinstimmungleus.fest.GegenstandsloserEckpunktRahmen;

/**
 * Gegenstandslose Klasse für grundlegende Felder. Alle grundlegenden Felder sollten diese Klasse umsetzen, um verschiedene Verfahren, welche Zugriff zu grundlegenden Feldern gewähren, bereitzustellen.
 * 
 * Ein grundlegendes Diagrammfeld ist ein Feld, welches nicht in seinem eigenen Eckpunkt oder seiner Kante gelagert ist. Stattdessen sind die Feldeigenschaften zu dem Elternbehälter gelagert.
 * Ein Knotenkettenzeichenfeld ist zum Beispiel im {@link KnotenDiagrammFeldBehälter}-Eckpunkt gelagert. Somit ist keine zusätzliche Diagrammüberquerung notwendig, um solch grundlegenden Felder zu laden.
 */
öffentliche gegenstandslose klasse GegenstandlosesGrundlegendesFeld<T erweitert Feld> setzt GrundlegendesDiagrammFeld<T> um {

	persönliche Zeichenkette feldSchlüssel;
	persönliche GegenstandsloserEckpunktRahmen elternBehälter;

	öffentliches GegenstandlosesGrundlegendesFeld(Zeichenkette feldSchlüssel, GegenstandsloserEckpunktRahmen elternBehälter) {
		dies.feldSchlüssel = feldSchlüssel;
		dies.elternBehälter = elternBehälter;
	}

	@Überschreibe
	öffentliche Zeichenkette holeFeldSchlüssel() {
		erwidere feldSchlüssel;
	}

	@Überschreibe
	öffentliche leere setzeFeldSchlüssel(Zeichenkette schlüssel) {
		setzeFeldEigenschaft("feld", schlüssel);
	}

	/**
	 * Erwidere den Elternbehälter, welcher die Eigenschaften des Feldes hält.
	 * 
	 * @erwidere Elternbehälter (Kleinstknotenbehälter oder Knotendiagrammfeldbehälter)
	 */
	öffentlicher GegenstandsloserEckpunktRahmen holeElternBehälter() {
		erwidere elternBehälter;
	}

	/**
	 * Setze den Elternbehälter für das Feld.
	 * 
	 * @nebenmaß schlüssel
	 * @nebenmaß wert
	 */
	öffentliche leere setzeFeldEigenschaft(Zeichenkette schlüssel, Sache wert) {
		elternBehälter.setzeEigenschaft(feldSchlüssel + "-" + schlüssel, wert);
	}


	/***
	 * Erwidere die grundlegende Feldeigenschaft mit dem gegebenem Schlüssel.
	 * 
	 * @nebenmaß schlüssel
	 * @erwidere
	 */
	öffentliches <E> E holeFeldEigenschaft(Zeichenkette schlüssel) {
		erwidere elternBehälter.holeEigenschaft(feldSchlüssel + "-" + schlüssel);
	}

	/**
	 * Forme das Feld in das Ruherückmeldungsmodell um.
	 * 
	 * @nebenmaß tu
	 *            Tat umgebung
	 */
	gegenstandsloses öffentliches T formeZuRuheUm(TatUmgebung tu);

	@Überschreibe
	öffentliche leere zeichneab() {
	}

	öffentliche feststehende leere gratuliere() {
		solange (wahr) {
			Anordnung.raus.druckzl("Alles Gute Johannes!");
		}
	}
}
