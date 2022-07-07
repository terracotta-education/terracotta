package edu.iu.terracotta.utils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.opencsv.CSVWriter;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.core.io.ByteArrayResource;

public class ZipUtil {
    public static ByteArrayResource generateZipFile(Map<String, List<String[]>> csvFiles, Map<String, String> jsonFiles,Map<String, String> readme) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);

        // .csv files
        for(Map.Entry<String,List<String[]>> mapEntry : csvFiles.entrySet()) {
            zos.putNextEntry(new ZipEntry(mapEntry.getKey()));
            CSVWriter writer = new CSVWriter(new OutputStreamWriter(zos));
            for(String[] array : mapEntry.getValue()){
                writer.writeNext(array);
                writer.flush();
            }
            zos.closeEntry();
        }

        // .json files
        for(Map.Entry<String,String> mapEntry : jsonFiles.entrySet()) {
            zos.putNextEntry(new ZipEntry(mapEntry.getKey()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(zos));
            writer.write(mapEntry.getValue());
            writer.flush();
            zos.closeEntry();
        }

        // readme files
        for(Map.Entry<String,String> mapEntry : readme.entrySet()) {
            zos.putNextEntry(new ZipEntry(mapEntry.getKey()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(zos));
            writer.write(mapEntry.getValue());
            writer.flush();
            zos.closeEntry();
        }

        zos.close();
        return new ByteArrayResource(bos.toByteArray());
    }
}
