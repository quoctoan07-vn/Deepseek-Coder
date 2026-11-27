package com.deepseek.coder;

import android.content.Context;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class RootManager {
    public static final String TMP_BIN = "/data/local/tmp/dc_root";
    public static final String TMP_LOG = "/data/local/tmp/dc_root.log";
    public static final String GAME_PACKAGE = "com.dts.freefiremax";

    private static final String GAME_FILES =
            "/storage/emulated/0/Android/data/" + GAME_PACKAGE + "/files";
    private static final String LOCAL_CONFIG = GAME_FILES + "/localConfig.json";

    private static final Object LOCK = new Object();

    private RootManager() {}

    public static boolean isRootAvailable() {
        String out = run("id");
        return out.contains("uid=0");
    }

    public static boolean isGameRunning() {
        String out = run("pidof " + GAME_PACKAGE + " 2>/dev/null");
        return !out.trim().isEmpty();
    }

    public static boolean isHelperRunning() {
        String out = run("pidof dc_root 2>/dev/null");
        return !out.trim().isEmpty();
    }

    public static boolean deployHelper(Context context) {
        String source = context.getApplicationInfo().nativeLibraryDir + "/libdcroot.so";
        if (!new File(source).exists()) {
            return false;
        }
        String cmd = "pkill -f '^" + TMP_BIN + "' >/dev/null 2>&1; killall dc_root >/dev/null 2>&1; sleep 1; "
                + "cp -f '" + source + "' '" + TMP_BIN + "' && chmod 755 '" + TMP_BIN + "' && echo DC_OK";
        return run(cmd).contains("DC_OK");
    }

    public static boolean startHelper(int port, String token) {
        String cmd = "nohup " + TMP_BIN + " --port=" + port + " --token=" + token
                + " --game=" + GAME_PACKAGE
                + " > " + TMP_LOG + " 2>&1 & echo DC_STARTED";
        String out = run(cmd);
        if (!out.contains("DC_STARTED")) return false;
        sleep(180);
        return isHelperRunning();
    }

    public static void stopHelper() {
        run("pkill -f '^" + TMP_BIN + "' >/dev/null 2>&1; killall dc_root >/dev/null 2>&1; true");
    }

    public static void launchGame() {
        String out = run("am start -n " + GAME_PACKAGE + "/com.dts.freefireth.FFMainActivity");
        if (!out.contains("Starting") && !out.contains("Warning")) {
            run("monkey -p " + GAME_PACKAGE + " -c android.intent.category.LAUNCHER 1");
        }
    }

    /**
     * Resets the guest account through the game's own SDK path instead of
     * clearing app data. The game reads localConfig.json at startup and, when
     * resetGuestBeforeLogin is set, calls GarenaMSDKMgr.ResetGuest() before a
     * guest login — exactly the built-in "Delete guest account" flow.
     *
     * The flag is removed again after startup so later launches are normal.
     */
    public static boolean resetGuestAccount() {
        String original = null;
        boolean existed = false;
        boolean wrote = false;

        try {
            original = readLocalConfig();
            existed = original != null;

            if (!writeLocalConfig(mergeResetFlag(existed ? original : "{}"))) {
                return false;
            }
            wrote = true;

            run("am force-stop " + GAME_PACKAGE);
            sleep(800);
            launchGame();

            for (int i = 0; i < 20 && !isGameRunning(); i++) {
                sleep(500);
            }
            sleep(10000); // let Unity read localConfig.json and reach the login UI
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (wrote) {
                try {
                    if (existed) {
                        writeLocalConfig(original);
                    } else {
                        run("rm -f '" + LOCAL_CONFIG + "'");
                    }
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String readLocalConfig() {
        String out = run("cat '" + LOCAL_CONFIG + "' 2>/dev/null");
        return out.trim().isEmpty() ? null : out;
    }

    private static boolean writeLocalConfig(String content) {
        String b64 = Base64.encodeToString(
                content.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        String tmp = LOCAL_CONFIG + ".tmp";
        String cmd = "mkdir -p '" + GAME_FILES + "'; "
                + "echo " + b64 + " | base64 -d > '" + tmp + "' && "
                + "OWNER=$(stat -c %u:%g '" + GAME_FILES + "') && "
                + "chown $OWNER '" + tmp + "' && chmod 660 '" + tmp + "' && "
                + "mv -f '" + tmp + "' '" + LOCAL_CONFIG + "' && echo DC_CONFIG_OK";
        return run(cmd).contains("DC_CONFIG_OK");
    }

    private static String mergeResetFlag(String json) {
        String body = json == null ? "{}" : json.trim();
        if (body.isEmpty() || body.charAt(0) != '{') body = "{}";

        body = body.replaceAll("(?i)\"resetGuest(BeforeLogin)?\"\\s*:\\s*(true|false)\\s*,?", "");
        body = body.replaceAll(",\\s*,", ",");
        body = body.replaceAll("\\{\\s*,", "{");
        body = body.replaceAll(",\\s*\\}", "}");

        int close = body.lastIndexOf('}');
        if (close < 0) return "{\"resetGuestBeforeLogin\":true}";

        String head = body.substring(0, close).trim();
        if (!head.equals("{") && !head.endsWith(",")) head += ",";
        return head + "\"resetGuestBeforeLogin\":true" + body.substring(close);
    }

    public static String getLastLog() {
        String out = run("tail -n 40 " + TMP_LOG + " 2>/dev/null");
        return out.trim();
    }

    public static String run(String command) {
        synchronized (LOCK) {
            Process process = null;
            StringBuilder out = new StringBuilder();
            try {
                process = new ProcessBuilder("su", "-c", command)
                        .redirectErrorStream(true)
                        .start();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) {
                    out.append(line).append('\n');
                }
                reader.close();
                process.waitFor();
            } catch (Exception e) {
                out.append("ERROR: ").append(e.getClass().getSimpleName());
                if (e.getMessage() != null) out.append(": ").append(e.getMessage());
            } finally {
                if (process != null) process.destroy();
            }
            return out.toString();
        }
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
