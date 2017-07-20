/*
 * TeleStax, Open Source Cloud Communications  
 * Copyright 2012, Telestax Inc and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.smsc.slee.services.mo;

import javax.slee.facilities.Tracer;

import org.mobicents.protocols.ss7.map.api.MAPProvider;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessage;
import org.mobicents.protocols.ss7.map.api.errors.MAPErrorMessageSMDeliveryFailure;
import org.mobicents.protocols.ss7.map.api.errors.SMEnumeratedDeliveryFailureCause;
import org.mobicents.protocols.ss7.map.api.primitives.NetworkResource;
import org.mobicents.protocols.ss7.map.api.service.sms.MAPDialogSms;
import org.mobicents.protocols.ss7.map.api.service.sms.SmsMessage;
import org.mobicents.smsc.library.CdrDetailedGenerator;
import org.mobicents.smsc.library.MessageDeliveryResultResponseInterface;

import com.cloudhopper.smpp.pdu.BaseSm;

/**
 *
 * @author servey vetyutnev
 *
 */
public class MessageDeliveryResultResponseMo implements MessageDeliveryResultResponseInterface {

	private boolean onlyChargingRequest;
	private boolean isMoOperation;
	private MAPDialogSms dialog;
	private MAPProvider provider;
	private SmsMessage evt;
	private long invokeId;
	private Tracer logger;

	public MessageDeliveryResultResponseMo(boolean onlyChargingRequest, boolean isMoOperation, MAPDialogSms dialog,
			MAPProvider provider, SmsMessage evt, long invokeId, Tracer logger) {
		this.onlyChargingRequest = onlyChargingRequest;
		this.isMoOperation = isMoOperation;
		this.dialog = dialog;
		this.provider = provider;
		this.evt = evt;
		this.invokeId = invokeId;
		this.logger = logger;
	}

	@Override
	public boolean isOnlyChargingRequest() {
		return onlyChargingRequest;
	}

	@Override
	public void responseDeliverySuccess() {
		try {
            if (dialog.getApplicationContext().getApplicationContextVersion().getVersion() >= 3) {
                if (this.isMoOperation)
                    dialog.addMoForwardShortMessageResponse(this.invokeId, null, null);
                else
                    dialog.addMtForwardShortMessageResponse(this.invokeId, null, null);
            } else {
                dialog.addForwardShortMessageResponse(this.invokeId);
            }

			if (this.logger.isFineEnabled()) {
				this.logger.fine("\nSent ForwardShortMessageResponse = " + evt);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending ForwardShortMessageResponse ", e);
		}
	}

	@Override
	public void responseDeliveryFailure(DeliveryFailureReason reason, MAPErrorMessage errMessage) {
		try {
			MAPErrorMessage errorMessage;
			if (errMessage != null && errMessage.isEmSMDeliveryFailure()) {
				MAPErrorMessageSMDeliveryFailure smDeliveryFailure = errMessage.getEmSMDeliveryFailure();
				errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSMDeliveryFailure(
						dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
						smDeliveryFailure.getSMEnumeratedDeliveryFailureCause(), null, null);
			} else {
				switch (reason) {
				case destinationUnavalable:
					errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
							NetworkResource.vlr, null, null);
					break;
				case invalidDestinationAddress:
					errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSMDeliveryFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
							SMEnumeratedDeliveryFailureCause.subscriberNotSCSubscriber, null, null);
					break;
				case permanentNetworkError:
					errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
							NetworkResource.vlr, null, null);
					break;
				case temporaryNetworkError:
					errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
							NetworkResource.vlr, null, null);
					break;
				default:
					errorMessage = this.provider.getMAPErrorMessageFactory().createMAPErrorMessageSystemFailure(
							dialog.getApplicationContext().getApplicationContextVersion().getVersion(),
							NetworkResource.vlr, null, null);
					break;
				}
			}
			dialog.sendErrorComponent(evt.getInvokeId(), errorMessage);
			if (this.logger.isInfoEnabled()) {
				this.logger.info("\nSent ErrorComponent = " + errorMessage);
			}

			dialog.close(false);
		} catch (Throwable e) {
			logger.severe("Error while sending Error message", e);
			return;
		}
	}
	
	public String getMessageType() {
	    return null;
	}
    
    public int getSeqNumber() {
        return -1;
    }

}
