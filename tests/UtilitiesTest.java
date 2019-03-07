import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class UtilitiesTest {

    private static void assertEqualsMap(HashMap<String, String> expected, HashMap<String, String> actual) {
        assertEquals(expected.size(), actual.size());
        for (Map.Entry<String, String> value : expected.entrySet()) {
            String actualValue = actual.get(value.getKey());
            assertNotNull(actualValue);
            assertEquals(value.getValue(), actualValue);
        }
    }

    @Test
    public void queryToMap() {
        HashMap<String, String> firstTest = new HashMap<>() {{
            put("name", "Jeff");
        }};
        assertEqualsMap(firstTest, Utilities.queryToMap("name=Jeff"));


        HashMap<String, String> thirdTest = new HashMap<>() {{
            put("name", "Jeff");
            put("jeff", "name");
        }};
        assertEqualsMap(thirdTest, Utilities.queryToMap("name=Jeff&jeff=name"));

        HashMap<String, String> fourthTest = new HashMap<>() {{
            put("name", "Jeff");
            put("jeff", "name");
            put("jeffs", "names");
        }};
        assertEqualsMap(fourthTest, Utilities.queryToMap("name=Jeff&jeff=name&jeffs=names"));
    }

    @Test
    public void nullOrEmpty() {
        assertTrue(Utilities.nullOrEmpty(""));
        assertFalse(Utilities.nullOrEmpty("j"));
    }
}