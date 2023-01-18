package dev.jeffpowell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;

import dev.jeffpowell.FolderChecksum.Checksum;

public class FolderChecksumTest {

    private Path getRootPath(String relativeSpot) {
        return Path.of("src/test/resources/" + relativeSpot).toAbsolutePath();
    }

    @Test
    public void testCalculateChecksumDiff_happy() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        Collection<Path> actual = sut.calculateChecksumDiff();
        assertEquals(0, actual.size());
    }

    @Test
    public void testCalculateChecksumDiff_unhappy() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightUnhappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        Set<Path> expected = Set.of(
            Path.of("empty dirs/singleEmpty"),
            Path.of("nested/nest"),
            Path.of("two"),
            Path.of("three")
        );
        Set<Path> actual = sut.calculateChecksumDiff().stream().collect(Collectors.toSet());;
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }

    @Test
    public void testReduceOverlappingPaths() {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        List<Path> input = List.of(
            Path.of("empty dirs/nestedEmpty"),
            Path.of("empty dirs/nestedEmpty/singleEmpty"),
            Path.of("empty dirs/singleEmpty"),
            Path.of("empty dirs"),
            Path.of("nested/nest"),
            Path.of("nested"), 
            Path.of("one"), 
            Path.of("two")
        );
        Set<Path> expected = Set.of(
            Path.of("empty dirs/nestedEmpty/singleEmpty"),
            Path.of("empty dirs/singleEmpty"),
            Path.of("nested/nest"),
            Path.of("one"), 
            Path.of("two")
        );
        Set<Path> actual = sut.reduceOverlappingPaths(input).stream().collect(Collectors.toSet());
        assertTrue(expected.containsAll(actual));
        assertTrue(actual.containsAll(expected));
    }
    
    @Test
    public void testGetDirectoryTreeByDepth() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        Map<Integer, List<Path>> expected = new HashMap<>();
        expected.put(1, List.of(Path.of("empty dirs"), Path.of("nested"), Path.of("one"), Path.of("two")));
        expected.put(2, List.of(Path.of("empty dirs/nestedEmpty"), Path.of("empty dirs/singleEmpty"), Path.of("nested/nest")));
        expected.put(3, List.of(Path.of("empty dirs/nestedEmpty/singleEmpty")));
        Map<Integer, List<Path>> actual = sut.getDirectoryTreeByDepth(leftRoot);
        assertEquals(expected, actual);
    }

    @Test
    public void testAllChecksums() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        Map<Integer, List<Path>> directoriesByDepth = new HashMap<>();
        directoriesByDepth.put(1, List.of(Path.of("one"), Path.of("two"), Path.of("nested"), Path.of("empty dirs")));
        directoriesByDepth.put(2, List.of(Path.of("nested/nest"), Path.of("empty dirs/singleEmpty"), Path.of("empty dirs/nestedEmpty")));
        directoriesByDepth.put(3, List.of(Path.of("empty dirs/nestedEmpty/singleEmpty")));
        Map<Path, Checksum> allChecksums = sut.allChecksums(directoriesByDepth);
        assertEquals(8, allChecksums.size());
    }

    @Test
    public void testIntegrate_GetDirectoryTreeByDepth_AllChecksums() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        Map<Integer, List<Path>> directoriesByDepth = sut.getDirectoryTreeByDepth(leftRoot);
        Map<Path, Checksum> allChecksums = sut.allChecksums(directoriesByDepth);
        assertEquals(8, allChecksums.size());
    }

    @Test
    public void testChecksum_singleFile() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, getRootPath("rightHappy"), new HashMap<>());
        String actual = sut.checksum(sut.getDigest(), leftRoot, Path.of("one"), new HashMap<>(), FolderChecksum.Checksum::left);
        assertEquals("30FAB299DA8007D60B860AAD8691F5A4", actual);
    }

    @Test
    public void testChecksum_multiFile() throws IOException {
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(getRootPath("leftHappy"), rightRoot, new HashMap<>());
        String actual = sut.checksum(sut.getDigest(), rightRoot, Path.of("two"), new HashMap<>(), FolderChecksum.Checksum::left);
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
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, checksumCache);
        String actual = sut.checksum(sut.getDigest(), leftRoot, relativePath, checksumCache, Checksum::left);
        assertEquals("86C681D0367E1CE8A0EB75D50F62879F", actual);
        actual = sut.checksum(sut.getDigest(), rightRoot, relativePath, checksumCache, Checksum::right);
        assertNotEquals("78099DC7EF860809F5DDC68CEB2FB159", actual);
    }

    @Test
    public void testChecksum_empty() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        Path relativePath = Path.of("empty dirs/singleEmpty");
        Path relativePath2 = Path.of("empty dirs/nestedEmpty/singleEmpty");
        Map<Path, Checksum> checksumCache = new HashMap<>();
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>());
        String actual = sut.checksum(sut.getDigest(), leftRoot, relativePath, checksumCache, Checksum::left);
        assertEquals("87D421CB13E6E9072ABEEDABE63CBC4E", actual);
        actual = sut.checksum(sut.getDigest(), rightRoot, relativePath2, checksumCache, Checksum::right);
        assertNotEquals("87D421CB13E6E9072ABEEDABE63CBC4E", actual);
    }
}
