/*
 * Copyright (c) 2013, Timothy Stack
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *  Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.sqlite.driver;

import org.junit.Test;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class SqliteParameterMetadataTest extends SqliteTestHelper {
    @Test
    public void testCount() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM test_table")) {
            ParameterMetaData pmd = ps.getParameterMetaData();

            assertEquals(0, pmd.getParameterCount());
        }

        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM test_table WHERE name = ?")) {
            ParameterMetaData pmd = ps.getParameterMetaData();

            assertEquals(1, pmd.getParameterCount());
        }
    }

    @Test
    public void testGetters() throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM test_table WHERE name = ?")) {
            ParameterMetaData pmd = ps.getParameterMetaData();

            assertEquals(ParameterMetaData.parameterNullableUnknown, pmd.isNullable(1));
            //assertTrue(pmd.isSigned(1));
            //assertTrue(pmd.getPrecision(1) > 0);
            //assertEquals(0, pmd.getScale(1));
            //assertEquals(Types.VARCHAR, pmd.getParameterType(1));
            //assertEquals("VARCHAR", pmd.getParameterTypeName(1));
            //assertEquals(String.class.getCanonicalName(), pmd.getParameterClassName(1));
            assertEquals(ParameterMetaData.parameterModeIn, pmd.getParameterMode(1));
            assertFalse(pmd.isWrapperFor(PrepStmt.class));
        }
    }
}
