package com.stormvr.app.moonlight;

/** Decisions use monotonic milliseconds and cumulative decoder output, not connection state. */
public final class VideoHealth {
    public static final int VIDEO_READY=1, REQUEST_KEYFRAME=2, NO_VIDEO=4;
    private long frames, changedAt, lastRequest;
    private boolean announced, warned;
    public VideoHealth(long now) { changedAt=now; lastRequest=now-3000; }
    public int update(long now, long rendered) {
        int events=0;
        if (rendered>frames) {
            frames=rendered;changedAt=now;
            if (!announced || warned) events|=VIDEO_READY;
            announced=true;warned=false;
        } else if (now-changedAt>=2000 && now-lastRequest>=3000) {
            lastRequest=now;events|=REQUEST_KEYFRAME;
        }
        if (!warned && now-changedAt>=8000) {warned=true;events|=NO_VIDEO;}
        return events;
    }
}
