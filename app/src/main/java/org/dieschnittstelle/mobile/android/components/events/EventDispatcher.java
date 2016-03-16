package org.dieschnittstelle.mobile.android.components.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.util.Log;

public class EventDispatcher {

	private static String logger = "EventDispatcher";

	/*
	 * we realise this class as a classical singleton
	 */
	private static EventDispatcher instance = new EventDispatcher();

	public static EventDispatcher getInstance() {
		return instance;
	}

	private EventDispatcher() {

	}

	/*
	 * the map of event listeners
	 */
	private Map<String, List<EventListener>> alllisteners = new HashMap<String, List<EventListener>>();

	// allow to pass an activity on whose uithread the listener will be run
	public void addEventListener(EventListenerOwner owner,EventMatcher eventMatcher, boolean runOnUIThread,EventListener callback) {
		doAddEventListener(eventMatcher,new EventListenerWrapper(owner,eventMatcher,runOnUIThread,callback));
	}

	public void doAddEventListener(EventMatcher eventMatcher, EventListenerWrapper callback) {

		// check whether the event type contains a "|" symbol that identifies
		// more than a single event
		if (eventMatcher.getType().indexOf(Event.TYPE_SEPARATOR) != -1) {
			Log.i(logger, "event specifies a disjunction of types: " + eventMatcher.getType()
					+ ". Will add a listener for each concrete type");
			String[] etypes = eventMatcher.getType().split("\\|");
			Log.i(logger, "split: "+ Arrays.asList(etypes));
			for (int i = 0; i < etypes.length; i++) {
				this.doAddEventListener(new EventMatcher(eventMatcher.getGroup(), etypes[i], eventMatcher.getTarget()), callback);
			}
		} else {
			Log.i(logger, "adding new event listener for event " + eventMatcher.toIdentifier());
			List<EventListener> currentlisteners = this.alllisteners.get(eventMatcher.toIdentifier());
			if (currentlisteners != null) {
				Log.i(logger, "adding listener to existing listeners.");
				currentlisteners.add(callback);
			} else {
				Log.i(logger, "creating new event listener list.");
				alllisteners.put(eventMatcher.toIdentifier(),
						new ArrayList<EventListener>(Arrays.asList(new EventListener[] { callback })));
			}
		}
	}

	public void notifyListeners(Event event) {
		List<EventListener> currentlisteners = alllisteners.get(event.toIdentifier());
		if (currentlisteners != null) {
			Log.i(logger, "will notify " + currentlisteners.size() + " listeners of event: " + event.toIdentifier());
			for (EventListener listener : currentlisteners) {
				listener.onEvent(event);
			}
		}
	}

	/*
	 * this one wraps around an eventlistener that will be run on the ui thread given an executing activity
	 */
	public static class EventListenerWrapper implements EventListener {

		private EventListener listener;
		private EventListenerOwner owner;
		// we represent the original matcher here
		private EventMatcher eventMatcher;
		private boolean runOnUIThread;

		public EventListenerWrapper(EventListenerOwner owner,EventMatcher eventMatcher, boolean runOnUIThread,EventListener callback) {
			this.owner = owner;
			this.listener = callback;
			this.eventMatcher = eventMatcher;
			this.runOnUIThread = runOnUIThread;
		}

		@Override
		public void onEvent(final Event event) {

			// here, we check whether the matcher context is identical with the event context!
			if (!this.eventMatcher.hasContext() || (this.eventMatcher.hasContext() && this.eventMatcher.getContext() == event.getContext())) {
				if (this.runOnUIThread) {
					Log.i(logger, "onEvent(): " + event + ": will run listener on UIThread...");
					// check whether the owner is either a fragment or an activity
					Activity uiThreadRunner = null;
					if (this.owner instanceof Activity) {
						uiThreadRunner = (Activity)this.owner;
					}
					else if (this.owner instanceof Fragment) {
						uiThreadRunner = ((Fragment)this.owner).getActivity();
					}

					// check whether we have found a runner
					if (uiThreadRunner != null) {
						Log.i(logger, "onEvent(): " + event + ": will run listener on UIThread...");
						uiThreadRunner.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								listener.onEvent(event);
							}
						});
					}
					else {
						Log.e(logger,"onEvent(): owner cannot provide run on uithread: " + owner + ". Will run on current thread instead.");
						listener.onEvent(event);
					}
				} else {
					Log.i(logger, "onEvent(): " + event + ": will run listener on current thread...");
					listener.onEvent(event);
				}
			}
			else {
				Log.i(logger, "onEvent(): " + event + ": will not run listener with context constraint " + eventMatcher.getContext() + ". It does not match the context of the actual event: " + event.getContext());
			}
		}
	}

//	public void unbindController()
}
