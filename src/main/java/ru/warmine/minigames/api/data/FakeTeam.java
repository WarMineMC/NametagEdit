package ru.warmine.minigames.api.data;

import com.google.common.collect.*;
import org.bukkit.entity.Player;
import ru.warmine.minigames.utils.Utils;
import ru.warmine.minigames.packets.VersionChecker;
import ru.warmine.minigames.packets.VersionChecker.BukkitVersion;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This class represents a Scoreboard Team. It is used
 * to keep track of the current members of a Team, and
 * is responsible for
 */
@Data
public class FakeTeam {

    // Because some networks use NametagEdit on multiple servers, we may have clashes
    // with the same Team names. The UNIQUE_ID ensures there will be no clashing.
    private static final String UNIQUE_ID = Utils.generateUUID();
    // This represents the number of FakeTeams that have been created.
    // It is used to generate a unique Team name.
    private static int ID = 0;
    private Set<String> members;
    private SetMultimap<String, String> fakeMembers;
    private String name;
    private String prefix = "";
    private String suffix = "";

    public FakeTeam(String prefix, String suffix, int sortPriority, boolean playerTag) {
        this(prefix, suffix, sortPriority, Collections.emptyList(), playerTag);
    }

    public FakeTeam(String prefix, String suffix, int sortPriority, Collection<String> members, boolean playerTag) {
        this.name = UNIQUE_ID + "_" + getNameFromInput(sortPriority) + ++ID + (playerTag ? "+P" : "");

 		// Adding a VersionChecker for proper limits to ensure they're no crashes.
		if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_13_R1) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_14_R1) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
        } else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_14_R2) {
            this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_15_R1) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_16_R1) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_16_R2) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else if(VersionChecker.getBukkitVersion() == BukkitVersion.v1_16_R3) {
        	this.name = this.name.length() > 128 ? this.name.substring(0, 128) : this.name;
		} else {
        	this.name = this.name.length() > 16 ? this.name.substring(0, 16) : this.name;
		}

		this.prefix = prefix;
		this.suffix = suffix;
		this.members = new HashSet<>(members);
		this.fakeMembers = HashMultimap.create();
	}

	public void addFakeMember(String target, Player player) {
        this.fakeMembers.put(target, player.getName());
    }

    public void addFakeMember(String target, Collection<Player> players) {
        this.fakeMembers.putAll(target, players.stream().map(Player::getName).collect(Collectors.toSet()));
    }

    public void removeFakeMember(String target, String player) {
        this.fakeMembers.remove(target, player);
    }

    public Set<String> removeFakeMember(String target) {
        return this.fakeMembers.removeAll(target);
    }

    public boolean isFakeMember(String target) {
        return this.fakeMembers.containsKey(target);
    }

    public Collection<String> getFakeTargets(String viewer) {
        return this.fakeMembers.asMap()
                .entrySet().stream()
                .filter(entry -> entry.getValue().contains(viewer))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public Collection<String> removeViewer(String viewer) {
        Collection<String> targets = this.getFakeTargets(viewer);

        for (String target : targets) {
            this.fakeMembers.remove(target, viewer);
        }

        return targets;
    }

    public boolean isViewer(String viewer) {
        return this.fakeMembers.containsValue(viewer);
    }

    public void addMember(String player) {
        members.add(player);
    }

    public boolean isSimilar(String prefix, String suffix) {
        return this.prefix.equals(prefix) && this.suffix.equals(suffix);
    }

    /**
     * This is a special method to sort nametags in
     * the tablist. It takes a priority and converts
     * it to an alphabetic representation to force a
     * specific sort.
     *
     * @param input the sort priority
     * @return the team name
     */
    private String getNameFromInput(int input) {
        if (input < 0) return "Z";
        char letter = (char) ((input / 5) + 65);
        int repeat = input % 5 + 1;
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < repeat; i++) {
            builder.append(letter);
        }
        return builder.toString();
    }

}
