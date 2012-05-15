package org.mobicents.smsc.slee.resources.smpp.server;

import javax.naming.InitialContext;
import javax.slee.Address;
import javax.slee.AddressPlan;
import javax.slee.SLEEException;
import javax.slee.facilities.EventLookupFacility;
import javax.slee.facilities.Tracer;
import javax.slee.resource.ActivityAlreadyExistsException;
import javax.slee.resource.ActivityFlags;
import javax.slee.resource.ActivityHandle;
import javax.slee.resource.ActivityIsEndingException;
import javax.slee.resource.ConfigProperties;
import javax.slee.resource.FailureReason;
import javax.slee.resource.FireEventException;
import javax.slee.resource.FireableEventType;
import javax.slee.resource.IllegalEventException;
import javax.slee.resource.InvalidConfigurationException;
import javax.slee.resource.Marshaler;
import javax.slee.resource.ReceivableService;
import javax.slee.resource.ResourceAdaptor;
import javax.slee.resource.ResourceAdaptorContext;
import javax.slee.resource.SleeEndpoint;
import javax.slee.resource.StartActivityException;
import javax.slee.resource.UnrecognizedActivityHandleException;

import org.mobicents.smsc.smpp.DefaultSmppServerHandler;

public class SmppServerResourceAdaptor implements ResourceAdaptor {

	private static final String CONF_JNDI = "jndiName";

	private transient Tracer tracer;
	private transient ResourceAdaptorContext raContext;
	private transient SleeEndpoint sleeEndpoint = null;
	private transient EventLookupFacility eventLookup = null;
	private EventIDCache eventIdCache = null;
	private SmppServerSessionsImpl smppServerSession = null;

	private DefaultSmppServerHandler defaultSmppServerHandler = null;

	private transient static final Address address = new Address(AddressPlan.IP, "localhost");

	private String jndiName = null;

	public SmppServerResourceAdaptor() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void activityEnded(ActivityHandle activityHandle) {
		if (this.tracer.isFineEnabled()) {
			this.tracer.fine("Activity with handle " + activityHandle + " ended");
		}
		SmppServerTransactionHandle serverTxHandle = (SmppServerTransactionHandle) activityHandle;
		final SmppServerTransactionImpl serverTx = serverTxHandle.getActivity();
		serverTxHandle.setActivity(null);

		if (serverTx != null) {
			serverTx.clear();
		}
	}

