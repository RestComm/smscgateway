/**
 * 
 */
package org.mobicents.smsc.slee.resources.scheduler;

import com.eaio.uuid.UUID;

/**
 * @author Amit Bhayani
 * 
 */
public class SchedulerActivityImpl implements SchedulerActivity {
	private final SchedulerActivityHandle handle;

	private final String id;
	private final SchedulerResourceAdaptor ra;

	public SchedulerActivityImpl(SchedulerResourceAdaptor ra) {
		this.id = new UUID().toString();
		this.ra = ra;
		this.handle = new SchedulerActivityHandle(this, this.id);
	}

	public void endActivity() throws Exception {
		this.ra.endAcitivity(this.handle);
	}

	protected SchedulerActivityHandle getActivityHandle() {
		return this.handle;
	}

	@Override
	public int hashCode() {
		final int prime = 37;
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
		SchedulerActivityImpl other = (SchedulerActivityImpl) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}
