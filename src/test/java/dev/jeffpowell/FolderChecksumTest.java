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
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.junit.Test;

public class FolderChecksumTest {

    private Path getRootPath(String relativeSpot) {
        return Path.of("src/test/resources/" + relativeSpot).toAbsolutePath();
    }

    @Test
    public void testCalculateChecksumDiff_happy() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightHappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
        Collection<Path> actual = sut.calculateChecksumDiff();
        assertEquals(0, actual.size());
    }

    @Test
    public void testCalculateChecksumDiff_unhappy() throws IOException {
        Path leftRoot = getRootPath("leftHappy");
        Path rightRoot = getRootPath("rightUnhappy");
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
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
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
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
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
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
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
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
        FolderChecksum sut = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
        Map<Integer, List<Path>> directoriesByDepth = sut.getDirectoryTreeByDepth(leftRoot);
        Map<Path, Checksum> allChecksums = sut.allChecksums(directoriesByDepth);
        assertEquals(8, allChecksums.size());
    }

    
}
