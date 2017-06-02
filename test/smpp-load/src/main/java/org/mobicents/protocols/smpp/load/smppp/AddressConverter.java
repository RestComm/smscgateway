package org.mobicents.protocols.smpp.load.smppp;

import com.cloudhopper.smpp.type.Address;

public class AddressConverter extends GlobalConverter {

    public AddressConverter(GlobalContext ctx) {
        super(ctx);
    }

    @Override
    public Object convert(Class type, Object o) {
        Address address = new Address((byte) 0x01, (byte) 0x01, (String) o);
        return address;
    }

}
