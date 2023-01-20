package dev.jeffpowell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderChecksum {
    private final Path leftRoot;
    private final Path rightRoot;
    private final MessageDigest digest;
    private final Map<Path, Checksum> checksums;
    private final ExecutorService executor;
    
    public FolderChecksum(Path leftRoot, Path rightRoot, Map<Path, Checksum> checksums, ExecutorService executor) {
        this.leftRoot = leftRoot;
        this.rightRoot = rightRoot;
        this.checksums = checksums;
        this.executor = executor;
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Collection<Path> calculateChecksumDiff() throws IOException
    {
        System.out.println("Walking entire tree under " + leftRoot);
        Map<Integer, List<Path>> directoriesByDepth_left = getDirectoryTreeByDepth(leftRoot);
        System.out.println("Walking entire tree under " + rightRoot);
        Map<Integer, List<Path>> directoriesByDepth_right = getDirectoryTreeByDepth(rightRoot);
        directoriesByDepth_right.entrySet().stream().forEach(e -> directoriesByDepth_left.get(e.getKey()).addAll(e.getValue()));
        Map<Path, Checksum> allChecksums = allChecksums(directoriesByDepth_left);
        List<Path> badPaths = allChecksums.entrySet().stream()
            .filter(e -> !e.getValue().left().equals(e.getValue().right()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        return reduceOverlappingPaths(badPaths);
    }

    Collection<Path> reduceOverlappingPaths(List<Path> paths) {
        Map<Path, Path> deepestPathsKnown = new HashMap<>();
        for (Path path : paths) {
            if (deepestPathsKnown.containsKey(path)) {
                continue;
            }
            Path i = path;
            while(i != null) {
                deepestPathsKnown.put(i, path);
                i = i.getParent();
            }
        }
        return deepestPathsKnown.values().stream().collect(Collectors.toSet());
    }

    Map<Integer, List<Path>> getDirectoryTreeByDepth(Path directory) throws IOException {
        try (Stream<Path> pathStream = Files.walk(directory)) {
            return pathStream
                .filter(p -> !p.equals(directory))
                .filter(p -> p.toFile().isDirectory())
                .map(directory::relativize)
                .collect(Collectors.groupingBy(Path::getNameCount));
        }
    }

    Map<Path, Checksum> allChecksums(Map<Integer, List<Path>> directoriesByDepth) {
        int maxDepth = directoriesByDepth.keySet().stream().max(Comparator.naturalOrder()).orElse(0);
        Set<Path> visited = new HashSet<>();
        for (int depth = maxDepth; depth >= 1; depth--) {
            System.out.println("Exploring " + directoriesByDepth.get(depth).size() + " folders at depth " + depth);
            Map<Path, Future<Checksum>> futures = new HashMap<>();
            for (Path d : directoriesByDepth.get(depth)) {
                if (!visited.add(d)) {
                    continue;
                }
                futures.put(d, executor.submit(new ChecksumJob(leftRoot, rightRoot, d, checksums)));
            }
            checksums.putAll(futures.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> {
                        try {
                            return e.getValue().get();
                        } catch (InterruptedException | ExecutionException e1) {
                            return new Checksum(e.getKey().toString(), "");
                        }
                    }
                )));
        }
        return checksums;
    }

    
    public Path getLeftRoot() {
        return leftRoot;
    }

    public Path getRightRoot() {
        return rightRoot;
    }

    public MessageDigest getDigest() {
        return digest;
    }

    public Map<Path, Checksum> getChecksums() {
        return checksums;
    }
}
