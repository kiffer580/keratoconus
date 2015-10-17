package be.uza.keratoconus.systemtrayapp.api;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which dispalys a window with user documentation.
 * @author Chris Gray
 *
 */
@ProviderType
public interface HtmlViewerService {

	void showPage(String path, String title);

}
