package org.dieschnittstelle.mobile.android.components.controller;

import android.app.Fragment;

import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by master on 02.03.17.
 *
 * this class implements generic lifecycle methods for owners of event listeners which allow, e.g., to postpone execution of event handlers
 *
 */
public class LifecycleHandling {

    // we manage a static set of obsolete views
    private static Set<Fragment> obsoleteViews = new HashSet<Fragment>();

    public static void onDestroy(EventListenerOwner owner) {
       // also remove a view from the obsolete views on destroy
       obsoleteViews.remove(owner);
       EventDispatcher.getInstance().unbindController(owner);
   }

    /**
     * here the return value indicates whether the view had been paused or not (i.e. whether it is being created the first time)
     *
     * @param owner
     * @return
     */
    public static boolean onResume(EventListenerOwner owner) {
        boolean paused = EventDispatcher.getInstance().isPaused(owner);

        if (obsoleteViews.contains(owner)) {
            // we anticipate ondestroy for obsolete views
            onDestroy(owner);
            ((Fragment)owner).getFragmentManager().popBackStack();
        }
        else {
            EventDispatcher.getInstance().setControllerActive(owner);
            EventDispatcher.getInstance().resumePendingEvents(owner);
        }

        return !paused;
    }

    public static void onPause(EventListenerOwner owner) {
        EventDispatcher.getInstance().setControllerPaused(owner);
    }

    public static void cancelPendingEvents(EventListenerOwner owner) {
        EventDispatcher.getInstance().cancelPendingEvents(owner);
    }

    public static void markViewAsObsolete(Fragment view) {
        obsoleteViews.add(view);
    }

}
