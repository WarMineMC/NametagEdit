package ru.warmine.minigames.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum TeamAction {
    CREATE(0),
    REMOVE(1),
    UPDATE(2),
    ADD_MEMBER(3),
    REMOVE_MEMBER(4);

    public final int id;
}
