package org.nethergames.gsms.data.model;

public class StateUpdateRequest {
    private String serverUniqueId;
    private boolean queueing;
    private boolean touchQueueing;

    public String getServerUniqueId() {
        return serverUniqueId;
    }

    public void setServerUniqueId(String serverUniqueId) {
        this.serverUniqueId = serverUniqueId;
    }

    public boolean isQueueing() {
        return queueing;
    }

    public void setQueueing(boolean queueing) {
        this.queueing = queueing;
    }

    public boolean isTouchQueueing() {
        return touchQueueing;
    }

    public void setTouchQueueing(boolean touchQueueing) {
        this.touchQueueing = touchQueueing;
    }
}
