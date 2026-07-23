package fr.maxlego08.essentials.storage.database.repositeries;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.dto.PlayerAddressDTO;
import fr.maxlego08.essentials.storage.database.Repository;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.database.DatabaseType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
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

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
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
        String query = "SELECT pt.unique_id, pt.address, MIN(pt.created_at) AS first_seen, MAX(pt.created_at) AS last_seen FROM " + prefix + "user_play_times pt LEFT JOIN " + getTableName() + " pa ON pa.unique_id = pt.unique_id AND pa.address = pt.address WHERE pt.address IS NOT NULL AND pt.address <> '' AND pa.unique_id IS NULL GROUP BY pt.unique_id, pt.address";
        List<PlayerAddressDTO> addresses = new ArrayList<>();

        try (Connection connection = getConnection(); PreparedStatement statement = connection.prepareStatement(query); ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                try {
                    Timestamp firstSeen = resultSet.getTimestamp("first_seen");
                    Timestamp lastSeen = resultSet.getTimestamp("last_seen");
                    String address = resultSet.getString("address");
                    if (firstSeen == null || lastSeen == null || address == null || address.length() > 45) continue;
                    addresses.add(new PlayerAddressDTO(UUID.fromString(resultSet.getString("unique_id")), address, firstSeen, lastSeen));
                } catch (RuntimeException exception) {
                    this.plugin.getLogger().severe(exception.getMessage());
                }
            }
        } catch (SQLException exception) {
            this.plugin.getLogger().severe(exception.getMessage());
            return;
        }

        addresses.forEach(address -> upsert(address.unique_id(), address.address(), address.first_seen(), address.last_seen()));
    }
}
