package be.uza.keratoconus.systemtrayapp.api;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which dispalys a window with user documentation.
 * @author Chris Gray
 *
 */
@ProviderType
public interface HtmlViewerService {

	public static final String SHOWPAGE_TITLE = "title";
	public static final String SHOWPAGE_PATH = "path";
	public static final String SHOWPAGE_TOPIC = "be/kiffer/uza/keratoconus/htmlviewer/showpage";

	void showPage(String path, String title);

}
