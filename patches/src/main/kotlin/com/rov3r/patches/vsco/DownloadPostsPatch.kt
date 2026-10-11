package com.rov3r.patches.vsco

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch

private const val EXTENSION_CLASS =
    "Lcom/rov3r/extension/vsco/DownloadPostsPatch;"
private const val SETTINGS_EXTENSION_CLASS =
    "Lcom/rov3r/extension/vsco/PatchSettings;"
private const val PROFILE_EXTENSION_CLASS =
    "Lcom/rov3r/extension/vsco/ProfilePhotoPatch;"

/** Installs the media-action icons consumed by the injected extension. */
private val downloadIconResourcePatch = resourcePatch {
    execute {
        get("res/drawable/morphe_vsco_download.xml", copy = false).writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/ds_color_primary"
                    android:fillType="evenOdd"
                    android:pathData="M5,18L5,21L19,21L19,18L21,18L21,23L3,23L3,18L5,18ZM13,1L13,14.168L17.293,9.876L18.707,11.29L12,17.997L5.293,11.29L6.707,9.876L11,14.168L11,1L13,1Z" />
            </vector>
            """.trimIndent()
        )
        get("res/drawable/morphe_vsco_copy.xml", copy = false).writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/ds_color_primary"
                    android:pathData="M16,1L4,1C2.9,1 2,1.9 2,3L2,17L4,17L4,3L16,3L16,1ZM19,5L8,5C6.9,5 6,5.9 6,7L6,21C6,22.1 6.9,23 8,23L19,23C20.1,23 21,22.1 21,21L21,7C21,5.9 20.1,5 19,5ZM19,21L8,21L8,7L19,7L19,21Z" />
            </vector>
            """.trimIndent()
        )
        get("res/drawable/morphe_vsco_share.xml", copy = false).writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="24dp"
                android:height="24dp"
                android:viewportWidth="24"
                android:viewportHeight="24">
                <path
                    android:fillColor="@color/ds_color_primary"
                    android:pathData="M18,16.08C17.24,16.08 16.56,16.38 16.04,16.85L8.91,12.7C8.96,12.47 9,12.24 9,12C9,11.76 8.96,11.53 8.91,11.3L15.96,7.19C16.5,7.69 17.21,8 18,8C19.66,8 21,6.66 21,5C21,3.34 19.66,2 18,2C16.34,2 15,3.34 15,5C15,5.24 15.04,5.47 15.09,5.7L8.04,9.81C7.5,9.31 6.79,9 6,9C4.34,9 3,10.34 3,12C3,13.66 4.34,15 6,15C6.79,15 7.5,14.69 8.04,14.19L15.16,18.35C15.11,18.56 15.08,18.78 15.08,19C15.08,20.61 16.39,21.92 18,21.92C19.61,21.92 20.92,20.61 20.92,19C20.92,17.39 19.61,16.08 18,16.08Z" />
            </vector>
            """.trimIndent()
        )
    }
}

@Suppress("unused")
val downloadPostsPatch = bytecodePatch(
    name = "Download posts",
    description = "Adds configurable original-quality downloads, profile tools, feed cleanup, media controls, and privacy options.",
    default = true
) {
    compatibleWith(VSCO_COMPATIBILITY)
    dependsOn(downloadIconResourcePatch)
    extendWith("extensions/vsco.mpe")

    execute {
        SetUpImageFingerprint.method.addInstruction(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->setCurrentModel(Ljava/lang/Object;)V"
        )

        SetupListenersFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $EXTENSION_CLASS->attachDownloadButton(Landroid/view/View;)V"
        )

        LithiumActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static { p0 }, $SETTINGS_EXTENSION_CLASS->install(Landroid/app/Activity;)V"
        )
        ProfileHeaderImageFingerprint.method.addInstruction(
            0,
            "invoke-static { p1, p2 }, $PROFILE_EXTENSION_CLASS->bindHeader(Ljava/lang/Object;Ljava/lang/Object;)V"
        )
        ProfileSpacesHeaderImageFingerprint.method.addInstruction(
            0,
            "invoke-static { p0, p1 }, $PROFILE_EXTENSION_CLASS->bindSpacesHeader(Ljava/lang/Object;Ljava/lang/Object;)V"
        )
    }
}
