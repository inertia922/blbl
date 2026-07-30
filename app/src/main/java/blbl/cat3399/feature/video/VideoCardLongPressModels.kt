package blbl.cat3399.feature.video

import androidx.annotation.DrawableRes
import blbl.cat3399.R
import blbl.cat3399.core.model.VideoCard
import blbl.cat3399.core.prefs.AppPrefs

enum class VideoCardConfiguredLongPressAction {
    MANUAL,
    VIDEO_DISLIKE,
    UP_DISLIKE,
    OPEN_UP,
    DISMISS,
    ;

    companion object {
        fun fromPref(value: String): VideoCardConfiguredLongPressAction {
            return when (AppPrefs.normalizeVideoCardLongPressAction(value)) {
                AppPrefs.VIDEO_CARD_LONG_PRESS_ACTION_VIDEO_DISLIKE -> VIDEO_DISLIKE
                AppPrefs.VIDEO_CARD_LONG_PRESS_ACTION_UP_DISLIKE -> UP_DISLIKE
                AppPrefs.VIDEO_CARD_LONG_PRESS_ACTION_OPEN_UP -> OPEN_UP
                AppPrefs.VIDEO_CARD_LONG_PRESS_ACTION_DISMISS -> DISMISS
                else -> MANUAL
            }
        }
    }
}

enum class VideoCardQuickActionId {
    VIDEO_DISLIKE,
    UP_DISLIKE,
    OPEN_UP,
    DISMISS,
}

data class VideoCardQuickAction(
    val id: VideoCardQuickActionId,
    @DrawableRes val iconResId: Int,
    val contentDescription: CharSequence,
) {
    companion object {
        fun videoDislike(label: CharSequence): VideoCardQuickAction =
            VideoCardQuickAction(
                id = VideoCardQuickActionId.VIDEO_DISLIKE,
                iconResId = R.drawable.ic_video_card_not_interested,
                contentDescription = label,
            )

        fun upDislike(label: CharSequence): VideoCardQuickAction =
            VideoCardQuickAction(
                id = VideoCardQuickActionId.UP_DISLIKE,
                iconResId = R.drawable.ic_video_card_up_dislike,
                contentDescription = label,
            )

        fun openUp(label: CharSequence): VideoCardQuickAction =
            VideoCardQuickAction(
                id = VideoCardQuickActionId.OPEN_UP,
                iconResId = R.drawable.ic_player_up,
                contentDescription = label,
            )

        fun dismiss(label: CharSequence): VideoCardQuickAction =
            VideoCardQuickAction(
                id = VideoCardQuickActionId.DISMISS,
                iconResId = R.drawable.ic_video_card_delete,
                contentDescription = label,
            )
    }
}

interface VideoCardActionDelegate {
    fun resolveLongPressAction(
        card: VideoCard,
        position: Int,
    ): VideoCardConfiguredLongPressAction

    fun manualActions(
        card: VideoCard,
        position: Int,
    ): List<VideoCardQuickAction>

    fun onActionSelected(
        card: VideoCard,
        position: Int,
        action: VideoCardQuickAction,
    )
}

internal fun VideoCard.hasVideoDetailIdentity(): Boolean =
    bvid.isNotBlank() || (aid ?: 0L) > 0L
