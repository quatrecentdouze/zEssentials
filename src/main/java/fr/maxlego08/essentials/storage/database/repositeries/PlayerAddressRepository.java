package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PlayerAddressDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.database.DatabaseType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class PlayerAddressRepository extends Repository {

    public PlayerAddressRepository(EssentialsPlugin plugin, DatabaseConnection connection) {
        super(plugin, connection, "player_addresses");
    }

    public boolean upsert(UUID uniqueId, String address, Date firstSeen, Date lastSeen) {
        if (address == null || address.isBlank() || address.length() > 45) return false;

        boolean mysql = this.connection.getDatabaseConfiguration().getDatabaseType() != DatabaseType.SQLITE;
        String query = mysql
                ? "INSERT INTO " + getTableName() + " (unique_id, address, first_seen, last_seen) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE first_seen = LEAST(first_seen, VALUES(first_seen)), last_seen = GREATEST(last_seen, VALUES(last_seen))"
                : "INSERT INTO " + getTableName() + " (unique_id, address, first_seen, last_seen) VALUES (?, ?, ?, ?) ON CONFLICT(unique_id, address) DO UPDATE SET first_seen = MIN(first_seen, excluded.first_seen), last_seen = MAX(last_seen, excluded.last_seen)";

        try (PreparedStatement statement = getConnection().prepareStatement(query)) {
            statement.setString(1, uniqueId.toString());
            statement.setString(2, address);
            statement.setTimestamp(3, new Timestamp(firstSeen.getTime()));
            statement.setTimestamp(4, new Timestamp(lastSeen.getTime()));
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            this.plugin.getLogger().severe(exception.getMessage());
            return false;
        }
    }

    public List<PlayerAddressDTO> select(UUID uniqueId) {
        return select(PlayerAddressDTO.class, table -> table.where("unique_id", uniqueId).orderByDesc("last_seen"));
    }

    public boolean exists(UUID uniqueId, String address) {
        return select(PlayerAddressDTO.class, table -> table.where("unique_id", uniqueId).where("address", address)).stream().findFirst().isPresent();
    }

    public void backfillPlayTimes() {
        String prefix = this.connection.getDatabaseConfiguration().getTablePrefix();
        String query = "SELECT unique_id, address, MIN(created_at) AS first_seen, MAX(created_at) AS last_seen FROM " + prefix + "user_play_times WHERE address IS NOT NULL AND address <> '' GROUP BY unique_id, address";

        try (PreparedStatement statement = getConnection().prepareStatement(query); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    upsert(UUID.fromString(resultSet.getString("unique_id")), resultSet.getString("address"), resultSet.getTimestamp("first_seen"), resultSet.getTimestamp("last_seen"));
                } catch (RuntimeException exception) {
                    this.plugin.getLogger().severe(exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().severe(exception.getMessage());
        }
    }
}
