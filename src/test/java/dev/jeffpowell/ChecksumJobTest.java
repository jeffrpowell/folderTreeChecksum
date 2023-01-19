package dev.jeffpowell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ChecksumJobTest {

    private Path getRootPath(String relativeSpot) {
        return Path.of("src/test/resources/" + relativeSpot).toAbsolutePath();
    }

    @Test
    public void testChecksum_singleFile() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        ChecksumJob sut = new ChecksumJob(leftRoot, rightRoot, Path.of("one"), new HashMap<>());
        String actual = sut.checksum(leftRoot, Checksum::left);
        assertEquals("30FAB299DA8007D60B860AAD8691F5A4", actual);
    }

    @Test
    public void testChecksum_multiFile() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        ChecksumJob sut = new ChecksumJob(leftRoot, rightRoot, Path.of("two"), new HashMap<>());
        String actual = sut.checksum(rightRoot, Checksum::right);
        assertEquals("E785691C963FB709614569CCF0F8D0B2", actual);
    }

    @Test
    public void testChecksum_nestedFile() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        Path relativePath = Path.of("nested");
        Path nestedDir = Path.of("nested/nest");
        Map<Path, Checksum> checksumCache = new HashMap<>();
        checksumCache.put(nestedDir, new Checksum("78099DC7EF860809F5DDC68CEB2FB159", "68099DC7EF860809F5DDC68CEB2FB150"));
        ChecksumJob sut = new ChecksumJob(leftRoot, rightRoot, relativePath, checksumCache);
        String actual = sut.checksum(leftRoot, Checksum::left);
        assertEquals("86C681D0367E1CE8A0EB75D50F62879F", actual);
        actual = sut.checksum(rightRoot, Checksum::right);
        assertNotEquals("78099DC7EF860809F5DDC68CEB2FB159", actual);
    }

    @Test
    public void testChecksum_empty() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        Path relativePath = Path.of("empty dirs/singleEmpty");
        Path relativePath2 = Path.of("empty dirs/nestedEmpty/singleEmpty");
        Map<Path, Checksum> checksumCache = new HashMap<>();
        ChecksumJob sut = new ChecksumJob(leftRoot, rightRoot, relativePath, checksumCache);
        String leftActual = sut.checksum(leftRoot, Checksum::left);
        // assertEquals("87D421CB13E6E9072ABEEDABE63CBC4E", actual);
        ChecksumJob sut2 = new ChecksumJob(leftRoot, rightRoot, relativePath2, checksumCache);
        String rightActual = sut2.checksum(rightRoot, Checksum::right);
        assertNotEquals(leftActual, rightActual);
    }
}
