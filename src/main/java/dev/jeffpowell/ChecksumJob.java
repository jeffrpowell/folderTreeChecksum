package dev.jeffpowell;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChecksumJob implements Callable<Checksum>{
    private final MessageDigest digest;
    private final Path leftRoot;
    private final Path rightRoot;
    private final Path directory;
    private final Map<Path, Checksum> checksumCache;

    public ChecksumJob(Path leftRoot, Path rightRoot, Path directory, Map<Path, Checksum> checksumCache) {
        this.leftRoot = leftRoot;
        this.rightRoot = rightRoot;
        this.directory = directory;
        this.checksumCache = checksumCache;
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public Checksum call() throws Exception {
        String leftChecksum = checksum(leftRoot, Checksum::left);
        String rightChecksum = checksum(rightRoot, Checksum::right);
        return new Checksum(leftChecksum, rightChecksum);
    }

    String checksum(Path root, Function<Checksum, String> pickChecksumFn) {
        List<Path> paths = new ArrayList<>();
        if (!Files.exists(root.resolve(directory))) {
            return root.resolve(directory).toString();
        }
        try (Stream<Path> fileStream = Files.walk(root.resolve(directory), 1)) {
            paths = fileStream.filter(p -> !p.equals(root.resolve(directory))).collect(Collectors.toList());
        }
        catch (IOException e) {
            return root.resolve(directory).toString();
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
                catch (IOException e) {
                    digest.update(p.toString().getBytes());
                }
            }
        }
        digest.update(directory.toString().getBytes());
        String checksum = bytesToHex(digest.digest());
        digest.reset();
        return checksum;
    }

    private static String bytesToHex(byte[] bytes) {
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

    public Path getDirectory() {
        return directory;
    }

    public Map<Path, Checksum> getChecksumCache() {
        return checksumCache;
    }
    
    
}
