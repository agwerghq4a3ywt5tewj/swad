package com.fallengod.testament.bosses.impl;

import com.fallengod.testament.TestamentPlugin;
import com.fallengod.testament.bosses.GodBoss;
import com.fallengod.testament.enums.BossType;
import com.fallengod.testament.enums.GodType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChronosBoss extends GodBoss {
    
    private final Map<UUID, Location> playerPreviousLocations;
    private final Map<UUID, Double> playerPreviousHealth;
    private int temporalRewindCooldown = 0;
    
    public ChronosBoss(TestamentPlugin plugin) {
        super(plugin, BossType.TIME_KEEPER, GodType.TIME);
        this.playerPreviousLocations = new HashMap<>();
        this.playerPreviousHealth = new HashMap<>();
    }
    
    @Override
    protected void setupBoss() {
        if (entity instanceof Shulker shulker) {
            shulker.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
            shulker.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1));
            shulker.setCustomName("§d§lChronos Incarnate");
            shulker.setCustomNameVisible(true);
            shulker.setGlowing(true);
        }
        
        // Start tracking player states for temporal rewind
        startPlayerStateTracking();
    }
    
    @Override
    protected void useSpecialAbility() {
        switch (phase) {
            case 1 -> timeDilation();
            case 2 -> ageAcceleration();
            case 3 -> temporalRewind();
        }
    }
    
    private void timeDilation() {
        // Slows all players to 10% speed
        Location center = entity.getLocation();
        
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 25) {
                // Extreme slowness effect
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 400, 9)); // Level 10 slowness
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 400, 5));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 400, 3));
                
                player.sendMessage("§d§lTime Dilation! §7Time crawls to a near standstill!");
                player.sendActionBar("§d⏰ Time flows like molasses around you");
            }
        }
        
        // Time distortion effects
        for (int radius = 5; radius <= 25; radius += 5) {
            for (double angle = 0; angle < 360; angle += 30) {
                double x = center.getX() + radius * Math.cos(Math.toRadians(angle));
                double z = center.getZ() + radius * Math.sin(Math.toRadians(angle));
                Location particleLoc = new Location(center.getWorld(), x, center.getY() + 1, z);
                
                center.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                center.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 2, 0.2, 0.2, 0.2, 0.02);
            }
        }
        
        entity.getWorld().playSound(center, Sound.BLOCK_BEACON_AMBIENT, 2.0f, 0.3f);
        entity.getWorld().playSound(center, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 0.8f);
        
        // Announce
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 30) {
                player.sendMessage("§d§lTime Dilation! §7Chronos bends time to his will!");
            }
        }
    }
    
    private void ageAcceleration() {
        // Rapidly damages armor and weapons
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(entity.getLocation()) <= 20) {
                // Age all equipment
                for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
                    var item = player.getInventory().getItem(slot);
                    if (item != null && item.getType().getMaxDurability() > 0) {
                        // Reduce durability by 20-50 points
                        int damage = 20 + (int)(Math.random() * 30);
                        int currentDurability = item.getType().getMaxDurability() - 
                            (item.hasItemMeta() && item.getItemMeta().hasEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING) ? 
                             item.getDurability() / 2 : item.getDurability());
                        
                        if (currentDurability > damage) {
                            item.setDurability((short)(item.getDurability() + damage));
                        } else {
                            // Item breaks
                            player.getInventory().setItem(slot, null);
                            player.sendMessage("§c⚠ Your " + item.getType().name().toLowerCase().replace("_", " ") + " crumbles to dust!");
                        }
                    }
                }
                
                // Visual aging effects
                player.getWorld().spawnParticle(Particle.ASH, player.getLocation().add(0, 1, 0), 30, 1, 1, 1, 0.1);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 20, 0.5, 0.5, 0.5, 0.05);
                
                // Damage player
                player.damage(8.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                player.sendMessage("§d§lAge Acceleration! §7Time ravages your equipment!");
            }
        }
        
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 2.0f, 0.5f);
    }
    
    private void temporalRewind() {
        if (temporalRewindCooldown > 0) {
            temporalRewindCooldown--;
            return;
        }
        
        // Resets boss health to previous state and teleports players back
        double currentHealth = entity.getHealth();
        double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        
        // Heal boss significantly
        double healAmount = Math.min(maxHealth * 0.3, maxHealth - currentHealth);
        entity.setHealth(currentHealth + healAmount);
        
        // Rewind players to previous positions and health
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(entity.getLocation()) <= 35) {
                UUID playerId = player.getUniqueId();
                
                // Teleport to previous location
                Location prevLoc = playerPreviousLocations.get(playerId);
                if (prevLoc != null) {
                    // Temporal rewind effects at current location
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 50, 1, 1, 1, 0.2);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.5f);
                    
                    player.teleport(prevLoc);
                    
                    // Temporal rewind effects at previous location
                    player.getWorld().spawnParticle(Particle.PORTAL, prevLoc, 50, 1, 1, 1, 0.2);
                    player.getWorld().spawnParticle(Particle.END_ROD, prevLoc, 30, 1, 1, 1, 0.1);
                }
                
                // Restore previous health
                Double prevHealth = playerPreviousHealth.get(playerId);
                if (prevHealth != null && prevHealth > player.getHealth()) {
                    player.setHealth(Math.min(player.getMaxHealth(), prevHealth));
                }
                
                player.sendMessage("§d§lTemporal Rewind! §7Time flows backward!");
                player.sendActionBar("§d⏰ You are pulled back through time");
            }
        }
        
        // Boss rewind effects
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 100, 3, 3, 3, 0.2);
        entity.getWorld().spawnParticle(Particle.ENCHANT, entity.getLocation(), 80, 2, 2, 2, 0.15);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.8f);
        
        temporalRewindCooldown = 120; // 6 second cooldown
        
        // Announce
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(entity.getLocation()) <= 40) {
                player.sendMessage("§d§lTemporal Rewind! §7Chronos undoes the flow of time!");
            }
        }
    }
    
    private void startPlayerStateTracking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Store player states every 5 seconds for temporal rewind
                for (Player player : entity.getWorld().getPlayers()) {
                    if (player.getLocation().distance(entity.getLocation()) <= 50) {
                        UUID playerId = player.getUniqueId();
                        playerPreviousLocations.put(playerId, player.getLocation().clone());
                        playerPreviousHealth.put(playerId, player.getHealth());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds
    }
    
    @Override
    protected void enterPhase2() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        
        // Time effects become more severe
        temporalRewindCooldown = 0;
        
        // Temporal distortion around boss
        new BukkitRunnable() {
            int duration = 200; // 10 seconds
            
            @Override
            public void run() {
                if (!isAlive() || duration <= 0) {
                    cancel();
                    return;
                }
                
                Location center = entity.getLocation();
                for (int i = 0; i < 5; i++) {
                    Location distortLoc = center.clone().add(
                        (Math.random() - 0.5) * 15,
                        Math.random() * 5,
                        (Math.random() - 0.5) * 15
                    );
                    
                    center.getWorld().spawnParticle(Particle.END_ROD, distortLoc, 1);
                    center.getWorld().spawnParticle(Particle.ENCHANT, distortLoc, 2);
                }
                
                duration--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 100, 5, 5, 5, 0.2);
    }
    
    @Override
    protected void enterPhase3() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 2));
        
        // Constant temporal anomalies
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Random time effects
                if (Math.random() < 0.3) {
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(entity.getLocation()) <= 20) {
                            // Random temporal effect
                            int effect = (int)(Math.random() * 3);
                            switch (effect) {
                                case 0 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 5));
                                case 1 -> player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 3));
                                case 2 -> player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 60, 1));
                            }
                            
                            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds
        
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 200, 8, 8, 8, 0.3);
    }
    
    @Override
    protected void onDeath() {
        Location loc = entity.getLocation();
        
        // Temporal collapse
        entity.getWorld().spawnParticle(Particle.END_ROD, loc, 300, 20, 20, 20, 0.5);
        entity.getWorld().spawnParticle(Particle.ENCHANT, loc, 200, 15, 15, 15, 0.3);
        entity.getWorld().playSound(loc, Sound.BLOCK_BEACON_DEACTIVATE, 3.0f, 0.5f);
        entity.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 2.0f, 1.5f);
        
        // Time implosion effect
        for (int radius = 25; radius >= 1; radius--) {
            final int r = radius;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (double angle = 0; angle < 360; angle += 20) {
                    double x = loc.getX() + r * Math.cos(Math.toRadians(angle));
                    double z = loc.getZ() + r * Math.sin(Math.toRadians(angle));
                    Location implosionLoc = new Location(loc.getWorld(), x, loc.getY(), z);
                    
                    loc.getWorld().spawnParticle(Particle.END_ROD, implosionLoc, 2);
                }
            }, (25 - radius) * 2L);
        }
        
        // Drop time loot
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.CLOCK, 5));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.AMETHYST_CLUSTER, 10));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.NETHER_STAR, 3));
        
        // Drop Time God Fragment 7 (final fragment)
        loc.getWorld().dropItemNaturally(loc, com.fallengod.testament.items.FragmentItem.createFragment(GodType.TIME, 7));
        
        // Announce death
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) <= 200) {
                player.sendTitle("§d§lCHRONOS DEFEATED", "§7Time flows normally once more", 20, 80, 20);
                player.sendMessage("§6§l⚔ Chronos Incarnate has been vanquished! Time itself mourns! ⚔");
            }
        }
        
        // Clean up tracking maps
        playerPreviousLocations.clear();
        playerPreviousHealth.clear();
    }
}