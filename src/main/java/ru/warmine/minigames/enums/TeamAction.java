package ru.warmine.minigames.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TeamAction {
    CREATE(0),
    REMOVE(1),
    UPDATE(2),
    ADD_MEMBER(3),
    REMOVE_MEMBER(4);

    private final int id;
}
