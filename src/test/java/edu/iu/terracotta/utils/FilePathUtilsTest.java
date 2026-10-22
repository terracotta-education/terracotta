package edu.iu.terracotta.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.iu.terracotta.exceptions.app.FileStorageException;

public class FilePathUtilsTest {

    @TempDir private Path tempDir;

    @Test
    public void testResolvesRelativePathInsideRoot() {
        Path result = FilePathUtils.resolveWithinRoot(tempDir.toString(), "2026/10/22/10/abc");

        assertEquals(tempDir.toAbsolutePath().normalize().resolve("2026/10/22/10/abc"), result);
    }

    @Test
    public void testResolvesAgainstPathRoot() {
        Path result = FilePathUtils.resolveWithinRoot(tempDir, "file.txt");

        assertEquals(tempDir.toAbsolutePath().normalize().resolve("file.txt"), result);
    }

    @Test
    public void testNormalizesDotSegmentsThatStayInsideRoot() {
        Path result = FilePathUtils.resolveWithinRoot(tempDir.toString(), "a/../b.txt");

        assertEquals(tempDir.toAbsolutePath().normalize().resolve("b.txt"), result);
    }

    @Test
    public void testRejectsParentTraversal() {
        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot(tempDir.toString(), "../../etc/passwd"));
    }

    @Test
    public void testRejectsTraversalAfterAValidSegment() {
        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot(tempDir.toString(), "2026/../../outside.txt"));
    }

    @Test
    public void testRejectsAbsolutePath() {
        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot(tempDir.toString(), "/etc/passwd"));
    }

    @Test
    public void testRejectsSiblingDirectoryWithSamePrefix() {
        String root = tempDir.resolve("root").toString();

        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot(root, "../root-other/file.txt"));
    }

    @Test
    public void testRejectsNullPath() {
        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot(tempDir.toString(), null));
    }

    @Test
    public void testRejectsNullRoot() {
        assertThrows(FileStorageException.class, () -> FilePathUtils.resolveWithinRoot((Path) null, "file.txt"));
    }

}
