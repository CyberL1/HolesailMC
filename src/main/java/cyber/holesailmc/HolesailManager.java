package cyber.holesailmc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
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

        InputStream releaseManifestInputStream = HolesailMC.class.getResourceAsStream("/assets/" + HolesailMC.MOD_ID + "/release-manifest.json");

        if (releaseManifestInputStream == null) {
            throw new RuntimeException("releaseManifestInputStream == null");
        }

        InputStreamReader releaseManifestReader = new InputStreamReader(releaseManifestInputStream, StandardCharsets.UTF_8);
        JsonObject releaseManifest = JsonParser.parseReader(releaseManifestReader).getAsJsonObject();

        String targetAsset = String.format("%s-%s.zip", System.getProperty("os.name"), System.getProperty("os.arch")).toLowerCase().replace("amd", "x");
        HolesailMC.LOGGER.info("Downloading {}", targetAsset);

        HttpRequest assetDownloadRequest = HttpRequest.newBuilder().GET().uri(URI.create(String.format("https://github.com/holesail/holesail/releases/download/%s/%s", releaseManifest.get("tag").getAsString(), targetAsset))).header("User-Agent", "HolesailMC/1.0").build();

        return httpClient.sendAsync(assetDownloadRequest, HttpResponse.BodyHandlers.ofFile(HolesailMC.modDirectory.resolve(targetAsset))).thenAcceptAsync(assetResponse -> {
            try {
                if (assetResponse.statusCode() != 200) {
                    throw new RuntimeException("Status not 200: " + assetResponse.statusCode());
                }

                Path downloadedZip = assetResponse.body();

                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

                byte[] bytes = Files.readAllBytes(downloadedZip);
                byte[] digestedBytes = messageDigest.digest(bytes);

                String zipChecksum = HexFormat.of().formatHex(digestedBytes);

                HolesailMC.LOGGER.info("Verifying checksum: {}", zipChecksum);

                String expectedChecksum = releaseManifest.get("checksums").getAsJsonObject().get(System.getProperty("os.name").toLowerCase()).getAsJsonObject().get(System.getProperty("os.arch").toLowerCase()).getAsString();

                if (!Objects.equals(expectedChecksum, zipChecksum)) {
                    Files.deleteIfExists(HolesailMC.modDirectory.resolve(targetAsset));
                    throw new RuntimeException("Checksum mismatch: expected: " + expectedChecksum + ", got: " + zipChecksum);
                }

                ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(downloadedZip));

                while (zipInputStream.getNextEntry() != null) {
                    Files.createDirectories(GetBinaryDirectory());
                    Files.copy(zipInputStream, GetBinaryFile());
                }

                if (!GetBinaryFile().toFile().canExecute()) GetBinaryFile().toFile().setExecutable(true);
                Files.deleteIfExists(HolesailMC.modDirectory.resolve(targetAsset));
            } catch (NoSuchAlgorithmException | IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public List<ProcessHandle> GetProcesses() {
        Stream<ProcessHandle> processes = ProcessHandle.allProcesses().filter(process -> process.info().command().orElse("").equals(GetBinaryFile().toFile().getAbsolutePath()));
        return processes.toList();
    }

    public ProcessHandle FindProcessFor(String key) {
        List<ProcessHandle> allProcessList = GetProcesses();

        List<ProcessHandle> processHandleList = allProcessList.stream().filter(process -> process.info().commandLine().orElse("").split(" ")[1].equals(key)).toList();
        return !processHandleList.isEmpty() ? processHandleList.getFirst() : null;
    }

    public Process StartProcessFor(String key) throws IOException {
        ProcessBuilder processBuilder;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            processBuilder = new ProcessBuilder(GetBinaryFile().toFile().getAbsolutePath(), key, "--port", String.valueOf(serverSocket.getLocalPort())).directory(GetBinaryDirectory().toFile());
        }

        try {
            return processBuilder.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start Holesail process:", e);
        }
    }

    public int ExtractPort(String commandLine) {
        return Integer.parseInt(commandLine.split("--port")[1].trim().replace("]", ""));
    }

    public String ExtractConnector(String commandLine) {
        return "hs://" + commandLine.split("hs://")[1].replace("--port", "").replace("]","").split(" ")[0];
    }
}
