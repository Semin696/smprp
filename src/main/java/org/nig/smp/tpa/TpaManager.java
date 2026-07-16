package org.nig.smp.tpa;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final Map<UUID, TpaRequest> requests = new ConcurrentHashMap<>();
    private static final long TIMEOUT_MS = 120_000L;

    public void createRequest(UUID sender, UUID receiver, TpaRequest.Type type) {
        requests.put(receiver, new TpaRequest(sender, receiver, type, System.currentTimeMillis() + TIMEOUT_MS));
    }

    public TpaRequest getRequest(UUID receiver) {
        TpaRequest req = requests.get(receiver);
        if (req == null) return null;
        if (req.isExpired()) {
            requests.remove(receiver);
            return null;
        }
        return req;
    }

    public void removeRequest(UUID receiver) {
        requests.remove(receiver);
    }

    public void removePlayerRequests(UUID playerId) {
        requests.values().removeIf(req ->
            req.sender().equals(playerId) || req.receiver().equals(playerId));
    }
}
