package org.mobicents.smsc.library;

/**
 * 
 * @author Amit Bhayani
 * @author sergey vetyutnev
 *
 */
public enum ErrorAction {
    subscriberBusy,
    memoryCapacityExceededFlag, // MNRF
    mobileNotReachableFlag, // MNRF
    notReachableForGprs, // MNRG
    permanentFailure,
    temporaryFailure,

//    temporaryFailure, permanentFailure;

}
