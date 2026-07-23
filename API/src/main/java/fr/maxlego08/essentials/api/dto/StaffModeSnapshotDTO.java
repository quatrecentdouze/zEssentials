package fr.maxlego08.essentials.api.dto;

import java.util.Date;
import java.util.UUID;

public record StaffModeSnapshotDTO(UUID unique_id, String inventory, String armor, String offhand, int held_slot,
                                   String game_mode, boolean allow_flight, boolean flying, boolean vanished,
                                   Date created_at, Date updated_at) {
}
