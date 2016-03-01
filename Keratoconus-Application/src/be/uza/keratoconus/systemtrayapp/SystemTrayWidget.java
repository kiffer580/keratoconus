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

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.configuration.api.Classification;
import be.uza.keratoconus.configuration.api.Classification.Category;
import be.uza.keratoconus.configuration.api.ClassificationService;
import be.uza.keratoconus.configuration.api.PentacamConfigurationService;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEvent;
import be.uza.keratoconus.datafiles.event.AnalysisResultsEventConstants;
import be.uza.keratoconus.datafiles.event.FileEvent;
import be.uza.keratoconus.datafiles.event.FileEventConstants;
import be.uza.keratoconus.userprefs.api.UserPreferences;

@Component(immediate = true, properties = EventConstants.EVENT_TOPIC + "="
		+ AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC + ","
		+ EventConstants.EVENT_TOPIC + "="
		+ FileEventConstants.DATAFILE_CREATED_TOPIC + ","
		+ EventConstants.EVENT_TOPIC + "="
		+ FileEventConstants.DATAFILE_CHANGED_TOPIC)
public class SystemTrayWidget implements EventHandler {

	private static final int ICON_ANIMATION_HALF_PERIOD_MSEC = 700;
	private final SystemTray systemTray;
	private TrayIcon trayIcon;
	private UserPreferences prefs;

	public SystemTrayWidget() {
		if (!SystemTray.isSupported()) {
			throw new RuntimeException(
					"system tray is not supported on this platform!");
		}
		systemTray = SystemTray.getSystemTray();
	}

	private PentacamConfigurationService pentacamConfigurationService;

	private ClassificationService classificationService;

	private EventAdmin eventAdmin;

	private LogService logService;

	private Map<Classification.Category, Image> iconMap;
	private AtomicReference<Thread> animationThreadRef = new AtomicReference<>();
	private AnalysisResultsEvent currentAnalysisResultsEvent;

	@Reference
	protected void setPentacamConfigurationService(
			PentacamConfigurationService pcs) {
		pentacamConfigurationService = pcs;
	}

	@Reference
	protected void setClassificationService(ClassificationService cs) {
		classificationService = cs;
	}

	@Reference
	protected void setLogService(LogService ls) {
		logService = ls;
	}

	@Reference
	protected void setEventAdmin(EventAdmin ea) {
		eventAdmin = ea;
	}

	@Reference
	protected void setPrefs(UserPreferences p) {
		prefs = p;
		if (trayIcon != null) {
			trayIcon.setPopupMenu((PopupMenu) p);
		}
	}

	@Activate
	protected void activate(ComponentContext cc) throws AWTException {
		iconMap = new HashMap<>();
		final String baseIconPath = prefs.getBaseIconPath();
		fetchIcon(baseIconPath, null);

		for (Classification.Category cat : Classification.Category.values()) {
			String iconPath = prefs.getIconPath(cat.getName());
			fetchIcon(iconPath, cat);
		}

		SwingUtilities.invokeLater(this::addAppToTray);
	}

	private void fetchIcon(String iconPath, Category cat) {
		final File iconFile = new File(iconPath);
		try {
			final BufferedImage unscaledImage = ImageIO.read(iconFile);
			final Dimension size = new TrayIcon(unscaledImage).getSize();
			final Image scaledImage = unscaledImage.getScaledInstance(
					size.width, -1, Image.SCALE_SMOOTH);
			iconMap.put(cat, scaledImage);
		} catch (IOException | IllegalArgumentException e) {
			logService.log(LogService.LOG_ERROR,
					"Exception thrown when fetching icon " + (cat == null ? "base form" : "variant '" + cat + "'"), e);
		}
	}

	@Deactivate
	protected void deactivate() {
		systemTray.remove(trayIcon);
		for (Entry<Category, Image> entry : iconMap.entrySet()) {
			entry.getValue().flush();
		}
		SwingUtilities.invokeLater(() -> {
			stopAnimation();
			iconMap = null;
			trayIcon = null;
		});
	}

