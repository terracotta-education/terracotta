package edu.iu.terracotta.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.core.io.ByteArrayResource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ZipUtil {

    public static ByteArrayResource generateZipFile(Map<String, String> files) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
            // .csv files
            for (Map.Entry<String, String> mapEntry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(mapEntry.getKey());
                zipOutputStream.putNextEntry(zipEntry);

                try (FileInputStream fileInputStream = new FileInputStream(mapEntry.getValue())) {
                    byte[] bytes = new byte[1024];
                    int length;

                    while ((length = fileInputStream.read(bytes)) >= 0) {
                        zipOutputStream.write(bytes, 0, length);
                    }
                }

                Files.deleteIfExists(Paths.get(mapEntry.getValue()));

                zipOutputStream.closeEntry();
            }
        }

        return new ByteArrayResource(byteArrayOutputStream.toByteArray());
    }

}
