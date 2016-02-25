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

/**
*
* @author sergey vetyutnev
*
*/
public interface MProcRuleOamMessages {

    public static final String CREATE_MPROC_RULE_FAIL_RULE_CLASS_NAME_NULL_VALUE = "Creation of MProcRule failed. ruleClassName parameter can not be null";

    public static final String CREATE_MPROC_RULE_FAIL_RULE_CLASS_NOT_FOUND = "Creation of MProcRule failed. ruleClass is not found for ruleClassName: %s";

    public static final String CREATE_MPROC_RULE_FAIL_ALREADY_EXIST = "Creation of MProcRule failed. Other MProcRule with id=%d already exist";

    public static final String MODIFY_MPROC_RULE_FAIL_NOT_EXIST = "Modification of MProcRule failed. No MProcRule with id=%d exist";

    public static final String DESTROY_MPROC_RULE_FAIL_NOT_EXIST = "Destroying of MProcRule failed. No MProcRule with id=%d exist";

    public static final String SET_RULE_PARAMETERS_FAIL_NO_PARAMETERS_POVIDED = "Setting of MProcRule parameters failed: no parameter is provided";

}
