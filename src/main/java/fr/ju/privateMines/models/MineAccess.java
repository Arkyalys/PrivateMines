package fr.ju.privateMines.models;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public class MineAccess {
    private final UUID mineOwner;
    private final Set<UUID> permanentBannedUsers;
    private final Map<UUID, Long> temporaryBannedUsers;
    private final Set<UUID> deniedUsers;
    public MineAccess(UUID mineOwner) {
        this.mineOwner = mineOwner;
        this.permanentBannedUsers = new HashSet<>();
        this.temporaryBannedUsers = new HashMap<>();
        this.deniedUsers = new HashSet<>();
    }
    public boolean isBanned(UUID user) {
        if (permanentBannedUsers.contains(user)) {
            return true;
        }
        if (temporaryBannedUsers.containsKey(user)) {
            long expiration = temporaryBannedUsers.get(user);
            long currentTime = System.currentTimeMillis();
            if (currentTime > expiration) {
                temporaryBannedUsers.remove(user);
                return false;
            }
            return true;
        }
        return false;
    }
    public long getBanExpiration(UUID user) {
        if (temporaryBannedUsers.containsKey(user)) {
            return temporaryBannedUsers.get(user);
        }
        return -1;
    }
    public boolean isPermanentlyBanned(UUID user) {
        return permanentBannedUsers.contains(user);
    }
    public boolean isTemporarilyBanned(UUID user) {
        if (!temporaryBannedUsers.containsKey(user)) {
            return false;
        }
        long expiration = temporaryBannedUsers.get(user);
        long currentTime = System.currentTimeMillis();
        if (currentTime > expiration) {
            temporaryBannedUsers.remove(user);
            return false;
        }
        return true;
    }
    public boolean isDenied(UUID user) {
        return deniedUsers.contains(user);
    }
    public boolean canAccess(UUID user) {
        if (user.equals(mineOwner)) {
            return true;
        }
        return !isBanned(user) && !isDenied(user);
    }
    public void addPermanentBan(UUID user) {
        permanentBannedUsers.add(user);
        temporaryBannedUsers.remove(user);
    }
    public void addTemporaryBan(UUID user, long durationSeconds) {
        long expirationTime = System.currentTimeMillis() + (durationSeconds * 1000);
        temporaryBannedUsers.put(user, expirationTime);
        permanentBannedUsers.remove(user);
    }
    public void addDeniedUser(UUID user) {
        deniedUsers.add(user);
    }
    public void removeBan(UUID user) {
        permanentBannedUsers.remove(user);
        temporaryBannedUsers.remove(user);
    }
    public void removeDeniedUser(UUID user) {
        deniedUsers.remove(user);
    }
    public Set<UUID> getPermanentBannedUsers() {
        return new HashSet<>(permanentBannedUsers);
    }
    public Map<UUID, Long> getTemporaryBannedUsers() {
        long currentTime = System.currentTimeMillis();
        temporaryBannedUsers.entrySet().removeIf(entry -> entry.getValue() < currentTime);
        return new HashMap<>(temporaryBannedUsers);
    }
    public Set<UUID> getDeniedUsers() {
        return new HashSet<>(deniedUsers);
    }
} 