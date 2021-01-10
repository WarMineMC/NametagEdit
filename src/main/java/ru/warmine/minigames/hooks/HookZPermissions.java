package ru.warmine.minigames.hooks;

import ru.warmine.minigames.NametagHandler;
import lombok.AllArgsConstructor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsPlayerUpdateEvent;

@AllArgsConstructor
public class HookZPermissions implements Listener {

    private NametagHandler handler;

    @EventHandler
    public void onZPermissionsRankChangeEvent(ZPermissionsPlayerUpdateEvent event) {
        handler.applyTagToPlayer(event.getPlayer(), false);
    }

}