package com.fallengod.testament.bosses.impl;

import com.fallengod.testament.TestamentPlugin;
import com.fallengod.testament.bosses.GodBoss;
import com.fallengod.testament.enums.BossType;
import com.fallengod.testament.enums.GodType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Hoglin;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BloodBoss extends GodBoss {
    
    private double initialHealth;
    private boolean berserkerModeActive = false;
    
    public BloodBoss(TestamentPlugin plugin) {
        super(plugin, BossType.BLOOD_BERSERKER, GodType.BLOOD);
    }
    
    @Override
    protected void setupBoss() {
        if (entity instanceof Hoglin hoglin) {
            hoglin.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 2));
            hoglin.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 1));
            hoglin.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 1));
            hoglin.setCustomName("§4§lCrimson War Beast");
            hoglin.setCustomNameVisible(true);
            hoglin.setGlowing(true);
            
            this.initialHealth = hoglin.getHealth();
        }
        
        // Start blood frenzy tracking
        startBloodFrenzyTracking();
    }
    
    @Override
    protected void useSpecialAbility() {
        switch (phase) {
            case 1 -> bloodFrenzy();
            case 2 -> crimsonSurge();
            case 3 -> berserkerCharge();
        }
    }
    
    private void bloodFrenzy() {
        // Damage increases as health decreases
        double healthPercent = entity.getHealth() / entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
        
        // Calculate frenzy level (more damage as health gets lower)
        int frenzyLevel = (int)((1.0 - healthPercent) * 5); // 0-5 based on missing health
        
        if (frenzyLevel > 0) {
            // Apply increasing strength
            entity.removePotionEffect(PotionEffectType.STRENGTH);
            entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 2 + frenzyLevel));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, frenzyLevel));
            
            // Blood particle effects
            Location loc = entity.getLocation();
            for (int i = 0; i < 10 + (frenzyLevel * 5); i++) {
                Location bloodLoc = loc.clone().add(
                    (Math.random() - 0.5) * 4,
                    Math.random() * 2,
                    (Math.random() - 0.5) * 4
                );
                
                loc.getWorld().spawnParticle(Particle.BLOCK, bloodLoc, 1, Material.REDSTONE_BLOCK.createBlockData());
                loc.getWorld().spawnParticle(Particle.ITEM, bloodLoc, 1, new org.bukkit.inventory.ItemStack(Material.REDSTONE));
            }
            
            // Damage nearby players based on frenzy level
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distance(loc) <= 8) {
                    double damage = 5.0 + (frenzyLevel * 2.0);
                    player.damage(damage);
                    player.sendMessage("§4§lBlood Frenzy! §cThe beast's rage grows with its wounds!");
                    player.sendActionBar("§4🩸 Frenzy Level: " + frenzyLevel);
                }
            }
            
            entity.getWorld().playSound(loc, Sound.ENTITY_HOGLIN_ANGRY, 2.0f, 0.8f - (frenzyLevel * 0.1f));
        }
    }
    
    private void crimsonSurge() {
        // Heals by dealing damage to players
        Location center = entity.getLocation();
        double totalDamageDealt = 0;
        
        // Create expanding blood wave
        for (int radius = 1; radius <= 15; radius++) {
            final int r = radius;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (double angle = 0; angle < 360; angle += 20) {
                    double x = center.getX() + r * Math.cos(Math.toRadians(angle));
                    double z = center.getZ() + r * Math.sin(Math.toRadians(angle));
                    Location surgeLoc = new Location(center.getWorld(), x, center.getY(), z);
                    
                    // Blood particles
                    center.getWorld().spawnParticle(Particle.BLOCK, surgeLoc, 3, Material.REDSTONE_BLOCK.createBlockData());
                    center.getWorld().spawnParticle(Particle.ITEM, surgeLoc, 2, new org.bukkit.inventory.ItemStack(Material.REDSTONE));
                    
                    // Damage players in wave
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(surgeLoc) <= 2) {
                            double damage = 12.0;
                            player.damage(damage);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1));
                            player.sendMessage("§4§lCrimson Surge! §cYour life force feeds the beast!");
                            
                            // Heal boss based on damage dealt
                            double currentHealth = entity.getHealth();
                            double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                            double healAmount = damage * 0.5; // Heal 50% of damage dealt
                            
                            entity.setHealth(Math.min(maxHealth, currentHealth + healAmount));
                            
                            // Healing particles on boss
                            entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 5, 1, 1, 1, 0.1);
                        }
                    }
                }
            }, radius * 3L);
        }
        
        entity.getWorld().playSound(center, Sound.ENTITY_HOGLIN_HURT, 2.0f, 0.5f);
        
        // Announce
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(center) <= 20) {
                player.sendMessage("§4§lCrimson Surge! §cBlood magic flows across the battlefield!");
            }
        }
    }
    
    private void berserkerCharge() {
        // Devastating charge attack
        Location startLoc = entity.getLocation();
        
        // Find nearest player to charge at
        Player target = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Player player : entity.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(startLoc);
            if (distance <= 30 && distance < nearestDistance) {
                target = player;
                nearestDistance = distance;
            }
        }
        
        if (target == null) return;
        
        final Player finalTarget = target;
        Location targetLoc = target.getLocation();
        
        // Calculate charge path
        org.bukkit.util.Vector direction = targetLoc.subtract(startLoc).toVector().normalize();
        
        // Charge effects
        new BukkitRunnable() {
            int ticks = 0;
            Location currentLoc = startLoc.clone();
            
            @Override
            public void run() {
                if (!isAlive() || ticks >= 40) { // 2 second charge
                    cancel();
                    return;
                }
                
                // Move boss forward
                currentLoc.add(direction.clone().multiply(0.8));
                entity.teleport(currentLoc);
                
                // Charge particles
                entity.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 10, Material.REDSTONE_BLOCK.createBlockData());
                entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, currentLoc, 3, 1, 0.5, 1, 0.1);
                
                // Damage players in path
                for (Player player : entity.getWorld().getPlayers()) {
                    if (player.getLocation().distance(currentLoc) <= 3) {
                        double damage = 25.0; // Massive charge damage
                        player.damage(damage);
                        player.setVelocity(direction.clone().multiply(2).setY(1)); // Knockback
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 2));
                        player.sendMessage("§4§lBerserker Charge! §cYou are trampled by the raging beast!");
                        
                        // Blood explosion on impact
                        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 30, Material.REDSTONE_BLOCK.createBlockData());
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HOGLIN_ATTACK, 2.0f, 0.8f);
                    }
                }
                
                // Charge sound
                if (ticks % 5 == 0) {
                    entity.getWorld().playSound(currentLoc, Sound.ENTITY_HOGLIN_STEP, 1.5f, 0.8f);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Announce charge
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(startLoc) <= 35) {
                player.sendMessage("§4§lBerserker Charge! §cThe war beast charges with unstoppable fury!");
            }
        }
    }
    
    private void startBloodFrenzyTracking() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Continuous blood frenzy effects
                double healthPercent = entity.getHealth() / entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                
                if (healthPercent <= 0.25 && !berserkerModeActive) {
                    // Enter ultimate berserker mode
                    berserkerModeActive = true;
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 5));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 3));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 2));
                    
                    // Berserker activation effects
                    Location loc = entity.getLocation();
                    entity.getWorld().spawnParticle(Particle.BLOCK, loc, 100, Material.REDSTONE_BLOCK.createBlockData());
                    entity.getWorld().playSound(loc, Sound.ENTITY_HOGLIN_ANGRY, 3.0f, 0.5f);
                    
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(loc) <= 50) {
                            player.sendTitle("§4§lBERSERKER MODE", "§cThe war beast enters ultimate rage!", 20, 60, 20);
                        }
                    }
                }
                
                // Passive blood aura
                if (healthPercent <= 0.5) {
                    Location loc = entity.getLocation();
                    for (Player player : entity.getWorld().getPlayers()) {
                        if (player.getLocation().distance(loc) <= 6) {
                            player.damage(1.0); // Passive damage aura
                            player.getWorld().spawnParticle(Particle.ITEM, player.getLocation(), 2, new org.bukkit.inventory.ItemStack(Material.REDSTONE));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every second
    }
    
    @Override
    protected void enterPhase2() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 3));
        
        // Becomes faster and more aggressive
        entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation(), 100, Material.REDSTONE_BLOCK.createBlockData());
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 2.0f, 0.7f);
    }
    
    @Override
    protected void enterPhase3() {
        entity.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, Integer.MAX_VALUE, 4));
        entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 2));
        
        // Gains strength from taking damage
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isAlive()) {
                    cancel();
                    return;
                }
                
                // Regenerate health based on missing health
                double healthPercent = entity.getHealth() / entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                if (healthPercent < 1.0) {
                    double healAmount = (1.0 - healthPercent) * 2.0; // More healing when more injured
                    double currentHealth = entity.getHealth();
                    double maxHealth = entity.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                    
                    entity.setHealth(Math.min(maxHealth, currentHealth + healAmount));
                    entity.getWorld().spawnParticle(Particle.HEART, entity.getLocation().add(0, 2, 0), 3, 1, 1, 1, 0.1);
                }
            }
        }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds
        
        entity.getWorld().spawnParticle(Particle.BLOCK, entity.getLocation(), 200, Material.REDSTONE_BLOCK.createBlockData());
    }
    
    @Override
    protected void onDeath() {
        Location loc = entity.getLocation();
        
        // Blood explosion death
        entity.getWorld().spawnParticle(Particle.BLOCK, loc, 300, Material.REDSTONE_BLOCK.createBlockData());
        entity.getWorld().spawnParticle(Particle.ITEM, loc, 200, new org.bukkit.inventory.ItemStack(Material.REDSTONE));
        entity.getWorld().playSound(loc, Sound.ENTITY_HOGLIN_DEATH, 3.0f, 0.5f);
        entity.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.8f);
        
        // Blood rain effect
        for (int i = 0; i < 50; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Location rainLoc = loc.clone().add(
                    (Math.random() - 0.5) * 30,
                    Math.random() * 15 + 10,
                    (Math.random() - 0.5) * 30
                );
                
                loc.getWorld().spawnParticle(Particle.BLOCK, rainLoc, 5, Material.REDSTONE_BLOCK.createBlockData());
            }, i * 2L);
        }
        
        // Drop blood loot
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.REDSTONE, 32));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.REDSTONE_BLOCK, 16));
        loc.getWorld().dropItemNaturally(loc, new org.bukkit.inventory.ItemStack(Material.NETHER_STAR, 3));
        
        // Drop Blood God Fragment 7 (final fragment)
        loc.getWorld().dropItemNaturally(loc, com.fallengod.testament.items.FragmentItem.createFragment(GodType.BLOOD, 7));
        
        // Announce death
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distance(loc) <= 200) {
                player.sendTitle("§4§lWAR BEAST DEFEATED", "§7The crimson rage is finally silenced", 20, 80, 20);
                player.sendMessage("§6§l⚔ The Crimson War Beast has fallen! The battlefield is silent! ⚔");
            }
        }
    }
}