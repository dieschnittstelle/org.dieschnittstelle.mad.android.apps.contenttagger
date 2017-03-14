package org.dieschnittstelle.mobile.android.components.events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.app.Fragment;
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
	private Map<String, List<EventListenerWrapper>> alllisteners = new HashMap<String, List<EventListenerWrapper>>();

	/*
	 * a list of paused event listener owners
	 */
	private Set<EventListenerOwner> pausedControllers = new HashSet<EventListenerOwner>();

	/*
	 * a map of pending events for listener owners,  we use a package class of listener and event
	 */
	private static class PendingEvent {

		EventListenerWrapper listener;
		Event event;

		public PendingEvent(EventListenerWrapper listener, Event event) {
			this.listener = listener;
			this.event = event;
		}

		public void resume() {
			Log.i(logger,"resuming pending event " + event + ", on: " + listener.getOwner());
			listener.onEvent(event);
		}

		@Override
		public String toString() {
			return "PendingEvent{" +
					"event=" + event.toIdentifier() +
					'}';
		}
	}

	private Map<EventListenerOwner,List<PendingEvent>> pendingEvents = new HashMap<EventListenerOwner,List<PendingEvent>>();


	// allow to pass an activity on whose uithread the listener will be run
	public void addEventListener(EventListenerOwner owner,EventMatcher eventMatcher, boolean runOnUIThread,EventListener callback) {
		addEventListener(owner,eventMatcher,runOnUIThread,callback,false);
	}

	// allow to specify that some event listener shall be executed regardless of whether its owner is paused (this is for handling obsoletion of views)
	public void addEventListener(EventListenerOwner owner,EventMatcher eventMatcher, boolean runOnUIThread,EventListener callback, boolean executeOnPause) {
		doAddEventListener(eventMatcher,new EventListenerWrapper(owner,eventMatcher,runOnUIThread,executeOnPause,callback));
	}


	private void doAddEventListener(EventMatcher eventMatcher, EventListenerWrapper callback) {

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
			List<EventListenerWrapper> currentlisteners = this.alllisteners.get(eventMatcher.toIdentifier());
			if (currentlisteners != null) {
				Log.i(logger, "adding listener to existing listeners.");
				currentlisteners.add(callback);
			} else {
				Log.i(logger, "creating new event listener list.");
				alllisteners.put(eventMatcher.toIdentifier(),
						new ArrayList<EventListenerWrapper>(Arrays.asList(new EventListenerWrapper[] { callback })));
			}
		}
	}

	public void notifyListeners(Event event) {
		List<EventListenerWrapper> currentlisteners = alllisteners.get(event.toIdentifier());
		if (currentlisteners != null) {
			Log.i(logger, "will notify " + currentlisteners.size() + " listeners of event: " + event.toIdentifier());
			Log.d(logger, "all listeners are: " + alllisteners);
			for (EventListenerWrapper listener : currentlisteners) {
				// in case the listener is paused, we will add the event as pending event for it
				if (isPaused(listener.getOwner()) && !listener.isExecuteOnPause()) {
					addPendingEvent(listener.getOwner(),listener, event);
				}
				else {
					if (isPaused(listener.getOwner())) {
						Log.i(logger,"run listener on event " + event + ", for paused owner: " + listener.getOwner());
					}
					listener.onEvent(event);
				}
			}
		}
	}

	public void resumePendingEvents(EventListenerOwner owner) {
		List<PendingEvent> events = this.pendingEvents.get(owner);
		if (events == null || events.size() == 0) {
			Log.i(logger,"resumePendingEvents(): no pending events found for owner: " + owner);
			return;
		}

		Log.i(logger,"resumePendingEvents(): will resume " + events.size() + " pending events for owner: " + owner + ", events are: " + events);
		this.pendingEvents.remove(owner);
		for (PendingEvent event : events) {
			event.resume();
		}
	}

	public void setControllerPaused(EventListenerOwner owner) {
		Log.i(logger,"setControllerPaused(): " + owner);
		this.pausedControllers.add(owner);
		Log.i(logger,"setControllerPaused(): paused controllers are now: " + this.pausedControllers);
	}

	public void setControllerActive(EventListenerOwner owner) {
		Log.i(logger,"setControllerActive(): " + owner);
		this.pausedControllers.remove(owner);
		Log.i(logger,"setControllerActive(): paused controllers are now: " + this.pausedControllers);
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
		private boolean executeOnPause;

		public EventListenerOwner getOwner() {
			return this.owner;
		}

		public EventListenerWrapper(EventListenerOwner owner,EventMatcher eventMatcher, boolean runOnUIThread,boolean executeOnPause, EventListener callback) {
			this.owner = owner;
			this.listener = callback;
			this.eventMatcher = eventMatcher;
			this.runOnUIThread = runOnUIThread;
			this.executeOnPause = executeOnPause;
		}

		public boolean isExecuteOnPause() {
			return executeOnPause;
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
						Log.i(logger, "onEvent(): " + event + ": got activity as uiThreadRunner: " + uiThreadRunner);
					}

					// there is a problem if an event is handled by a fragment that is not visible currently. In this case, the activity will be null, and runOnUIThread will not be available...

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

	public boolean isPaused(EventListenerOwner owner) {
		return this.pausedControllers.contains(owner);
	}

	/*
	 * TODO: pending event handling could be made sensitive to the event type, e.g. if for some entity a delete event is added then any other events related to this entity could be removed...
	 */
	private void addPendingEvent(EventListenerOwner owner, EventListenerWrapper listener, Event event) {
		Log.i(logger,"addPendingEvent(): " + event + ", for: " + listener.getOwner());
		List<PendingEvent> pendingListeners = this.pendingEvents.get(owner);
		if (pendingListeners == null) {
			pendingListeners = new ArrayList<PendingEvent>();
			this.pendingEvents.put(owner,pendingListeners);
		}
		pendingListeners.add(new PendingEvent(listener,event));
	}

	/*
	 * remove all listeners for some given owner
	 */
	public void unbindController(EventListenerOwner owner) {
		Log.d(logger,"unbindController(): " + owner);

		for (String event : this.alllisteners.keySet()) {
			List<EventListenerWrapper> currentListeners = this.alllisteners.get(event);
			for (int i=currentListeners.size()-1;i>=0;i--) {
				if (currentListeners.get(i).getOwner() == owner) {
					Log.d(logger,"removing listener for " + event + " and owner: " + owner);
					currentListeners.remove(i);
				}
			}
		}
		// we also need to remove the owner from the paused controllers (in case it is contained there)
		this.pausedControllers.remove(owner);
		// ... and from the pending events
		this.cancelPendingEvents(owner);
	}

	/*
	 * only unbind the pending events
	 */
	public void cancelPendingEvents(EventListenerOwner owner) {
		this.pendingEvents.remove(owner);
	}

}
