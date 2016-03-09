package be.uza.keratoconus.systemtrayapp;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import netscape.javascript.JSObject;

import org.osgi.service.log.LogService;

public class HtmlViewerStateListener implements
		ChangeListener<Worker.State> {
	private WebEngine webEngine;
	private LogService logService;

	HtmlViewerStateListener(WebEngine webEngine, LogService logService) {
		this.webEngine = webEngine;
		this.logService = logService;
	}
	
	public void changed(ObservableValue<? extends Worker.State> p, Worker.State oldState, Worker.State newState) {
		if (newState == Worker.State.SUCCEEDED) {
			JSObject jsObject = (JSObject) webEngine.executeScript("window");
			jsObject.setMember("java", this);
		}
	}

	public void showInDefaultBrowser(String uriString) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop.getDesktop().browse(new URI(uriString));
			} else {
				Runtime.getRuntime().exec(
						"cmd /k start " + uriString);
			}
		} catch (URISyntaxException | IOException e) {
			logService.log(LogService.LOG_INFO, "failed to show URI '" + uriString + "' in default browser", e);
		}
	}
}