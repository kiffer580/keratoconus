package be.uza.keratoconus.systemtrayapp.api;

import java.util.HashMap;

import org.osgi.service.event.Event;

public class ShowPageEvent extends Event {
	@SuppressWarnings("serial")
	public ShowPageEvent(String path, String title) {
		super(HtmlViewerService.SHOWPAGE_TOPIC, new HashMap<String,Object>() {
			{
				put(HtmlViewerService.SHOWPAGE_PATH, path);
				put(HtmlViewerService.SHOWPAGE_TITLE, title);
			}
		});
	}
}