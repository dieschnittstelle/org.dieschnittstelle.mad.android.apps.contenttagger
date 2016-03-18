package org.dieschnittstelle.mobile.android.components.events;

import android.content.Context;

import org.dieschnittstelle.mobile.android.components.model.Entity;

/**
 * Created by master on 15.03.16.
 *
 * originally, there was only the event class, but EventMatcher makes more clear what is meant
 */
public class EventMatcher<T> extends Event<T> {

    public EventMatcher(String group, String type, String target) {
        super(group,type,target);
    }

    public EventMatcher(String group, String type, Class targetClass) {
        super(group,type,targetClass.getName());
    }

    public EventMatcher(String group, String type, String target,EventGenerator context) {
        super(group,type,target,context);
    }

    public EventMatcher(String group, String type, Class targetClass,EventGenerator context) {
        super(group,type,targetClass.getName(),context);
    }


}
