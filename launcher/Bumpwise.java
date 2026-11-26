// Starts the Bumpwise command line for the Claude Code plugin (https://gilware.com/bumpwise/cli/).
// Run as a single source file, so it needs only Java 17 or later: java Bumpwise.java mcp
//
// - Uses the jar the installer put in place (~/.local/share/bumpwise/bumpwise.jar, on Windows
//   %LOCALAPPDATA%\Bumpwise\bumpwise.jar), or the one named by BUMPWISE_JAR.
// - With no jar yet, or an older one than VERSION, downloads bumpwise-<VERSION>.jar from gilware.com, checks its
//   SHA-256 and saves it there (a jar named by BUMPWISE_JAR is never replaced). Nothing else is downloaded and
//   nothing is sent. If that download fails, an older jar that is already there is still used.
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
import java.util.jar.JarFile;

public class Bumpwise {

    static final String VERSION = "1.1.3";
    static final String SHA256 = "dd159161b04fb518a3ec6adaeeb975d6b7231784825e259415fa26eeb0b1160c";
    static final String URL = "https://gilware.com/bumpwise/cli/bumpwise-" + VERSION + ".jar";

    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() < 17) {
            fail("Bumpwise needs Java 17 or later, and 'java' is version " + Runtime.version()
                    + ". Install a newer one (for example from https://adoptium.net) and restart Claude Code.");
        }
        var jar = jar();
        if (!Files.isRegularFile(jar)) {
            System.err.println("bumpwise: first start, downloading " + URL + " to " + jar);
            var problem = download(jar);
            if (problem != null) {
                fail(problem);
            }
        } else if (System.getenv("BUMPWISE_JAR") == null || System.getenv("BUMPWISE_JAR").isBlank()) {
            var installed = version(jar);
            if (installed != null && older(installed, VERSION)) {
                System.err.println("bumpwise: updating " + jar + " from " + installed + " to " + VERSION);
                var problem = download(jar);
                if (problem != null) {
                    System.err.println("bumpwise: " + problem + " Using " + installed + " for now.");
                }
            }
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

    /** The Implementation-Version of a jar, or null when it has none or cannot be read. */
    static String version(Path jar) {
        try (var file = new JarFile(jar.toFile())) {
            var manifest = file.getManifest();
            return manifest == null ? null : manifest.getMainAttributes().getValue("Implementation-Version");
        } catch (IOException e) {
            return null;
        }
    }

    /** Whether dotted version {@code a} (1.1.0) is older than {@code b}; anything that is not plain numbers is not older. */
    static boolean older(String a, String b) {
        var x = a.trim().split("\\.");
        var y = b.trim().split("\\.");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p, q;
            try {
                p = i < x.length ? Integer.parseInt(x[i]) : 0;
                q = i < y.length ? Integer.parseInt(y[i]) : 0;
            } catch (NumberFormatException e) {
                return false;
            }
            if (p != q) {
                return p < q;
            }
        }
        return false;
    }

    /** Downloads the pinned jar to {@code jar}; returns null when done, or what went wrong (nothing is changed then). */
    static String download(Path jar) throws IOException, InterruptedException {
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
            return "the download from " + URL + " failed (" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()) + "). Check the internet connection and restart Claude Code.";
        }
        if (response.statusCode() != 200) {
            Files.deleteIfExists(part);
            return "the download from " + URL + " failed with HTTP " + response.statusCode() + ". Try again later.";
        }
        if (!SHA256.equals(sha256(part))) {
            Files.deleteIfExists(part);
            return "the downloaded file is not the published one (SHA-256 mismatch). Nothing was changed; try again later.";
        }
        Files.move(part, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        System.err.println("bumpwise: installed " + VERSION);
        return null;
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
