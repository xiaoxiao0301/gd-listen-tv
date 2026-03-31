package com.xiaoxiao0301.amberplay.core.media

import com.xiaoxiao0301.amberplay.domain.model.Song

/**
 * Extended player controller interface that exposes all lifecycle methods needed by
 * [com.xiaoxiao0301.amberplay.feature.player.PlayerViewModel].
 *
 * Feature-layer ViewModels that only need playback state / seek / skip should inject
 * [IPlayerController]. PlayerViewModel, which manages crossfade, queue callbacks and
 * service lifecycle, injects this extended interface instead of the concrete class (A-05).
 */
interface IFullPlayerController : IPlayerController {

    /** Crossfade duration in milliseconds; 0 means disabled. */
    var crossfadeMs: Int

    /** Callback registered by PlayerViewModel to centralise queue navigation. */
    var onSkipToIndex: ((Int) -> Unit)?

    /** Callback invoked when playback ends naturally (non-repeat mode). */
    var onPlaybackEnded: (() -> Unit)?

    /** Start playing [song] from [url], applying crossfade if configured. */
    fun playSong(song: Song, url: String)

    /** Toggle play / pause. */
    fun playOrPause()

    /**
     * Synchronises queue position to [PlaybackState] so UI can show the correct
     * track index out of total queue size without reading the DB again.
     */
    fun updateQueueContext(index: Int, size: Int)

    /** Adjust playback speed; clamped to [0.5, 2.0]. */
    fun setSpeed(speed: Float)

    /** Switch between SEQUENTIAL / REPEAT_ALL / REPEAT_ONE / SHUFFLE. */
    fun setPlayMode(mode: PlayMode)

    /** Set a sleep timer that will pause playback after [minutes] minutes. */
    fun setSleepTimer(minutes: Int)

    /** Cancel any active sleep timer. */
    fun cancelSleepTimer()

    /** Release all ExoPlayer resources. */
    fun release()
}
