package org.dieschnittstelle.mobile.android.components.controller;

import org.dieschnittstelle.mobile.android.components.events.EventDispatcher;
import org.dieschnittstelle.mobile.android.components.events.EventListenerOwner;

/**
 * Created by master on 02.03.17.
 *
 * this class implements generic lifecycle methods for owners of event listeners which allow, e.g., to postpone execution of event handlers
 *
 */
public class LifecycleHandling {

   public static void onDestroy(EventListenerOwner owner) {
       EventDispatcher.getInstance().unbindController(owner);
   }

    public static void onResume(EventListenerOwner owner) {
        EventDispatcher.getInstance().setControllerActive(owner);
        EventDispatcher.getInstance().resumePendingEvents(owner);
    }

    public static void onPause(EventListenerOwner owner) {
        EventDispatcher.getInstance().setControllerPaused(owner);
    }

    public static void unbindPendingEvents(EventListenerOwner owner) {
        EventDispatcher.getInstance().cancelPendingEvents(owner);
    }


}
