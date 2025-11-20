package org.example.audio_ecommerce.entity.Enum;

public enum GhnStatus {
    READY_PICKUP,
    CANCELED,
    ON_DELIVERY,
    READY_TO_PICK,          // ready_to_pick
    PICKING,                // picking
    CANCEL,                 // cancel
    MONEY_COLLECT_PICKING,  // money_collect_picking
    PICKED,                 // picked
    STORING,                // storing
    TRANSPORTING,           // transporting
    SORTING,                // sorting
    DELIVERING,             // delivering
    MONEY_COLLECT_DELIVERING, // money_collect_delivering
    DELIVERED,              // delivered
    DELIVERY_FAIL,          // delivery_fail
    WAITING_TO_RETURN,      // waiting_to_return
    RETURN,                 // return
    RETURN_TRANSPORTING,    // return_transporting
    RETURN_SORTING,         // return_sorting
    RETURNING,              // returning
    RETURN_FAIL,            // return_fail
    RETURNED,               // returned
    EXCEPTION,              // exception
    DAMAGE,                 // damage
    LOST;                   // lost

    public static GhnStatus fromGhnCode(String code) {
        if (code == null) return null;
        return switch (code) {
            case "ready_to_pick" -> READY_TO_PICK;
            case "picking" -> PICKING;
            case "cancel" -> CANCEL;
            case "money_collect_picking" -> MONEY_COLLECT_PICKING;
            case "picked" -> PICKED;
            case "storing" -> STORING;
            case "transporting" -> TRANSPORTING;
            case "sorting" -> SORTING;
            case "delivering" -> DELIVERING;
            case "money_collect_delivering" -> MONEY_COLLECT_DELIVERING;
            case "delivered" -> DELIVERED;
            case "delivery_fail" -> DELIVERY_FAIL;
            case "waiting_to_return" -> WAITING_TO_RETURN;
            case "return" -> RETURN;
            case "return_transporting" -> RETURN_TRANSPORTING;
            case "return_sorting" -> RETURN_SORTING;
            case "returning" -> RETURNING;
            case "return_fail" -> RETURN_FAIL;
            case "returned" -> RETURNED;
            case "exception" -> EXCEPTION;
            case "damage" -> DAMAGE;
            case "lost" -> LOST;
            default -> null;
        };
    }
}
