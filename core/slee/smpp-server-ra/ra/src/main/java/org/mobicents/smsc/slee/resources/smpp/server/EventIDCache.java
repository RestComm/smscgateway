/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.slee.resources.smpp.server;

import java.util.concurrent.ConcurrentHashMap;

import javax.slee.EventTypeID;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.ResourceAdaptorContext;

/**
 * 
 * @author amit bhayani
 * 
 */
public class EventIDCache {

	private ConcurrentHashMap<String, FireableEventType> eventIds = new ConcurrentHashMap<String, FireableEventType>();
	private static final String EVENT_VENDOR = "org.mobicents";
	private static final String EVENT_VERSION = "1.0";

	private Tracer tracer;

	protected EventIDCache(ResourceAdaptorContext raContext) {
		tracer = raContext.getTracer(EventIDCache.class.getSimpleName());
	}

	protected FireableEventType getEventId(EventLookupFacility eventLookupFacility, String eventName) {

		FireableEventType eventType = eventIds.get(eventName);
		if (eventType == null) {
			try {
				eventType = eventLookupFacility.getFireableEventType(new EventTypeID(eventName, EVENT_VENDOR,
						EVENT_VERSION));
			} catch (Throwable e) {
				this.tracer.severe("Error while looking up for SMPP Server Events", e);
			}
			if (eventType != null) {
				eventIds.put(eventName, eventType);
			}
		}
		return eventType;
	}
}
