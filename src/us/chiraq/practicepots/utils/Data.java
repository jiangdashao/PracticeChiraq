package us.chiraq.practicepots.utils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import us.chiraq.practicepots.Nanny;
import us.chiraq.practicepots.game.Arena;
import us.chiraq.practicepots.game.Ladder;
import us.chiraq.practicepots.game.kit.Kit;
import us.chiraq.practicepots.profile.Profile;
import us.chiraq.practicepots.utils.ItemStackSerializer;
import us.chiraq.practicepots.utils.LocationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Data implements Runnable {
    private static Nanny main = Nanny.getInstance();
    private static int pagesSize = main.getLangFile().getInt("STATS.LEADERBOARD.AMOUNTPERPAGE");
    
	public void run() {
		saveProfiles();
	}
    
    @SuppressWarnings("deprecation")
	public static void loadProfiles() {
        DBCursor dbc = main.getCollection().find();
        while (dbc.hasNext()) {
            BasicDBObject savedMap;
            BasicDBObject dbo = (BasicDBObject)dbc.next();
            UUID uuid = UUID.fromString(dbo.getString("uuid"));
            HashMap<Ladder, Integer> elo = null;
            ArrayList<Kit> kits = null;
            HashMap<Ladder, Integer> rw = null;
            HashMap<Ladder, Integer> rl = null;
            HashMap<Ladder, Integer> urw = null;
            HashMap<Ladder, Integer> url = null;
            if (dbo.containsKey("ladders")) {
                elo = new HashMap<Ladder, Integer>();
                savedMap = (BasicDBObject)dbo.get("ladders");
                for (String ladderString : savedMap.keySet()) {
                    elo.put(Ladder.getLadder(ladderString), savedMap.getInt(ladderString));
                }
            }
            if (dbo.containsKey("rw")) {
            	rw = new HashMap<Ladder, Integer>();
            	savedMap = (BasicDBObject)dbo.get("rw");
            	for (String ladderString : savedMap.keySet()) {
            		rw.put(Ladder.getLadder(ladderString), savedMap.getInt(ladderString));
            	}
            }
            if (dbo.containsKey("rl")) {
            	rl = new HashMap<Ladder, Integer>();
            	savedMap = (BasicDBObject)dbo.get("rl");
            	for (String ladderString : savedMap.keySet()) {
            		rl.put(Ladder.getLadder(ladderString), savedMap.getInt(ladderString));
            	}
            }
            if (dbo.containsKey("urw")) {
            	urw = new HashMap<Ladder, Integer>();
            	savedMap = (BasicDBObject)dbo.get("urw");
            	for (String ladderString : savedMap.keySet()) {
            		urw.put(Ladder.getLadder(ladderString), savedMap.getInt(ladderString));
            	}
            }
            if (dbo.containsKey("url")) {
            	url = new HashMap<Ladder, Integer>();
            	savedMap = (BasicDBObject)dbo.get("url");
            	for (String ladderString : savedMap.keySet()) {
            		url.put(Ladder.getLadder(ladderString), savedMap.getInt(ladderString));
            	}
            }
            if (dbo.containsKey("kits")) {
                kits = new ArrayList<Kit>();
                savedMap = (BasicDBObject)dbo.get("kits");
                for (String kitName : savedMap.keySet()) {
                    String kitData = savedMap.getString(kitName);
                    String ladderName = kitData.substring(kitData.lastIndexOf(";") + 1, kitData.length()).replace(" ", "");
                    Ladder ladder = Ladder.getLadder(ladderName);
                    if (ladder == null) continue;
                    String itemString = kitData.substring(0, kitData.lastIndexOf(";") - 1);
                    String[] array = itemString.split("\\|");
                    String armor = array[0];
                    String inventory = array[1];
                    Kit kit = new Kit(ladder, ItemStackSerializer.deserializeItemStackArray(armor), ItemStackSerializer.deserializeItemStackArray(inventory));
                    kit.setName(kitName);
                    kits.add(kit);
                }
            }
            int totalMatches = 0;
            if (dbo.containsField("totalmatches")) {
            	totalMatches = dbo.getInt("totalmatches");
            }
            Profile profile = new Profile(uuid);
            if (kits != null) {
                profile.setKits(kits);
            }
            if (elo == null) continue;
            profile.setRank(elo);
            if (rw == null) continue;
            profile.setRankedWins(rw);
            if (rl == null) continue;
            profile.setRankedLosses(rl);
            if (urw == null) continue;
            profile.setUnRankedWins(urw);
            if (url == null) continue;
            profile.setUnRankedLosses(url);
            double globalElo = 0.00;
            globalElo =  calculateGlobalElo(profile);
            profile.setGlobalElo(globalElo);
            profile.setTotalMatches(totalMatches);
            for (Ladder ladder : Ladder.getLadders()) {
                if (profile.getRank().containsKey(ladder)) continue;
                profile.getRank().put(ladder, ladder.getDefaultElo());
            }
        }
    }
    
    public static void saveProfiles() {
        DBCollection collection = main.getCollection();
        Bukkit.getLogger().log(Level.INFO, "Preparing to save " + Profile.getProfiles().size() + " profiles.");
        for (Profile profile : Profile.getProfiles()) {
            DBCursor dbc = collection.find(new BasicDBObject("uuid", profile.getUuid().toString()));
            BasicDBObject dbo = new BasicDBObject("uuid", profile.getUuid().toString());
            BasicDBObject ladders = new BasicDBObject();
            for (Ladder ladder2 : profile.getRank().keySet()) {
                if (ladder2 == null) continue;
                ladders.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRank().get(ladder2));
            }
            BasicDBObject rw = new BasicDBObject();
            for (Ladder ladder2 : profile.getRankedWins().keySet()) {
            	if (ladder2 == null) continue;
            	rw.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRankedWins().get(ladder2));
            }
            BasicDBObject rl = new BasicDBObject();
            for (Ladder ladder2 : profile.getRankedLosses().keySet()) {
            	if (ladder2 == null) continue;
            	rl.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRankedLosses().get(ladder2));
            }
            BasicDBObject urw = new BasicDBObject();
            for (Ladder ladder2 : profile.getUnRankedWins().keySet()) {
            	if (ladder2 == null) continue;
            	urw.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getUnRankedWins().get(ladder2));
            }
            BasicDBObject url = new BasicDBObject();
            for (Ladder ladder2 : profile.getUnRankedLosses().keySet()) {
            	if (ladder2 == null) continue;
            	url.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getUnRankedLosses().get(ladder2));
            }
            BasicDBObject kits = new BasicDBObject();
            for (Kit kit : profile.getKits()) {
                kits.put(kit.getName(), (Object)(Kit.serialize(kit) + ";" + kit.getLadder().getName().toLowerCase()));
            }
            dbo.put("kits", (Object)kits);
            dbo.put("ladders", (Object)ladders);
            dbo.put("rw", (Object)rw);
            dbo.put("rl", (Object)rl);
            dbo.put("urw", (Object)urw);
            dbo.put("url", (Object)url);
            dbo.append("globalelo", profile.getGlobalElo());
            dbo.append("totalmatches", profile.getTotalMatches());
            if (dbc.hasNext()) {
                collection.update(dbc.getQuery(), dbo);
                continue;
            }
            collection.insert(dbo);
        }
    }
    
    public static void saveProfile(Profile profile) {
    	 DBCollection collection = main.getCollection();
    	 DBCursor dbc = collection.find(new BasicDBObject("uuid", profile.getUuid().toString()));
         BasicDBObject dbo = new BasicDBObject("uuid", profile.getUuid().toString());
         BasicDBObject ladders = new BasicDBObject();
         for (Ladder ladder2 : profile.getRank().keySet()) {
             if (ladder2 == null) continue;
             ladders.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRank().get(ladder2));
         }
         BasicDBObject rw = new BasicDBObject();
         for (Ladder ladder2 : profile.getRankedWins().keySet()) {
         	if (ladder2 == null) continue;
         	rw.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRankedWins().get(ladder2));
         }
         BasicDBObject rl = new BasicDBObject();
         for (Ladder ladder2 : profile.getRankedLosses().keySet()) {
         	if (ladder2 == null) continue;
         	rl.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getRankedLosses().get(ladder2));
         }
         BasicDBObject urw = new BasicDBObject();
         for (Ladder ladder2 : profile.getUnRankedWins().keySet()) {
         	if (ladder2 == null) continue;
         	urw.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getUnRankedWins().get(ladder2));
         }
         BasicDBObject url = new BasicDBObject();
         for (Ladder ladder2 : profile.getUnRankedLosses().keySet()) {
         	if (ladder2 == null) continue;
         	url.put(ladder2.getName().toLowerCase().replace(" ", ""), (Object)profile.getUnRankedLosses().get(ladder2));
         }
         BasicDBObject kits = new BasicDBObject();
         for (Kit kit : profile.getKits()) {
             kits.put(kit.getName(), (Object)(Kit.serialize(kit) + ";" + kit.getLadder().getName().toLowerCase()));
         }
         dbo.put("kits", (Object)kits);
         dbo.put("ladders", (Object)ladders);
         dbo.put("rw", (Object)rw);
         dbo.put("rl", (Object)rl);
         dbo.put("urw", (Object)urw);
         dbo.put("url", (Object)url);
         dbo.append("globalelo", profile.getGlobalElo());
         dbo.append("totalmatches", profile.getTotalMatches());
         if (dbc.hasNext()) {
             collection.update(dbc.getQuery(), dbo);
             return;
         }
         collection.insert(dbo);
    }
    
    public static void saveArenas() {
        File file = new File(Nanny.getInstance().getDataFolder(), "arenas.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration((File)file);
        for (String key : configuration.getKeys(false)) {
            configuration.set(key, (Object)null);
        }
        try {
            configuration.save(file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        for (Arena arena : Arena.getArenas()) {
            if (arena.getSpawnLocations() != null && arena.getSpawnLocations().length == 2) {
                configuration.set("ARENA." + arena.getId() + ".LOCATION_ONE", (Object)LocationSerialization.serializeLocation(arena.getSpawnLocations()[0]));
                configuration.set("ARENA." + arena.getId() + ".LOCATION_TWO", (Object)LocationSerialization.serializeLocation(arena.getSpawnLocations()[1]));
                continue;
            }
            configuration.createSection("ARENA." + arena.getId());
        }
        try {
            configuration.save(file);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadArenas() {
        YamlConfiguration configuration;
        File file = new File(Nanny.getInstance().getDataFolder(), "arenas.yml");
        if (file.exists() && (configuration = YamlConfiguration.loadConfiguration((File)file)).contains("ARENA")) {
            for (String key : configuration.getConfigurationSection("ARENA").getKeys(false)) {
                Arena arena = new Arena(key);
                if (!configuration.contains("ARENA." + key + ".LOCATION_ONE")) continue;
                Location[] spawnLocations = new Location[]{LocationSerialization.deserializeLocation(configuration.getString("ARENA." + key + ".LOCATION_ONE")), LocationSerialization.deserializeLocation(configuration.getString("ARENA." + key + ".LOCATION_TWO"))};
                arena.setSpawnLocations(spawnLocations);
            }
        }
    }

    public static void loadLadders() {
        File directory = new File(Nanny.getInstance().getDataFolder(), "ladders");
        if (!directory.exists()) {
            directory.mkdir();
        }
        for (File ladderFile : directory.listFiles()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration((File)ladderFile);
            String name = yamlConfiguration.getString("NAME");
            Material material = Material.valueOf((String)yamlConfiguration.getString("MATERIAL").toUpperCase());
            short data = (short)yamlConfiguration.getInt("DATA");
            int defaultElo = yamlConfiguration.getInt("DEFAULT_ELO");
            Ladder ladder = new Ladder(name, defaultElo, material, data);
            boolean unranked = yamlConfiguration.getBoolean("QUEUE.UNRANKED");
            boolean ranked = yamlConfiguration.getBoolean("QUEUE.RANKED");
            boolean premiumRanked = yamlConfiguration.getBoolean("QUEUE.PREMIUMRANKED");
            if (yamlConfiguration.contains("KIT")) {
                String[] array = yamlConfiguration.getString("KIT").split("\\|");
                String armor = array[0];
                String inventory = array[1];
                Kit kit = new Kit(ladder, ItemStackSerializer.deserializeItemStackArray(armor), ItemStackSerializer.deserializeItemStackArray(inventory));
                kit.setName("Default " + ladder.getName() + " Kit");
                ladder.setKit(kit);
            }
            for (String arenaString : yamlConfiguration.getStringList("ARENAS")) {
                Arena arena = Arena.getArena(arenaString);
                if (arena == null) continue;
                ladder.getArenas().add(arena);
            }
            ladder.setUnranked(unranked);
            ladder.setRanked(ranked);
            ladder.setPremiumRanked(premiumRanked);
        }
        if (Ladder.getLadders().isEmpty()) {
            Bukkit.broadcastMessage((String)((Object)ChatColor.GREEN + Nanny.getInstance().getName() + ": Created default ladder No Debuff"));
            new Ladder("No Debuff", 1000, Material.DIAMOND_SWORD, 0);
        } else {
            Bukkit.getConsoleSender().sendMessage((Object)ChatColor.GREEN + Nanny.getInstance().getName() + ":  Successfully loaded " + Ladder.getLadders().size() + " ladders!");
        }
    }
    
    public static double calculateGlobalElo(Profile profile) {
    	int amount = 0;
    	int elo = 0;
    	double totalElo = 0;
    	for (Ladder l : Ladder.getLadders()) {
    		amount++;
    		elo = profile.getRank().get(l);
    		totalElo = elo + totalElo;
    	}
    	return totalElo / amount;
    }
    
    public static int getPosition(Profile profile, String path, double score) {
    	BasicDBObject gtQuery = new BasicDBObject();
    	gtQuery.put(path, new BasicDBObject("$gt", score));
    	DBCursor dbc = main.getCollection().find(gtQuery);
    	return dbc.count()+1;
    }
    
    public static void showLadderLeaderboard(Player sender, Profile profile, Ladder ladder, int page) {
    	DBCollection c = main.getCollection();
    	int size = c.find().size();
    	int pages = size/pagesSize;
    	if (size%pagesSize != 0) pages++;
    	
    	String ladderName = ladder.getName().toLowerCase().replace(" ", "");
    	String property = "ladders." + ladderName;
    	
    	if (page == -1) {
    		page = (int) Math.ceil(((double)Data.getPosition(profile, property, profile.getRank().get(ladder))/pagesSize));
    	} else {
    		if(page > pages) page = pages;
    	}

    	int position = 0 + (page-1)*pagesSize;
		//DBCursor dbc = c.find(new BasicDBObject(), new BasicDBObject(property, -1)).skip(pagesSize * (page - 1)).batchSize(pagesSize);
    	DBCursor dbc = c.find().sort(new BasicDBObject(property, -1)).skip(pagesSize * (page - 1)).limit(pagesSize);
		
		List<String> display = new ArrayList<String>();
		final String format = main.getLangFile().getString("STATS.LEADERBOARD.FORMAT");
		while (dbc.hasNext()) {
			position++;
			BasicDBObject savedMap;
			BasicDBObject dbo = (BasicDBObject)dbc.next();
			
            UUID uuid = UUID.fromString(dbo.getString("uuid"));
			
			savedMap = (BasicDBObject) dbo.get("ladders");
			int elo = (int) savedMap.getInt(ladderName);
			savedMap = (BasicDBObject) dbo.get("rw");
			int rw = (int) savedMap.getInt(ladderName);
			savedMap = (BasicDBObject) dbo.get("rl");
			int rl = (int) savedMap.getInt(ladderName);
				
			String entry = format;
			entry = entry.replace("%POSITION%", position + "").replace("%NAME%", Bukkit.getOfflinePlayer(uuid).getName() + "").replace("%ELO%", elo + "").replace("%WINS%", rw + "").replace("%LOSSES%", rl + "");  
			display.add(entry);
		}
		String header = main.getLangFile().getString("STATS.LEADERBOARD.HEADER");
		header = header.replace("%LADDER%", ladder.getName()).replace("%PAGE%", page + "").replace("%PAGES%", pages + "");
		sender.sendMessage(header);
		for (String s : display) {
			sender.sendMessage(s);
		}
    }
    
    public static void showGlobalLeaderboard(Player sender, Profile profile, int page) {
    	DBCollection c = main.getCollection();
    	int size = c.find().size();
    	int pages = size/pagesSize;
    	if (size%pagesSize != 0) pages++;
    	
    	String name = "globalelo";
    	
    	if (page == -1) {
    		page = (int) Math.ceil(((double)Data.getPosition(profile, name, profile.getGlobalElo())/pagesSize));
    	} else {
    		if(page > pages) page = pages;
    	}
    	
    	int position = 0 + (page-1)*pagesSize;
    	
    	DBCursor dbc = c.find().sort(new BasicDBObject(name, -1)).skip(pagesSize * (page - 1)).limit(pagesSize);
		
		List<String> display = new ArrayList<String>();
		final String format = main.getLangFile().getString("STATS.LEADERBOARD.FORMAT");
		while (dbc.hasNext()) {
			position++;
			BasicDBObject dbo = (BasicDBObject)dbc.next();
			
            UUID uuid = UUID.fromString(dbo.getString("uuid"));
			
            int elo = (int) dbo.getInt(name);
            
            Profile p = Profile.getProfile(uuid);
            int rw = p.calculateTotalFromHashMap(p.getRankedWins());
            int rl = p.calculateTotalFromHashMap(p.getRankedLosses());
				
			String entry = format;
			entry = entry.replace("%POSITION%", position + "").replace("%NAME%", Bukkit.getOfflinePlayer(uuid).getName() + "").replace("%ELO%", elo + "").replace("%WINS%", rw + "").replace("%LOSSES%", rl + "");  
			display.add(entry);
		}
		String header = main.getLangFile().getString("STATS.LEADERBOARD.HEADER");
		header = header.replace("%LADDER%", "Global").replace("%PAGE%", page + "").replace("%PAGES%", pages + "");
		sender.sendMessage(header);
		for (String s : display) {
			sender.sendMessage(s);
		}
    }
    
}
