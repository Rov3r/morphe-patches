package com.rov3r.extension.vsco;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/** Runtime code injected into VSCO by the Morphe patch. */
@SuppressWarnings({"unused", "JavaReflectionMemberAccess", "deprecation"})
public final class DownloadPostsPatch {
    private static final String BUTTON_TAG = "rov3r_morphe_vsco_download_button";
    private static final String NETWORK_UTILITY = "co.vsco.vsn.utility.NetworkUtility";
    private static final String PREFERENCES = "rov3r_morphe_vsco";
    private static final String DESTINATION_KEY = "download_destination";
    private static final String PICKER_TAG = "rov3r_morphe_vsco_directory_picker";

    private static WeakReference<Object> currentModel = new WeakReference<>(null);

    private DownloadPostsPatch() {
    }

    /** Called at the start of MediaDetailView.setUpImage(ImageMediaModel). */
    public static void setCurrentModel(Object model) {
        currentModel = new WeakReference<>(model);
    }

    /** Called at the start of MediaDetailView.setupListeners(). */
    public static void attachDownloadButton(View detailView) {
        Context context = detailView.getContext();
        int favoriteId = resourceId(context, "id", "detail_view_favorite_button");
        View favoriteButton = detailView.findViewById(favoriteId);
        if (favoriteButton == null || !(favoriteButton.getParent() instanceof ViewGroup)) {
            return;
        }

        ViewGroup actionRow = (ViewGroup) favoriteButton.getParent();
        if (actionRow.findViewWithTag(BUTTON_TAG) != null) {
            return;
        }

        int iconId = resourceId(context, "drawable", "morphe_vsco_download");
        if (iconId == 0) {
            return;
        }

        ImageButton button = new ImageButton(context);
        button.setTag(BUTTON_TAG);
        button.setContentDescription("Download post. Long press to change save folder.");
        button.setImageResource(iconId);
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setLayoutParams(copyLayoutParams(favoriteButton.getLayoutParams()));

        int forwardId = resourceId(context, "id", "detail_view_forward_button");
        View forwardButton = detailView.findViewById(forwardId);
        if (forwardButton != null) {
            button.setPadding(
                forwardButton.getPaddingLeft(),
                forwardButton.getPaddingTop(),
                forwardButton.getPaddingRight(),
                forwardButton.getPaddingBottom()
            );
        }

        button.setOnClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            Context viewContext = view.getContext();
            Uri destination = savedDestination(viewContext);
            if (destination == null) {
                chooseDestination(viewContext, true);
                return;
            }
            queueDownload(viewContext, destination);
        });
        button.setOnLongClickListener(view -> {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            chooseDestination(view.getContext(), false);
            return true;
        });

        actionRow.addView(button, actionRow.indexOfChild(favoriteButton) + 1);
    }

    private static void chooseDestination(Context context, boolean downloadAfterSelection) {
        Activity activity = findActivity(context);
        if (activity == null) {
            showToast(context, "Couldn't open the folder picker");
            return;
        }

        if (activity.getFragmentManager().findFragmentByTag(PICKER_TAG) != null) {
            return;
        }

        DirectoryPickerFragment picker = DirectoryPickerFragment.create(downloadAfterSelection);
        activity.getFragmentManager()
            .beginTransaction()
            .add(picker, PICKER_TAG)
            .commitAllowingStateLoss();
    }

    private static Uri savedDestination(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
        String value = preferences.getString(DESTINATION_KEY, null);
        if (value == null || value.isEmpty()) {
            return null;
        }

        Uri uri = Uri.parse(value);
        List<android.content.UriPermission> permissions =
            context.getContentResolver().getPersistedUriPermissions();
        for (android.content.UriPermission permission : permissions) {
            if (permission.isReadPermission()
                && permission.isWritePermission()
                && uri.equals(permission.getUri())) {
                return uri;
            }
        }

        preferences.edit().remove(DESTINATION_KEY).apply();
        return null;
    }

    private static void queueDownload(Context context, Uri destination) {
        final DownloadInfo info = currentDownloadInfo();
        if (info == null) {
            showToast(context, "Couldn't download - media unavailable");
            return;
        }

        final Context appContext = context.getApplicationContext();
        showToast(appContext, "Downloading " + info.fileName);
        new Thread(() -> {
            boolean saved = downloadToFolder(appContext, destination, info);
            showToast(
                appContext,
                saved ? "Saved " + info.fileName : "Couldn't download " + info.fileName
            );
        }, "Morphe-VSCO-Download").start();
    }

    private static DownloadInfo currentDownloadInfo() {
        Object model = currentModel.get();
        if (model == null) {
            return null;
        }

        try {
            boolean isVideo = (boolean) invoke(model, "isDsco");
            String mediaId = safeFilePart((String) invoke(model, "getIdStr"));
            String sourceUrl = (String) invoke(
                model,
                isVideo ? "getDscoUrl" : "getResponsiveImageUrl"
            );
            String downloadUrl = makeAbsoluteMediaUrl(sourceUrl, isVideo);
            if (downloadUrl == null || downloadUrl.isEmpty()) {
                return null;
            }

            Object owner = invoke(model, "getOwnerSiteData");
            String username = safeFilePart(readString(owner, "username", "getUsername"));
            if (username.isEmpty()) {
                username = "unknown";
            }

            String extension = isVideo ? ".mp4" : imageExtension(downloadUrl);
            String mimeType = isVideo ? "video/mp4" : imageMimeType(extension);
            String fileName = "vsco_" + (mediaId.isEmpty() ? "post" : mediaId)
                + "_" + System.currentTimeMillis() + extension;
            return new DownloadInfo(downloadUrl, username, fileName, mimeType);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean downloadToFolder(Context context, Uri treeUri, DownloadInfo info) {
        ContentResolver resolver = context.getContentResolver();
        Uri outputUri = null;
        HttpURLConnection connection = null;
        try {
            Uri root = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            );
            Uri userFolder = findOrCreateDirectory(resolver, root, info.username);
            if (userFolder == null) {
                return false;
            }

            connection = (HttpURLConnection) new URL(info.url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) VSCO");
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return false;
            }

            outputUri = DocumentsContract.createDocument(
                resolver,
                userFolder,
                info.mimeType,
                info.fileName
            );
            if (outputUri == null) {
                return false;
            }

            try (
                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream rawOutput = resolver.openOutputStream(outputUri, "w");
                OutputStream output = rawOutput == null ? null : new BufferedOutputStream(rawOutput)
            ) {
                if (output == null) {
                    DocumentsContract.deleteDocument(resolver, outputUri);
                    return false;
                }
                byte[] buffer = new byte[32768];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
            }
            return true;
        } catch (IOException | RuntimeException ignored) {
            if (outputUri != null) {
                try {
                    DocumentsContract.deleteDocument(resolver, outputUri);
                } catch (Exception ignoredDeleteFailure) {
                    // The provider may already have removed an incomplete document.
                }
            }
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Uri findOrCreateDirectory(ContentResolver resolver, Uri parent, String name)
        throws IOException {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(
            parent,
            DocumentsContract.getDocumentId(parent)
        );
        String[] projection = {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        try (Cursor cursor = resolver.query(children, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (name.equals(cursor.getString(1))
                        && DocumentsContract.Document.MIME_TYPE_DIR.equals(cursor.getString(2))) {
                        return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0));
                    }
                }
            }
        }
        return DocumentsContract.createDocument(
            resolver,
            parent,
            DocumentsContract.Document.MIME_TYPE_DIR,
            name
        );
    }

    private static Activity findActivity(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity) {
                return (Activity) current;
            }
            Context next = ((ContextWrapper) current).getBaseContext();
            if (next == current) {
                break;
            }
            current = next;
        }
        return current instanceof Activity ? (Activity) current : null;
    }

    private static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(
            () -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }

    private static String makeAbsoluteMediaUrl(String sourceUrl, boolean isVideo)
        throws ReflectiveOperationException {
        if (sourceUrl == null || sourceUrl.isEmpty()) {
            return null;
        }

        Class<?> utilityClass = Class.forName(NETWORK_UTILITY);
        Field instanceField = utilityClass.getField("INSTANCE");
        Object utility = instanceField.get(null);
        Method method = utilityClass.getMethod(
            isVideo ? "getVideoUrl" : "getFullResImgixImageUrl",
            String.class
        );
        return (String) method.invoke(utility, sourceUrl);
    }

    private static Object invoke(Object target, String methodName)
        throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static String readString(Object target, String fieldName, String getterName)
        throws ReflectiveOperationException {
        if (target == null) {
            return "";
        }

        try {
            Field field = target.getClass().getField(fieldName);
            Object value = field.get(target);
            return value instanceof String ? (String) value : "";
        } catch (NoSuchFieldException ignored) {
            Object value = invoke(target, getterName);
            return value instanceof String ? (String) value : "";
        }
    }

    private static int resourceId(Context context, String type, String name) {
        return context.getResources().getIdentifier(name, type, context.getPackageName());
    }

    private static ViewGroup.LayoutParams copyLayoutParams(ViewGroup.LayoutParams original) {
        if (original instanceof LinearLayout.LayoutParams) {
            return new LinearLayout.LayoutParams((LinearLayout.LayoutParams) original);
        }
        if (original instanceof RelativeLayout.LayoutParams) {
            return new RelativeLayout.LayoutParams((RelativeLayout.LayoutParams) original);
        }
        if (original instanceof FrameLayout.LayoutParams) {
            return new FrameLayout.LayoutParams((FrameLayout.LayoutParams) original);
        }
        if (original instanceof ViewGroup.MarginLayoutParams) {
            return new ViewGroup.MarginLayoutParams((ViewGroup.MarginLayoutParams) original);
        }
        return new ViewGroup.LayoutParams(original);
    }

    private static String safeFilePart(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String imageExtension(String url) {
        String lower = url.toLowerCase();
        if (lower.contains(".png")) {
            return ".png";
        }
        if (lower.contains(".webp")) {
            return ".webp";
        }
        return ".jpg";
    }

    private static String imageMimeType(String extension) {
        if (".png".equals(extension)) {
            return "image/png";
        }
        if (".webp".equals(extension)) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static final class DownloadInfo {
        private final String url;
        private final String username;
        private final String fileName;
        private final String mimeType;

        private DownloadInfo(String url, String username, String fileName, String mimeType) {
            this.url = url;
            this.username = username;
            this.fileName = fileName;
            this.mimeType = mimeType;
        }
    }

    public static final class DirectoryPickerFragment extends Fragment {
        private static final int REQUEST_DIRECTORY = 9827;
        private static final String DOWNLOAD_AFTER_SELECTION = "download_after_selection";
        private boolean pickerStarted;

        static DirectoryPickerFragment create(boolean downloadAfterSelection) {
            DirectoryPickerFragment fragment = new DirectoryPickerFragment();
            Bundle arguments = new Bundle();
            arguments.putBoolean(DOWNLOAD_AFTER_SELECTION, downloadAfterSelection);
            fragment.setArguments(arguments);
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        @Override
        public void onResume() {
            super.onResume();
            if (pickerStarted) {
                return;
            }
            pickerStarted = true;
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            );
            startActivityForResult(intent, REQUEST_DIRECTORY);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            Activity activity = getActivity();
            if (activity == null || requestCode != REQUEST_DIRECTORY) {
                removeSelf();
                return;
            }

            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                Uri destination = data.getData();
                int flags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                try {
                    activity.getContentResolver().takePersistableUriPermission(destination, flags);
                    activity.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                        .edit()
                        .putString(DESTINATION_KEY, destination.toString())
                        .apply();
                    showToast(activity, "Download folder saved");
                    Bundle arguments = getArguments();
                    if (arguments != null && arguments.getBoolean(DOWNLOAD_AFTER_SELECTION, false)) {
                        queueDownload(activity, destination);
                    }
                } catch (SecurityException ignored) {
                    showToast(activity, "Couldn't save access to that folder");
                }
            }
            removeSelf();
        }

        private void removeSelf() {
            if (isAdded()) {
                getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
            }
        }
    }
}
