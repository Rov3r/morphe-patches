package com.rov3r.extension.vsco;

import android.app.DownloadManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Runtime code injected into VSCO by the Morphe patch. */
@SuppressWarnings({"unused", "JavaReflectionMemberAccess"})
public final class DownloadPostsPatch {
    private static final String BUTTON_TAG = "rov3r_morphe_vsco_download_button";
    private static final String NETWORK_UTILITY = "co.vsco.vsn.utility.NetworkUtility";

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
        button.setContentDescription("Download post");
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
            DownloadResult result = queueDownload(view.getContext());
            String message = result.queued
                ? "Saved to Downloads/VSCO/" + result.username
                : "Couldn't download — media unavailable";
            Toast.makeText(view.getContext(), message, Toast.LENGTH_SHORT).show();
        });

        actionRow.addView(button, actionRow.indexOfChild(favoriteButton) + 1);
    }

    private static DownloadResult queueDownload(Context context) {
        Object model = currentModel.get();
        if (model == null) {
            return DownloadResult.failed();
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
                return DownloadResult.failed();
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

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle(fileName);
            request.setDescription("Saving to Downloads/VSCO/" + username);
            request.setMimeType(mimeType);
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            );
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "VSCO" + File.separator + username + File.separator + fileName
            );
            request.setAllowedOverRoaming(true);

            DownloadManager manager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager == null) {
                return DownloadResult.failed();
            }

            manager.enqueue(request);
            return DownloadResult.queued(username);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return DownloadResult.failed();
        }
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

    private static final class DownloadResult {
        private final boolean queued;
        private final String username;

        private DownloadResult(boolean queued, String username) {
            this.queued = queued;
            this.username = username;
        }

        private static DownloadResult failed() {
            return new DownloadResult(false, "");
        }

        private static DownloadResult queued(String username) {
            return new DownloadResult(true, username);
        }
    }
}
