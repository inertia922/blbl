package blbl.cat3399.feature.video

import android.content.Context
import blbl.cat3399.R
import blbl.cat3399.core.api.BiliApi
import blbl.cat3399.core.api.BiliApiException
import blbl.cat3399.core.log.AppLog
import blbl.cat3399.core.model.VideoCard
import blbl.cat3399.core.net.BiliClient
import blbl.cat3399.core.ui.AppToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface VideoCardDismissBehavior {
    data object LocalNotInterested : VideoCardDismissBehavior

    data object DeleteHistory : VideoCardDismissBehavior

    data object DeleteToView : VideoCardDismissBehavior

    data class DeleteFavFolderItem(
        val mediaId: Long,
    ) : VideoCardDismissBehavior
}

class VideoCardActionController(
    private val context: Context,
    private val scope: CoroutineScope,
    private val dismissBehavior: VideoCardDismissBehavior,
    private val onOpenDetail: (VideoCard, Int) -> Unit,
    private val onOpenUp: (VideoCard) -> Unit,
    private val onCardRemoved: (String) -> Unit = {},
    private val longPressActionProvider: () -> String = { BiliClient.prefs.videoCardLongPressAction },
) : VideoCardActionDelegate {
    private val inFlightActionKeys = HashSet<String>()

    override fun resolveLongPressAction(
        card: VideoCard,
        position: Int,
    ): VideoCardConfiguredLongPressAction = VideoCardConfiguredLongPressAction.fromPref(longPressActionProvider())

    override fun manualActions(
        card: VideoCard,
        position: Int,
    ): List<VideoCardQuickAction> {
        return listOf(
            VideoCardQuickAction.videoDislike(context.getString(R.string.video_card_action_video_dislike)),
            VideoCardQuickAction.upDislike(context.getString(R.string.video_card_action_up_dislike)),
            VideoCardQuickAction.openUp(context.getString(R.string.video_card_action_open_up)),
            VideoCardQuickAction.dismiss(context.getString(dismissActionLabelRes())),
        )
    }

    override fun onActionSelected(
        card: VideoCard,
        position: Int,
        action: VideoCardQuickAction,
    ) {
        when (action.id) {
            VideoCardQuickActionId.VIDEO_DISLIKE -> dislikeVideo(card)
            VideoCardQuickActionId.UP_DISLIKE -> dislikeUp(card)
            VideoCardQuickActionId.OPEN_UP -> onOpenUp(card)
            VideoCardQuickActionId.DISMISS -> dismissCard(card)
        }
    }

    private fun dislikeVideo(card: VideoCard) {
        val stableKey = card.stableKey()
        val actionKey = buildActionKey(card = card, suffix = "dislike_video")
        if (!markInFlight(actionKey)) return
        VideoCardVisibilityFilter.hide(stableKey)
        onCardRemoved(stableKey)
        AppToast.show(context, context.getString(R.string.video_card_action_video_dislike_done))
        scope.launch {
            try {
                withContext(Dispatchers.IO) { BiliApi.videoFeedbackDislike(card) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
            } finally { clearInFlight(actionKey) }
        }
    }

    private fun dislikeUp(card: VideoCard) {
        val stableKey = card.stableKey()
        val upMid = card.ownerMid?.takeIf { it > 0L }
        if (upMid == null) {
            AppToast.show(context, context.getString(R.string.video_card_action_up_dislike_unavailable))
            return
        }
        val actionKey = buildActionKey(card = card, suffix = "dislike_up")
        if (!markInFlight(actionKey)) return
        VideoCardVisibilityFilter.hide(stableKey)
        onCardRemoved(stableKey)
        AppToast.show(context, context.getString(R.string.video_card_action_up_dislike_done))
        scope.launch {
            try {
                withContext(Dispatchers.IO) { BiliApi.modifyRelation(fid = upMid, act = 5) }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
            } finally { clearInFlight(actionKey) }
        }
    }

    private fun dismissCard(card: VideoCard) {
        when (dismissBehavior) {
            VideoCardDismissBehavior.LocalNotInterested -> {
                val actionKey = buildActionKey(card = card, suffix = "not_interested")
                if (!markInFlight(actionKey)) return

                val stableKey = card.stableKey()
                VideoCardVisibilityFilter.hide(stableKey)
                onCardRemoved(stableKey)
                AppToast.show(context, context.getString(R.string.video_card_action_not_interested_done))

                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            BiliApi.videoFeedbackDislike(card)
                        }
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        AppLog.w("VideoCardAction", "report not interested failed stableKey=$stableKey", t)
                    } finally {
                        clearInFlight(actionKey)
                    }
                }
            }

            else -> {
                val actionKey = buildActionKey(card = card, suffix = "dismiss")
                if (!markInFlight(actionKey)) return

                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            when (val behavior = dismissBehavior) {
                                VideoCardDismissBehavior.DeleteHistory -> {
                                    BiliApi.historyDelete(kid = buildHistoryKid(card))
                                }

                                VideoCardDismissBehavior.DeleteToView -> {
                                    BiliApi.toViewDelete(aid = resolveAid(card))
                                }

                                is VideoCardDismissBehavior.DeleteFavFolderItem -> {
                                    val aid = resolveAid(card)
                                    BiliApi.favResourceDelete(
                                        aid = aid,
                                        mediaId = behavior.mediaId,
                                    )
                                }

                                VideoCardDismissBehavior.LocalNotInterested -> Unit
                            }
                        }
                        onCardRemoved(card.stableKey())
                        AppToast.show(context, context.getString(R.string.video_card_action_delete_done))
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        AppToast.show(context, errorMessage(t))
                    } finally {
                        clearInFlight(actionKey)
                    }
                }
            }
        }
    }

    private suspend fun resolveAid(card: VideoCard): Long {
        card.aid?.takeIf { it > 0L }?.let { return it }
        val bvid = card.bvid.trim().takeIf { it.isNotBlank() } ?: throw BiliApiException(apiCode = -400, apiMessage = "missing_video_id")
        return BiliApi.videoDetail(bvid).aid?.takeIf { it > 0L }
            ?: throw BiliApiException(apiCode = -400, apiMessage = "missing_video_id")
    }

    private suspend fun buildHistoryKid(card: VideoCard): String {
        return when (card.business?.trim()) {
            "archive" -> "archive_${resolveAid(card)}"
            "pgc" -> {
                val seasonId = card.seasonId?.takeIf { it > 0L } ?: throw BiliApiException(apiCode = -400, apiMessage = "missing_history_kid")
                "pgc_$seasonId"
            }

            else -> throw BiliApiException(apiCode = -400, apiMessage = "missing_history_kid")
        }
    }

    private fun dismissActionLabelRes(): Int {
        return when (dismissBehavior) {
            VideoCardDismissBehavior.LocalNotInterested -> R.string.video_card_action_not_interested
            else -> R.string.video_card_action_delete
        }
    }

    private fun buildActionKey(
        card: VideoCard,
        suffix: String,
    ): String = "${card.stableKey()}|$suffix"

    private fun markInFlight(actionKey: String): Boolean {
        synchronized(inFlightActionKeys) {
            return inFlightActionKeys.add(actionKey)
        }
    }

    private fun clearInFlight(actionKey: String) {
        synchronized(inFlightActionKeys) {
            inFlightActionKeys.remove(actionKey)
        }
    }

    private fun errorMessage(t: Throwable): String {
        val raw = (t as? BiliApiException)?.apiMessage?.trim().orEmpty().ifBlank { t.message.orEmpty().trim() }
        return when {
            raw.isBlank() -> context.getString(R.string.video_card_action_failed)
            raw == "missing_history_kid" -> context.getString(R.string.video_card_action_missing_video_id)
            raw == "missing_video_id" -> context.getString(R.string.video_card_action_missing_video_id)
            else -> raw
        }
    }
}
