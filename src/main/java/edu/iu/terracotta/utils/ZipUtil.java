package edu.iu.terracotta.utils;

import com.opencsv.CSVWriter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.core.io.ByteArrayResource;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static ByteArrayResource generateZipFile(Map<String, List<String[]>> csvFiles) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);

        for(Map.Entry<String,List<String[]>> mapEntry : csvFiles.entrySet()) {
            zos.putNextEntry(new ZipEntry(mapEntry.getKey()));
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(zos));
            for(String[] array : mapEntry.getValue()){
                writer.writeNext(array);
                writer.flush();
            }
            zos.closeEntry();
        }
        zos.close();
        return new ByteArrayResource(bos.toByteArray());
    }
}
