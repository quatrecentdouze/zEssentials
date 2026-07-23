package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.StaffModeSnapshotDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;

import java.util.Optional;
import java.util.UUID;

public class StaffModeSnapshotRepository extends Repository {

    public StaffModeSnapshotRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "staff_mode_snapshots");
    }

    public void upsert(StaffModeSnapshotDTO snapshot) {
        upsert(table -> {
            table.uuid("unique_id", snapshot.unique_id()).primary();
            table.string("inventory", snapshot.inventory());
            table.string("armor", snapshot.armor());
            table.string("offhand", snapshot.offhand());
            table.decimal("held_slot", snapshot.held_slot());
            table.string("game_mode", snapshot.game_mode());
            table.bool("allow_flight", snapshot.allow_flight());
            table.bool("flying", snapshot.flying());
            table.bool("vanished", snapshot.vanished());
        });
    }

    public Optional<StaffModeSnapshotDTO> select(UUID uniqueId) {
        return select(StaffModeSnapshotDTO.class, table -> table.where("unique_id", uniqueId)).stream().findFirst();
    }

    public boolean delete(UUID uniqueId) {
        return delete(table -> table.where("unique_id", uniqueId)) > 0;
    }
}
