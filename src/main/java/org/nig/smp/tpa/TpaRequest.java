package org.nig.smp.tpa;

import java.util.UUID;

public record TpaRequest(UUID sender, UUID receiver, Type type, long expiresAt) {

    public enum Type {
        TPA, TPAHERE
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
