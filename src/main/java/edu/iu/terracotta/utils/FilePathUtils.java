package edu.iu.terracotta.utils;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import edu.iu.terracotta.exceptions.app.FileStorageException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class FilePathUtils {

    /**
     * Resolve a relative path against a storage root directory. Throws a FileStorageException if the
     * result would point outside of the root, e.g. an absolute path or one that climbs out with "..".
     *
     * @param root the storage root directory
     * @param relativePath the path below the root, e.g. a stored file URI or a file name
     * @return the normalized absolute path
     */
    public Path resolveWithinRoot(String root, String relativePath) {
        if (root == null || relativePath == null) {
            throw new FileStorageException(String.format("Error 139: invalid file path: [%s]", relativePath));
        }

        try {
            Path rootPath = Paths.get(root).toAbsolutePath().normalize();
            Path resolved = rootPath.resolve(relativePath).normalize();

            if (!resolved.startsWith(rootPath)) {
                throw new FileStorageException(String.format("Error 139: file path is outside of its storage directory: [%s]", relativePath));
            }

            return resolved;
        } catch (InvalidPathException e) {
            throw new FileStorageException(String.format("Error 139: invalid file path: [%s]", relativePath), e);
        }
    }

    public Path resolveWithinRoot(Path root, String relativePath) {
        return resolveWithinRoot(root != null ? root.toString() : null, relativePath);
    }

}
