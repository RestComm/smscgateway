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
package org.mobicents.smsc.slee.services.http.server.tx.utils.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mobicents.smsc.slee.services.http.server.tx.exceptions.HttpApiException;
import org.mobicents.smsc.slee.services.http.server.tx.utils.HttpRequestUtils;
import org.mobicents.smsc.slee.services.http.server.tx.utils.stub.SleeTracerStub;
import org.testng.annotations.Test;

/**
 * The Class HttpRequestUtilsTest.
 */
public final class HttpRequestUtilsTest {

    private static final Log LOG = LogFactory.getLog(HttpRequestUtilsTest.class);

    private static final String HTTP_POST_BODY_O1 = "";
    private static final String HTTP_POST_BODY_O2 = "POST\r\n";
    private static final String HTTP_POST_BODY_ARABIC_OK_DATA_FILE = "src/test/resources/http-post-arabic-ok.bin";
    private static final int HTTP_POST_BODY_ARABIC_OK_DATA_FILE_BYTES = 211;
    private static final int HTTP_POST_BODY_ARABIC_OK_PARAMETERS = 8;

    /**
     * Test extract parameters from post.
     */
    @Test
    public void testExtractParametersFromPost() {
        LOG.info("Starting test: testExtractParametersFromPost.");
        try {
            final Map<String, String[]> result01 = convert(HTTP_POST_BODY_O1);
            assertNotNull(result01);
            assertEquals(result01.size(), 0);
            final Map<String, String[]> result02 = convert(HTTP_POST_BODY_O2);
            assertNotNull(result02);
            assertEquals(result02.size(), 0);
            final Map<String, String[]> result03 = convert(HTTP_POST_BODY_ARABIC_OK_DATA_FILE_BYTES,
                    new FileInputStream(HTTP_POST_BODY_ARABIC_OK_DATA_FILE));
            assertNotNull(result03);
            assertEquals(result03.size(), HTTP_POST_BODY_ARABIC_OK_PARAMETERS);
            assertEquals("e$services@HMG123", result03.get(HttpRequestUtils.P_PASSWORD)[0]);
            assertEquals("966500866422", result03.get(HttpRequestUtils.P_TO)[0]);
        } catch (FileNotFoundException e) {
            fail("Unexpected test error. Message: " + e.getMessage() + ".", e);
        } catch (HttpApiException e) {
            fail("Unexpected test error. Message: " + e.getMessage() + ".", e);
        }
    }

    private static final Map<String, String[]> convert(final String aBody) throws HttpApiException {
        return HttpRequestUtils.extractParametersFromPost(
                new SleeTracerStub(HttpRequestUtilsTest.class.getSimpleName()), aBody.length(),
                new ByteArrayInputStream(aBody.getBytes()));
    }

    private static final Map<String, String[]> convert(final int aBodyLength, final InputStream aBodyInputStream)
            throws HttpApiException {
        return HttpRequestUtils.extractParametersFromPost(
                new SleeTracerStub(HttpRequestUtilsTest.class.getSimpleName()), aBodyLength, aBodyInputStream);
    }
}
