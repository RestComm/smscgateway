package org.mobicents.smsc.domain;

public enum CounterCategory {
    Scheduler,
    MProc,
    SmppIn,
    SmppOut,
    SipIn,
    SipOut,
    HttpIn,
    MapIn,
    MapOut,
    RequestQueueSize,
    ResponseQueueSize,
    ClientEsmeConnectQueueSize,
    ClientEsmeEnquireLinkQueueSize,
    EsmeReconnectsTotal,
    EsmeReconnectsSuccessful,
    EsmeReconnectsFailed,
    EsmesStartedTotal,
    SleeEventQueueSize;
}
