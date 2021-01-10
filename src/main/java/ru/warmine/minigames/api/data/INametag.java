package ru.warmine.minigames.api.data;

public interface INametag {
    String getPrefix();

    String getSuffix();

    int getSortPriority();

    boolean isPlayerTag();
}