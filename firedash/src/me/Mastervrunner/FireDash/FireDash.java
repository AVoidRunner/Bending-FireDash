package me.Mastervrunner.FireDash;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/////import com.github.h0lysp4nk.voidwalker.Voidwalker;
import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.AirAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

import me.Mastervrunner.FireDash.*;

public class FireDash extends FireAbility implements AddonAbility{

	//Listener of your ability. Listener doesn't have anything special for multi-shot ability.
			private Listener MSL;
			
			//Cooldown of your ability.
			private long cooldown;
			//Shot number of your ability.
			private int charge;
			//Range of your projectiles.
			private int range;
			//Damage of your projectiles.
			private double damage;
			//Speed of your projectiles.
			private double speed = 2;
			//Maximum waiting time for players to shoot all of the charges.
			private long duration;
			//Delay between shots to prevent spamming.
			private long timeBetweenShots;
			//A temporary variable for keeping track of last shot time. It is required to put delay between shots.
			private long lastShotTime;
			//Holding last projectile's unique id. Later, we will use this in hasmap as a key.
			private int lastProjectileId;
			
			//Each shot has different location so we use a hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> locations;
			//Each shot has different directions so we use a hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Vector> directions;
			//Each shot has different starting location so we hashmap to keep track of them. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> startLocations;
			//This hashmap is required for removing shots when they touch an entity, a block or when they are out of range. Key of this hashmap is the id of projectile.
			private HashMap<Integer, Location> deadProjectiles;
			
			public FireDash(Player player) {
				super(player);
				
				//Don't continue if your ability is on cooldown.
				if (bPlayer.isOnCooldown(this)) {
					return;
				}
				
				//Don't continue if player can't use this ability.
				if (!bPlayer.canBend(this)) {
					return;
				}
				
				
				setField();
				//We start the ability.
				start();
				
			}
			
			public void setField() {
				//Cooldown of your ability.
				cooldown = ConfigManager.getConfig().getLong("ExtraAbilities.Mastervrunner.Fire.FireDash.Cooldown");

				//Maximum projectile number of your ability.
				charge = 1;
				
				range = ConfigManager.getConfig().getInt("ExtraAbilities.Mastervrunner.Fire.FireDash.Range");

				damage = ConfigManager.getConfig().getInt("ExtraAbilities.Mastervrunner.Fire.FireDash.Damage");
				
				duration = 1000;
				timeBetweenShots = 1;
				lastShotTime = getStartTime();
				
				deadProjectiles = new HashMap<Integer, Location>();
				locations = new HashMap<Integer, Location>();
				directions = new HashMap<Integer, Vector>();
				startLocations = new HashMap<Integer, Location>();
				
				lastProjectileId = 1;
				Location loc = player.getLocation().add(0, 1, 0);
				locations.put(lastProjectileId, loc);
				directions.put(lastProjectileId, player.getLocation().getDirection());
				startLocations.put(lastProjectileId, loc.clone());
				
				charge--;
								
				int points = 36;
				double radius2 = 3.0d;
				Location origin = player.getLocation();
				double moveval = 1.1;

				for (int i = 0; i < points; i++) {
				    double angle = 2 * Math.PI * i / points;
				    Location point = origin.clone().add(0.0d, radius * Math.sin(angle), 0.0d);
				    point.add(player.getLocation().getDirection().multiply(Math.cos(angle)));
			
				    player.getWorld().spawnParticle(Particle.FLAME, point, 0);
				    
				    
				}
				
				double speedyness = 2;
				double speedConfig = ConfigManager.getConfig().getDouble("ExtraAbilities.Mastervrunner.Fire.FireDash.DashSpeed");
				
				
				Vector setVel = player.getVelocity();
				setVel.add(player.getLocation().getDirection().multiply(speedConfig));
				
				player.setVelocity(setVel);
				
				
			}
			
			boolean goUpBack = true;
			
			int points = 10;
			double radius = 3.0d;
			Location origin = player.getLocation();
			
			int iterations = 0;
			

