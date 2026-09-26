package app.adm.extension.media;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.widget.PopupMenu;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

public final class MediaGrabber {
    private static final int MENU_ID = 0x4D454449;
    private static final int MAX_CANDIDATES = 100;
    private static final Map<WebView, PageState> STATES =
            Collections.synchronizedMap(new WeakHashMap<WebView, PageState>());
    private static volatile WeakReference<WebView> lastWebView = new WeakReference<WebView>(null);
    private static volatile WeakReference<Activity> pendingActivity = new WeakReference<Activity>(null);

    private MediaGrabber() {
    }

    public static void onPageStarted(WebView webView, String url) {
        if (webView == null) {
            return;
        }
        PageState state = stateFor(webView);
        synchronized (state) {
            state.pageUrl = url;
            state.candidates.clear();
        }
        lastWebView = new WeakReference<WebView>(webView);
        Activity activity = activityOf(webView.getContext());
        if (activity != null) {
            pendingActivity = new WeakReference<Activity>(activity);
        }
    }

    private static Activity activityOf(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == null || base == context) {
                break;
            }
            context = base;
        }
        return null;
    }

    public static void onRequest(WebView webView, String url) {
        if (webView == null || url == null) {
            return;
        }
        lastWebView = new WeakReference<WebView>(webView);
        MediaKind kind = classify(url);
        if (kind == null) {
            return;
        }
        PageState state = stateFor(webView);
        synchronized (state) {
            if (state.candidates.containsKey(url)) {
                return;
            }
            state.candidates.put(url, createCandidate(url, kind));
            while (state.candidates.size() > MAX_CANDIDATES) {
                String oldest = state.candidates.keySet().iterator().next();
                state.candidates.remove(oldest);
            }
        }
    }

    public static void onPopupShown(Object host) {
        if (!(host instanceof PopupMenu)) {
            return;
        }
        Menu menu = ((PopupMenu) host).getMenu();
        if (menu == null || menu.findItem(MENU_ID) != null) {
            return;
        }
        MenuItem item = menu.add(0, MENU_ID, 0, "Media grabber");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        Activity activity = pendingActivity.get();
        if (activity != null) {
            Toast.makeText(activity, "Media grabber menu hook fired", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean onPopupItemClick(Object host, MenuItem item) {
        if (item == null || item.getItemId() != MENU_ID) {
            return false;
        }
        Activity activity = pendingActivity.get();
        if (activity == null) {
            return true;
        }
        showCandidates(activity);
        return true;
    }

    private static PageState stateFor(WebView webView) {
        synchronized (STATES) {
            PageState state = STATES.get(webView);
            if (state == null) {
                state = new PageState();
                STATES.put(webView, state);
            }
            return state;
        }
    }

    private static void showCandidates(Activity activity) {
        WebView webView = lastWebView.get();
        List<Candidate> candidates = new ArrayList<Candidate>();
        if (webView != null) {
            PageState state = stateFor(webView);
            synchronized (state) {
                candidates.addAll(state.candidates.values());
            }
        }
        if (candidates.isEmpty()) {
            new AlertDialog.Builder(activity)
                    .setTitle("Media grabber")
                    .setMessage("No direct video or subtitle URLs were observed on this page.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String[] labels = new String[candidates.size()];
        for (int index = 0; index < candidates.size(); index++) {
            labels[index] = candidates.get(index).label;
        }
        final WebView selectedWebView = webView;
        new AlertDialog.Builder(activity)
                .setTitle("Media grabber")
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Candidate candidate = candidates.get(which);
                        if (candidate.kind.direct) {
                            enqueue(activity, selectedWebView, candidate);
                        } else {
                            Toast.makeText(activity, candidate.label + " (resolver pending)", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static void enqueue(Activity activity, WebView webView, Candidate candidate) {
        DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager == null) {
            Toast.makeText(activity, "Download service is unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(candidate.url));
            String cookies = CookieManager.getInstance().getCookie(candidate.url);
            if (cookies != null && !cookies.isEmpty()) {
                request.addRequestHeader("Cookie", cookies);
            }
            if (webView != null) {
                String userAgent = webView.getSettings().getUserAgentString();
                if (userAgent != null && !userAgent.isEmpty()) {
                    request.addRequestHeader("User-Agent", userAgent);
                }
            }
            String filename = candidate.filename;
            request.setTitle(filename);
            request.setDescription("ADM media grabber");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, filename);
            manager.enqueue(request);
            Toast.makeText(activity, "Queued " + filename, Toast.LENGTH_SHORT).show();
        } catch (RuntimeException exception) {
            Toast.makeText(activity, "Could not queue " + candidate.filename, Toast.LENGTH_SHORT).show();
        }
    }

    private static MediaKind classify(String url) {
        Uri uri;
        try {
            uri = Uri.parse(url);
        } catch (RuntimeException exception) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null) {
            return null;
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return null;
        }
        String path = uri.getPath();
        if (path == null) {
            return null;
        }
        String lowerPath = path.toLowerCase(Locale.ROOT);
        if (lowerPath.endsWith(".m3u8")) {
            return MediaKind.HLS;
        }
        if (lowerPath.endsWith(".mpd")) {
            return MediaKind.DASH;
        }
        if (lowerPath.endsWith(".vtt") || lowerPath.endsWith(".srt")) {
            return MediaKind.SUBTITLE;
        }
        if (lowerPath.endsWith(".mp4") || lowerPath.endsWith(".m4v") || lowerPath.endsWith(".webm")
                || lowerPath.endsWith(".mkv") || lowerPath.endsWith(".mov")) {
            return MediaKind.VIDEO;
        }
        if (lowerPath.endsWith(".aac") || lowerPath.endsWith(".mp3") || lowerPath.endsWith(".m4a")
                || lowerPath.endsWith(".ogg") || lowerPath.endsWith(".flac")) {
            return MediaKind.AUDIO;
        }
        if (lowerPath.endsWith(".h264") || lowerPath.endsWith(".264") || lowerPath.endsWith(".m4s")) {
            return MediaKind.RAW;
        }
        return null;
    }

    private static Candidate createCandidate(String url, MediaKind kind) {
        Uri uri = Uri.parse(url);
        String segment = uri.getLastPathSegment();
        if (segment == null || segment.isEmpty()) {
            segment = kind.name().toLowerCase(Locale.ROOT);
        }
        String filename = sanitize(segment);
        return new Candidate(url, kind, filename, labelFor(kind, filename));
    }

    private static String labelFor(MediaKind kind, String filename) {
        if (kind == MediaKind.VIDEO) {
            return "Video: " + filename + " (size unknown)";
        }
        if (kind == MediaKind.SUBTITLE) {
            return "Subtitles: " + filename + " (size unknown)";
        }
        if (kind == MediaKind.AUDIO) {
            return "Audio: " + filename + " (size unknown)";
        }
        if (kind == MediaKind.HLS) {
            return "HLS: " + filename + " (resolver pending)";
        }
        if (kind == MediaKind.DASH) {
            return "DASH: " + filename + " (resolver pending)";
        }
        return "Elementary stream: " + filename + " (resolver pending)";
    }

    private static String sanitize(String value) {
        String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isEmpty() ? "media" : sanitized;
    }

    private enum MediaKind {
        VIDEO(true),
        SUBTITLE(true),
        AUDIO(true),
        HLS(false),
        DASH(false),
        RAW(false);

        private final boolean direct;

        MediaKind(boolean direct) {
            this.direct = direct;
        }
    }

    private static final class Candidate {
        private final String url;
        private final MediaKind kind;
        private final String filename;
        private final String label;

        private Candidate(String url, MediaKind kind, String filename, String label) {
            this.url = url;
            this.kind = kind;
            this.filename = filename;
            this.label = label;
        }
    }

    private static final class PageState {
        private String pageUrl;
        private final LinkedHashMap<String, Candidate> candidates = new LinkedHashMap<String, Candidate>();
    }
}
