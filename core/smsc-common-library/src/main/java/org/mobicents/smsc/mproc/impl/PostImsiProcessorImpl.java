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

package org.mobicents.smsc.mproc.impl;

import org.apache.log4j.Logger;
import org.mobicents.smsc.mproc.PostImsiProcessor;

/**
*
* @author sergey vetyutnev
*
*/
public class PostImsiProcessorImpl implements PostImsiProcessor {

    private boolean needDropMessages = false;
    private Logger logger;

    public PostImsiProcessorImpl(Logger logger) {
        this.logger = logger;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    // results of message processing
    public boolean isNeedDropMessages() {
        return needDropMessages;
    }

    @Override
    public void dropMessages() {
        needDropMessages = true;
    }

}