	@Override
	public void activityUnreferenced(ActivityHandle arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void administrativeRemove(ActivityHandle arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void eventProcessingFailed(ActivityHandle arg0, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5, FailureReason arg6) {
		// TODO Auto-generated method stub

	}

	@Override
	public void eventProcessingSuccessful(ActivityHandle arg0, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5) {
		// TODO Auto-generated method stub

	}

	@Override
	public void eventUnreferenced(ActivityHandle arg0, FireableEventType arg1, Object arg2, Address arg3,
			ReceivableService arg4, int arg5) {
		// TODO Auto-generated method stub

	}

	@Override
	public Object getActivity(ActivityHandle activityHandle) {
		SmppServerTransactionHandle serverTxHandle = (SmppServerTransactionHandle) activityHandle;
		return serverTxHandle.getActivity();
	}

	@Override
	public ActivityHandle getActivityHandle(Object activity) {
		if (activity instanceof SmppServerTransactionImpl) {
			final SmppServerTransactionImpl wrapper = ((SmppServerTransactionImpl) activity);
			if (wrapper.getRa() == this) {
				return wrapper.getActivityHandle();
			}
		}

		return null;
	}

	@Override
	public Marshaler getMarshaler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getResourceAdaptorInterface(String arg0) {
		return this.smppServerSession;
	}

	@Override
	public void queryLiveness(ActivityHandle activityHandle) {
		final SmppServerTransactionHandle handle = (SmppServerTransactionHandle) activityHandle;
		final SmppServerTransactionImpl smppServerTxActivity = handle.getActivity();
		if (smppServerTxActivity == null || smppServerTxActivity.getWrappedPduRequest() == null) {
			sleeEndpoint.endActivity(handle);
		}
		
	}

	@Override
	public void raActive() {
		try {
			InitialContext ic = new InitialContext();
			this.defaultSmppServerHandler = (DefaultSmppServerHandler) ic.lookup(this.jndiName);
			this.defaultSmppServerHandler.setSmppSessionHandlerInterface(this.smppServerSession
					.getSmppSessionHandlerInterface());

			if (tracer.isInfoEnabled()) {
				tracer.info("Activated RA Entity " + this.raContext.getEntityName());
			}
		} catch (Exception e) {
			this.tracer.severe("Failed to activate SMPP Server RA ", e);
		}
	}

	@Override
	public void raConfigurationUpdate(ConfigProperties properties) {
		raConfigure(properties);
	}

	@Override
	public void raConfigure(ConfigProperties properties) {
		if (tracer.isFineEnabled()) {
			tracer.fine("Configuring RA Entity " + this.raContext.getEntityName());
		}

		try {
			this.jndiName = (String) properties.getProperty(CONF_JNDI).getValue();
		} catch (Exception e) {
			tracer.severe("Configuring of SMPP Server RA failed ", e);
		}
	}

	@Override
	public void raInactive() {
		this.smppServerSession.closeSmppSessions();
		this.defaultSmppServerHandler.setSmppSessionHandlerInterface(null);
		this.defaultSmppServerHandler = null;

		if (tracer.isInfoEnabled()) {
			tracer.info("Inactivated RA Entity " + this.raContext.getEntityName());
		}
	}

	@Override
	public void raStopping() {
		// TODO Auto-generated method stub

	}

	@Override
	public void raUnconfigure() {
		this.jndiName = null;
	}

	@Override
	public void raVerifyConfiguration(ConfigProperties properties) throws InvalidConfigurationException {

		try {
			String jndiName = (String) properties.getProperty(CONF_JNDI).getValue();
			if (jndiName == null) {
				throw new InvalidConfigurationException("JNDI Name cannot be null");
			}
		} catch (Exception e) {
			throw new InvalidConfigurationException(e.getMessage(), e);
		}
	}

	@Override
	public void serviceActive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceInactive(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void serviceStopping(ReceivableService arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setResourceAdaptorContext(ResourceAdaptorContext raContext) {
		this.tracer = raContext.getTracer(getClass().getSimpleName());
		this.raContext = raContext;
		this.sleeEndpoint = raContext.getSleeEndpoint();
		this.eventLookup = raContext.getEventLookupFacility();
		this.eventIdCache = new EventIDCache(raContext);
		this.smppServerSession = new SmppServerSessionsImpl(this);
	}

	@Override
	public void unsetResourceAdaptorContext() {
		this.raContext = null;
		this.sleeEndpoint = null;
		this.eventLookup = null;
		this.eventIdCache = null;
		this.smppServerSession = null;
	}

	/**
	 * Protected
	 */

	protected void startNewSmppServerTransactionActivity(SmppServerTransactionImpl txImpl)
			throws ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
			StartActivityException {
		sleeEndpoint.startActivity(txImpl.getActivityHandle(), txImpl, ActivityFlags.REQUEST_ENDED_CALLBACK);
	}

	protected void startNewSmppTransactionSuspendedActivity(SmppServerTransactionImpl txImpl)
			throws ActivityAlreadyExistsException, NullPointerException, IllegalStateException, SLEEException,
			StartActivityException {
		sleeEndpoint.startActivitySuspended(txImpl.getActivityHandle(), txImpl, ActivityFlags.REQUEST_ENDED_CALLBACK);
	}

	protected void endActivity(SmppServerTransactionImpl txImpl) {
		try {
			this.sleeEndpoint.endActivity(txImpl.getActivityHandle());
		} catch (Exception e) {
			this.tracer.severe("Error while Ending Activity " + txImpl, e);
		}
	}

	protected ResourceAdaptorContext getRAContext() {
		return this.raContext;
	}

	/**
	 * Private methods
	 */
	protected void fireEvent(String eventName, ActivityHandle handle, Object event) {

		FireableEventType eventID = eventIdCache.getEventId(this.eventLookup, eventName);

		if (eventID == null) {
			tracer.severe("Event id for " + eventID + " is unknown, cant fire!!!");
		} else {
			try {
				sleeEndpoint.fireEvent(handle, eventID, event, address, null);
			} catch (UnrecognizedActivityHandleException e) {
				this.tracer.severe("Error while firing event", e);
			} catch (IllegalEventException e) {
				this.tracer.severe("Error while firing event", e);
			} catch (ActivityIsEndingException e) {
				this.tracer.severe("Error while firing event", e);
			} catch (NullPointerException e) {
				this.tracer.severe("Error while firing event", e);
			} catch (SLEEException e) {
				this.tracer.severe("Error while firing event", e);
			} catch (FireEventException e) {
				this.tracer.severe("Error while firing event", e);
			}
		}
	}

}
