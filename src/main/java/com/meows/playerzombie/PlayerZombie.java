package com.meows.playerzombie;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class PlayerZombie extends JavaPlugin implements Listener {

    private boolean isListenerRegistered = false; // Флаг для регистрации слушателя
    private Map<UUID, Long> lastZombieSpawnTime = new HashMap<>(); // Хранит время последнего появления зомби для каждого игрока
    private Random random = new Random(); // Генератор случайных чисел для проверки шанса

    @Override
    public void onEnable() {
        // Регистрация событий, если это ещё не сделано
        if (!isListenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, this);
            isListenerRegistered = true;
        }

        // Сохранение конфигурации, если её ещё нет
        saveDefaultConfig();

        getLogger().info("PlayerZombie включён!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PlayerZombie выключен!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation();
        UUID playerUUID = player.getUniqueId();

        // Логирование причины смерти
        // getLogger().info("Игрок " + player.getName() + " умер от " +
        // player.getLastDamageCause());

        // Проверка кулдауна между появлениями зомби
        int cooldownSeconds = getConfig().getInt("zombie-spawn-cooldown", 10);
        if (cooldownSeconds > 0) {
            long currentTime = System.currentTimeMillis();
            if (lastZombieSpawnTime.containsKey(playerUUID)) {
                long lastSpawnTime = lastZombieSpawnTime.get(playerUUID);
                long timeSinceLastSpawn = (currentTime - lastSpawnTime) / 1000; // В секундах
                
                if (timeSinceLastSpawn < cooldownSeconds) {
                    // Кулдаун ещё не прошёл, зомби не создаём
                    return;
                }
            }
        }

        // Проверка шанса появления зомби
        int spawnChance = getConfig().getInt("zombie-spawn-chance", 50);
        spawnChance = Math.max(0, Math.min(100, spawnChance)); // Ограничиваем от 0 до 100
        if (spawnChance < 100) {
            int randomValue = random.nextInt(100) + 1; // От 1 до 100
            if (randomValue > spawnChance) {
                // Шанс не выпал, зомби не создаём
                return;
            }
        }

        // Убеждаемся, что чанк загружен перед созданием зомби
        // Это важно, когда игрок умирает один и чанк может быть выгружен
        if (!deathLocation.getChunk().isLoaded()) {
            deathLocation.getChunk().load(true);
        }

        // Получение типа зомби из конфига
        String zombieType = getConfig().getString("zombie-type", "ZOMBIE");
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(zombieType.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Неверный тип зомби в конфиге: " + zombieType + ". Используется ZOMBIE.");
            entityType = EntityType.ZOMBIE;
        }

        // Создание зомби указанного типа
        if (entityType == EntityType.ZOMBIE || entityType == EntityType.HUSK) {
            Zombie zombie = (Zombie) deathLocation.getWorld().spawnEntity(deathLocation, entityType);
            zombie.setCustomName(player.getName()); // Установка имени
            zombie.setCustomNameVisible(true); // Делает имя видимым

            // Проверка, может ли зомби подбирать вещи
            boolean canPickupItems = getConfig().getBoolean("zombie-can-pickup-items", false);
            zombie.setCanPickupItems(canPickupItems);
            
            // Обновляем время последнего появления зомби для этого игрока
            if (cooldownSeconds > 0) {
                lastZombieSpawnTime.put(playerUUID, System.currentTimeMillis());
            }
        } else {
            getLogger().warning("Указанный тип " + zombieType + " не является зомби. Сущность не создана.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("playerzombie")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("playerzombie.reload")) {
                    reloadPlugin(sender);
                } else {
                    sender.sendMessage("§cУ вас нет прав для выполнения этой команды!");
                }
                return true;
            }
        }
        return false;
    }

    private void reloadPlugin(CommandSender sender) {
        // Выключение плагина
        onDisable();
        // Перезагрузка конфигурации
        reloadConfig();
        // Включение плагина
        onEnable();
        sender.sendMessage("§aПлагин PlayerZombie полностью перезагружен!");
    }
}
