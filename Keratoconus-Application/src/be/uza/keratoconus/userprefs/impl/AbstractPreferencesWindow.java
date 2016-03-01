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


package be.uza.keratoconus.userprefs.impl;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;
import be.uza.keratoconus.userprefs.api.UserPreferences;

public abstract class AbstractPreferencesWindow implements PreferencesWindow {

	private final String title;
	private final String header;
	private final String footer;
	private Pane content;
	protected UserPreferences prefs;

	public AbstractPreferencesWindow(String title, String header) {
		this.title = title;
		this.header = header;
		footer = null;
	}

	public AbstractPreferencesWindow(String title, String header, String footer) {
		this.title = title;
		this.header = header;
		this.footer = footer;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getHeader() {
		return header;
	}

	@Override
	public Node getContent() {
		return content;
	}

	@Override
	public String getFooter() {
		return footer;
	}

	protected void setContent(Pane content) {
		this.content = content;
	}

	@Override
	public void setup(UserPreferences prefs) {
		this.prefs = prefs;
		setContent(createContent());
	}

	abstract Pane createContent();

	protected <T extends Enum<?>> RadioButton createButton(
			final ToggleGroup tg, T value, T selectedValue) {

		return createButton(tg, value, null, selectedValue);
	}

	protected <T extends Enum<?>> RadioButton createButton(
			final ToggleGroup tg, T value, String description, T selectedValue) {
		String valueName = value.name();
		String title = valueName.substring(0, 1)
				+ valueName.substring(1).toLowerCase();
		RadioButton rb = new RadioButton();
		if (description == null) {
			rb.setText(title);
		} else {
			Text titleText = new Text(title);
			titleText.getStyleClass().add(STYLE_CLASS_TITLE);
			Label descriptionLabel = new Label(description);
			descriptionLabel.getStyleClass().add(STYLE_CLASS_DESCRIPTION);
			rb.setGraphic(new VBox(titleText, descriptionLabel));
		}
		rb.getStyleClass().add(STYLE_CLASS_BUTTON);
		rb.setToggleGroup(tg);
		rb.setUserData(value);
		if (value == selectedValue) {
			rb.setSelected(true);
			rb.requestFocus();
		}
		return rb;
	}
}
