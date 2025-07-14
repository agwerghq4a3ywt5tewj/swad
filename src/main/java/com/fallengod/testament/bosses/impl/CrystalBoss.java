package com.fallengod.testament.bosses.impl;

import com.fallengod.testament.TestamentPlugin;
import com.fallengod.testament.bosses.GodBoss;
import com.fallengod.testament.enums.BossType;
import com.fallengod.testament.enums.GodType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CrystalBoss extends GodBoss {
    
    private final List<Location> crystalShards;
    private boolean crystalShieldActive = false;
    private int crystalShieldHealth = 0;
    
    public CrystalBoss(TestamentPlugin plugin) {
        super(plugin, BossType.CRYSTAL_RESONATOR, GodType.CRYSTAL);
        this.crystalShards = new ArrayList<>();
    }
    
    @Override
    protected void setupBoss() {
        if (entity instanceof Allay allay) {
            allay.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
            allay.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 1));
            allay.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
            allay.setCustomName("§b§lHarmonic Destroyer");
            allay.setCustomNameVisible(true);
        }
        
        // Start resonance field
        startResonanceField();
    }
    
    @Override
    protected void useSpecialAbility() {
        switch (phase) {
            case 1 -> sonicBoom();
            case 2 -> crystalPrison();
            case 3 -> resonanceWave();
        }
    }
    
    private void sonicBoom() {
        // Shatters blocks and damages players
        Location center = entity.getLocation();
        Vector direction = center.getDirection();
        
        // Create cone of sonic destruction
        for (int distance = 1; distance <= 12; distance++) {
            final int d = distance;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (int angle = -45; angle <= 45; angle += 15) {
                    Vector rotated = rotateVector(direction, angle);
                    Location sonicLoc = center.clone().add(rotated.multiply(d));
                    
                    // Sonic boom particles
                    center.getWorld().spawnParticle(Particle.SONIC_BOOM, sonicLoc, 1);
                    center.getWorld().spawnParticle(Particle.SWEEP_ATTACK, sonicLoc, 2, 0.5, 0.5, 0.5, 0.1);
                    
                    // Shatter blocks
                    for (int x = -1; x <= 1; x++) {
                        for (int y = -1; y <= 1; y++) {
                            for (int z = -1; z <= 1; z++) {
                                Location blockLoc = sonicLoc.clone().add(x, y, z);
                                if (blockLoc.getBlock().getType() != Material.BEDROCK && 
                                    blockLoc.getBlock().getType() != Material.AIR &&
                                    blockLoc.getBlock().getType().getHardness() < 50.0f) {
                                    
                                    // Store original block for restoration
                                    Material originalBlock = blockLoc.getBlock().getType();
                                    blockLoc.getBlock().setType(Material.AIR);
                                    
                                    // Shatter particles
                                    blockLoc.getWorld().spawnParticle(Particle.BLOCK, blockLoc, 10, originalBlock.createBlockData());
                                    
                                    // Restore block after 15 seconds
                                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                                        if (blockLoc.getBlock().getType() == Material.AIR) {
                                            blockLoc.getBlock().setType(originalBlock);
                                        }
                                    }, 300L);
                                }
                            }
                        }
                    }
                    
                    // Damage players
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(sonicLoc) <= 2) {
                            player.damage(15.0);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            player.sendMessage("§b§lSonic Boom! §7Harmonic waves shatter your senses!");
                        }
                    }
                }
            }, distance * 2L);
        }
        
        entity.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.2f);
        
        // Announce
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 15) {
                player.sendMessage("§b§lSonic Boom! §7Reality fractures under harmonic pressure!");
            }
        }
    }
    
    private void crystalPrison() {
        // Traps players in crystal cages
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(entity.getLocation()) <= 20) {
                Location playerLoc = player.getLocation();
                
                // Create crystal cage around player
                List<Location> cageBlocks = new ArrayList<>();
                
                // Cage walls
                for (int x = -2; x <= 2; x++) {
                    for (int y = 0; y <= 3; y++) {
                        for (int z = -2; z <= 2; z++) {
                            if (Math.abs(x) == 2 || Math.abs(z) == 2 || y == 0 || y == 3) {
                                Location cageLoc = playerLoc.clone().add(x, y, z);
                                if (cageLoc.getBlock().getType() == Material.AIR) {
                                    cageLoc.getBlock().setType(Material.AMETHYST_BLOCK);
                                    cageBlocks.add(cageLoc.clone());
                                }
                            }
                        }
                    }
                }
                
                // Crystal prison effects
                player.getWorld().spawnParticle(Particle.END_ROD, playerLoc.add(0, 1, 0), 50, 2, 2, 2, 0.1);
                player.getWorld().playSound(playerLoc, Sound.BLOCK_AMETHYST_CLUSTER_PLACE, 2.0f, 1.0f);
                
                player.damage(8.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 200, 3));
                player.sendMessage("§b§lCrystal Prison! §7You are trapped in harmonic crystal!");
                
                // Remove cage after 10 seconds
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    for (Location cageLoc : cageBlocks) {
                        if (cageLoc.getBlock().getType() == Material.AMETHYST_BLOCK) {
                            cageLoc.getBlock().setType(Material.AIR);
                            cageLoc.getWorld().spawnParticle(Particle.BLOCK, cageLoc, 5, Material.AMETHYST_BLOCK.createBlockData());
                        }
                    }
                    player.sendMessage("§7The crystal prison shatters!");
                }, 200L);
                
                break; // Only trap one player per use
            }
        }
    }
    
    private void resonanceWave() {
        // Area-wide damage that ignores armor
        Location center = entity.getLocation();
        
        // Create expanding resonance wave
        for (int radius = 1; radius <= 25; radius++) {
            final int r = radius;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (double angle = 0; angle < 360; angle += 15) {
                    double x = center.getX() + r * Math.cos(Math.toRadians(angle));
                    double z = center.getZ() + r * Math.sin(Math.toRadians(angle));
                    Location waveLoc = new Location(center.getWorld(), x, center.getY(), z);
                    
                    // Resonance particles
                    center.getWorld().spawnParticle(Particle.END_ROD, waveLoc, 2, 0.1, 0.1, 0.1, 0.05);
                    center.getWorld().spawnParticle(Particle.ENCHANT, waveLoc, 3, 0.2, 0.2, 0.2, 0.1);
                    
                    // Damage players (ignores armor)
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(waveLoc) <= 2) {
                            // Direct health damage (bypasses armor)
                            double currentHealth = player.getHealth();
                            double damage = 10.0;
                            player.setHealth(Math.max(1.0, currentHealth - damage));
                            
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 2));
                            player.sendMessage("§b§lResonance Wave! §7Harmonic energy bypasses all defenses!");
                            
                            // Resonance effects on player
                            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
                        }
                    }
                }
                
                // Wave sound
                if (r % 5 == 0) {
                    center.getWorld().playSound(center, Sound.BLOCK_AMETHYST_CLUSTER_HIT, 1.0f, 1.0f + (r * 0.05f));
                }
            }, radius * 3L);
        }
        
        entity.getWorld().playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 2.0f, 0.8f);
        
        // Announce
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 30) {
                player.sendMessage("§b§lResonance Wave! §7Harmonic destruction spreads outward!");
            }
        }
    }
    
    private void startResonanceField() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Create floating crystal shards around boss
                Location center = entity.getLocation();
                
                // Spawn crystal shards
                if (crystalShards.size() < 8) {
                    for (int i = crystalShards.size(); i < 8; i++) {
                        double angle = (i * 45.0);
                        double x = center.getX() + 5 * Math.cos(Math.toRadians(angle));
                        double z = center.getZ() + 5 * Math.sin(Math.toRadians(angle));
                        Location shardLoc = new Location(center.getWorld(), x, center.getY() + 2, z);
                        
                        crystalShards.add(shardLoc);
                    }
                }
                
                // Update crystal shard positions (orbit around boss)
                for (int i = 0; i < crystalShards.size(); i++) {
                    double angle = (System.currentTimeMillis() * 0.01) + (i * 45.0);
                    double x = center.getX() + 5 * Math.cos(Math.toRadians(angle));
                    double z = center.getZ() + 5 * Math.sin(Math.toRadians(angle));
                    Location shardLoc = new Location(center.getWorld(), x, center.getY() + 2, z);
                    
                    crystalShards.set(i, shardLoc);
                    
                    // Crystal shard particles
                    center.getWorld().spawnParticle(Particle.END_ROD, shardLoc, 1, 0.1, 0.1, 0.1, 0.01);
                    center.getWorld().spawnParticle(Particle.ENCHANT, shardLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                
                // Crystal shield regeneration
                if (crystalShieldActive && crystalShieldHealth < 100) {
                    crystalShieldHealth += 5;
                    if (crystalShieldHealth >= 100) {
                        crystalShieldHealth = 100;
                        entity.getWorld().playSound(center, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // Every 0.5 seconds
    }
    
    private Vector rotateVector(Vector vector, double angleDegrees) {
        double angleRadians = Math.toRadians(angleDegrees);
        double cos = Math.cos(angleRadians);
        double sin = Math.sin(angleRadians);
        
        double x = vector.getX() * cos - vector.getZ() * sin;
        double z = vector.getX() * sin + vector.getZ() * cos;
        
        return new Vector(x, vector.getY(), z);
    }
    
    @Override
    protected void enterPhase2() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
        
        // Activate crystal shield
        crystalShieldActive = true;
        crystalShieldHealth = 100;
        
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 100, 3, 3, 3, 0.2);
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 2.0f, 1.2f);
        
        // Announce shield
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(entity.getLocation()) <= 30) {
                player.sendMessage("§b§lCrystal Shield! §7Harmonic barriers protect the destroyer!");
            }
        }
    }
    
    @Override
    protected void enterPhase3() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 2));
        
        // More frequent sonic attacks
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Random sonic bursts
                if (Math.random() < 0.4) {
                    Location center = entity.getLocation();
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(center) <= 15) {
                            player.damage(5.0);
                            player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1);
                            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.0f, 1.5f);
                            break;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 30L); // Every 1.5 seconds
        
        entity.getWorld().spawnParticle(Particle.END_ROD, entity.getLocation(), 200, 5, 5, 5, 0.3);
    }
    
    @Override
    protected void onDeath() {
        Location loc = entity.getLocation();
        
        // Harmonic collapse
        entity.getWorld().spawnParticle(Particle.END_ROD, loc, 300, 20, 20, 20, 0.5);
        entity.getWorld().spawnParticle(Particle.ENCHANT, loc, 200, 15, 15, 15, 0.3);
        entity.getWorld().playSound(loc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 3.0f, 0.5f);
        entity.getWorld().playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 2.0f);
        
        // Crystal explosion effect
        for (int i = 0; i < 20; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location crystalLoc = loc.clone().add(
                    (Math.random() - 0.5) * 20,
                    Math.random() * 10,
                    (Math.random() - 0.5) * 20
                );
                
                loc.getWorld().spawnParticle(Particle.BLOCK, crystalLoc, 10, Material.AMETHYST_BLOCK.createBlockData());
                loc.getWorld().playSound(crystalLoc, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.5f);
            }, i * 3L);
        }
        
        // Drop crystal loot
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.AMETHYST_SHARD, 20));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.AMETHYST_CLUSTER, 8));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.NETHER_STAR, 3));
        
        // Drop Crystal God Fragment 7 (final fragment)
        loc.getWorld().dropItemNaturally(loc, com.fallengod.testament.items.FragmentItem.createFragment(GodType.CRYSTAL, 7));
        
        // Announce death
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) <= 200) {
                player.sendTitle("§b§lHARMONIC DESTROYER DEFEATED", "§7The resonance falls silent", 20, 80, 20);
                player.sendMessage("§6§l⚔ The Harmonic Destroyer has been shattered! Silence returns! ⚔");
            }
        }
        
        // Clean up crystal shards
        crystalShards.clear();
    }
}