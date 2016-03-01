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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;
import be.uza.keratoconus.userprefs.api.UserPreferences.ChartType;

@Component(properties = "rank=10")
public class ChartTypePreferencesWindow extends AbstractPreferencesWindow
		implements PreferencesWindow {

	public ChartTypePreferencesWindow() {
		super(
				"Chart type",
				"Choose the kind of chart which will be used to visualise the probability distribution of the possible indications:");
	}

	@Override
	protected Pane createContent() {
		final ToggleGroup tg = new ToggleGroup();
		ChartType currentType = prefs.getDetailChartType();
		RadioButton barButton = createButton(tg, ChartType.BAR, currentType);
		RadioButton pieButton = createButton(tg, ChartType.PIE, currentType);

		tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

			@Override
			public void changed(ObservableValue<? extends Toggle> obs,
					Toggle oldval, Toggle newval) {
				if (newval != null) {
					prefs.configureDetailChartType((ChartType) newval
							.getUserData());
				}

			}
		});
		return new VBox(barButton, pieButton);
	}
}
