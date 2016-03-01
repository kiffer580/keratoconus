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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties = "rank=50")
public class ResultDisplayPreferencesWindow extends AbstractPreferencesWindow implements PreferencesWindow {
	
	private static final String MENU_TEXT_ENABLE_ANIMATE = "Animate taskbar icon";
	private static final String MENU_TEXT_ENABLE_MESSAGES = "Show taskbar messages";
	private static final String MENU_TEXT_ENABLE_POPUPS = "Show floating popups";

	public ResultDisplayPreferencesWindow() {
		super("Results display", "Choose how the user will be notified of the result of the analysis:", "Note that if the floating pop-up window is not enabled here it can still be shown by double-clicking on the task bar icon.");
	}

	@Override
	public Pane createContent() {
		CheckBox animateCheckBox = createResultDisplayCheckBox(
				MENU_TEXT_ENABLE_ANIMATE, "The icon in the task bar will flash; the colour depends on the category of the result.", prefs.isAnimateEnabled());
		CheckBox messagesCheckBox = createResultDisplayCheckBox(
				MENU_TEXT_ENABLE_MESSAGES, "A small message 'bubble' appears next to the task bar icon.", prefs.isMessagesEnabled());
		CheckBox popupsCheckBox = createResultDisplayCheckBox(
				MENU_TEXT_ENABLE_POPUPS, "A pop-up window is shown where detailed information can also be viewed.", prefs.isPopupsEnabled());
		
		return new VBox(animateCheckBox, messagesCheckBox, popupsCheckBox);
	}

	private CheckBox createResultDisplayCheckBox(String text, String description, boolean selected) {
		CheckBox cb = new CheckBox();
		Text textText = new Text(text);
		textText.getStyleClass().add(STYLE_CLASS_TITLE);
		Label descriptionLabel = new Label(description);
		descriptionLabel.getStyleClass().add(STYLE_CLASS_DESCRIPTION);
		cb.getStyleClass().add(STYLE_CLASS_CHECKBOX);
		cb.setGraphic(new VBox(textText, descriptionLabel));
		cb.setSelected(selected);
		cb.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(ObservableValue<? extends Boolean> obs,
					Boolean oldVal, Boolean newVal) {
				checkBoxChanged(text, newVal);	
			}
			
		});
		return cb;
	}

	protected void checkBoxChanged(String text, Boolean newVal) {
		switch (text) {
		case MENU_TEXT_ENABLE_ANIMATE:
			prefs.configureAnimateOnOff(newVal);
			break;
		case MENU_TEXT_ENABLE_MESSAGES:
			prefs.configureMessagesOnOff(newVal);
			break;
		case MENU_TEXT_ENABLE_POPUPS:
			prefs.configurePopupsOnOff(newVal);
			default:
		}
		
	}

	/*
	 * 
	 * 28 11:00 Deurne Clara Snellingsstraat 27 Inkomen bewijs
	 * 
	 * schulden
	 */
}
