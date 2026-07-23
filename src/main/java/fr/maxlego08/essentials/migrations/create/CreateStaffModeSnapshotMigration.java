package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreateStaffModeSnapshotMigration extends Migration {

    @Override
    public void up() {
        create("%prefix%staff_mode_snapshots", table -> {
            table.uuid("unique_id").foreignKey("%prefix%users").primary();
            table.longText("inventory");
            table.longText("armor");
            table.longText("offhand");
            table.integer("held_slot");
            table.string("game_mode", 32);
            table.bool("allow_flight");
            table.bool("flying");
            table.bool("vanished");
            table.timestamps();
        });
    }
}
