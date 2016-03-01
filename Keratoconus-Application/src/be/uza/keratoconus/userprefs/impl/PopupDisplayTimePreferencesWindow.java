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
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties = "rank=20")
public class PopupDisplayTimePreferencesWindow extends
		AbstractPreferencesWindow implements PreferencesWindow {

	public PopupDisplayTimePreferencesWindow() {
		super(
				"Popup display time",
				"Choose the time in seconds for which the pop-up will be shown, for each category of result:");
	}

	@Override
	public Pane createContent() {
		int row = 0;
		final GridPane gridPane = new GridPane();
		for (Classification.Category cat : Classification.Category.values()) {
			Text description = new Text(cat.getDescription());
			description.getStyleClass().add(STYLE_CLASS_TITLE);
			gridPane.add(description, 0, row);
			final Slider slider = new Slider(0.0D, 60.0D,
					prefs.getDisplayTimeSeconds(cat));
			slider.setShowTickMarks(true);
			slider.setShowTickLabels(true);
			slider.setMajorTickUnit(10D);
			slider.setBlockIncrement(5D);
			slider.setSnapToTicks(true);
			slider.valueProperty().addListener(new ChangeListener<Number>() {
				public void changed(ObservableValue<? extends Number> obs,
						Number oldval, Number newval) {
					prefs.configureDisplayTimeSeconds(cat, newval.doubleValue());
				}
			});
			gridPane.add(slider, 1, row);
			++row;
		}
		return new VBox(gridPane);
	}

}
