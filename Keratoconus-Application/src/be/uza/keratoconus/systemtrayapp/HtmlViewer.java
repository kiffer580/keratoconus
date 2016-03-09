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

package be.uza.keratoconus.systemtrayapp;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.model.api.AvailableModelsService;
import be.uza.keratoconus.systemtrayapp.api.HtmlViewerService;

@Component(properties = EventConstants.EVENT_TOPIC
		+ "=" + HtmlViewerService.SHOWPAGE_TOPIC)
public class HtmlViewer implements HtmlViewerService, EventHandler {

	private Stage theStage;
	private WebView webView;
	private AvailableModelsService availableModelsService;
	private LogService logService;
	private VBox viewPane;
	
	@Reference
	protected void setAvailableModelsService(AvailableModelsService ams) {
		availableModelsService = ams;
	}

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Override
	public void handleEvent(Event event) {
		Platform.runLater(() -> showPage((String) event.getProperty(HtmlViewerService.SHOWPAGE_PATH),
				(String) event.getProperty(HtmlViewerService.SHOWPAGE_TITLE)));
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
		
		viewPane = new VBox(webView);
		Scene scene = new Scene(viewPane);
		webEngine.getLoadWorker().stateProperty().addListener(
				new HtmlViewerStateListener(webEngine, logService));
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
			WebEngine webEngine = webView.getEngine();
			String heightString = webEngine
					.executeScript(
							"window.getComputedStyle(document.getElementById('shrinkwrap'), null).getPropertyValue('height')")
					.toString();
			double height = Double.valueOf(heightString.replace(
					"px", ""));

			String widthString = webEngine
					.executeScript(
							"window.getComputedStyle(document.getElementById('shrinkwrap'), null).getPropertyValue('width')")
					.toString();
			double width = Double.valueOf(widthString.replace(
					"px", ""));

			Platform.runLater(() -> {
				webView.setPrefHeight(height + 10);
				webView.setPrefWidth(width);
				String selectedModelName = availableModelsService.getSelectedModelName();
				webEngine.executeScript("document.getElementById('modelname').textContent='" + selectedModelName + "'");
				webEngine.executeScript("document.getElementById('modeldescription').textContent='" + availableModelsService.getModelDescription(selectedModelName) + "'");
				theStage.sizeToScene();
			});
		} catch (Exception e) {
			// Don't worry too much if it doesn't work - if e.g. there is no 'shrinkwrap'
			// element then we just let the window have the default size (800x600).
			logService.log(LogService.LOG_INFO, "failed to shrink window to size of 'shrinkwrap' div", e);
		}
	}
}
