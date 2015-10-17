package be.uza.keratoconus.userprefs.impl;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import aQute.bnd.annotation.component.Component;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties="rank=30")
public class PopupPositionPreferencesWindow extends AbstractPreferencesWindow implements PreferencesWindow {

	public PopupPositionPreferencesWindow() {
		super("Popup position", "Choose the position on the screen where the pop-up result window will appear:");
	}
	
	@Override
	public  Pane createContent() {
		final GridPane pane = new GridPane();
		final ToggleGroup tg = new ToggleGroup();
		Pos currentPosition = prefs.getMainPopupPosition();
		Text leftText = new Text("Left");
		leftText.getStyleClass().add(STYLE_CLASS_TITLE);
		GridPane.setHgrow(leftText, Priority.ALWAYS);
		GridPane.setHalignment(leftText, HPos.CENTER);
		pane.add(leftText, 1, 0);
		Text centreText = new Text("Centre");
		centreText.getStyleClass().add(STYLE_CLASS_TITLE);
		GridPane.setHgrow(centreText, Priority.ALWAYS);
		GridPane.setHalignment(centreText, HPos.CENTER);
		pane.add(centreText, 2, 0);
		Text rightText = new Text("Right");
		rightText.getStyleClass().add(STYLE_CLASS_TITLE);
		GridPane.setHgrow(rightText, Priority.ALWAYS);
		GridPane.setHalignment(rightText, HPos.CENTER);
		pane.add(rightText, 3, 0);
		
		Text topText = new Text("Top");
		topText.getStyleClass().add(STYLE_CLASS_TITLE);
		pane.add(topText, 0, 1);
		addPositionButton(pane, tg, currentPosition, Pos.TOP_LEFT, 1, 1);
		addPositionButton(pane, tg, currentPosition, Pos.TOP_CENTER, 2, 1);
		addPositionButton(pane, tg, currentPosition, Pos.TOP_RIGHT, 3, 1);
		
		Text middleText = new Text("Middle");
		middleText.getStyleClass().add(STYLE_CLASS_TITLE);
		pane.add(middleText, 0, 2);
		addPositionButton(pane, tg, currentPosition, Pos.CENTER_LEFT, 1, 2);
		addPositionButton(pane, tg, currentPosition, Pos.CENTER, 2, 2);
		addPositionButton(pane, tg, currentPosition, Pos.CENTER_RIGHT, 3, 2);
		
		Text bottomText = new Text("Bottom");
		bottomText.getStyleClass().add(STYLE_CLASS_TITLE);
		pane.add(bottomText, 0, 3);
		addPositionButton(pane, tg, currentPosition, Pos.BOTTOM_LEFT, 1, 3);
		addPositionButton(pane, tg, currentPosition, Pos.BOTTOM_CENTER, 2, 3);
		addPositionButton(pane, tg, currentPosition, Pos.BOTTOM_RIGHT, 3, 3);
		tg.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {

			@Override
			public void changed(ObservableValue<? extends Toggle> obs,
					Toggle oldval, Toggle newval) {
				if (newval != null) {
					prefs.configureMainPopupPosition((Pos) newval.getUserData());
				}

			}
		});
		return new VBox(pane);
	}

	private void addPositionButton(GridPane pane,
			final ToggleGroup tg, Pos currentPosition, Pos pos, int col, int row) {
		RadioButton radioButton = new RadioButton();
		radioButton.getStyleClass().add(STYLE_CLASS_BUTTON);
		GridPane.setHalignment(radioButton, HPos.CENTER);
		pane.add(radioButton, col, row);
		radioButton.setToggleGroup(tg);
		radioButton.setUserData(pos);
		if (currentPosition == pos) {
			radioButton.setSelected(true);
			radioButton.requestFocus();
		}
	}

}
