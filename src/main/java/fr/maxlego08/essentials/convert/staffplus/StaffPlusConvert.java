package fr.maxlego08.essentials.convert.staffplus;

import fr.maxlego08.essentials.api.EssentialsPlugin;
import fr.maxlego08.essentials.api.convert.Convert;
import fr.maxlego08.essentials.storage.database.repositeries.PlayerAddressRepository;
import fr.maxlego08.essentials.storage.database.repositeries.UserRepository;
import fr.maxlego08.essentials.storage.storages.SqlStorage;
import fr.maxlego08.essentials.zutils.utils.ZUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class StaffPlusConvert extends ZUtils implements Convert {

    private final EssentialsPlugin plugin;

    public StaffPlusConvert(EssentialsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void convert(CommandSender sender) {
        if (!(this.plugin.getStorageManager().getStorage() instanceof SqlStorage sqlStorage)) {
            notify(sender, "&cStaff+ migration requires SQL storage.");
            return;
        }
        File folder = new File(this.plugin.getDataFolder().getParentFile(), "Staff");
        File configurationFile = new File(folder, "config.yml");
        if (!configurationFile.isFile()) {
            notify(sender, "&cUnable to find plugins/Staff/config.yml.");
            return;
        }
        notify(sender, "&eStarting Staff+ IP migration...");
        this.plugin.getScheduler().runAsync(task -> migrate(sender, sqlStorage, folder, YamlConfiguration.loadConfiguration(configurationFile)));
    }

    private void migrate(CommandSender sender, SqlStorage sqlStorage, File folder, YamlConfiguration configuration) {
        int imported = 0;
        int skipped = 0;
        int invalid = 0;

        try {
            SourceData sourceData = readSource(folder, configuration);
            invalid = sourceData.invalid();
            UserRepository users = sqlStorage.with(UserRepository.class);
            PlayerAddressRepository addresses = sqlStorage.with(PlayerAddressRepository.class);
            for (StaffPlayer staffPlayer : sourceData.players()) {
                try {
                    UUID uniqueId = staffPlayer.uniqueId();
                    String username = staffPlayer.username();
                    String address = staffPlayer.address();
                    Date lastSeen = staffPlayer.lastSeen();
                    boolean exists = addresses.exists(uniqueId, address);
                    users.upsert(uniqueId, username);
                    if (!addresses.upsert(uniqueId, address, lastSeen, lastSeen)) {
                        invalid++;
                    } else if (exists) {
                        skipped++;
                    } else {
                        imported++;
                    }
                } catch (Exception exception) {
                    invalid++;
                }
            }
            notify(sender, "&aStaff+ migration completed. &fImported: &a" + imported + "&f, ignored: &e" + skipped + "&f, invalid: &c" + invalid + "&f.");
        } catch (Exception exception) {
            this.plugin.getLogger().severe(exception.getMessage());
            notify(sender, "&cStaff+ migration failed: " + exception.getMessage());
        }
    }

    private SourceData readSource(File folder, YamlConfiguration configuration) throws Exception {
        List<StaffPlayer> players = new ArrayList<>();
        int invalid = 0;
        try (Connection source = openSource(folder, configuration); Statement statement = source.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT uuid, username, ip_address, last_seen FROM staff_players")) {
            while (resultSet.next()) {
                try {
                    UUID uniqueId = UUID.fromString(resultSet.getString("uuid"));
                    String username = resultSet.getString("username");
                    String address = resultSet.getString("ip_address");
                    Timestamp timestamp = resultSet.getTimestamp("last_seen");
                    if (username == null || username.isBlank() || address == null || address.isBlank() || address.length() > 45) {
                        invalid++;
                        continue;
                    }
                    Date lastSeen = timestamp == null ? new Date() : new Date(timestamp.getTime());
                    players.add(new StaffPlayer(uniqueId, username, address, lastSeen));
                } catch (RuntimeException exception) {
                    invalid++;
                }
            }
        }
        return new SourceData(players, invalid);
    }

    private Connection openSource(File folder, YamlConfiguration configuration) throws Exception {
        if (configuration.getString("storage-type", "sqlite").equalsIgnoreCase("mysql")) {
            String host = configuration.getString("mysql-host", "localhost");
            int port = configuration.getInt("mysql-port", 3306);
            String database = configuration.getString("mysql-database", "minecraft");
            String username = configuration.getString("mysql-username", "root");
            String password = configuration.getString("mysql-password", "");
            boolean ssl = configuration.getBoolean("mysql-use-ssl", false);
            return DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + database + "?useSsl=" + ssl, username, password);
        }
        File database = new File(folder, "database.db");
        if (!database.isFile()) throw new IllegalStateException("Unable to find plugins/Staff/database.db");
        Class.forName("org.sqlite.JDBC");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 10000");
            return connection;
        } catch (Exception exception) {
            connection.close();
            throw exception;
        }
    }

    private void notify(CommandSender sender, String content) {
        if (sender instanceof Player player) {
            this.plugin.getScheduler().runAtEntity(player, task -> message(player, content));
        } else {
            message(sender, content);
        }
    }

    private record StaffPlayer(UUID uniqueId, String username, String address, Date lastSeen) {
    }

    private record SourceData(List<StaffPlayer> players, int invalid) {
    }
}
