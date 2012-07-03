package me.dbla8888.plugins.hardcoremode;

import java.io.File;
import java.util.Collection;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.scheduler.BukkitScheduler;

public class Hardcoremode extends JavaPlugin implements Listener {
    
    private static final Logger logger = Logger.getLogger("Minecraft");
    Player[] players;
    OfflinePlayer[] offlineplayers;
    boolean deathevent = false;
    Random random = new Random();
    int worldcounter = 0;

        
     /**
     * onEnable is called on server startup at a stage specified in the
     * plugin.yml file under the 'startup' tag, or when it is explicitly 
     * called by the plugin manager.
     */
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        deathevent = false;
        System.out.println(this + " is now enabled!");
    }
    
    /**
     * onDisable is called on server shutdown and when explicitly called by
     * the plugin manager.  
     */
    @Override
    public void onDisable() {
        System.out.println(this + " is now disabled!");
    }

   /**
     * called when a player joins the server. Attempts to put the player in the
     * right place.
     * @param event 
     */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event)
    {
        final Player player = event.getPlayer();
        
        if(deathevent)
        {
            player.teleport(getServer().getWorld("nether").getSpawnLocation());     
        }
        
        if(player.getWorld() == getServer().getWorld("dummyworld") && getServer().getWorld("world" + worldcounter) != null)
        {
                player.teleport(getServer().getWorld("world"+ worldcounter).getSpawnLocation());
        }
        
        if(player.getWorld() == getServer().getWorld("nether") && !deathevent)
        {
            player.teleport(getServer().getWorld("world"+ worldcounter).getSpawnLocation());
        }
//            {//need to add some error handling, should be good for the time being though
//                getServer().getScheduler().scheduleAsyncDelayedTask(this, 
//                    new Runnable() {
//                        public void run() {
//                           player.teleport(getServer().getWorld("world" + worldcounter).getSpawnLocation());
//                        }
//                    }, 20*30);
//            }
        
    }
    
     /**
     * runs on death of a player
     * we want to:
     * 1. Send all players to nether
     * 2. Ensure any dead players are alive
     * 3. Set the global spawn to be in the 'nether'
     * 4. Delete old 'world', 'world_nether', and 'world_the_end' 
     * 5. Generate new 'world'.
     * 6. Let everyone stew in the nether for awhile
     * 7. Set the 'world' time to morning
     * 8. Send everyone back to the new world
     * @param event 
     */
    @EventHandler
    public void onPlayerDeath(EntityDeathEvent event) {        
        
        if(event.getEntityType().equals(EntityType.PLAYER) && "nether".equals(event.getEntity().getWorld().getName()))
        {
            event.getEntity().teleport(getServer().getWorld("nether").getSpawnLocation());
            event.getEntity().setHealth(20); 
        }
        
        if(event.getEntityType().equals(EntityType.PLAYER) && !"nether".equals(event.getEntity().getWorld().getName()))
        {
            
            deathevent = true;   
            players = getServer().getOnlinePlayers();
            
//Step 1 & 2           
            for(Player player: players)
            {
                
                
                player.teleport(getServer().getWorld("nether").getSpawnLocation());
                player.setHealth(20);
                
                //the pigzombies dont forget if you attack them, and we dont want them spawn camping
                Collection<PigZombie> pigzombies = getServer().getWorld("nether").getEntitiesByClass(PigZombie.class);
                 for(PigZombie pigzombie: pigzombies)
                 {
                     pigzombie.setAngry(false);
                 }
            }
//step 3 built into playerspawn event
            
//step 4
                       
            getServer().unloadWorld("world"+ worldcounter , false);
            getServer().unloadWorld("world"+ worldcounter +"_nether", false);
            getServer().unloadWorld("world"+ worldcounter +"_the_end", false);
            
            //try{
            //String worldfolder = getServer().getWorldContainer().getAbsolutePath();
            //if(!(new File(worldfolder + "\\world").delete() &&
            //new File(worldfolder + "\\world_nether").delete()&&
            //new File(worldfolder + "\\world_the_end").delete()))
            //    System.out.println("failed to delete worlds");
            //}catch(Exception e){e.printStackTrace();}
            
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter);
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter+"_nether");
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter+"_the_end");
            

//step 5
            worldcounter++;
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + " normal");
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + "_nether nether ");
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + "_the_end the_end ");
            
            getServer().dispatchCommand(getServer().getConsoleSender(), 
                    "mw link world"+ worldcounter + " world"+ worldcounter + "_nether ");
            getServer().dispatchCommand(getServer().getConsoleSender(), 
                    "mw link-end world"+ worldcounter + " world"+ worldcounter + "_the_end ");
            
//step 6, 7, & 8
            BukkitScheduler scheduler = this.getServer().getScheduler();

            int taskID = scheduler.scheduleAsyncDelayedTask(this, 
               new Runnable() {
                    public void run() {
                        getServer().getWorld("world" + worldcounter).setTime(0000);
                        players = getServer().getOnlinePlayers();                     
                        for(Player player: players)
                        {
                            player.teleport(getServer().getWorld("world"+ worldcounter).getSpawnLocation());
                            player.setHealth(20);
                        }
                        deathevent = false;
                    }
               }, 20*60*1);
            if(taskID == -1){
                    this.log(Level.WARNING, "failed to schedule!");
            }
        }
    }
    
     /**
     * sends a log message to the server console
     * @param level
     * @param message 
     */
    public void log(Level level, String message)
    {
            PluginDescriptionFile desc = this.getDescription();
            logger.log(level, desc.getName() + " v" + desc.getVersion() + ": " + message);
    }
}

