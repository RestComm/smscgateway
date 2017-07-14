/*
 * Telestax, Open Source Cloud Communications Copyright 2011-2017,
 * Telestax Inc and individual contributors by the @authors tag.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.mobicents.smsc.domain;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.restcomm.smpp.EsmeCluster;

/**
 * The Class CustomSmsRoutingRule.
 */
public final class CustomSmsRoutingRule extends DefaultSmsRoutingRule {

    private static final Log LOG = LogFactory.getLog(CustomSmsRoutingRule.class);

    @Override
    public String getEsmeClusterName(final int aTon, final int anNpi, final String anAddress, final String aName,
            final int aNetworkId) {
        final EsmeCluster ec = getEsmeManagement().getEsmeCluster(aNetworkId);
        if (ec == null) {
            LOG.warn("No cluster configured for NetworkId: " + aNetworkId + ".");
            return null;
        }
        if (ec.isOkFor(aTon, anNpi, anAddress, aName)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Using cluster: " + ec + ". TON: " + aTon + ". NPI: " + anNpi + ". Address: " + anAddress
                        + ". ESME Name (source): " + aName + ".");
            }
            return ec.getClusterName();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Selected cluster: " + ec + " is not applicable for the following parameters. TON: " + aTon
                    + ". NPI: " + anNpi + ". Address: " + anAddress + ". ESME Name (source): " + aName + ".");
        }
        return null;
    }
}
