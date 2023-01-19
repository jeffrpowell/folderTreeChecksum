package dev.jeffpowell;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class App 
{
    public static void main( String[] args ) throws IOException
    {
        Path leftRoot = Path.of(args[0]);
        Path rightRoot = Path.of(args[1]);
        System.out.println("Comparing the following file trees for equality:");
        System.out.println(leftRoot);
        System.out.println(rightRoot);
        FolderChecksum folderChecksum = new FolderChecksum(leftRoot, rightRoot, new HashMap<>(), Executors.newCachedThreadPool());
        Collection<Path> badPaths = folderChecksum.calculateChecksumDiff();
        System.out.println("\nResults:\n");
        if (badPaths.isEmpty()) {
            System.out.println("Entire file tree is certified to be identical!");
        }
        else {
            System.out.println("The following paths have a mismatch:");
            badPaths.stream().forEach(System.out::println);
        }
    }
}
