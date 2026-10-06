package com.rov3r.patches.vsco

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch

private const val EXTENSION_CLASS =
    "Lcom/rov3r/extension/vsco/DownloadPostsPatch;"

/** Installs the download icon consumed by the injected extension. */
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
    }
}

@Suppress("unused")
val downloadPostsPatch = bytecodePatch(
    name = "Download posts",
    description = "Adds a button that downloads VSCO images and DSCO videos at their original quality.",
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
    }
}
