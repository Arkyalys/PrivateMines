package fr.ju.privateMines.models;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public class MineAccess {
    private final UUID mineOwner;
    private final Set<UUID> authorizedMembers;
    public MineAccess(UUID mineOwner) {
        this.mineOwner = mineOwner;
        this.authorizedMembers = new HashSet<>();
    }
    public boolean canAccess(UUID user) {
        return user.equals(mineOwner) || authorizedMembers.contains(user);
    }
    public void addMember(UUID user) {
        if (!user.equals(mineOwner)) {
            authorizedMembers.add(user);
        }
    }
    public void removeMember(UUID user) {
        authorizedMembers.remove(user);
    }
    public Set<UUID> getAuthorizedMembers() {
        return new HashSet<>(authorizedMembers);
    }
} 