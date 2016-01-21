package org.mobicents.smsc.slee.resources.scheduler;

import javax.slee.resource.ActivityHandle;

public class SchedulerActivityHandle implements ActivityHandle {

	private final String id;

	private final SchedulerActivity schedulerActivity;

	public SchedulerActivityHandle(SchedulerActivity schedulerActivity, String id) {
		super();
		this.id = id;
		this.schedulerActivity = schedulerActivity;
	}

	public SchedulerActivity getActivity() {
		return this.schedulerActivity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SchedulerActivityHandle other = (SchedulerActivityHandle) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
