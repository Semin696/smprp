package org.nig.smp.oraxen.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.nig.smp.SDSPlugin;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VirtualFile implements Comparable<VirtualFile> {

    private String parentFolder;
    private String name;
    private InputStream inputStream;

    public VirtualFile(String parentFolder, String name, InputStream inputStream) {
        parentFolder = parentFolder.replace("\\", "/");
        this.parentFolder = parentFolder.endsWith("/")
                ? parentFolder.substring(0, parentFolder.length() - 1)
                : parentFolder;
        this.name = name;
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getPath() {
        return parentFolder.isEmpty() ? name : parentFolder + "/" + name;
    }

    public void setPath(String newPath) {
        int idx = newPath.lastIndexOf("/");
        this.parentFolder = idx > 0 ? newPath.substring(0, idx) : "";
        this.name = newPath.substring(idx + 1);
    }

    @Override
    public int compareTo(VirtualFile other) {
        return other.getPath().compareTo(getPath());
    }

    public JsonElement toJsonElement() {
        try {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            inputStream.close();
            inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            return JsonParser.parseString(content);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject toJsonObject() {
        JsonElement element = toJsonElement();
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    public boolean isJsonObject() {
        return toJsonElement() != null;
    }
}