	/**
	 * Sets up a system tray icon for the application.
	 */
	private void addAppToTray() {
		try {
			// ensure awt toolkit is initialized.
			java.awt.Toolkit.getDefaultToolkit();

			// app requires system tray support, just exit if there is no
			// support.
			if (!java.awt.SystemTray.isSupported()) {
				System.err
						.println("No system tray support, application exiting.");
				Platform.exit();
			}

			// set up a system tray icon.
			java.awt.SystemTray systemTray = java.awt.SystemTray
					.getSystemTray();
			Image scaledImage = iconMap.get(null);
			trayIcon = new TrayIcon(scaledImage,
					pentacamConfigurationService.getApplicationTitle(),
					(PopupMenu) prefs);
			systemTray.add(trayIcon);

			trayIcon.addActionListener(e -> {
				String command = e.getActionCommand();
				if (command == null && currentAnalysisResultsEvent != null) {
					eventAdmin.postEvent(currentAnalysisResultsEvent);
				}
			});
		} catch (java.awt.AWTException /* | IOException */e) {
			System.err.println("Unable to init system tray");
			e.printStackTrace();
		}
	}

	@Override
	public void handleEvent(Event event) {
		final String topic = event.getTopic();
		if (AnalysisResultsEventConstants.ANALYSIS_RESULTS_TOPIC.equals(topic)) {
			handleAnalysisResultsEvent((AnalysisResultsEvent) event);
		} else if (FileEventConstants.DATAFILE_CREATED_TOPIC.equals(topic)) {
			handleDataFileEvent((FileEvent) event);
		} else if (FileEventConstants.DATAFILE_CHANGED_TOPIC.equals(topic)) {
			handleDataFileEvent((FileEvent) event);
		}
	}

	private void handleAnalysisResultsEvent(AnalysisResultsEvent event) {
		currentAnalysisResultsEvent = new AnalysisResultsEvent(event, true);
		String headlineKey = ((AnalysisResultsEvent) event).getHeadlineKey();
		Category headlineCategory = ((AnalysisResultsEvent) event)
				.getHeadlineCategory();

		trayIcon.setToolTip(pentacamConfigurationService.getApplicationTitle()
				+ "\n" + classificationService.getHeadline(headlineKey));

		if (prefs != null && prefs.isMessagesEnabled() && !event.isRecall()) {
			final MessageType type;
			String headlineText = classificationService
					.getHeadline(headlineKey);
			switch (headlineCategory) {
			case MAIN:
				type = MessageType.ERROR;
				break;
			case SIDE:
			case AMBIGUOUS:
				type = MessageType.WARNING;
				break;
			case NORMAL:
				type = MessageType.INFO;
				break;
			default:
				type = MessageType.NONE;
			}
			SwingUtilities.invokeLater(() -> trayIcon.displayMessage(null,
					headlineText, type));
		}

		if (prefs != null && prefs.isAnimateEnabled() && !event.isRecall()) {
			Thread animationThread = new Thread(
					() -> animateIcon(headlineCategory));
			Thread oldThread = animationThreadRef.getAndSet(animationThread);
			if (oldThread != null) {
				oldThread.interrupt();
			}
			animationThread.start();
		}
	}

	private void handleDataFileEvent(FileEvent event) {
		if (event.getFileName().toUpperCase().startsWith("PENTACAM")) {
			SwingUtilities.invokeLater(this::stopAnimation);
		}
	}

	private void stopAnimation() {
		Thread animationThread = animationThreadRef.getAndSet(null);
		if (animationThread != null) {
			animationThread.interrupt();
		}
		Image plainIcon = iconMap.get(null);
		trayIcon.setImage(plainIcon);
		trayIcon.setToolTip(pentacamConfigurationService.getApplicationTitle());
		currentAnalysisResultsEvent = null;
	}

	private void animateIcon(Category headlineCategory) {
		Image plainIcon = iconMap.get(null);
		Image highlightedIcon = iconMap.get(headlineCategory);
		try {
			trayIcon.setImage(highlightedIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(plainIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(highlightedIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(plainIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(highlightedIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(plainIcon);
			Thread.sleep(ICON_ANIMATION_HALF_PERIOD_MSEC);
			trayIcon.setImage(highlightedIcon);
		} catch (InterruptedException ie) {
		} catch (Exception e) {
			logService.log(LogService.LOG_WARNING,
					"Exception thrown while animating sytem tray icon", e);
		} finally {
			animationThreadRef.compareAndSet(Thread.currentThread(), null);
		}
	}
}
