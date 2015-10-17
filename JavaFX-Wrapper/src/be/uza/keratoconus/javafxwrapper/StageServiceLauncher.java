package be.uza.keratoconus.javafxwrapper;

import javafx.application.Application;
import javafx.application.Platform;
import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;

/**
 * When this component is created it will tell JavaFX to launch an application
 * by instantiating the {@link StageServiceImpl} class.
 * <p>
 * Note that if this component is ever activated a second time, the JavaFX
 * framework will throw a <tt>java.lang.IllegalStateException: 
 * Application launch must not be called more than once</tt>. Which sucks.
 * 
 * @author chris
 *
 */
@Component(immediate = true)
public class StageServiceLauncher {

	@Activate
	public void activate() {
		new Thread(() -> {
			Thread.currentThread().setContextClassLoader(
					this.getClass().getClassLoader());
			Application.launch(StageServiceImpl.class);
		}).start();
	}

	@Deactivate
	public void deactivate() {
		Platform.exit();
	}

}
