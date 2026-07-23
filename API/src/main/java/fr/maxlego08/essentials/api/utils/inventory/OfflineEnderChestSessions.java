package fr.maxlego08.essentials.api.utils.inventory;

import org.bukkit.inventory.Inventory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OfflineEnderChestSessions {

    private static final ConcurrentHashMap<UUID, Session> BY_TARGET = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, UUID> TARGET_BY_VIEWER = new ConcurrentHashMap<>();

    private OfflineEnderChestSessions() {
    }

    public static synchronized boolean acquire(UUID viewerUniqueId, UUID targetUniqueId) {
        if (TARGET_BY_VIEWER.containsKey(viewerUniqueId)) return false;
        Session session = new Session(viewerUniqueId, targetUniqueId, null);
        if (BY_TARGET.putIfAbsent(targetUniqueId, session) != null) return false;
        TARGET_BY_VIEWER.put(viewerUniqueId, targetUniqueId);
        return true;
    }

    public static synchronized void bind(UUID viewerUniqueId, Inventory inventory) {
        UUID targetUniqueId = TARGET_BY_VIEWER.get(viewerUniqueId);
        if (targetUniqueId == null) return;
        BY_TARGET.computeIfPresent(targetUniqueId, (key, session) -> new Session(session.viewerUniqueId(), session.targetUniqueId(), inventory));
    }

    public static synchronized boolean owns(UUID viewerUniqueId, UUID targetUniqueId) {
        Session session = BY_TARGET.get(targetUniqueId);
        return session != null && session.viewerUniqueId().equals(viewerUniqueId);
    }

    public static synchronized Optional<Session> releaseViewer(UUID viewerUniqueId) {
        UUID targetUniqueId = TARGET_BY_VIEWER.remove(viewerUniqueId);
        if (targetUniqueId == null) return Optional.empty();
        return Optional.ofNullable(BY_TARGET.remove(targetUniqueId));
    }

    public static synchronized Optional<Session> releaseTarget(UUID targetUniqueId) {
        Session session = BY_TARGET.remove(targetUniqueId);
        if (session == null) return Optional.empty();
        TARGET_BY_VIEWER.remove(session.viewerUniqueId());
        return Optional.of(session);
    }

    public record Session(UUID viewerUniqueId, UUID targetUniqueId, Inventory inventory) {
    }
}
