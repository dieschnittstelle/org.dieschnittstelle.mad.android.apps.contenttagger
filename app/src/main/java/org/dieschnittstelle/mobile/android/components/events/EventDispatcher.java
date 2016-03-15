package org.dieschnittstelle.mobile.android.components.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
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
	public void addEventListener(EventMatcher eventMatcher, EventListener callback,Activity controller) {
		doAddEventListener(eventMatcher,new EventListenerWrapper(callback,controller,eventMatcher));
	}

	// allow to pass an activity on whose uithread the listener will be run
	public void addEventListener(EventMatcher eventMatcher, EventListener callback) {
		doAddEventListener(eventMatcher, new EventListenerWrapper(callback, null, eventMatcher));
	}


	public void doAddEventListener(EventMatcher eventMatcher, EventListenerWrapper callback) {

		// check whether the event type contains a "|" symbol that identifies
		// more than a single event
		if (eventMatcher.getType().indexOf("|") != -1) {
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
		private Activity controller;
		// we represent the original matcher here
		private EventMatcher eventMatcher;

		public EventListenerWrapper(EventListener listener,Activity controller,EventMatcher matcher) {
			this.controller = controller;
			this.listener = listener;
			this.eventMatcher = matcher;
		}

		@Override
		public void onEvent(final Event event) {

			// here, we check whether the matcher context is identical with the event context!
			if (!this.eventMatcher.hasContext() || (this.eventMatcher.hasContext() && this.eventMatcher.getContext() == event.getContext())) {
				if (this.controller != null) {
					Log.i(logger, "onEvent(): " + event + ": will run listener on UIThread...");
					this.controller.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							listener.onEvent(event);
						}
					});
				} else {
					listener.onEvent(event);
				}
			}
			else {
				Log.i(logger, "onEvent(): " + event + ": will not run listener with context constraint " + eventMatcher.getContext() + ". It does not match the context of the actual event: " + event.getContext());
			}
		}
	}
}
