package com.rov3r.patches.vsco

import app.morphe.patcher.Fingerprint

private const val MEDIA_DETAIL_VIEW = "Lcom/vsco/cam/detail/MediaDetailView;"
private const val IMAGE_MEDIA_MODEL =
    "Lco/vsco/vsn/response/models/media/image/ImageMediaModel;"

/** Locates the image-binding method used to retain the active post. */
internal object SetUpImageFingerprint : Fingerprint(
    definingClass = MEDIA_DETAIL_VIEW,
    name = "setUpImage",
    returnType = "V",
    parameters = listOf(IMAGE_MEDIA_MODEL)
)

internal object SetupListenersFingerprint : Fingerprint(
    definingClass = MEDIA_DETAIL_VIEW,
    name = "setupListeners",
    returnType = "V",
    parameters = emptyList()
)
