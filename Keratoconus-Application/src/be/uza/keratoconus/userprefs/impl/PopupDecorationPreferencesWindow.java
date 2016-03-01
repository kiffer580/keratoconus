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
import javafx.stage.StageStyle;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties = "rank=40")
public class PopupDecorationPreferencesWindow extends AbstractPreferencesWindow
		implements PreferencesWindow {

	public PopupDecorationPreferencesWindow() {
		super(
				"Popup window",
				"Choose the style of the pop-up window which presents the results of the analysis:",
				"The application must be restarted in order for the new style to take effect.");
	}

	@Override
	public Pane createContent() {

		final ToggleGroup tg = new ToggleGroup();
		StageStyle currentStyle = prefs.getMainPopupStageStyle();
		RadioButton decoratedButton = createButton(tg, StageStyle.DECORATED,
				"Standard window with frame, title bar, and controls.",
				currentStyle);
		RadioButton utilityButton = createButton(
				tg,
				StageStyle.UTILITY,
				"With frame, title bar but no controls.  Also suppresses the window's icon in the task bar.",
				currentStyle);
		RadioButton undecoratedButton = createButton(tg,
				StageStyle.UNDECORATED,
				"No frame or title bar. Click in the window to dismiss.",
				currentStyle);
		RadioButton transparentButton = createButton(
				tg,
				StageStyle.TRANSPARENT,
				"No frame or title bar, translucent. Click in the window to dismiss.",
				currentStyle);
		tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

			@Override
			public void changed(ObservableValue<? extends Toggle> obs,
					Toggle oldval, Toggle newval) {
				if (newval != null) {
					prefs.configureMainPopupStageStyle((StageStyle) newval
							.getUserData());
				}

			}
		});

		return new VBox(decoratedButton, utilityButton, undecoratedButton,
				transparentButton);
	}
}
