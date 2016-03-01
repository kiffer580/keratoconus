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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.AvailableModelsService;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties = "rank=5")
public class ModelPreferencesWindow extends AbstractPreferencesWindow implements
		PreferencesWindow {

	private LogService logService;

	private AvailableModelsService availableModelsService;

	private String selectedModelName;

	public ModelPreferencesWindow() {
		super("Classification model",
				"Choose the classification model from among those available:",
				"The application will be restarted in order for the new choice to take effect.");
	}

	@Reference
	protected void setAvailableModelsService(AvailableModelsService ams) {
		this.availableModelsService = ams;
	}

	@Reference
	protected void setLogService(LogService ls) {
		this.logService = ls;
	}

	@Activate
	protected void activate(ComponentContext context,
			Map<String, Object> properties) {
		List<String> availableModelNames = availableModelsService
				.getAvailableModelNames();
		if (availableModelNames.size() < 2) {
			logService.log(context.getServiceReference(), LogService.LOG_INFO,
					"Found only one model so let's use it: "
							+ availableModelNames.get(0));
			context.disableComponent((String) properties
					.get(Constants.SERVICE_PID));
		} else {
			logService.log(context.getServiceReference(), LogService.LOG_INFO,
					"Available model names: " + availableModelNames);
		}

	}

	@Override
	public Pane createContent() {
		List<String> availableModelNames = availableModelsService
				.getAvailableModelNames();
		String defaultModelName = availableModelNames.get(0);
		selectedModelName = prefs.getSelectedModelName();
		if (selectedModelName == null) {
			selectedModelName = defaultModelName;
			logService.log(LogService.LOG_INFO,
					"No model selected in user preferences, defaulting to: "
							+ selectedModelName);
		} else {
			logService.log(LogService.LOG_INFO,
					"Model selected in user preferences: " + selectedModelName);
			if (!availableModelNames.contains(selectedModelName)) {
				logService.log(LogService.LOG_WARNING,
						"Model selected in user preferences: "
								+ selectedModelName
								+ " is not available, defaulting to: "
								+ defaultModelName);
				selectedModelName = defaultModelName;
			}
		}

		final List<RadioButton> buttons = new ArrayList<>();
		try {
			availableModelsService.selectModel(selectedModelName);

			final ToggleGroup tg = new ToggleGroup();
			for (String modelName : availableModelsService
					.getAvailableModelNames()) {
				String title = modelName.substring(0, 1)
						+ modelName.substring(1).toLowerCase();
				RadioButton rb = new RadioButton();
				Text titleText = new Text(title);
				titleText.getStyleClass().add(STYLE_CLASS_TITLE);
				Label descriptionLabel = new Label(
						availableModelsService.getModelDescription(modelName));
				descriptionLabel.getStyleClass().add(STYLE_CLASS_DESCRIPTION);
				rb.setGraphic(new VBox(titleText, descriptionLabel));
				rb.getStyleClass().add(STYLE_CLASS_BUTTON);
				rb.setToggleGroup(tg);
				rb.setUserData(modelName);
				if (modelName.equals(selectedModelName)) {
					rb.setSelected(true);
					rb.requestFocus();
				}
				buttons.add(rb);

			}
			tg.selectedToggleProperty().addListener(
					new ChangeListener<Toggle>() {

						@Override
						public void changed(
								ObservableValue<? extends Toggle> obs,
								Toggle oldval, Toggle newval) {
							if (newval != null) {
								prefs.configureSelectedModelName((String) newval
										.getUserData());
							}

						}
					});
		} catch (Exception e) {
			logService.log(LogService.LOG_ERROR,
					"Failed to delect classification model");
		}

		return new VBox(buttons.toArray(new RadioButton[buttons.size()]));
	}
}
