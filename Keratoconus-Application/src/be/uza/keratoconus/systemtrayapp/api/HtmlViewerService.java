/*
    This file is part of Keratoconus Assistant.

    Keratoconus Assistant is free software: you can redistribute it 
    and/or modify it under the terms of the GNU General Public License 
    as published by the Free Software Foundation, either version 3 of 
    the License, or (at your option) any later version.

    Keratoconus Assistant is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Keratoconus Assistant.  If not, see 
    <http://www.gnu.org/licenses/>.
 */

package be.uza.keratoconus.systemtrayapp.api;

import aQute.bnd.annotation.ProviderType;

/**
 * Service which displays a window with user documentation.
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
