package be.uza.keratoconus.userprefs.impl;

import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import be.uza.keratoconus.systemtrayapp.SystemTrayMenu;
import be.uza.keratoconus.userprefs.api.PreferencesWindow;

public class UserPreferencesMenu extends Menu {

	private static class ComparableMenuItem extends MenuItem implements Comparable<ComparableMenuItem> {
		private static final long serialVersionUID = 1L;
		
		private Integer rank;
		
		private ComparableMenuItem(String label, int rank) {
			super(label);
			this.rank = rank;
		}

		@Override
		public int compareTo(ComparableMenuItem other) {
			return rank.compareTo(other.rank);
		}
	}
	
	private static final long serialVersionUID = 1L;
	
	private Map<String, MenuItem> menuMap = new HashMap<>();
	private SystemTrayMenu prefsMenu;
	
	public UserPreferencesMenu(String label, SystemTrayMenu menu) {
		super(label);
		this.prefsMenu = menu;
		
//		ComparableMenuItem patientRecordItem = new ComparableMenuItem("Patient records", 0);
//		add(patientRecordItem);
//		patientRecordItem.addActionListener(event -> Platform.runLater(() -> prefsMenu.showPatientRecordDirectoryDialogue()));
	}

	public void addPreferencesWindow(PreferencesWindow pw, int rank) {
		pw.setup(prefsMenu);
		final String title = pw.getTitle();
		final ComparableMenuItem item = new ComparableMenuItem(title, rank);
		menuMap.put(title, item);
		int position = getItemCount();
		for (int i = 0; i < position; ++i) {
			MenuItem other = getItem(i);
			if (other instanceof ComparableMenuItem && ((ComparableMenuItem) other).compareTo(item) < 0) {
				position = i;
			}
		}
		insert(item, position);
		item.addActionListener(event -> Platform.runLater(() -> displayContent(pw)));
	}

	private void displayContent(PreferencesWindow pw) {
		final Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
		final String title = pw.getTitle();
		final String header = pw.getHeader();
		final Node content = pw.getContent();
		final String footer = pw.getFooter();
		
		final Stage stage = new Stage();
		stage.setTitle(title);
		final VBox root = new VBox();
		root.getStyleClass().add(PreferencesWindow.STYLE_CLASS_ROOT);
		
		if (header != null) {
			final Label headerText = new Label(header);
			headerText.getStyleClass().add(PreferencesWindow.STYLE_CLASS_HEADER);
			root.getChildren().add(headerText);
		}
		if (!content.getStyleClass().contains(PreferencesWindow.STYLE_CLASS_CONTENT)) {
			content.getStyleClass().add(PreferencesWindow.STYLE_CLASS_CONTENT);
		}
		root.getChildren().add(content);
		if (footer != null) {
			final Label footerText = new Label(footer);
			footerText.getStyleClass().add(PreferencesWindow.STYLE_CLASS_FOOTER);
			root.getChildren().add(footerText);
		}
		
		Scene scene = new Scene(root);
		stage.setScene(scene);
		scene.getStylesheets().clear();
		scene.getStylesheets().add("file:css/userprefs.css");
		stage.show();
		Point mouseLocation = MouseInfo.getPointerInfo().getLocation();
		stage.setX(Math.max(primScreenBounds.getMinX() + 10, Math.min(mouseLocation.getX(), primScreenBounds.getMaxX() - stage.getWidth() - 10)));
		stage.setY(Math.max(primScreenBounds.getMinY() + 10, Math.min(mouseLocation.getY(), primScreenBounds.getMaxY() - stage.getHeight() - 10)));
	}

	public void removePreferencesWindow(PreferencesWindow pw) {
		String title = pw.getTitle();
		remove(menuMap.remove(title));
	}
}
