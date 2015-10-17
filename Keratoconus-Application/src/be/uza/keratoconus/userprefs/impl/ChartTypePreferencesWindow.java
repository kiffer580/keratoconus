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

	/*
	 * 
	 * 28 11:00 Deurne Clara Snellingsstraat 27 Inkomen bewijs
	 * 
	 * schulden
	 */
}
