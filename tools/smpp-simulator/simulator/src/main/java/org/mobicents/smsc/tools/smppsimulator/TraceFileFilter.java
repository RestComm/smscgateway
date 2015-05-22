/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.mobicents.smsc.tools.smppsimulator;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
*
* @author sergey vetyutnev
*
*/
public class TraceFileFilter extends FileFilter {

    private String name;

    public TraceFileFilter(String name) {
        this.name = name;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory())
            return true;

        String s = f.getName();
        int i1 = s.lastIndexOf('.');
        if (i1 > 0) {
            String s1 = s.substring(i1 + 1);

            if (this.name.equals("Pcap")) {
                if (s1.equals("pcap"))
                    return true;
            }
        }

        return false;
    }

    @Override
    public String getDescription() {
        return this.name;
    }

}
