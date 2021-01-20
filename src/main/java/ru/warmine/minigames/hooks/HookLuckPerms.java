package ru.warmine.minigames.hooks;

import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.event.EventBus;
import me.lucko.luckperms.api.event.user.UserDataRecalculateEvent;
import ru.warmine.minigames.NametagHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class HookLuckPerms implements Listener {
    private final NametagHandler handler;

    public HookLuckPerms(NametagHandler handler) {
        this.handler = handler;
        EventBus eventBus = Bukkit.getServicesManager().load(LuckPermsApi.class).getEventBus();
        eventBus.subscribe(handler.getPlugin(), UserDataRecalculateEvent.class, this::onUserDataRecalculateEvent);
    }

    private void onUserDataRecalculateEvent(UserDataRecalculateEvent event) {
        User user = event.getUser();
        Player player = Bukkit.getPlayer(user.getUuid());

        if (player != null) {
            handler.applyTagToPlayer(player, false);
        }
    }

}
