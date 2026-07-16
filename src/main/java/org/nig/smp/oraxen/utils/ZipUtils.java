package org.nig.smp.oraxen.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private ZipUtils() {}

    public static void writeZipFile(File outputFile, List<VirtualFile> fileList) {
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos, StandardCharsets.UTF_8)) {

            for (VirtualFile file : fileList) {
                addToZip(file.getPath(), file.getInputStream(), zos);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void addToZip(String zipFilePath, InputStream fis, ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zipEntry.setTime(0);
        zos.putNextEntry(zipEntry);

        if (fis == null) {
            try { zos.closeEntry(); } catch (IOException ignored) {}
            return;
        }

        byte[] bytes = new byte[1024];
        int length;
        try (InputStream in = fis) {
            while ((length = in.read(bytes)) >= 0)
                zos.write(bytes, 0, length);
        } catch (IOException ignored) {
        } finally {
            try { zos.closeEntry(); } catch (IOException ignored) {}
        }
    }
}
