package org.dieschnittstelle.mobile.android.components.events;

public interface EventListener<T> {

	public void onEvent(Event<T> event);
	
}
