package cyber.holesailmc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

public class HolesailManager {
    private static final HolesailManager instance = new HolesailManager();

    public static HolesailManager getInstance() {
        return instance;
    }

    public Path GetBinaryDirectory() {
        return HolesailMC.modDirectory.resolve("bin");
    }

    public Path GetBinaryFile() {
        boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("win");
        return GetBinaryDirectory().resolve("holesail" + (isWindows ? ".exe" : ""));
    }

    public CompletableFuture<Void> DownloadHolesail() {
        final HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(30)).build();
        final HttpRequest holesailReleaseGetRequest = HttpRequest.newBuilder().GET().uri(URI.create("https://api.github.com/repos/holesail/holesail/releases/latest")).header("User-Agent", "HolesailMC/1.0").build();

        return httpClient.sendAsync(holesailReleaseGetRequest, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray assets = release.getAsJsonArray("assets");

            String targetAsset = String.format("%s-%s.zip", System.getProperty("os.name"), System.getProperty("os.arch")).toLowerCase().replace("amd", "x");
            URI assetUrl = null;

            for (JsonElement element : assets) {
                JsonObject asset = element.getAsJsonObject();

                if (Objects.equals(asset.get("name").getAsString(), targetAsset)) {
                    assetUrl = URI.create(asset.get("browser_download_url").getAsString());
                    break;
                }
            }

            if (assetUrl == null) {
                throw new RuntimeException("assetUrl is null");
            }

            HolesailMC.LOGGER.info("Downloading {}", targetAsset);

            HttpRequest assetDownloadRequest = HttpRequest.newBuilder().GET().uri(assetUrl).header("User-Agent", "HolesailMC/1.0").build();

            httpClient.sendAsync(assetDownloadRequest, HttpResponse.BodyHandlers.ofFile(HolesailMC.modDirectory.resolve(targetAsset))).thenAccept(assetResponse -> {
                if (assetResponse.statusCode() != 200) {
                    HolesailMC.LOGGER.error("Failed: {}", assetResponse.statusCode());
                    return;
                }

                HolesailMC.LOGGER.info("Finished");

                Path downloadedZip = assetResponse.body();
                try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(downloadedZip))) {
                    while (zipInputStream.getNextEntry() != null) {
                        Files.createDirectories(GetBinaryDirectory());
                        Files.copy(zipInputStream, GetBinaryFile());
                    }

                    if (!GetBinaryFile().toFile().canExecute()) GetBinaryFile().toFile().setExecutable(true);
                    Files.deleteIfExists(HolesailMC.modDirectory.resolve(targetAsset));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    public ProcessHandle FindProcessFor(String key) {
        Stream<ProcessHandle> processes = ProcessHandle.allProcesses().filter(process -> process.info().commandLine().orElse("").equals(GetBinaryFile().toFile().getAbsolutePath() + " " + key));
        List<ProcessHandle> processHandleList = processes.toList();

        return !processHandleList.isEmpty() ? processHandleList.getFirst() : null;
    }

    public void StartProcessFor(String key) {
        ProcessBuilder processBuilder = new ProcessBuilder(GetBinaryFile().toFile().getAbsolutePath(), key).directory(GetBinaryDirectory().toFile()).redirectErrorStream(true);

        try {
            processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Holesail process:", e);
        }
    }
}
