package io.th0rgal.oraxen.pack.upload.hosts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.th0rgal.oraxen.utils.SHA1Utils;
import io.th0rgal.oraxen.utils.logs.Logs;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

public class GitHubHostingProvider implements HostingProvider {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 10_000;
    private static final int SOCKET_TIMEOUT_MS = 60_000;
    private static final String RELEASE_TAG = "resource-pack";
    private static final String ASSET_NAME = "pack.zip";

    private final String repo;
    private final String token;
    private final String apiBase;
    private final String uploadBase;

    private String packUrl;
    private String sha1;
    private UUID packUUID;

    public GitHubHostingProvider(ConfigurationSection config) {
        this.repo = config.getString("repo");
        this.token = config.getString("token");
        this.apiBase = "https://api.github.com/repos/" + repo;
        this.uploadBase = "https://uploads.github.com/repos/" + repo;
    }

    @Override
    public boolean uploadPack(File resourcePack) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            calculateSHA1(resourcePack);

            int releaseId = getOrCreateRelease(client);
            if (releaseId == -1) return false;

            deleteExistingAsset(client, releaseId);

            String downloadUrl = uploadAsset(client, releaseId, resourcePack);
            if (downloadUrl == null) return false;

            this.packUrl = downloadUrl;
            Logs.logInfo("Resource pack uploaded to GitHub: " + downloadUrl);
            return true;
        } catch (Exception e) {
            Logs.logError("Failed to upload resource pack to GitHub: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void calculateSHA1(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        this.sha1 = SHA1Utils.bytesToHex(hashBytes);
        this.packUUID = UUID.nameUUIDFromBytes(hashBytes);
    }

    private int getOrCreateRelease(CloseableHttpClient client) throws IOException {
        HttpGet getReleases = new HttpGet(apiBase + "/releases?per_page=10");
        setAuth(getReleases);
        getReleases.setConfig(requestConfig());

        try (CloseableHttpResponse response = client.execute(getReleases)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                Logs.logError("Failed to list GitHub releases: " + response.getStatusLine().getStatusCode());
                return -1;
            }

            JsonArray releases = JsonParser.parseString(body).getAsJsonArray();
            for (JsonElement el : releases) {
                JsonObject release = el.getAsJsonObject();
                if (RELEASE_TAG.equals(release.get("tag_name").getAsString())) {
                    return release.get("id").getAsInt();
                }
            }
        }

        HttpPost createRelease = new HttpPost(apiBase + "/releases");
        setAuth(createRelease);
        createRelease.setConfig(requestConfig());

        JsonObject payload = new JsonObject();
        payload.addProperty("tag_name", RELEASE_TAG);
        payload.addProperty("name", "Resource Pack");
        payload.addProperty("body", "Auto-uploaded resource pack for Oraxen");
        payload.addProperty("draft", false);
        payload.addProperty("prerelease", false);
        createRelease.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(createRelease)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 201) {
                Logs.logError("Failed to create GitHub release: " + response.getStatusLine().getStatusCode() + " " + body);
                return -1;
            }
            return JsonParser.parseString(body).getAsJsonObject().get("id").getAsInt();
        }
    }

    private void deleteExistingAsset(CloseableHttpClient client, int releaseId) throws IOException {
        HttpGet getAssets = new HttpGet(apiBase + "/releases/" + releaseId + "/assets");
        setAuth(getAssets);
        getAssets.setConfig(requestConfig());

        try (CloseableHttpResponse response = client.execute(getAssets)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) return;

            JsonArray assets = JsonParser.parseString(body).getAsJsonArray();
            for (JsonElement el : assets) {
                JsonObject asset = el.getAsJsonObject();
                if (ASSET_NAME.equals(asset.get("name").getAsString())) {
                    int assetId = asset.get("id").getAsInt();

                    HttpDelete deleteAsset = new HttpDelete(apiBase + "/releases/assets/" + assetId);
                    setAuth(deleteAsset);
                    deleteAsset.setConfig(requestConfig());
                    try (CloseableHttpResponse resp = client.execute(deleteAsset)) {
                        EntityUtils.consumeQuietly(resp.getEntity());
                    }
                    return;
                }
            }
        }
    }

    private String uploadAsset(CloseableHttpClient client, int releaseId, File resourcePack) throws IOException {
        HttpPost uploadRequest = new HttpPost(
                uploadBase + "/releases/" + releaseId + "/assets?name=" + ASSET_NAME
        );
        setAuth(uploadRequest);
        uploadRequest.setConfig(requestConfig());
        uploadRequest.setHeader("Content-Type", "application/zip");

        HttpEntity fileEntity = MultipartEntityBuilder.create()
                .addBinaryBody("file", resourcePack, ContentType.APPLICATION_OCTET_STREAM, ASSET_NAME)
                .build();
        uploadRequest.setEntity(fileEntity);

        try (CloseableHttpResponse response = client.execute(uploadRequest)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 201) {
                Logs.logError("Failed to upload asset to GitHub: " + response.getStatusLine().getStatusCode() + " " + body);
                return null;
            }
            JsonObject asset = JsonParser.parseString(body).getAsJsonObject();
            return asset.get("browser_download_url").getAsString();
        }
    }

    private void setAuth(HttpRequestBase request) {
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("Accept", "application/vnd.github.v3+json");
    }

    private static RequestConfig requestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .build();
    }

    @Override
    public String getPackURL() {
        return packUrl;
    }

    @Override
    public byte[] getSHA1() {
        return SHA1Utils.hexToBytes(sha1);
    }

    @Override
    public String getOriginalSHA1() {
        return sha1;
    }

    @Override
    public UUID getPackUUID() {
        return packUUID;
    }
}
