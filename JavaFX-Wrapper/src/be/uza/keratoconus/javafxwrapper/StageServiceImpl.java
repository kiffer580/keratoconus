package be.uza.keratoconus.javafxwrapper;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceRegistration;

import be.uza.keratoconus.javafxwrapper.api.StageService;

/**
 * This class is instantiated by the JavaFX framework when the
 * {@link StageServiceLauncher} component calls
 * {@link Application#launch(Class, String...)}. JavaFX will then call our
 * {@link #start(Stage)} method, and at this point we register a
 * {@link StageService} with the OSGi framework. Conversely, when JavaFX calls
 * our {@link #stop()} method we unregister the {@link StageService}.
 * 
 * @author chris
 *
 */
public class StageServiceImpl extends Application implements StageService {

	private Stage theStage;
	private volatile BundleContext bundleContext;
	private ServiceRegistration<StageService> registration;

	@Override
	public Stage getStage() {
		return theStage;
	}

	@Override
	public void start(Stage s) throws Exception {
		bundleContext = ((BundleReference) StageServiceImpl.class
				.getClassLoader()).getBundle().getBundleContext();
		theStage = s;
		Platform.setImplicitExit(false);
		registration = bundleContext.registerService(StageService.class, this,
				null);
	}

	@Override
	public void stop() throws Exception {
		registration.unregister();
	}
}
