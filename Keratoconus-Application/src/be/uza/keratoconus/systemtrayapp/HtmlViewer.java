package be.uza.keratoconus.systemtrayapp;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.w3c.dom.Document;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.AvailableModelsService;
import be.uza.keratoconus.model.api.ModelService;
import be.uza.keratoconus.systemtrayapp.api.HtmlViewerService;
import be.uza.keratoconus.userprefs.api.UserPreferences;

@Component
public class HtmlViewer implements HtmlViewerService {

	private Stage theStage;
	private WebView webView;
	private AvailableModelsService availableModelsService;
	
	@Reference
	protected void setAvailableModelsService(AvailableModelsService ams) {
		availableModelsService = ams;
	}

	@Override
	public void showPage(String path, String title) {
		theStage = new Stage();
		theStage.initStyle(StageStyle.UTILITY);
		webView = new WebView();
		WebEngine webEngine = webView.getEngine();
		webEngine.load("file:///"
				+ System.getProperty("user.dir").replace('\\', '/') + path);
		webEngine.documentProperty().addListener(this::changed);
		Scene scene = new Scene(webView);
		theStage.setTitle(title);
		theStage.setScene(scene);
		theStage.show();
	}

	/**
	 * Once the document has been loaded, if it contains an element (e.g. a div) with the id
	 * 'shrinkwrap' then resize the webview to neatly enclose this element.
	 * @param prop
	 * @param oldDoc
	 * @param newDoc
	 */
	private void changed(
			ObservableValue<? extends Document> prop,
			Document oldDoc, Document newDoc) {
		try {
			String heightString = webView
					.getEngine()
					.executeScript(
							"window.getComputedStyle(document.getElementById('shrinkwrap'), null).getPropertyValue('height')")
					.toString();
			double height = Double.valueOf(heightString.replace(
					"px", ""));

			String widthString = webView
					.getEngine()
					.executeScript(
							"window.getComputedStyle(document.getElementById('shrinkwrap'), null).getPropertyValue('width')")
					.toString();
			double width = Double.valueOf(widthString.replace(
					"px", ""));

			Platform.runLater(() -> {
				webView.setPrefHeight(height + 10);
				webView.setPrefWidth(width);
				String selectedModelName = availableModelsService.getSelectedModelName();
				webView.getEngine().executeScript("document.getElementById('modelname').textContent='" + selectedModelName + "'");
				webView.getEngine().executeScript("document.getElementById('modeldescription').textContent='" + availableModelsService.getModeDescription(selectedModelName) + "'");
				theStage.sizeToScene();
			});
		} catch (Exception e) {
			// Don't worry too much if it doesn't work - if e.g. there is no 'shrinkwrap'
			// element then we just let the window have the default size (800x600).
		}
	}
}
