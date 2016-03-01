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

package be.uza.keratoconus.userprefs.api;

import javafx.scene.Node;

public interface PreferencesWindow {

	static final String STYLE_CLASS_BUTTON = "ka-button";
	static final String STYLE_CLASS_TITLE = "ka-title";
	static final String STYLE_CLASS_DESCRIPTION = "ka-description";
	static final String STYLE_CLASS_SLIDER = "ka-slider";
	static final String STYLE_CLASS_CHECKBOX = "ka-checkbox";
	public static final String STYLE_CLASS_ROOT = "ka-root";
	public static final String STYLE_CLASS_FOOTER = "ka-footer";
	public static final String STYLE_CLASS_CONTENT = "ka-content";
	public static final String STYLE_CLASS_HEADER = "ka-header";

	void setup(UserPreferences prefs);
	
	String getTitle();
	
	String getHeader();
	
	Node getContent();

	String getFooter();

}
