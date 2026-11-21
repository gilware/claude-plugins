// Starts the Bumpwise command line for the Claude Code plugin (https://gilware.com/bumpwise/cli/).
// Run as a single source file, so it needs only Java 17 or later: java Bumpwise.java mcp
//
// - Uses the jar the installer put in place (~/.local/share/bumpwise/bumpwise.jar, on Windows
//   %LOCALAPPDATA%\Bumpwise\bumpwise.jar), or the one named by BUMPWISE_JAR.
// - With no jar yet, downloads bumpwise-<VERSION>.jar from gilware.com once, checks its SHA-256 and saves it there.
//   Nothing else is downloaded and nothing is sent.
// - Then runs the jar in this same JVM. Messages go to stderr: stdout carries only MCP messages.

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

public class Bumpwise {

    static final String VERSION = "1.0.0";
    static final String SHA256 = "fc98e9affe8758c11ad2d6051b39fdca05ea8caf77f5633c7bbde5a9cdb57516";
    static final String URL = "https://gilware.com/bumpwise/cli/bumpwise-" + VERSION + ".jar";

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() < 17) {
            fail("Bumpwise needs Java 17 or later, and 'java' is version " + Runtime.version()
                    + ". Install a newer one (for example from https://adoptium.net) and restart Claude Code.");
        }
        var jar = jar();
        if (!Files.isRegularFile(jar)) {
            download(jar);
        }
        run(jar, args);
    }

    static Path jar() {
        var named = System.getenv("BUMPWISE_JAR");
        if (named != null && !named.isBlank()) {
            return Path.of(named);
        }
        var localAppData = System.getenv("LOCALAPPDATA");
        if (System.getProperty("os.name", "").startsWith("Windows") && localAppData != null) {
            return Path.of(localAppData, "Bumpwise", "bumpwise.jar");
        }
        return Path.of(System.getProperty("user.home"), ".local", "share", "bumpwise", "bumpwise.jar");
    }

    static void download(Path jar) throws IOException, InterruptedException {
        System.err.println("bumpwise: first start, downloading " + URL + " to " + jar);
        Files.createDirectories(jar.getParent());
        var part = jar.resolveSibling(".download.part");
        var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(20)).build();
        var request = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofMinutes(2))
                .header("User-Agent", "bumpwise-claude-plugin/" + VERSION).GET().build();
        HttpResponse<Path> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(part));
        } catch (IOException e) {
            Files.deleteIfExists(part);
            fail("the download from " + URL + " failed (" + e.getMessage() + "). Check the internet connection and restart Claude Code.");
            return;
        }
        if (response.statusCode() != 200) {
            Files.deleteIfExists(part);
            fail("the download from " + URL + " failed with HTTP " + response.statusCode() + ". Try again later.");
        }
        if (!SHA256.equals(sha256(part))) {
            Files.deleteIfExists(part);
            fail("the downloaded file is not the published one (SHA-256 mismatch). Nothing was changed; try again later.");
        }
        Files.move(part, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.err.println("bumpwise: installed " + VERSION);
    }

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            var digest = MessageDigest.getInstance("SHA-256");
            var buffer = new byte[65536];
            for (int n; (n = in.read(buffer)) > 0; ) {
                digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static void run(Path jar, String[] args) throws Exception {
        var loader = new URLClassLoader(new URL[] {jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
        try {
            loader.loadClass("dev.bumpwise.cli.Main").getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
            fail(jar + " is not the Bumpwise command line. Delete it, or set BUMPWISE_JAR, and restart Claude Code.");
        } catch (InvocationTargetException e) {
            throw e.getCause() instanceof Exception cause ? cause : e;
        }
    }

    static void fail(String message) {
        System.err.println("bumpwise: " + message);
        System.exit(2);
    }
}
