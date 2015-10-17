package be.uza.keratoconus.logging;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.log.LogService;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import be.uza.keratoconus.datafiles.event.KeratoconusEventConstants;

@Component(properties = org.osgi.service.event.EventConstants.EVENT_TOPIC + "="
		+ KeratoconusEventConstants.EVENT_TOPIC_PREFIX + "*")
public class EventLogger implements EventHandler {

	private LogService logService;
	
	@Reference
	protected void setLogService(LogService s) {
		logService = s;
	}

	@Override
	public void handleEvent(Event event) {
			logService.log(LogService.LOG_INFO, event.toString());
	}
	
	
}
