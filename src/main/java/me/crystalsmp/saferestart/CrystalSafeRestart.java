package me.crystalsmp.saferestart;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;

public final class CrystalSafeRestart extends JavaPlugin implements Listener {
    private BukkitTask task;
    private int remaining;
    private final Map<UUID, Boolean> oldInvulnerable = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Crystal-SafeRestart enabled.");
    }

    @Override public void onDisable() {
        if (task != null) task.cancel();
        restore();
    }

    private String c(String s) { return ChatColor.translateAlternateColorCodes('&', s); }
    private String m(String key) {
        return c(getConfig().getString("messages.prefix","") + getConfig().getString("messages."+key,""));
    }

    private Location safe() {
        World w = Bukkit.getWorld(getConfig().getString("safe-location.world","world"));
        if (w == null) return null;
        return new Location(w,
            getConfig().getDouble("safe-location.x"),
            getConfig().getDouble("safe-location.y"),
            getConfig().getDouble("safe-location.z"),
            (float)getConfig().getDouble("safe-location.yaw"),
            (float)getConfig().getDouble("safe-location.pitch"));
    }

    private void start(CommandSender sender) {
        if (task != null) { sender.sendMessage(m("already-running")); return; }
        Location loc = safe();
        if (loc == null) { sender.sendMessage(m("no-location")); return; }

        remaining = Math.max(1, getConfig().getInt("countdown-seconds",60));

        for (Player p : Bukkit.getOnlinePlayers()) {
            oldInvulnerable.put(p.getUniqueId(), p.isInvulnerable());
            p.setInvulnerable(true);
            p.teleport(loc);
            p.sendMessage(m("started").replace("%seconds%", String.valueOf(remaining)));
        }
        Bukkit.broadcastMessage(m("teleported"));

        task = Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (remaining <= 0) {
                task.cancel();
                task = null;
                restore();
                Bukkit.broadcastMessage(c("&c&lСервер перезагружается..."));
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                return;
            }
            if (remaining <= 10 || remaining % 10 == 0) {
                Bukkit.broadcastMessage(m("countdown").replace("%seconds%", String.valueOf(remaining)));
            }
            remaining--;
        }, 20L, 20L);
    }

    private void cancel(CommandSender sender) {
        if (task == null) { sender.sendMessage(m("cancelled")); return; }
        task.cancel(); task = null; remaining = 0;
        restore();
        Bukkit.broadcastMessage(m("cancelled"));
    }

    private void restore() {
        for (Map.Entry<UUID,Boolean> e : oldInvulnerable.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p != null && p.isOnline()) p.setInvulnerable(e.getValue());
        }
        oldInvulnerable.clear();
    }

    @EventHandler public void damage(EntityDamageEvent e) {
        if (task != null && e.getEntity() instanceof Player) e.setCancelled(true);
    }

    @EventHandler public void damageByEntity(EntityDamageByEntityEvent e) {
        if (task != null && (e.getEntity() instanceof Player || e.getDamager() instanceof Player))
            e.setCancelled(true);
    }

    @EventHandler public void quit(PlayerQuitEvent e) {
        oldInvulnerable.remove(e.getPlayer().getUniqueId());
    }

    @Override public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("saferestart")) {
            if (!s.hasPermission("crystaleffects.restart")) { s.sendMessage(m("no-permission")); return true; }
            start(s); return true;
        }
        if (cmd.getName().equalsIgnoreCase("restartcancel")) {
            if (!s.hasPermission("crystaleffects.restart")) { s.sendMessage(m("no-permission")); return true; }
            cancel(s); return true;
        }
        if (cmd.getName().equalsIgnoreCase("restartset")) {
            if (!s.hasPermission("crystaleffects.restart.set")) { s.sendMessage(m("no-permission")); return true; }
            if (!(s instanceof Player p)) { s.sendMessage("Только игрок может установить точку."); return true; }
            Location l = p.getLocation();
            getConfig().set("safe-location.world", l.getWorld().getName());
            getConfig().set("safe-location.x", l.getX());
            getConfig().set("safe-location.y", l.getY());
            getConfig().set("safe-location.z", l.getZ());
            getConfig().set("safe-location.yaw", l.getYaw());
            getConfig().set("safe-location.pitch", l.getPitch());
            saveConfig();
            p.sendMessage(m("location-set")); return true;
        }
        return false;
    }
}
