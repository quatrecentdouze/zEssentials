package fr.maxlego08.essentials.module.modules.tlmstaff;

import fr.maxlego08.essentials.ZEssentialsPlugin;
import fr.maxlego08.essentials.api.dto.StaffModeSnapshotDTO;
import fr.maxlego08.essentials.api.dto.UserDTO;
import fr.maxlego08.essentials.api.event.events.user.UserQuitEvent;
import fr.maxlego08.essentials.api.user.Option;
import fr.maxlego08.essentials.api.user.User;
import fr.maxlego08.essentials.module.ZModule;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TLMStaffModule extends ZModule {

    private final NamespacedKey toolKey;
    private final NamespacedKey ownerKey;
    private final Map<UUID, StaffModeSnapshotDTO> activeSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, InspectSession> inspectSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<UUID>> teleportPools = new ConcurrentHashMap<>();
    private final Set<UUID> transitions = ConcurrentHashMap.newKeySet();
    private boolean altAlertEnabled;
    private String altAlertPermission;
    private String altAlertMessage;
    private String altAlertSeparator;
    private String altAlertOnlineFormat;
    private String altAlertOfflineFormat;
    private String altAlertBannedFormat;
    private boolean inspectEnabled;
    private String inspectTitle;
    private boolean staffModeEnabled;
    private String teleportExemptPermission;
    private Material vanishMaterial;
    private String vanishName;
    private List<String> vanishLore;
    private int vanishSlot;
    private boolean vanishGlow;
    private Material teleportMaterial;
    private String teleportName;
    private List<String> teleportLore;
    private int teleportSlot;
    private boolean teleportGlow;
    private String inspectOpenedMessage;
    private String inspectSelfMessage;
    private String inspectOfflineMessage;
    private String inspectTargetLeftMessage;
    private String staffEnabledMessage;
    private String staffDisabledMessage;
    private String staffTransitionMessage;
    private String staffStorageErrorMessage;
    private String teleportEmptyMessage;
    private String teleportSuccessMessage;

    public TLMStaffModule(ZEssentialsPlugin plugin) {
        super(plugin, "tlmstaff");
        this.toolKey = new NamespacedKey(plugin, "tlmstaff-tool");
        this.ownerKey = new NamespacedKey(plugin, "tlmstaff-owner");
    }

    @Override
    public void loadConfiguration() {
        super.loadConfiguration();
        var configuration = getConfiguration();
        this.altAlertEnabled = configuration.getBoolean("alt-alert.enabled", true);
        this.altAlertPermission = configuration.getString("alt-alert.permission", "essentials.tlmstaff.altalert");
        this.altAlertMessage = configuration.getString("alt-alert.message", "&8[&cAlt Alert&8] %player% &7joined with known accounts: %accounts%");
        this.altAlertSeparator = configuration.getString("alt-alert.separator", "&8, ");
        this.altAlertOnlineFormat = configuration.getString("alt-alert.formats.online", "&a%account%");
        this.altAlertOfflineFormat = configuration.getString("alt-alert.formats.offline", "&7%account%");
        this.altAlertBannedFormat = configuration.getString("alt-alert.formats.banned", "&c%account%");
        this.inspectEnabled = configuration.getBoolean("inspect.enabled", true);
        this.inspectTitle = configuration.getString("inspect.title", "&7Inspect: &a%player%");
        this.staffModeEnabled = configuration.getBoolean("staff-mode.enabled", true);
        this.teleportExemptPermission = configuration.getString("staff-mode.random-teleport-exempt-permission", "essentials.tlmstaff.randomteleport.exempt");
        this.vanishMaterial = readMaterial(configuration.getString("staff-mode.vanish-item.material"), Material.LIME_DYE);
        this.vanishName = configuration.getString("staff-mode.vanish-item.name", "&aVanish");
        this.vanishLore = configuration.getStringList("staff-mode.vanish-item.lore");
        this.vanishSlot = configuration.getInt("staff-mode.vanish-item.slot", 4);
        this.vanishGlow = configuration.getBoolean("staff-mode.vanish-item.glow", true);
        this.teleportMaterial = readMaterial(configuration.getString("staff-mode.teleport-item.material"), Material.COMPASS);
        this.teleportName = configuration.getString("staff-mode.teleport-item.name", "&bRandom teleport");
        this.teleportLore = configuration.getStringList("staff-mode.teleport-item.lore");
        this.teleportSlot = configuration.getInt("staff-mode.teleport-item.slot", 8);
        this.teleportGlow = configuration.getBoolean("staff-mode.teleport-item.glow", true);
        this.inspectOpenedMessage = configuration.getString("messages.inspect-opened", "&aYou are inspecting &f%player%&a.");
        this.inspectSelfMessage = configuration.getString("messages.inspect-self", "&cYou cannot inspect yourself.");
        this.inspectOfflineMessage = configuration.getString("messages.inspect-offline", "&cThis player is not online.");
        this.inspectTargetLeftMessage = configuration.getString("messages.inspect-target-left", "&cThe inspected player disconnected.");
        this.staffEnabledMessage = configuration.getString("messages.staff-enabled", "&aStaff mode enabled.");
        this.staffDisabledMessage = configuration.getString("messages.staff-disabled", "&cStaff mode disabled.");
        this.staffTransitionMessage = configuration.getString("messages.staff-transition", "&eA staff mode transition is already running.");
        this.staffStorageErrorMessage = configuration.getString("messages.staff-storage-error", "&cUnable to persist or restore your staff inventory.");
        this.teleportEmptyMessage = configuration.getString("messages.teleport-empty", "&cNo eligible player is online.");
        this.teleportSuccessMessage = configuration.getString("messages.teleport-success", "&aTeleported to &f%player%&a.");
    }

    private Material readMaterial(String value, Material fallback) {
        Material material = value == null ? null : Material.matchMaterial(value);
        return material == null ? fallback : material;
    }

    public void toggleStaff(Player player) {
        if (!staffModeEnabled) {
            message(player, staffStorageErrorMessage);
            return;
        }

        UUID uniqueId = player.getUniqueId();
        if (!transitions.add(uniqueId)) {
            message(player, staffTransitionMessage);
            return;
        }

        StaffModeSnapshotDTO active = activeSnapshots.get(uniqueId);
        if (active != null) {
            this.plugin.getScheduler().runAtEntity(player, task -> restoreNow(player, active, true));
            return;
        }

        this.plugin.getScheduler().runAsync(task -> {
            Optional<StaffModeSnapshotDTO> stored = getStorage().getStaffModeSnapshot(uniqueId);
            if (stored.isPresent()) {
                StaffModeSnapshotDTO snapshot = stored.get();
                activeSnapshots.put(uniqueId, snapshot);
                this.plugin.getScheduler().runAtEntity(player, entityTask -> restoreNow(player, snapshot, true));
                return;
            }
            this.plugin.getScheduler().runAtEntityWithFallback(player, entityTask -> saveAndActivate(player), () -> transitions.remove(uniqueId));
        });
    }

    private void saveAndActivate(Player player) {
        UUID uniqueId = player.getUniqueId();
        StaffModeSnapshotDTO snapshot;
        try {
            snapshot = captureSnapshot(player);
        } catch (RuntimeException exception) {
            transitions.remove(uniqueId);
            message(player, staffStorageErrorMessage);
            return;
        }

        this.plugin.getScheduler().runAsync(task -> {
            if (!getStorage().upsertStaffModeSnapshot(snapshot)) {
                transitions.remove(uniqueId);
                this.plugin.getScheduler().runAtEntity(player, entityTask -> message(player, staffStorageErrorMessage));
                return;
            }
            activeSnapshots.put(uniqueId, snapshot);
            this.plugin.getScheduler().runAtEntityWithFallback(player, entityTask -> activateNow(player), () -> transitions.remove(uniqueId));
        });
    }

    private StaffModeSnapshotDTO captureSnapshot(Player player) {
        User user = getUser(player);
        boolean vanished = user != null && user.getOption(Option.VANISH);
        Date now = new Date();
        return new StaffModeSnapshotDTO(player.getUniqueId(), StaffInventoryCodec.encode(player.getInventory().getStorageContents()),
                StaffInventoryCodec.encode(player.getInventory().getArmorContents()), StaffInventoryCodec.encode(new ItemStack[]{player.getInventory().getItemInOffHand()}),
                player.getInventory().getHeldItemSlot(), player.getGameMode().name(), player.getAllowFlight(), player.isFlying(), vanished, now, now);
    }

    private void activateNow(Player player) {
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        player.setGameMode(GameMode.CREATIVE);
        player.getInventory().setItem(normalizeSlot(vanishSlot), createTool(player, "vanish", vanishMaterial, vanishName, vanishLore, vanishGlow));
        player.getInventory().setItem(normalizeSlot(teleportSlot), createTool(player, "teleport", teleportMaterial, teleportName, teleportLore, teleportGlow));
        transitions.remove(player.getUniqueId());
        message(player, staffEnabledMessage);
    }

    private int normalizeSlot(int slot) {
        return Math.max(0, Math.min(35, slot));
    }

    private ItemStack createTool(Player owner, String type, Material material, String name, List<String> lore, boolean glow) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(color(name));
        itemMeta.setLore(lore.stream().map(this::color).toList());
        itemMeta.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, type);
        itemMeta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, owner.getUniqueId().toString());
        if (glow) {
            itemMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void restoreNow(Player player, StaffModeSnapshotDTO snapshot, boolean notify) {
        try {
            player.closeInventory();
            player.getInventory().clear();
            player.getInventory().setStorageContents(StaffInventoryCodec.decode(snapshot.inventory()));
            player.getInventory().setArmorContents(StaffInventoryCodec.decode(snapshot.armor()));
            ItemStack[] offhand = StaffInventoryCodec.decode(snapshot.offhand());
            player.getInventory().setItemInOffHand(offhand.length == 0 || offhand[0] == null ? new ItemStack(Material.AIR) : offhand[0]);
            player.getInventory().setHeldItemSlot(Math.max(0, Math.min(8, snapshot.held_slot())));
            player.setGameMode(GameMode.valueOf(snapshot.game_mode()));
            player.setFlying(false);
            player.setAllowFlight(snapshot.allow_flight());
            if (snapshot.allow_flight() && snapshot.flying()) player.setFlying(true);
            User user = getUser(player);
            if (user != null) {
                user.setOption(Option.VANISH, snapshot.vanished());
                updateVanishState(plugin, player, snapshot.vanished());
            }
            if (!getStorage().deleteStaffModeSnapshot(player.getUniqueId())) {
                transitions.remove(player.getUniqueId());
                message(player, staffStorageErrorMessage);
                return;
            }
            activeSnapshots.remove(player.getUniqueId());
            teleportPools.remove(player.getUniqueId());
            transitions.remove(player.getUniqueId());
            if (notify) message(player, staffDisabledMessage);
        } catch (RuntimeException exception) {
            transitions.remove(player.getUniqueId());
            message(player, staffStorageErrorMessage);
            this.plugin.getLogger().severe(exception.getMessage());
        }
    }

    public void openInspect(Player staff, Player target) {
        if (!inspectEnabled || !target.isOnline()) {
            message(staff, inspectOfflineMessage);
            return;
        }
        if (staff.getUniqueId().equals(target.getUniqueId())) {
            message(staff, inspectSelfMessage);
            return;
        }

        this.plugin.getScheduler().runAtEntityWithFallback(target, task -> {
            InspectData data = captureInspectData(target);
            this.plugin.getScheduler().runAtEntityWithFallback(staff, staffTask -> openInspectNow(staff, target.getUniqueId(), target.getName(), data), () -> inspectSessions.remove(staff.getUniqueId()));
        }, () -> this.plugin.getScheduler().runAtEntity(staff, task -> message(staff, inspectOfflineMessage)));
    }

    private void openInspectNow(Player staff, UUID targetUniqueId, String targetName, InspectData data) {
        InspectHolder holder = new InspectHolder(targetUniqueId);
        Inventory inventory = this.plugin.getComponentMessage().createInventory(inspectTitle.replace("%player%", targetName), 54, holder);
        holder.setInventory(inventory);
        renderInspect(inventory, data);
        inspectSessions.put(staff.getUniqueId(), new InspectSession(staff.getUniqueId(), targetUniqueId, inventory));
        Player target = Bukkit.getPlayer(targetUniqueId);
        if (target == null || !target.isOnline()) {
            inspectSessions.remove(staff.getUniqueId());
            message(staff, inspectOfflineMessage);
            return;
        }
        staff.openInventory(inventory);
        message(staff, inspectOpenedMessage.replace("%player%", targetName));
    }

    private InspectData captureInspectData(Player target) {
        ItemStack[] main = new ItemStack[36];
        for (int index = 0; index < main.length; index++) main[index] = cloneItem(target.getInventory().getItem(index));
        ItemStack[] armor = new ItemStack[]{cloneItem(target.getInventory().getHelmet()), cloneItem(target.getInventory().getChestplate()), cloneItem(target.getInventory().getLeggings()), cloneItem(target.getInventory().getBoots())};
        List<String> effects = new ArrayList<>();
        for (PotionEffect effect : target.getActivePotionEffects()) {
            String name = effect.getType().getName().replace('_', ' ').toLowerCase();
            effects.add("&7- " + name + " " + (effect.getAmplifier() + 1) + " (" + (effect.getDuration() / 20) + "s)");
        }
        return new InspectData(main, armor, cloneItem(target.getInventory().getItemInOffHand()), effects, target.getHealth(), target.getMaxHealth(), target.getFoodLevel(), target.getLevel());
    }

    private ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    private void renderInspect(Inventory inventory, InspectData data) {
        for (int index = 0; index < 36; index++) inventory.setItem(index, cloneItem(data.main()[index]));
        ItemStack separator = displayItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int index = 36; index <= 44; index++) inventory.setItem(index, separator);
        for (int index = 0; index < 4; index++) inventory.setItem(45 + index, cloneItem(data.armor()[index]));
        inventory.setItem(49, cloneItem(data.offhand()));
        inventory.setItem(50, displayItem(Material.ENDER_CHEST, "&aEnder Chest", List.of("&7Click to inspect the target's Ender Chest.")));
        List<String> effectLore = data.effects().isEmpty() ? List.of("&7No active potion effects.") : data.effects();
        inventory.setItem(51, displayItem(Material.POTION, "&ePotion Effects", effectLore));
        inventory.setItem(52, displayItem(Material.GOLDEN_APPLE, "&cTarget Health & Hunger", List.of("&7Health: &c" + String.format("%.1f", data.health()) + " / " + String.format("%.1f", data.maxHealth()), "&7Food Level: &e" + data.food() + " / 20", "&7XP Level: &a" + data.level())));
        inventory.setItem(53, displayItem(Material.BARRIER, "&cClose Viewer", List.of()));
    }

    private ItemStack displayItem(Material material, String name, List<String> lore) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(color(name));
        itemMeta.setLore(lore.stream().map(this::color).toList());
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void syncInspect(InspectSession session) {
        Player target = Bukkit.getPlayer(session.targetUniqueId());
        if (target == null || !target.isOnline()) return;
        ItemStack[] main = new ItemStack[36];
        for (int index = 0; index < 36; index++) main[index] = cloneItem(session.inventory().getItem(index));
        ItemStack[] armor = new ItemStack[4];
        for (int index = 0; index < 4; index++) armor[index] = cloneItem(session.inventory().getItem(45 + index));
        ItemStack offhand = cloneItem(session.inventory().getItem(49));
        this.plugin.getScheduler().runAtEntityWithFallback(target, task -> {
            for (int index = 0; index < 36; index++) target.getInventory().setItem(index, main[index]);
            target.getInventory().setHelmet(armor[0]);
            target.getInventory().setChestplate(armor[1]);
            target.getInventory().setLeggings(armor[2]);
            target.getInventory().setBoots(armor[3]);
            target.getInventory().setItemInOffHand(offhand == null ? new ItemStack(Material.AIR) : offhand);
            refreshTargetViewers(target);
        }, () -> closeInspectTarget(session.targetUniqueId()));
    }

    private void refreshTargetViewers(Player target) {
        if (inspectSessions.values().stream().noneMatch(session -> session.targetUniqueId().equals(target.getUniqueId()))) return;
        InspectData data = captureInspectData(target);
        for (InspectSession session : List.copyOf(inspectSessions.values())) {
            if (!session.targetUniqueId().equals(target.getUniqueId())) continue;
            Player viewer = Bukkit.getPlayer(session.viewerUniqueId());
            if (viewer == null) continue;
            this.plugin.getScheduler().runAtEntity(viewer, task -> {
                if (viewer.getOpenInventory().getTopInventory().equals(session.inventory())) renderInspect(session.inventory(), data);
            });
        }
    }

    private void refreshLater(Player target) {
        if (inspectSessions.values().stream().noneMatch(session -> session.targetUniqueId().equals(target.getUniqueId()))) return;
        this.plugin.getScheduler().runAtEntityLater(target, () -> refreshTargetViewers(target), 1L);
    }

    private void closeInspectTarget(UUID targetUniqueId) {
        for (InspectSession session : List.copyOf(inspectSessions.values())) {
            if (!session.targetUniqueId().equals(targetUniqueId)) continue;
            inspectSessions.remove(session.viewerUniqueId());
            Player viewer = Bukkit.getPlayer(session.viewerUniqueId());
            if (viewer == null) continue;
            this.plugin.getScheduler().runAtEntity(viewer, task -> {
                viewer.closeInventory();
                message(viewer, inspectTargetLeftMessage);
            });
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        User user = getUser(player);
        if (user == null) return;
        UUID uniqueId = user.getUniqueId();
        String address = user.getAddress();
        if ((address == null || address.isBlank()) && player.getAddress() != null && player.getAddress().getAddress() != null) {
            address = player.getAddress().getAddress().getHostAddress();
            user.setAddress(address);
        }
        if (address == null || address.isBlank()) return;
        Date now = new Date();
        String playerName = player.getName();
        String finalAddress = address;

        this.plugin.getScheduler().runAsync(task -> {
            getStorage().upsertPlayerAddress(uniqueId, finalAddress, now, now);
            Optional<StaffModeSnapshotDTO> snapshot = getStorage().getStaffModeSnapshot(uniqueId);
            List<UserDTO> accounts = List.of();
            Map<UUID, Boolean> banned = new HashMap<>();
            if (isEnable && altAlertEnabled) {
                try {
                    accounts = getStorage().getUsers(finalAddress).stream().filter(dto -> !dto.unique_id().equals(uniqueId)).toList();
                    for (UserDTO account : accounts) banned.put(account.unique_id(), getStorage().isBan(account.unique_id()));
                } catch (RuntimeException exception) {
                    this.plugin.getLogger().severe(exception.getMessage());
                }
            }
            if (snapshot.isPresent()) {
                StaffModeSnapshotDTO stored = snapshot.get();
                activeSnapshots.put(uniqueId, stored);
                this.plugin.getScheduler().runAtEntity(player, entityTask -> restoreNow(player, stored, false));
            }
            List<UserDTO> finalAccounts = accounts;
            if (!finalAccounts.isEmpty()) this.plugin.getScheduler().runNextTick(globalTask -> broadcastAltAlert(playerName, finalAccounts, banned));
        });
    }

    private void broadcastAltAlert(String playerName, List<UserDTO> accounts, Map<UUID, Boolean> banned) {
        String formattedAccounts = accounts.stream().map(account -> formatAccount(account, banned.getOrDefault(account.unique_id(), false))).collect(Collectors.joining(altAlertSeparator));
        String formattedPlayer = altAlertOnlineFormat.replace("%account%", playerName);
        String alert = altAlertMessage.replace("%player%", formattedPlayer).replace("%accounts%", formattedAccounts);
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (!recipient.hasPermission(altAlertPermission)) continue;
            this.plugin.getScheduler().runAtEntity(recipient, task -> message(recipient, alert));
        }
    }

    private String formatAccount(UserDTO account, boolean storageBanned) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(account.unique_id());
        boolean isBanned = storageBanned || offlinePlayer.isBanned();
        String format = isBanned ? altAlertBannedFormat : offlinePlayer.isOnline() ? altAlertOnlineFormat : altAlertOfflineFormat;
        return format.replace("%account%", account.name());
    }

    @EventHandler
    public void onUserQuit(UserQuitEvent event) {
        UUID uniqueId = event.getUser().getUniqueId();
        StaffModeSnapshotDTO snapshot = activeSnapshots.get(uniqueId);
        Player player = event.getUser().getPlayer();
        if (snapshot != null && player != null) restoreNow(player, snapshot, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof InspectHolder holder && event.getWhoClicked() instanceof Player viewer) {
            InspectSession session = inspectSessions.get(viewer.getUniqueId());
            if (session == null || !session.targetUniqueId().equals(holder.getTargetUniqueId())) {
                event.setCancelled(true);
                return;
            }
            int slot = event.getRawSlot();
            if (slot >= 36 && slot <= 44 || slot >= 50 && slot <= 53) {
                event.setCancelled(true);
                if (slot == 53) viewer.closeInventory();
                if (slot == 50) {
                    Player target = Bukkit.getPlayer(session.targetUniqueId());
                    if (target != null) {
                        viewer.closeInventory();
                        viewer.openInventory(target.getEnderChest());
                    }
                }
                return;
            }
            this.plugin.getScheduler().runAtEntityLater(viewer, () -> syncInspect(session), 1L);
            return;
        }
        if (event.getWhoClicked() instanceof Player player) refreshLater(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof InspectHolder) || !(event.getWhoClicked() instanceof Player viewer)) return;
        for (int slot : event.getRawSlots()) {
            if (slot >= 36 && slot <= 44 || slot >= 50 && slot <= 53) {
                event.setCancelled(true);
                return;
            }
        }
        InspectSession session = inspectSessions.get(viewer.getUniqueId());
        if (session != null) this.plugin.getScheduler().runAtEntityLater(viewer, () -> syncInspect(session), 1L);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof InspectHolder) inspectSessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemStack = event.getItem();
        String tool = readTool(player, itemStack);
        if (tool != null) {
            event.setCancelled(true);
            if (tool.equals("vanish")) toggleVanish(player);
            if (tool.equals("teleport")) teleportRandom(player);
        }
        refreshLater(player);
    }

    private String readTool(Player player, ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta() || !activeSnapshots.containsKey(player.getUniqueId())) return null;
        ItemMeta itemMeta = itemStack.getItemMeta();
        String owner = itemMeta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        if (!player.getUniqueId().toString().equals(owner)) return null;
        return itemMeta.getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    private boolean isStaffTool(ItemStack itemStack) {
        return itemStack != null && itemStack.hasItemMeta() && itemStack.getItemMeta().getPersistentDataContainer().has(toolKey, PersistentDataType.STRING);
    }

    private void toggleVanish(Player player) {
        User user = getUser(player);
        if (user == null) return;
        boolean vanished = !user.getOption(Option.VANISH);
        user.setOption(Option.VANISH, vanished);
        updateVanishState(plugin, player, vanished);
    }

    private void teleportRandom(Player staff) {
        UUID staffUniqueId = staff.getUniqueId();
        Deque<UUID> pool = teleportPools.computeIfAbsent(staffUniqueId, ignored -> new ArrayDeque<>());
        pool.removeIf(uniqueId -> !isEligibleTarget(staff, Bukkit.getPlayer(uniqueId)));
        if (pool.isEmpty()) {
            List<UUID> eligible = Bukkit.getOnlinePlayers().stream().filter(player -> isEligibleTarget(staff, player)).map(Entity::getUniqueId).collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(eligible);
            pool.addAll(eligible);
        }
        UUID targetUniqueId = pool.pollFirst();
        Player target = targetUniqueId == null ? null : Bukkit.getPlayer(targetUniqueId);
        if (target == null) {
            message(staff, teleportEmptyMessage);
            return;
        }
        this.plugin.getScheduler().runAtEntityWithFallback(target, task -> {
            var location = target.getLocation().clone();
            this.plugin.getScheduler().teleportAsync(staff, location).thenAccept(success -> {
                if (success) this.plugin.getScheduler().runAtEntity(staff, staffTask -> message(staff, teleportSuccessMessage.replace("%player%", target.getName())));
            });
        }, () -> this.plugin.getScheduler().runAtEntity(staff, task -> message(staff, teleportEmptyMessage)));
    }

    private boolean isEligibleTarget(Player staff, Player target) {
        return target != null && target.isOnline() && !target.getUniqueId().equals(staff.getUniqueId()) && (teleportExemptPermission == null || teleportExemptPermission.isBlank() || !target.hasPermission(teleportExemptPermission));
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isStaffTool(event.getItemDrop().getItemStack())) event.getItemDrop().remove();
        refreshLater(event.getPlayer());
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) refreshLater(player);
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        refreshLater(event.getPlayer());
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        refreshLater(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uniqueId = event.getPlayer().getUniqueId();
        inspectSessions.remove(uniqueId);
        closeInspectTarget(uniqueId);
        teleportPools.remove(uniqueId);
        transitions.remove(uniqueId);
    }

    @Override
    public void onDisable() {
        for (Map.Entry<UUID, StaffModeSnapshotDTO> entry : List.copyOf(activeSnapshots.entrySet())) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) restoreNow(player, entry.getValue(), false);
        }
    }

    private record InspectSession(UUID viewerUniqueId, UUID targetUniqueId, Inventory inventory) {
    }

    private record InspectData(ItemStack[] main, ItemStack[] armor, ItemStack offhand, List<String> effects,
                               double health, double maxHealth, int food, int level) {
    }
}
