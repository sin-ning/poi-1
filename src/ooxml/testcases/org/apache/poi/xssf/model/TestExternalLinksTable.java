/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */

package org.apache.poi.xssf.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.poi.ss.usermodel.Name;
import org.apache.poi.xssf.XSSFTestDataSamples;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public final class TestExternalLinksTable {
    @Test
    public void none() {
        XSSFWorkbook wb = XSSFTestDataSamples.openSampleWorkbook("SampleSS.xlsx");
        assertEquals(null, wb.getExternalLinksTable());
    }
    
    @Test
    public void basicRead() {
        XSSFWorkbook wb = XSSFTestDataSamples.openSampleWorkbook("ref-56737.xlsx");
        assertNotNull(wb.getExternalLinksTable());
        Name name = null;
        
        ExternalLinksTable links = wb.getExternalLinksTable();
        assertEquals(3, links.getSheetNames().size());
        assertEquals(2, links.getDefinedNames().size());
        
        assertEquals("Uses",    links.getSheetNames().get(0));
        assertEquals("Defines", links.getSheetNames().get(1));
        assertEquals("56737",   links.getSheetNames().get(2));
        
        name = links.getDefinedNames().get(0);
        assertEquals("NR_Global_B2", name.getNameName());
        assertEquals(-1, name.getSheetIndex());
        assertEquals(null, name.getSheetName());
        assertEquals("'Defines'!$B$2", name.getRefersToFormula());
        
        name = links.getDefinedNames().get(1);
        assertEquals("NR_To_A1", name.getNameName());
        assertEquals(1, name.getSheetIndex());
        assertEquals("Defines", name.getSheetName());
        assertEquals("'Defines'!$A$1", name.getRefersToFormula());
    }
    
    @Test
    public void basicReadWriteRead() {
        XSSFWorkbook wb = XSSFTestDataSamples.openSampleWorkbook("ref-56737.xlsx");
        Name name = wb.getExternalLinksTable().getDefinedNames().get(1);
        name.setNameName("Testing");
        name.setRefersToFormula("$A$1");
        
        wb = XSSFTestDataSamples.writeOutAndReadBack(wb);
        ExternalLinksTable links = wb.getExternalLinksTable();
        
        name = links.getDefinedNames().get(0);
        assertEquals("NR_Global_B2", name.getNameName());
        assertEquals(-1, name.getSheetIndex());
        assertEquals(null, name.getSheetName());
        assertEquals("'Defines'!$B$2", name.getRefersToFormula());
        
        name = links.getDefinedNames().get(1);
        assertEquals("Testing", name.getNameName());
        assertEquals(1, name.getSheetIndex());
        assertEquals("Defines", name.getSheetName());
        assertEquals("$A$1", name.getRefersToFormula());
    }
    
    @Test
    public void readWithReferencesToTwoExternalBooks() {
        XSSFWorkbook wb = XSSFTestDataSamples.openSampleWorkbook("ref2-56737.xlsx");
        
        // TODO Fix so we can see both of them...
    }
}