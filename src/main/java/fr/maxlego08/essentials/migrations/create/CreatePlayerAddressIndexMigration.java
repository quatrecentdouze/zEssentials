package fr.maxlego08.essentials.migrations.create;

import fr.maxlego08.sarah.database.Migration;

public class CreatePlayerAddressIndexMigration extends Migration {

    @Override
    public void up() {
        index("%prefix%player_addresses", "address");
    }
}
