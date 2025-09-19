package org.example.audio_ecommerce.entity;

import lombok.Getter;

@Getter
public enum RoleEnum {
    ADMIN(1),
    GUEST(2),
    STAFF(3),
    CUSTOMER(4),
    OWNER(5);

    private final int value;

    RoleEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
