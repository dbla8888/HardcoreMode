package me.dbla8888.plugins.hardcoremode;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
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
    boolean deathevent;
    //Random random = new Random();
    int worldcounter;
    boolean debug = true;
    BukkitScheduler scheduler;
        
     /**
     * onEnable is called on server startup at a stage specified in the
     * plugin.yml file under the 'startup' tag, or when it is explicitly 
     * called by the plugin manager.
     */
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getWorld("nether").setKeepSpawnInMemory(true);
        scheduler = this.getServer().getScheduler();
        deathevent = false;
        worldcounter = getConfig().getInt("global.worldcounter" , 0);
        if(debug)log(Level.INFO, ChatColor.RED + "worldcounter: " + worldcounter);
        if(getServer().getWorld("world"+worldcounter) == null
                || getServer().getWorld("world"+worldcounter+"_nether") == null
                || getServer().getWorld("world"+worldcounter+"_the_end") == null)
        {
            generateWorlds();
        }
        
        System.out.println(this + " is now enabled!");
    }
    
    /**
     * onDisable is called on server shutdown and when explicitly called by
     * the plugin manager.  
     */
    @Override
    public void onDisable() {
        System.out.println(this + " is now disabled!");
        getConfig().set("global.worldcounter", worldcounter);
        try {
            getConfig().save(new File(getServer().getWorldContainer() + "\\plugins\\Hardcoremode\\config.yml"));
        } catch (IOException ex) {
            Logger.getLogger(Hardcoremode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

   /**
     * called when a player joins the server. Attempts to put the player in the
     * right place.
     * @param event 
     */
    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event)
    {
        Player player = event.getPlayer();
        if(debug)log(Level.INFO,"player in " + player.getWorld().getName());
        if(debug)log(Level.INFO, getServer().getWorld("world"+ worldcounter).getName());
        if(debug)log(Level.INFO,"deathevent: " + deathevent);
        
        
        if(getServer().getWorld("world"+ worldcounter) != null)
        {
            if( !player.getWorld().getName().equals(getServer().getWorld("world"+ worldcounter).getName())
                && !player.getWorld().getName().equals(getServer().getWorld("world"+worldcounter+"_nether").getName())
                && !player.getWorld().getName().equals(getServer().getWorld("world"+worldcounter+"_the_end").getName())
               )
            {
                scheduleTeleportPlayer(player);
            }
        }        
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
            getServer().broadcastMessage(ChatColor.RED+((Player)event.getEntity()).getDisplayName() +" has died!");
            players = getServer().getOnlinePlayers();
            
//Step 1 & 2           
            for(Player player: players)
            {                

                player.teleport(getServer().getWorld("nether").getSpawnLocation());
                player.setFireTicks(0);
                player.setFoodLevel(20);
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
            
            try{
            String worldfolder = getServer().getWorldContainer().getAbsolutePath();
            new File(worldfolder + "\\world").deleteOnExit();
            new File(worldfolder + "\\world_nether").deleteOnExit();
            new File(worldfolder + "\\world_the_end").deleteOnExit();
            }catch(Exception e){e.printStackTrace();}
            
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter);
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter+"_nether");
            getServer().dispatchCommand(getServer().getConsoleSender(), "mw delete world"+worldcounter+"_the_end");
            

//step 5
            worldcounter++;
            generateWorlds();
            
//step 6, 7, & 8
            scheduleTeleportAll();
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
    
    public void scheduleTeleportAll()
    {
        
        if(debug)log(Level.INFO, "Scheduling teleport all...");
        int taskID = scheduler.scheduleAsyncDelayedTask(this, 
           new Runnable() {
                public void run() {
                    getServer().getWorld("world" + worldcounter).setTime(0000);
                    players = getServer().getOnlinePlayers();                     
                    for(Player player: players)
                    {

                        player.teleport(getServer().getWorld("world"+ worldcounter).getSpawnLocation());
                        player.setFireTicks(0);
                        player.setFoodLevel(20);
                        player.setHealth(20);
                    }
                    deathevent = false;
                }
           }, 20*40*1);
        if(taskID == -1){
                this.log(Level.WARNING, "failed to schedule teleport!");
        }
    }
    
    private void scheduleTeleportPlayer(Player player)
    {
        final Player play3r = player;
        BukkitScheduler scheduler = this.getServer().getScheduler();
        if(debug)log(Level.INFO,"Scheduling teleport player...");
        play3r.sendMessage("Teleporting to overworld....");
        int taskID = scheduler.scheduleAsyncDelayedTask(this, 
           new Runnable() {
                public void run() 
                {                      
                    //getServer().getWorld("world" + worldcounter).setTime(0000);
                    if(play3r.isOnline())
                    {
                        play3r.teleport(getServer().getWorld("world"+ worldcounter).getSpawnLocation());
                        play3r.setHealth(20);
                    } 
                }
           }, 20*10*1);
        if(taskID == -1){
                this.log(Level.WARNING, "failed to schedule teleport!");
        }
    }
    
    private void generateWorlds()
    {
        getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + " normal");
        getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + "_nether nether ");
        getServer().dispatchCommand(getServer().getConsoleSender(), "mw create world"+ worldcounter + "_the_end the_end ");

        getServer().dispatchCommand(getServer().getConsoleSender(), 
                "mw link world"+ worldcounter + " world"+ worldcounter + "_nether");
        getServer().dispatchCommand(getServer().getConsoleSender(), 
                "mw link-end world"+ worldcounter + " world"+ worldcounter + "_the_end");

        getServer().getWorld("world"+worldcounter).setDifficulty(Difficulty.HARD);
        getServer().getWorld("world"+worldcounter+"_nether").setDifficulty(Difficulty.HARD);
        getServer().getWorld("world"+worldcounter+"_the_end").setDifficulty(Difficulty.HARD);

        //change difficulty in multiworld config :(
        File configFile = new File(
        getServer().getPluginManager().getPlugin("MultiWorld").getDataFolder(), "config.yml");

        getServer().getPluginManager().getPlugin("MultiWorld").getConfig().set(
                "worlds.world"+worldcounter+".difficulty", 3);
        getServer().getPluginManager().getPlugin("MultiWorld").getConfig().set(
                "worlds.world"+worldcounter+"_nether.difficulty", 3);
        getServer().getPluginManager().getPlugin("MultiWorld").getConfig().set(
                "worlds.world"+worldcounter+"_the_end.difficulty", 3);

        try {
            getServer().getPluginManager().getPlugin("MultiWorld").getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(Hardcoremode.class.getName()).log(Level.SEVERE, null, ex);
        }

        LinkedList<String> worlds = new LinkedList<String>();
        worlds.add("world"+worldcounter);
        worlds.add("world"+worldcounter+ "_nether");
        worlds.add("world"+worldcounter+ "_the_end");

        // add new worlds to monster apocalypse config so things to crash on MA event
        configFile = new File(
          getServer().getPluginManager().getPlugin("Monster Apocalypse").getDataFolder(), "config.yml");

        getServer().getPluginManager().getPlugin("Monster Apocalypse").getConfig().set("Worlds", worlds);

        try {
            getServer().getPluginManager().getPlugin("Monster Apocalypse").getConfig().save(configFile);
        } catch (IOException ex) {
            Logger.getLogger(Hardcoremode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}