			@Override
			public void progress() {
				
				//If no charge left (So you cannot shoot more projectiles,
				//and location hashmap is empty (So all of the projectiles are dead)
				//we can remove the move. (We won't add cooldown because we added it
				//when we shot the last projectile.)
				
				//If duration is over, add cooldown and remove the ability.
				if (System.currentTimeMillis() > getStartTime() + duration) {
					bPlayer.addCooldown(this);
					remove();
				}
				//bPlayer.addCooldown(this);
				
				//This for loop below is the thing that progress every projectile.
				//i is the projectile's id.
				
				//What this does is, for each projectile:
				//spawn the particle,
				//check for living entities around to damage one of them.
				//move the projectile to it's next position.
				//check for the range,
				//check if the projectile hits a block.
				for (Integer i : locations.keySet()) {
					//Spawn your i'th projectile's particle at it's location
					
					ParticleEffect.FLAME.display(locations.get(i), 5, 0.1, 0.1, 0.1, 0.0); 
					
					//Check living entities near i'th projectile to damage one of them.
					for (Entity e : GeneralMethods.getEntitiesAroundPoint(locations.get(i), 1.5)) {
						if (e instanceof LivingEntity && !e.getUniqueId().equals(player.getUniqueId())) {
							DamageHandler.damageEntity(e, damage, this);
							//After you damage an entity, you need to remove that projectile unless
							//you want it to go through entities.
							//If we remove it right here, that will break our for loop because we are using
							//locations.size() and removing an element will change it's size.
							//So we use a temporary hashmap to keep track of projectile we want to remove 
							//after for loop is over.
							deadProjectiles.put(i, locations.get(i));
						}
					}
					bPlayer.addCooldown(this);
					
				////	Methods
					//Move i'th projectile to it's next position.
					
					speed = ConfigManager.getConfig().getDouble("ExtraAbilities.Mastervrunner.Fire.FireDash.AttackSpeed");
					
					locations.get(i).add(directions.get(i).clone().multiply(speed));

					//If it is out of range or it hit a block, add it to the temporary hashmap to remove later.
					if (locations.get(i).distance(startLocations.get(i)) > range
							|| GeneralMethods.isSolid(locations.get(i).getBlock())) {
						deadProjectiles.put(i, locations.get(i));
					}
				}
				
				//Our loop that progress all of the projectiles is over.
				//Now we can safely remove the dead projectiles from every
				//hashmap we used that projectile in.
				for(Integer i : deadProjectiles.keySet()) {
					locations.remove(i);
					directions.remove(i);
					startLocations.remove(i);
				}
				//Finally, we clear our temporary hashmap.
				deadProjectiles.clear();
				
			}
			
			//We use this method to get this instance of your ability's charge value.
			public int getCharge() {
				return this.charge;
			}
			
			//We use this method to update this instance of your ability's charge value.
			public void setCharge(int charge) {
				this.charge = charge;
			}
			
			public int getLastProjectileId() {
				return this.lastProjectileId;
			}
			
			public void setLastProjectileId(int id) {
				this.lastProjectileId = id;
			}
			
			//We use this method to get this instance of your ability's lastShotTime value.
			public long getLastShotTime() {
				return this.lastShotTime;
			}
			
			//We use this method to update this instance of your ability's lastShotTime value.
			public void setLastShotTime(long time) {
				this.lastShotTime = time;
			}
			
			//We use this method to get this instance of your ability's timeBetweenShots value.
			public long getTimeBetweenShots() {
				return this.timeBetweenShots;
			}
			
			//We use this method to get this instance of your ability's locations hashmap.
			public HashMap<Integer, Location> getParticleLocations() {
				return this.locations;
			}
			
			//We use this method to get this instance of your ability's directions hashmap.
			public HashMap<Integer, Vector> getDirections() {
				return this.directions;
			}
			
			//We use this method to get this instance of your ability's startLocations hashmap.
			public HashMap<Integer, Location> getStartLocations() {
				return this.startLocations;
			}
			
			@Override
			public long getCooldown() {
				return this.cooldown;
			}

			@Override
			public Location getLocation() {
				return null;
			}

			@Override
			public String getName() {
				return "FireDash";
			}

			@Override
			public boolean isHarmlessAbility() {
				return false;
			}

			@Override
			public boolean isSneakAbility() {
				return false;
			}

			@Override
			public String getAuthor() {
				return "Mastervrunner";
			}

			@Override
			public String getVersion() {
				return "1.0";
			}
			
			@Override
			public String getDescription() {
				return "<LEFT CLICK>: Dash... With fire";
			}
			

			@Override
			public void load() {
				//We are registering our listener.
				MSL = new FireDashListener();
				ProjectKorra.plugin.getServer().getPluginManager().registerEvents(MSL, ProjectKorra.plugin);
				ProjectKorra.log.info("Succesfully enabled " + getName() + " by " + getAuthor());
				
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireDash.DashSpeed", 5);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireDash.Cooldown", 2000);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireDash.Range", 20);
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireDash.Damage", 5); 
				ConfigManager.getConfig().addDefault("ExtraAbilities.Mastervrunner.Fire.FireDash.AttackSpeed", 2);
				
				ConfigManager.defaultConfig.save();
				
			}

			@Override
			public void stop() {
				ProjectKorra.log.info("Successfully disabled " + getName() + " by " + getAuthor());
				//We are unregistering our listener.
				HandlerList.unregisterAll(MSL);
				super.remove();
			}
	
	
}
