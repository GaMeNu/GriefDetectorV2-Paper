# GriefDetector V2 - PaperMC
**WARNING: This plugin is still in early development, use at your own risk!**

**Note: this plugin was made for PaperMC. While it may work for Bukkit or Spigot servers, it is best reccomended to use Paper to avoid bugs and malfunctions.**

This repository is a port of GriefDetector v2 (GDv2) first made in BWUBot v2.

Feel free to point out/ridicule me for my terrible code! I would be grateful if you point out mistakes/places I can improve the code at

### Features:
- Saves every placed/broken block using SQLite.
- GriefDetector item to get the last player who modified the block.

## How to use
### Install on a server:
- Download the latest production-ready .JAR file from Releases (see image)
![Releases page, to the right of the repo's homepage](https://user-images.githubusercontent.com/98153342/217238119-b9377cce-3c01-4378-a5ff-ee0d5f0ffa3f.png)
- Place the .JAR file within your server's plugin folder.
- Start/restart your server
- It's as simple as that! GDv2 will take care of the rest of the setup.
### Usage in the server
Use the command `/gd` to receive the GriefDetector item.
Place the GriefDetector or Break a block with it to get the last person who modified that block

## How it works
- When a player first logs on, a "bwuID" is generated for them. That ID is linked to their Minecraft username and UUID.
- Every time a player logs on, their UUID and bwuID get loaded to a player cache in memory.
- Whenever a block is placed or broken, that block is saved to a different block cache in memory.
- When the block cache's size reaches 64, its contents get pile-dumped into the main database.
- When the GD item is used, the bwuID connected to that block is retrieved (either from the cache or the main DB), then their linked Username and UUID is retrieved from the DB and sent to the player who used the GD item.
- Powered by GM's hatered for himself and his life.

