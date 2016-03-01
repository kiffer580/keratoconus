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

import java.util.HashMap;

import org.osgi.service.event.Event;

public class ShowPageEvent extends Event {
	@SuppressWarnings("serial")
	public ShowPageEvent(String path, String title) {
		super(HtmlViewerService.SHOWPAGE_TOPIC, new HashMap<String,Object>() {
			{
				put(HtmlViewerService.SHOWPAGE_PATH, path);
				put(HtmlViewerService.SHOWPAGE_TITLE, title);
			}
		});
	}
}