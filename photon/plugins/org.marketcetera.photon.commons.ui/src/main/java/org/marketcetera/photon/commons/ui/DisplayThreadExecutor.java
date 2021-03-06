package org.marketcetera.photon.commons.ui;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import javax.annotation.concurrent.ThreadSafe;

import org.eclipse.swt.widgets.Display;
import org.marketcetera.photon.commons.GuiExecutor;
import org.marketcetera.photon.commons.SimpleExecutorService;
import org.marketcetera.photon.commons.Validate;
import org.marketcetera.util.misc.ClassVersion;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/* $License$ */

/**
 * An executor service that executes tasks asynchronously on a SWT display
 * thread. It does not support {@link #shutdownNow()}, which will simply call
 * {@link #shutdown()} and return null.
 * <p>
 * As long as the display is not disposed, tasks successfully submitted to this
 * executor service will execute even if a shutdown is requested in the
 * meantime. When all have been completed, {@link #isTerminated()} will return
 * true.
 * <p>
 * If the display is disposed, a clean shutdown will no longer be possible.
 * Tasks that were submitted, but not completed may be lost, and
 * {@link #isTerminated()} and
 * {@link #awaitTermination(long, java.util.concurrent.TimeUnit)} will always
 * return false. Tasks submitted after the display is disposed will be
 * immediately rejected.
 * 
 * @author <a href="mailto:will@marketcetera.com">Will Horn</a>
 * @version $Id$
 * @since 2.0.0
 */
@ClassVersion("$Id$")
@ThreadSafe
public final class DisplayThreadExecutor extends SimpleExecutorService
        implements GuiExecutor {

    private static final LoadingCache<Display,DisplayThreadExecutor> sMap = CacheBuilder.newBuilder().build(new CacheLoader<Display,DisplayThreadExecutor>() {
        @Override
        public DisplayThreadExecutor load(Display from)
                throws Exception
        {
            return new DisplayThreadExecutor(from);
        }});
    /**
     * Returns the singleton executor for the given SWT display.
     * 
     * @param display
     *            the SWT display
     * @return the executor service
     * @throws IllegalArgumentException
     *             if display is null
     */
    public static ExecutorService getInstance(final Display display) {
        Validate.notNull(display, "display"); //$NON-NLS-1$
        try {
            return sMap.get(display);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private final Display mDisplay;

    private DisplayThreadExecutor(Display display) {
        mDisplay = display;
    }

    @Override
    public void doExecute(Runnable command) {
        if (mDisplay.equals(Display.getCurrent())) {
            command.run();
        } else {
            mDisplay.asyncExec(command);
        }
    }
}
