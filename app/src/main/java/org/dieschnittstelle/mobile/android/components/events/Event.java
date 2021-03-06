package org.dieschnittstelle.mobile.android.components.events;

import android.content.Context;

import org.dieschnittstelle.mobile.android.components.model.Entity;

public class Event<T> {

	public static String TYPE_SEPARATOR = "|";

	// we declare some constants for event types
	public static final class CRUD {
		public static final String TYPE = "crud";
		public static final String CREATED = "created";
		public static final String READ = "read";
		public static final String READALL = "readall";
		public static final String UPDATED = "updated";
		public static final String DELETED = "deleted";
	}

	public static final class UI {
		public static final String TYPE = "ui";
	}

	public static String OR(String...  types) {
		StringBuffer buf = new StringBuffer();
		for (int i=0;i<types.length;i++) {
			buf.append(types[i]);
			if (i < (types.length-1)) {
				buf.append(TYPE_SEPARATOR);
			}
		}

		return buf.toString();
	}

	private String group;
	private String type;
	private String target;
	private T data;
	// additionally we specify a sender context that can serve as a further constraint
	private EventGenerator context;

	public Event(String group, String type, String target) {
		this.group = group;
		this.type = type;
		this.target = target;
	}

	public Event(String group, String type, String target, T data) {
		this.group = group;
		this.type = type;
		this.target = target;
		this.data = data;
	}

	public Event(String group, String type, Class targetClass, T data) {
		this.group = group;
		this.type = type;
		this.target = targetClass.getName();
		this.data = data;
	}

	public Event(String group, String type, String target,EventGenerator context) {
		this.group = group;
		this.type = type;
		this.target = target;
		this.context = context;
	}

	public Event(String group, String type, String target,EventGenerator context, T data) {
		this.group = group;
		this.type = type;
		this.target = target;
		this.data = data;
		this.context = context;
	}

	public Event(String group, String type, Class targetClass,EventGenerator context, T data) {
		this.group = group;
		this.type = type;
		this.target = targetClass.getName();
		this.data = data;
		this.context = context;
	}

	public String getGroup() {
		return group;
	}

	public String getType() {
		return type;
	}

	public String getTarget() {
		return target;
	}

	public T getData() {
		return data;
	}


	public boolean hasContext() {
		return this.context != null;
	}

	public EventGenerator getContext() {
		return this.context;
	}

	/*
	 * events will be characterised by the three string valued properties group (ui, crud, etc.), type, and target
	 */
	public String toIdentifier() {
		return (this.group != null ? this.group : "") + "_" + (this.type != null ? this.type : "") + "_"
				+ (this.target != null ? this.target : "");
	}
}
