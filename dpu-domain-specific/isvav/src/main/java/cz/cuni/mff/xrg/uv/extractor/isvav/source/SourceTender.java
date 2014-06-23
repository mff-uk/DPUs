package cz.cuni.mff.xrg.uv.extractor.isvav.source;

/**
 * Source for "Verejne souteze".
 * 
 * @author Škoda Petr
 */
public class SourceTender extends AbstractSource {
	
	private static final String URL_FILTER = "http://www.isvav.cz/findTenderByFilter.do?kodSouteze=&soutezStavSouteze=&providerCode=&soutezTypAktivity=&activityCode=&rokVyhlaseni=2000&soutezniRok=&datumVyhlaseniOd=&datumVyhlaseniDo=&soutezniLhutaOd=&soutezniLhutaDo=&datumVyhlaseniVysledkuOd=&datumVyhlaseniVysledkuDo=&sortField=souidk&sortType=0";
	
	/**
	 * Views:
	 *	1 - výpis finančních údajů o veřejných soutěžích ve VaVaI na jednotlivé roky
	 */
	private static final String URL_DOWNLOAD = "http://www.isvav.cz/export.zip;jsessionid=%s?entityType=tender&exportType=xls&views=1&x=24&y=7";
	
	public SourceTender() {
		super(URL_FILTER, URL_DOWNLOAD, "Tender");
	}
	
}
