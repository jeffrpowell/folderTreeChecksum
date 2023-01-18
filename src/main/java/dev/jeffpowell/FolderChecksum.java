package dev.jeffpowell;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FolderChecksum {
    private final Path leftRoot;
    private final Path rightRoot;
    private final MessageDigest digest;
    private final Map<Path, Checksum> checksums;
    
    public FolderChecksum(Path leftRoot, Path rightRoot, Map<Path, Checksum> checksums) {
        this.leftRoot = leftRoot;
        this.rightRoot = rightRoot;
        this.checksums = checksums;
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Collection<Path> calculateChecksumDiff() throws IOException
    {
        Map<Integer, List<Path>> directoriesByDepth_left = getDirectoryTreeByDepth(leftRoot);
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
        return deepestPathsKnown.values();
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

    public record Checksum(String left, String right){}
        
    Map<Path, Checksum> allChecksums(Map<Integer, List<Path>> directoriesByDepth) throws IOException {
        int maxDepth = directoriesByDepth.keySet().stream().max(Comparator.naturalOrder()).orElse(0);

        for (int depth = maxDepth; depth >= 1; depth--) {
            for (Path d : directoriesByDepth.get(depth)) {
                if (checksums.containsKey(d)) {
                    continue;
                }
                String leftChecksum = checksum(digest, leftRoot, d, checksums, Checksum::left);
                String rightChecksum = checksum(digest, rightRoot, d, checksums, Checksum::right);
                checksums.put(d, new Checksum(leftChecksum, rightChecksum));
            }
        }
        return checksums;
    }

    String checksum(MessageDigest digest, Path root, Path directory, Map<Path, Checksum> checksumCache, Function<Checksum, String> pickChecksumFn) throws IOException {
        List<Path> paths = new ArrayList<>();
        if (!Files.exists(root.resolve(directory))) {
            return root.resolve(directory).toString();
        }
        try (Stream<Path> fileStream = Files.walk(root.resolve(directory), 1)) {
            paths = fileStream.filter(p -> !p.equals(root.resolve(directory))).collect(Collectors.toList());
        }
        for (Path p : paths) {
            if (checksumCache.containsKey(root.relativize(p))) {
                String cachedChecksum = pickChecksumFn.apply(checksumCache.get(root.relativize(p)));
                digest.update(cachedChecksum.getBytes());
            }
            else {
                try (FileInputStream fileStream = new FileInputStream(p.toFile())) {
                    digest.update(fileStream.readAllBytes());
                }
            }
        }
        digest.update(directory.toString().getBytes());
        String checksum = bytesToHex(digest.digest());
        digest.reset();
        return checksum;
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for ( int j = 0; j < bytes.length; j++ ) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
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
