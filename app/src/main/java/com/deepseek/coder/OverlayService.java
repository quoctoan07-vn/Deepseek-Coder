package com.deepseek.coder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.Locale;

public class OverlayService extends Service {

    private static final String CHANNEL_ID = "dc_session";
    private static final int NOTIFICATION_ID = 1001;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final SecureRandom random = new SecureRandom();

    private WindowManager windowManager;
    private ESPView espView;
    private FloatingMenu floatingMenu;
    private volatile boolean started;
    private Thread connectThread;
    private volatile int connectionGeneration = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        AppState.serviceRunning = true;
        AppState.serviceStarting = true;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createEspOverlay();

        floatingMenu = new FloatingMenu(this, windowManager);
        floatingMenu.setConnected(false);

        Prefs prefs = new Prefs(this);
        prefs.syncToNative();

        startConnection();
        startStatusLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        started = true;
        return START_NOT_STICKY;
    }

    private void startConnection() {
        final int generation = ++connectionGeneration;
        connectThread = new Thread(() -> {
            if (!RootManager.deployHelper(this)) {
                if (generation == connectionGeneration) {
                    fail("Cannot deploy root helper. Is root available?");
                }
                return;
            }

            for (int attempt = 0; attempt < 4 && AppState.serviceRunning; attempt++) {
                if (generation != connectionGeneration) return;

                int port = 39000 + random.nextInt(2000);
                String token = randomToken(48);

                if (!RootManager.startHelper(port, token)) {
                    sleep(350);
                    continue;
                }

                for (int i = 0; i < 24 && AppState.serviceRunning; i++) {
                    if (generation != connectionGeneration) return;
                    if (NativeBridge.connect("127.0.0.1", port, token)) {
                        if (generation != connectionGeneration) {
                            NativeBridge.disconnect();
                            return;
                        }
                        AppState.connected = true;
                        AppState.serviceStarting = false;
                        main.post(() -> {
                            if (floatingMenu != null) floatingMenu.setConnected(true);
                        });
                        return;
                    }
                    sleep(150);
                }

                RootManager.stopHelper();
                sleep(400);
            }

            if (generation == connectionGeneration) {
                fail("Root helper did not start.\n" + RootManager.getLastLog());
            }
        }, "dc-connect");
        connectThread.start();
    }

    public void restartSession() {
        AppState.serviceStarting = true;
        AppState.connected = false;
        AppState.inMatch = false;
        NativeBridge.disconnect();
        connectionGeneration++;

        new Thread(() -> {
            RootManager.stopHelper();
            sleep(300);
            startConnection();
        }, "dc-restart").start();
    }

    private void startStatusLoop() {
        main.post(new Runnable() {
            @Override
            public void run() {
                int[] s = NativeBridge.status();
                AppState.connected = s[0] == 1;
                AppState.gameRunning = s[1] == 1;
                AppState.inMatch = s[2] == 1;
                AppState.players = s[3];
                AppState.bots = s[4];
                if (AppState.connected) AppState.serviceStarting = false;

                if (floatingMenu != null) floatingMenu.updateStatus(s);

                if (AppState.serviceRunning) {
                    main.postDelayed(this, 600);
                }
            }
        });
    }

    private void createEspOverlay() {
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                flags,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            params.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        espView = new ESPView(this);
        try {
            windowManager.addView(espView, params);
        } catch (Throwable t) {
            fail("Overlay permission is required.");
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "DeepSeek Coder session", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the overlay session alive");
            channel.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) piFlags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, piFlags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        return builder
                .setContentTitle(Text.get(Text.MENU_TITLE))
                .setContentText("Session active · overlay running")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void fail(String message) {
        AppState.serviceStarting = false;
        main.post(() -> {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            stopSelf();
        });
    }

    private String randomToken(int length) {
        final char[] alphabet = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[random.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    @Override
    public void onDestroy() {
        connectionGeneration++;
        AppState.serviceRunning = false;
        AppState.serviceStarting = false;
        AppState.connected = false;
        AppState.gameRunning = false;
        AppState.inMatch = false;

        NativeBridge.disconnect();

        if (floatingMenu != null) {
            floatingMenu.destroy();
            floatingMenu = null;
        }
        if (espView != null && windowManager != null) {
            try { windowManager.removeView(espView); } catch (Throwable ignored) {}
            espView = null;
        }

        new Thread(RootManager::stopHelper, "dc-stop").start();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
