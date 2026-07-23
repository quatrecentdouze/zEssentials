package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreatePlayerAddressMigration extends Migration {

    @Override
    public void up() {
        create("%prefix%player_addresses", table -> {
            table.uuid("unique_id").foreignKey("%prefix%users").primary();
            table.string("address", 45).primary();
            table.timestamp("first_seen");
            table.timestamp("last_seen");
        });
    }
}
