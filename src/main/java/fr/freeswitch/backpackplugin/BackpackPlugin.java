package fr.freeswitch.backpackplugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A plugin that adds a backpack feature to players in Bukkit.
 */
public class BackpackPlugin extends JavaPlugin implements Listener {
    private Map<String, Inventory> backpacks;
    private Gson gson;

    @Override
    public void onEnable() {
        backpacks = new HashMap<>();
        gson = new GsonBuilder().setPrettyPrinting().create();
        loadBackpacks();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveBackpacks();
        backpacks.clear();
    }

    /**
     * Handles the execution of a command.
     *
     * @param sender  The command sender.
     * @param command The command being executed.
     * @param label   The command label.
     * @param args    The command arguments.
     * @return {@code true} if the command was handled successfully, {@code false} otherwise.
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This plugin can only be used by players.");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (command.getName().equalsIgnoreCase("backpack")) {
            openBackpack(playerName);
            return true;
        }
        return false;
    }

    /**
     * Opens the backpack inventory for a player.
     *
     * @param playerName The name of the player.
     */
    private void openBackpack(String playerName) {
        Player player = Bukkit.getPlayerExact(playerName);
        if (player == null) return;

        Inventory backpack = backpacks.get(playerName);
        if (backpack == null) {
            backpack = Bukkit.createInventory(null, 27, "Backpack");
            backpacks.put(playerName, backpack);
        }

        player.openInventory(backpack);
    }

    /**
     * Saves the backpack inventories to disk.
     */
    private void saveBackpacks() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        for (Map.Entry<String, Inventory> entry : backpacks.entrySet()) {
            String playerName = entry.getKey();
            Inventory backpack = entry.getValue();
            String backpackData = serializeInventory(backpack);

            try (Writer writer = new FileWriter(new File(dataFolder, playerName + ".json"))) {
                gson.toJson(backpackData, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Loads the backpack inventories from disk.
     */
    private void loadBackpacks() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            return;
        }

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String playerName = file.getName().replace(".json", "");
            try (Reader reader = new FileReader(file)) {
                String backpackData = gson.fromJson(reader, String.class);
                Inventory backpack = deserializeInventory(backpackData);
                backpacks.put(playerName, backpack);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Serializes an inventory to a JSON string.
     *
     * @param inventory The inventory to serialize.
     * @return The serialized inventory as a JSON string.
     */
    private String serializeInventory(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        ItemStack[] serializedContents = new ItemStack[contents.length];

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null) {
                serializedContents[i] = new ItemStack(item.getType(), item.getAmount());
            }
        }

        return gson.toJson(serializedContents);
    }

    /**
     * Deserializes an inventory from a JSON string.
     *
     * @param inventoryData The JSON string representing the inventory.
     * @return The deserialized inventory.
     */
    private Inventory deserializeInventory(String inventoryData) {
        ItemStack[] serializedContents = gson.fromJson(inventoryData, ItemStack[].class);
        Inventory inventory = Bukkit.createInventory(null, 27);

        for (int i = 0; i < serializedContents.length; i++) {
            ItemStack item = serializedContents[i];
            if (item != null && item.getType() != Material.AIR) {
                inventory.setItem(i, item);
            }
        }

        return inventory;
    }

    /**
     * Handles the event when a player closes an inventory.
     *
     * @param event The inventory close event.
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (!inventory.getViewers().equals("Backpack")) {
            return;
        }

        saveBackpack(player.getName());
    }

    /**
     * Saves the backpack inventory of a player to disk.
     *
     * @param playerName The name of the player.
     */
    private void saveBackpack(String playerName) {
        Inventory backpack = backpacks.get(playerName);
        if (backpack == null) {
            return;
        }

        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        String backpackData = serializeInventory(backpack);
        try (Writer writer = new FileWriter(new File(dataFolder, playerName + ".json"))) {
            gson.toJson(backpackData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
