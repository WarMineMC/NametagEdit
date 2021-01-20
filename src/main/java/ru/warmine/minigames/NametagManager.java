package ru.warmine.minigames;

import java.util.*;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import ru.warmine.minigames.api.data.FakeTeam;
import ru.warmine.minigames.enums.TeamAction;
import ru.warmine.minigames.packets.PacketWrapper;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class NametagManager {

    private final HashMap<String, FakeTeam> TEAMS = new HashMap<>();
    private final HashMap<String, FakeTeam> CACHED_FAKE_TEAMS = new HashMap<>();
    private final NametagEdit plugin;

    /**
     * Gets the current team given a prefix and suffix
     * If there is no team similar to this, then a new
     * team is created.
     */
    private FakeTeam getFakeTeam(String prefix, String suffix) {
        for (FakeTeam fakeTeam : TEAMS.values()) {
            if (fakeTeam.isSimilar(prefix, suffix)) {
                return fakeTeam;
            }
        }

        return null;
    }

    private FakeTeam getOrCreateTeam(String prefix, String suffix, int sortPriority) {
        return this.getOrCreateTeam(prefix, suffix, sortPriority, Collections.emptyList());
    }

    private FakeTeam getOrCreateTeam(String prefix, String suffix, int sortPriority, Collection<String> members) {
        FakeTeam foundedTeam = this.getFakeTeam(prefix, suffix);

        if (foundedTeam != null) {
            return foundedTeam;
        }

        FakeTeam team = new FakeTeam(
                prefix,
                suffix,
                sortPriority,
                members,
                false
        );

        TEAMS.put(team.getName(), team);
        this.addTeamPackets(team);

        this.plugin.debug(String.format(
                "Created FakeTeam %s. Size: %d.",
                team.getName(),
                TEAMS.size()
        ));

        return team;
    }

    private void displayTeam(
            String player,
            String prefix,
            String suffix,
            int sortPriority,
            Collection<Player> viewers
    ) {
        if (Bukkit.getPlayerExact(player) == null) {
            return;
        }

        FakeTeam team = this.getOrCreateTeam(prefix, suffix, sortPriority);
        team.addFakeMember(player, viewers);

        this.displayTeamPackets(team, player, viewers);

        this.plugin.debug(String.format(
                "Showing fake %s's tag for viewers: %s. FakeTeam: %s",
                player,
                viewers.stream().map(Player::getName).collect(Collectors.joining(",")),
                team.getName()
        ));
    }

    /**
     * Adds a player to a FakeTeam. If they are already on this team,
     * we do NOT change that.
     */
    private void addPlayerToTeam(String player, String prefix, String suffix, int sortPriority, boolean playerTag) {
        FakeTeam previousTeam = getFakeTeam(player);

        if (previousTeam != null && previousTeam.isSimilar(prefix, suffix)) {
            plugin.debug(String.format(
                    "%s already belongs to a similar team (\"%s\").",
                    player,
                    previousTeam.getName()
            ));

            return;
        }

        this.reset(player);

        FakeTeam team = this.getOrCreateTeam(
                prefix,
                suffix,
                sortPriority
        );

        team.addMember(player);

        this.addPlayerToTeamPackets(team, player);
        this.cache(player, team);

        plugin.debug(String.format(
                "%s has been added to team %s.",
                player,
                team.getName()
        ));
    }

    public FakeTeam reset(String player) {
        return reset(player, decache(player));
    }

    private FakeTeam reset(String player, FakeTeam fakeTeam) {
        if (fakeTeam != null && fakeTeam.getMembers().remove(player)) {
            boolean delete;

            Player removing = Bukkit.getPlayerExact(player);
            if (removing != null) {
                delete = removePlayerFromTeamPackets(fakeTeam, removing.getName());
            } else {
                OfflinePlayer toRemoveOffline = Bukkit.getOfflinePlayer(player);
                delete = removePlayerFromTeamPackets(fakeTeam, toRemoveOffline.getName());
            }

            plugin.debug(player + " was removed from " + fakeTeam.getName());
            if (delete) {
                removeTeamPackets(fakeTeam);
                TEAMS.remove(fakeTeam.getName());
                plugin.debug("FakeTeam " + fakeTeam.getName() + " has been deleted. Size: " + TEAMS.size());
            }
        }

        return fakeTeam;
    }

    // ==============================================================
    // Below are public methods to modify the cache
    // ==============================================================
    private FakeTeam decache(String player) {
        return CACHED_FAKE_TEAMS.remove(player);
    }

    public FakeTeam getFakeTeam(String player) {
        return CACHED_FAKE_TEAMS.get(player);
    }

    private void cache(String player, FakeTeam fakeTeam) {
        CACHED_FAKE_TEAMS.put(player, fakeTeam);
    }

    // ==============================================================
    // Below are public methods to modify certain data
    // ==============================================================
    public void setNametag(String player, String prefix, String suffix) {
        setNametag(player, prefix, suffix, -1);
    }

    public void setNametag(String player, String prefix, String suffix, int sortPriority) {
        setNametag(player, prefix, suffix, sortPriority, false);
    }

    void setNametag(String player, String prefix, String suffix, int sortPriority, boolean playerTag) {
        addPlayerToTeam(player, prefix != null ? prefix : "", suffix != null ? suffix : "", sortPriority, playerTag);
    }

    public void displayFakeNametag(String player, String prefix, String suffix, int sortPriority, Collection<Player> viewers) {
        displayTeam(player, prefix, suffix, sortPriority, viewers);
    }

    void sendTeams(Player player) {
        String name = player.getName();

        for (FakeTeam fakeTeam : TEAMS.values()) {
            Collection<String> allMembers = fakeTeam.getMembers();

            if (fakeTeam.isViewer(name)) {
                Collection<String> targets = fakeTeam.getFakeTargets(name);
                allMembers.addAll(targets);

                this.plugin.debug(String.format(
                        "%s is a viewer of the %s team. Targets: %s. Adding fake members...",
                        name,
                        fakeTeam.getName(),
                        Joiner.on(", ").join(targets)
                ));
            }

            new PacketWrapper(
                    fakeTeam.getName(),
                    fakeTeam.getPrefix(),
                    fakeTeam.getSuffix(),
                    TeamAction.CREATE,
                    allMembers
            ).send(player);
        }
    }

    public void resetFakeTags(String player) {
        Player onlinePlayer = Bukkit.getPlayerExact(player);

        for (FakeTeam fakeTeam : TEAMS.values()) {
            if (fakeTeam.isFakeMember(player)) {
                Collection<String> viewers = fakeTeam.removeFakeMember(player);
                Collection<Player> viewersAsPlayers = viewers.stream()
                        .map(Bukkit::getPlayerExact)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                this.plugin.debug(String.format(
                        "Reset the %s's fake nametag for viewers: %s.",
                        player,
                        Joiner.on(", ").join(viewers)
                ));

                new PacketWrapper(
                        fakeTeam.getName(),
                        TeamAction.REMOVE_MEMBER,
                        Collections.singletonList(player)
                ).send(viewersAsPlayers);
            }

            if (fakeTeam.isViewer(player)) {
                Collection<String> targets = fakeTeam
                        .removeViewer(player)
                        .stream()
                        .filter(name -> Bukkit.getPlayerExact(name) != null)
                        .collect(Collectors.toSet());

                this.plugin.debug(String.format(
                        "%s is a viewer of the %s team. Targets: %s. Removing fake members...",
                        player,
                        fakeTeam.getName(),
                        Joiner.on(", ").join(targets)
                ));

                if (onlinePlayer != null && !targets.isEmpty()) {
                    new PacketWrapper(
                            fakeTeam.getName(),
                            TeamAction.REMOVE_MEMBER,
                            targets
                    ).send(onlinePlayer);
                }
            }
        }

        if (onlinePlayer != null) {
            this.sendTeams(onlinePlayer);
        }
    }

    void reset() {
        for (FakeTeam fakeTeam : TEAMS.values()) {
            removePlayerFromTeamPackets(fakeTeam, fakeTeam.getMembers());
            removeTeamPackets(fakeTeam);
        }
        CACHED_FAKE_TEAMS.clear();
        TEAMS.clear();
    }

    // ==============================================================
    // Below are private methods to construct a new Scoreboard packet
    // ==============================================================
    private void removeTeamPackets(FakeTeam fakeTeam) {
        new PacketWrapper(fakeTeam.getName(), fakeTeam.getPrefix(), fakeTeam.getSuffix(), TeamAction.REMOVE, new ArrayList<>()).send();
    }

    private boolean removePlayerFromTeamPackets(FakeTeam fakeTeam, String... players) {
        return removePlayerFromTeamPackets(fakeTeam, Arrays.asList(players));
    }

    private boolean removePlayerFromTeamPackets(FakeTeam fakeTeam, Collection<String> players) {
        new PacketWrapper(fakeTeam.getName(), TeamAction.REMOVE_MEMBER, players).send();
        fakeTeam.getMembers().removeAll(players);
        return fakeTeam.getMembers().isEmpty();
    }

    private void addTeamPackets(FakeTeam fakeTeam) {
        new PacketWrapper(fakeTeam.getName(), fakeTeam.getPrefix(), fakeTeam.getSuffix(), TeamAction.CREATE, fakeTeam.getMembers()).send();
    }

    private void addPlayerToTeamPackets(FakeTeam fakeTeam, String player) {
        new PacketWrapper(fakeTeam.getName(), TeamAction.ADD_MEMBER, Collections.singletonList(player)).send();
    }

    private void displayTeamPackets(FakeTeam fakeTeam, String player, Collection<Player> viewers) {
        new PacketWrapper(fakeTeam.getName(), TeamAction.ADD_MEMBER, Collections.singletonList(player)).send(viewers);
    }
}