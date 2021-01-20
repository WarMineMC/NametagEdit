package ru.warmine.minigames.packets;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import ru.warmine.minigames.NametagHandler;
import ru.warmine.minigames.enums.TeamAction;
import ru.warmine.minigames.utils.Utils;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class PacketWrapper {

    public String error;
    private final Object packet = PacketAccessor.createPacket();

    private static Constructor<?> ChatComponentText;
    private static Class<? extends Enum> typeEnumChatFormat;

    static {
        try {
            if (!PacketAccessor.isLegacyVersion()) {
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];

                Class<?> typeChatComponentText = Class.forName("net.minecraft.server." + version + ".ChatComponentText");
                ChatComponentText = typeChatComponentText.getConstructor(String.class);
                typeEnumChatFormat = (Class<? extends Enum>) Class.forName("net.minecraft.server." + version + ".EnumChatFormat");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PacketWrapper(String name, TeamAction mode, Collection<String> members) {
        if (mode != TeamAction.ADD_MEMBER && mode != TeamAction.REMOVE_MEMBER) {
            throw new IllegalArgumentException("Method must be join or leave for player constructor");
        }

        this.setupDefaults(name, mode);
        this.setupMembers(members);
    }

    public PacketWrapper(String name, String prefix, String suffix, TeamAction mode, Collection<?> players) {
        setupDefaults(name, mode);
        if (mode == TeamAction.CREATE || mode == TeamAction.UPDATE) {
            try {            	            	
                if (PacketAccessor.isLegacyVersion()) {
                    PacketAccessor.DISPLAY_NAME.set(packet, name);
                    PacketAccessor.PREFIX.set(packet, prefix);
                    PacketAccessor.SUFFIX.set(packet, suffix);
                } else {					
                    String color = ChatColor.getLastColors(prefix);
                    String colorCode = null;

                    if (!color.isEmpty()) {						
                        colorCode = color.substring(color.length() - 1);
                        String chatColor = ChatColor.getByChar(colorCode).name();

                        if (chatColor.equalsIgnoreCase("MAGIC"))
                            chatColor = "OBFUSCATED";

                        Enum<?> colorEnum = Enum.valueOf(typeEnumChatFormat, chatColor);
                        PacketAccessor.TEAM_COLOR.set(packet, colorEnum);
                    }

                    PacketAccessor.DISPLAY_NAME.set(packet, ChatComponentText.newInstance(name));
                    PacketAccessor.PREFIX.set(packet, ChatComponentText.newInstance(prefix));

                    if (colorCode != null)
                        suffix = ChatColor.getByChar(colorCode) + suffix;

                    PacketAccessor.SUFFIX.set(packet, ChatComponentText.newInstance(suffix));
                }

                PacketAccessor.PACK_OPTION.set(packet, 1);

                if (PacketAccessor.VISIBILITY != null) {
                    PacketAccessor.VISIBILITY.set(packet, "always");
                }

                if (mode == TeamAction.CREATE) {
                    ((Collection) PacketAccessor.MEMBERS.get(packet)).addAll(players);
                }
            } catch (Exception e) {
                error = e.getMessage();
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void setupMembers(Collection<?> players) {
        try {
            players = players == null || players.isEmpty() ? new ArrayList<>() : players;
            ((Collection) PacketAccessor.MEMBERS.get(packet)).addAll(players);
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    private void setupDefaults(String name, TeamAction mode) {
        try {
            PacketAccessor.TEAM_NAME.set(packet, name);
            PacketAccessor.PARAM_INT.set(packet, mode.getId());

            if (NametagHandler.DISABLE_PUSH_ALL_TAGS && PacketAccessor.PUSH != null) {
                PacketAccessor.PUSH.set(packet, "never");
            }
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    public void send() {
        PacketAccessor.sendPacket(Utils.getOnline(), packet);
    }

    public void send(Player player) {
        PacketAccessor.sendPacket(player, packet);
    }

    public void send(Collection<Player> players) {
        players.forEach(this::send);
    }
}