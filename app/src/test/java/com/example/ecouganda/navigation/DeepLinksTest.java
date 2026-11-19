package com.example.ecouganda.navigation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DeepLinksTest {

    @Test
    public void buildsUriThatParsesBackToTheSameId() {
        String uri = DeepLinks.reportUri("RPT-2041");
        assertEquals("ecouganda://reports/RPT-2041", uri);
        assertEquals("RPT-2041", DeepLinks.parseReportId("ecouganda", "reports", "/RPT-2041"));
    }

    @Test
    public void acceptsCaseAndTrailingSegments() {
        assertEquals("RPT-2048", DeepLinks.parseReportId("EcoUganda", "REPORTS", "/rpt-2048"));
        assertEquals("RPT-2048", DeepLinks.parseReportId("ecouganda", "reports", "//RPT-2048/details"));
    }

    @Test
    public void rejectsOtherLinksAndBadIds() {
        assertNull(DeepLinks.parseReportId("https", "reports", "/RPT-2041"));
        assertNull(DeepLinks.parseReportId("ecouganda", "projects", "/RPT-2041"));
        assertNull(DeepLinks.parseReportId("ecouganda", "reports", "/"));
        assertNull(DeepLinks.parseReportId("ecouganda", "reports", null));
        assertNull(DeepLinks.parseReportId("ecouganda", "reports", "/2041"));
        assertNull(DeepLinks.parseReportId("ecouganda", "reports", "/RPT-2041;drop"));
    }

    @Test
    public void reportsListLinkIsRecognised() {
        assertTrue(DeepLinks.isReportsLink("ecouganda", "reports"));
        assertFalse(DeepLinks.isReportsLink("ecouganda", null));
    }

    @Test
    public void normalisesExtrasFromPushPayloads() {
        assertEquals("RPT-7", DeepLinks.normaliseReportId("  rpt-7 "));
        assertNull(DeepLinks.normaliseReportId(""));
        assertNull(DeepLinks.normaliseReportId(null));
        assertNull(DeepLinks.normaliseReportId("RPT-123456789"));
    }
}
