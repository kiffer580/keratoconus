package be.uza.keratoconus.userprefs.impl;

import java.io.File;
import java.nio.file.FileSystems;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

@Component(properties = "rank=5")
public class PatientRecordPreferencesWindow extends AbstractPreferencesWindow implements PreferencesWindow {
	
	private static final String CURRENT_PATH_LABEL_PREFIX = "Path: ";
	private static final String MENU_TEXT_ENABLE = "Create graphical patient records";
	private Label currentPathLabel;
	private Button choosePathButton;
	private PentacamConfigurationService pentacamConfigurationService;

	public PatientRecordPreferencesWindow() {
		super("Patient record", "Choose whether and where graphical patient records will be stored.");
	}

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService s) {
		pentacamConfigurationService = s;
	}

	@Override
	public Pane createContent() {
		currentPathLabel = new Label();
		setCurrentPathLabel(prefs.getPatientRecordDirectory());
		StackPane.setAlignment(currentPathLabel, Pos.CENTER_LEFT);
		
		choosePathButton = new Button("change ...");
		choosePathButton.setVisible(prefs.isPatientRecordEnabled());
		choosePathButton.setOnMouseClicked(event -> showPatientRecordDirectoryDialogue());
		choosePathButton.getStyleClass().add(STYLE_CLASS_DESCRIPTION);
		StackPane.setAlignment(choosePathButton, Pos.CENTER_RIGHT);
		
		CheckBox enableCheckBox = patientRecordCheckBox(
				MENU_TEXT_ENABLE, currentPathLabel, prefs.isPatientRecordEnabled());

		return new VBox(enableCheckBox, new StackPane(choosePathButton));
	}

	private void setCurrentPathLabel(String absolutePath) {
		currentPathLabel.setText(CURRENT_PATH_LABEL_PREFIX + absolutePath);
	}

	private CheckBox patientRecordCheckBox(String text, Label label, boolean selected) {
		CheckBox cb = new CheckBox();
		Text textText = new Text(text);
		textText.getStyleClass().add(STYLE_CLASS_TITLE);
		label.getStyleClass().add(STYLE_CLASS_DESCRIPTION);
		label.managedProperty().bind(label.visibleProperty());
		label.setVisible(prefs.isPatientRecordEnabled());
		cb.getStyleClass().add(STYLE_CLASS_CHECKBOX);
		cb.setGraphic(new VBox(textText, label));
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
		case MENU_TEXT_ENABLE:
			prefs.configureGraphicalPatientRecordOnOff(newVal);
			currentPathLabel.setVisible(newVal);
			choosePathButton.setVisible(newVal);
			default:
		}
		
	}
	
	private void showPatientRecordDirectoryDialogue() {
	DirectoryChooser directoryChooser = new DirectoryChooser();
	directoryChooser.setTitle(pentacamConfigurationService
			.getApplicationTitle()
			+ " - choose directory where patient records will be stored");
	directoryChooser.setInitialDirectory(FileSystems.getDefault()
			.getPath(prefs.getPatientRecordDirectory()).toFile());
	Platform.runLater(() -> {
		File chosen = directoryChooser.showDialog(null);
		if (chosen != null) {
			String absolutePath = chosen.getAbsolutePath();
			prefs.configurePatientRecordDirectory(absolutePath);
			setCurrentPathLabel(absolutePath);
		}
	});
}

}
