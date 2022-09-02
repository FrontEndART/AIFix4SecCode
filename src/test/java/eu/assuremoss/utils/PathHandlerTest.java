package eu.assuremoss.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PathHandlerTest {
    String testProjectRelativePath = "test-project";
    String testProjectAbsolutePath = PathHandler.joinPath(Utils.getWorkingDir(), testProjectRelativePath);

    @Test
    void testToAbsolute() {
        Assertions.assertEquals(testProjectAbsolutePath, PathHandler.toAbsolute(testProjectRelativePath));
    }

    @Test
    void testIsAbsolute() {
        Assertions.assertTrue(PathHandler.isAbsolute(testProjectAbsolutePath));
        Assertions.assertFalse(PathHandler.isAbsolute(testProjectRelativePath));
    }
}
