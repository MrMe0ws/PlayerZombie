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

public class PlayerZombie extends JavaPlugin implements Listener {

    private boolean isListenerRegistered = false; // Флаг для регистрации слушателя

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

        // Логирование причины смерти
        // getLogger().info("Игрок " + player.getName() + " умер от " +
        // player.getLastDamageCause());

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
