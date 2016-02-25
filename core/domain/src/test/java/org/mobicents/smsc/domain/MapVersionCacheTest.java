/*
 * TeleStax, Open Source Cloud Communications  Copyright 2012. 
 * and individual contributors
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

package org.mobicents.smsc.domain;

import static org.testng.Assert.*;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;
import org.testng.annotations.Test;

/**
 * 
 * @author sergey vetyutnev
 * 
 */
public class MapVersionCacheTest {

    @Test(groups = { "cassandra" })
    public void testMapVersionCache() throws Exception {
        MapVersionCache cache = MapVersionCache.getInstance("Test");

        MAPApplicationContextVersion v = cache.getMAPApplicationContextVersion("111");
        assertNull(v);

        cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version3);
        v = cache.getMAPApplicationContextVersion("111");
        assertEquals(v, MAPApplicationContextVersion.version3);

        // MAP V2
        cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version2);
        v = cache.getMAPApplicationContextVersion("111");
        assertEquals(v, MAPApplicationContextVersion.version2);

        cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version2);
        v = cache.getMAPApplicationContextVersion("111");
        assertEquals(v, MAPApplicationContextVersion.version2);

        for (int i1 = 0; i1 < MapVersionNeg.mapV2RetestUpCount - 2; i1++) {
            cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version2);
            v = cache.getMAPApplicationContextVersion("111");
            assertEquals(v, MAPApplicationContextVersion.version2);
        }
        cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version2);
        v = cache.getMAPApplicationContextVersion("111");
        assertEquals(v, MAPApplicationContextVersion.version3);

        cache.setMAPApplicationContextVersion("111", MAPApplicationContextVersion.version2);
        v = cache.getMAPApplicationContextVersion("111");
        assertEquals(v, MAPApplicationContextVersion.version2);


        // MAP V1
        v = cache.getMAPApplicationContextVersion("222");
        assertNull(v);
        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);

        v = cache.getMAPApplicationContextVersion("222");
        assertEquals(v, MAPApplicationContextVersion.version3);
        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);

        v = cache.getMAPApplicationContextVersion("222");
        assertEquals(v, MAPApplicationContextVersion.version1);
        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);

        for (int i1 = 0; i1 < MapVersionNeg.mapV1RetestUpCount - 1; i1++) {
            v = cache.getMAPApplicationContextVersion("222");
            assertEquals(v, MAPApplicationContextVersion.version1);
            cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);
        }
        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);
        v = cache.getMAPApplicationContextVersion("222");
        assertEquals(v, MAPApplicationContextVersion.version3);

        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version1);
        v = cache.getMAPApplicationContextVersion("222");
        assertEquals(v, MAPApplicationContextVersion.version1);

        cache.setMAPApplicationContextVersion("222", MAPApplicationContextVersion.version3);
        v = cache.getMAPApplicationContextVersion("222");
        assertEquals(v, MAPApplicationContextVersion.version3);

    }
    
}
