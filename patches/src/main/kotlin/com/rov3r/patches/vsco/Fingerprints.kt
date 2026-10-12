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

internal object ShowImageMenuFingerprint : Fingerprint(
    definingClass = MEDIA_DETAIL_VIEW,
    name = "showImageMenu",
    returnType = "V",
    parameters = listOf("Lcom/vsco/cam/bottommenu/BottomMenuState;")
)

internal object LithiumActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/navigation/LithiumActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;")
)

/** Stable protobuf access getters used by VSCO's preset and tool catalog. */
internal object PresetEntitlementAccessFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/proto/studio/PresetEntitlement;",
    name = "getAccess",
    returnType = "Z",
    parameters = emptyList()
)

internal object ToolEntitlementAccessFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/proto/studio/ToolEntitlement;",
    name = "getAccess",
    returnType = "Z",
    parameters = emptyList()
)

internal object AssemblageLaunchMemberFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/studio/AssemblageLaunchArgs;",
    name = "isMember",
    returnType = "Z",
    parameters = emptyList()
)

internal object EffectUtilsMemberOnlyToolFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/effects/EffectUtils;",
    name = "isMemberOnlyImageTool",
    returnType = "Z",
    parameters = listOf("Ljava/lang/String;")
)

internal object PaywallMembershipFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/subscription/upsell/PaywallState;",
    name = "isMembershipActivated",
    returnType = "Z",
    parameters = emptyList()
)

internal object CellPaywallMembershipFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/subscription/upsell/CellPaywallState;",
    name = "isMembershipActivated",
    returnType = "Z",
    parameters = emptyList()
)

private const val SUBSCRIPTION_CODE =
    "Lco/vsco/vsn/response/models/site/SubscriptionCode;"
private const val SUBSCRIPTION_CODE_HELPERS =
    "Lco/vsco/vsn/response/models/site/SubscriptionCodeKt;"

internal object SubscriptionCodePlusFingerprint : Fingerprint(
    definingClass = SUBSCRIPTION_CODE_HELPERS,
    name = "isPlus",
    returnType = "Z",
    parameters = listOf(SUBSCRIPTION_CODE)
)

internal object SubscriptionCodeProFingerprint : Fingerprint(
    definingClass = SUBSCRIPTION_CODE_HELPERS,
    name = "isPro",
    returnType = "Z",
    parameters = listOf(SUBSCRIPTION_CODE)
)

internal object SubscriptionCodeOneFingerprint : Fingerprint(
    definingClass = SUBSCRIPTION_CODE_HELPERS,
    name = "isOne",
    returnType = "Z",
    parameters = listOf(SUBSCRIPTION_CODE)
)

internal object DraftProLockFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/proto/studio/Draft;",
    name = "getIsLockedForContainingProEntitlement",
    returnType = "Z",
    parameters = emptyList()
)

internal object ProfileHeaderImageFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/profile/profiles/views/ProfileHeaderDelegate;",
    name = "setHeaderProfileImage",
    returnType = "V",
    parameters = listOf(
        "Lcom/vsco/cam/profile/profiles/views/ProfileHeaderDelegate\$ProfileHeaderViewHolder;",
        "Lcom/vsco/cam/account/UserModel;"
    )
)

internal object ProfileSpacesHeaderImageFingerprint : Fingerprint(
    definingClass = "Lcom/vsco/cam/profile/profiles/views/ProfileSpacesTabHeaderCardView;",
    name = "setHeaderProfileImage",
    returnType = "V",
    parameters = listOf("Lcom/vsco/cam/account/UserModel;")
)
