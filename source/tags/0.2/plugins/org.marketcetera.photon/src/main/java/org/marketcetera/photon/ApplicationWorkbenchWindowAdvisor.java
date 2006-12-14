package org.marketcetera.photon;

import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;
import org.eclipse.core.internal.jobs.JobStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.progress.IProgressService;
import org.marketcetera.core.ClassVersion;
import org.marketcetera.core.HttpDatabaseIDFactory;
import org.marketcetera.core.NoMoreIDsException;
import org.marketcetera.photon.actions.ReconnectJMSAction;
import org.marketcetera.photon.ui.CommandStatusLineContribution;
import org.marketcetera.photon.ui.MainConsole;
import org.marketcetera.photon.views.WebBrowserView;

/**
 * Required by the RCP platform this class is responsible for setting up the
 * workbench upon startup.
 * @author gmiller
 *
 */
@ClassVersion("$Id$")
public class ApplicationWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

    @Override
	public boolean preWindowShellClose() {
    	try {
    		stopJMS();
    	} catch (Throwable t){}
    	try {
    		stopQuoteFeed();
    	} catch (Throwable t){}
    	return true;
    }

	/**
     * Simply calls superclass constructor.
     * @param configurer the configurer to pass to the superclass
     */
    public ApplicationWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#createActionBarAdvisor(org.eclipse.ui.application.IActionBarConfigurer)
     */
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
        return new ApplicationActionBarAdvisor(configurer);
    }
    
    /**
     * Sets a number of options on the IWorkbenchWindowConfigurer prior
     * to opening the window.
     * 
     * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#preWindowOpen()
     */
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(800, 600));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(true);
        configurer.setShowMenuBar(true);
//        IProduct product = Platform.getProduct();
//        String productName = product == null ? "" : product.getName()
//        configurer.setTitle(productName);
        configurer.setShowPerspectiveBar(true);
        configurer.setShowProgressIndicator(true);
    }
    
	/*
	 * Called after the window has opened, and all UI elements have been initialized,
	 * this method takes care of wiring UI components to
	 * the underlying model and controller elements.  For example it connects the
	 * Console view to a logger appender to feed it data.
	 * 
	 * 
	 * @see org.eclipse.ui.application.WorkbenchWindowAdvisor#postWindowOpen()
	 */
	@Override
	public void postWindowOpen() {

		initStatusLine();

		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager()
				.getConsoles();
		Logger mainConsoleLogger = PhotonPlugin.getMainConsoleLogger();
		for (IConsole console : consoles) {
			if (console instanceof MainConsole) {
				MainConsole mainConsole = (MainConsole) console;
				PhotonConsoleAppender appender = new PhotonConsoleAppender(
						mainConsole);
				appender.setLayout(new SimpleLayout());
				mainConsoleLogger.addAppender(appender);
			}
		} 
				

		IStatusLineManager statusLineManager = getWindowConfigurer().getActionBarConfigurer().getStatusLineManager();
		IContributionItem item = statusLineManager.find(CommandStatusLineContribution.ID);

		
		if (item instanceof CommandStatusLineContribution) {
			CommandStatusLineContribution cslc = (CommandStatusLineContribution) item;
			cslc.setIDFactory(PhotonPlugin.getDefault().getIDFactory());
		}

		mainConsoleLogger.info(
				"Application initialized: " + new Date());
		
		startJMS();
		startQuoteFeed();
		startIDFactory();

	}

	/** 
	 * Initializes the status line.
	 * 
	 */
	private void initStatusLine() {
		IStatusLineManager statusline = getWindowConfigurer()
				.getActionBarConfigurer().getStatusLineManager();
		statusline.setMessage("Online");
	}

	private void startIDFactory(){
		try {
			((HttpDatabaseIDFactory)PhotonPlugin.getDefault().getIDFactory()).grabIDs();
		} catch (NoMoreIDsException e) {
			PhotonPlugin.getMainConsoleLogger().warn("Error connecting to web app for ID base, reverting to built in IDFactory.");
		}
	}


	private void startJMS() {
		IProgressService service = PlatformUI.getWorkbench().getProgressService();
		try {
			ReconnectJMSAction.startJMS(service, PhotonPlugin.getDefault());
		} catch (Throwable t){
			PhotonPlugin.getMainConsoleLogger().error("Could not connect to message queue", t);
		}
	}


	private void stopJMS() {
		ReconnectJMSAction.stopJMS(PhotonPlugin.getDefault());
	}
	

	private void startQuoteFeed() {
    	PhotonPlugin.getDefault().getQuoteFeed().start();
	}
	
	private void stopQuoteFeed() {
    	PhotonPlugin.getDefault().getQuoteFeed().stop();
	}

}