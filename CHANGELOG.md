# Changelog

## 1.2.0 - 2026-03-08

> **Note:** This is the final feature update supporting Minecraft 1.20.1, aligning with the Create mod team’s support lifecycle.

> **⚠ Warning:** The backpack system has been significantly rewritten in this release. **Please back up your world before updating.**
> Backpack data structures have changed and worlds without a backup may experience data loss on existing backpacks.

## New Features
- **Backpack Jukebox Upgrade**
  - Adds a functional jukebox to your backpack.
  - Provides configurable buffs while music is playing (range and toggle options available).
  - Supports muting without stopping playback or disabling buffs.
- **Backpack Mechanical Heart Upgrade**
  - Grants bonus health while the backpack is worn.
  - Bonus heart amount is configurable (default: **+5 hearts**).
- **Simple Storage Filtered Interface**
  - Variant of the Storage Interface with a built-in Create filter slot.
  - Useful for controlling which items pass through when interacting with vanilla automation components.
  - Supports configurable scope: **filtered items only**, or **filtered items + empty storage**.

## Changes
### **Backpack Upgrade Panels**
Backpack upgrades now use dedicated UI panels within the backpack screen for upgrade settings.
All per-upgrade options previously scattered across different menus are now accessible on a per-backpack basis.

- **Magnet Upgrade**
  - Added configurable filters (supports Create list / attribute / package filters).
  - Option to ignore items processed by an Encased Fan moved to the panel UI.
- **Tool Swap Upgrade**
  - Added option to prioritize swords over axes as the primary weapon.
  - Silk Touch preference moved to the panel UI.
- **Jetpack Upgrade**
  - Hover bobbing toggle moved to the panel UI.
  - HUD airtime overlay toggle moved to the panel UI.
- **Ore Mining Upgrade**
  - Added toggle for ore vein preview highlighting.
  - Added option to restrict mining to ores only.
- **Feeder Upgrade**
  - Added configurable filters (supports Create list / attribute / package filters).
  - Chorus Fruit feeding toggle moved to the panel UI.
  - Activation message toggle moved to the panel UI.

#### **Storage Network Changes**
- Storage Controllers can now lock or unlock empty Storage Boxes on a per-network basis.
- Storage networks can now be visually highlighted to show all connected storage *(Right-click a Storage Controller with a wrench)*.
- Filter item render distance on Storage and Simple Storage Boxes now uses Create’s configuration settings.

#### **Other Changes**
- Moved many configuration options from Common to Server settings.
- Passer Blocks can now connect to Package Ports (Frog and Post).
- Updated recipe for the Ore Mining Upgrade.
- Flight Upgrade renamed to Jetpack Upgrade.
- Migrated player settings where possible following internal persistent data restructuring.
- Optimized the Storage Network search algorithm for improved performance.

## Bug Fixes
- Fixed Storage and Simple Storage Boxes rendering incorrectly in the Block Rotation menu.
- Fixed lighting issues on Storage and Simple Storage Boxes.
- Fixed Smart Passer deleting items when transferring exact amounts (#37).
- Fixed item swap issue when swapping between backpack and offhand / hotbar slots (#40).
- Fixed crash when Forbidden and Arcanus mod present (including ATM10) (#42)
- Fixed Magnet Upgrade picking up items when backpack had no free slots.
- Fixed Magnet Upgrade sometimes picking up items processed by an Encased Fan despite configuration.
- Fixed potential server crash when double-clicking a Storage Controller.
- Fixed potential endless loop in Storage Network searches.
- Fixed possible NPE with custom names on Storage Boxes.
- Fixed incorrect text appearing in some Ponder scenes.
- Fixed Simple Storage Box filter not updating correctly when mounted on a contraption.
- Fixed Storage Boxes ignoring the Filter Item while Void Mode is enabled.
- Fixed missing sound when equipping a Backpack from the ground. [NeoForge]

---

## 1.1.7 - 2025-11-29

## Bug Fixes
- Fixed an issue where quickly right-clicking between multiple Storage or Simple Storage Boxes could be incorrectly detected as a double click.
- Fixed log spam caused by using the Magnet Upgrade in a backpack that had been placed in the world. (#35)
- Fixed an issue where certain menus would close while the player was still within interaction range. [Forge]
- Fixed an issue where the Magnet Upgrade’s range would not update when modified via the config screen.

---
## 1.1.6 - 2025-11-10
## Changes
- Added Goggle overlay for Simple Storage Boxes (configurable), displaying the vanilla tooltip for items with:
  - Enchantments (enchanted books, armor)
  - Potion data (potions, tipped arrows)
  - Trim data (trimmed armor)

## Bug Fixes
- Fixed lighting issues affecting rendered filter items on Storage Boxes and Simple Storage Boxes.
- Fixed storage networks ignoring NBT/Data Components when inserting items (such as potions and enchanted books) (#33)

---
## 1.1.5 - 2025-11-05
## Changes
- **Create 6.0.7+ Support**
  - Updated to support Create 6.0.7 and later.
  - Create 6.0.7 is now the minimum supported version.
- **Storage**
  - Added a new tag: `symmetry_wand_blacklist`
    - items with this tag will be ignored by Create's Wand of Symmetry when placing blocks
  - Storage Boxes, Simple Storage Boxes, Backpacks, Create Toolboxes and Vanilla Shulker Boxes have been added to this tag 
- **Optional Dependency**
  - Every Compat (Wood Good): Added support for Simple Storage Boxes and Storage Trims when additional wood type are available via other mods (i.e. Biomes O' Plenty, Ecologics, etc.) (#30)

## Bug Fixes
- Fixed items disappearing from backpacks when placed with Wand of Symmetry in hotbar (symmetry_wand_blacklist). (#29)
- Fixed right-clicking a Storage Controller’s front face with a wrench incorrectly adding it to the storage network
- Fixed double-clicking with an empty hand on a Storage Box now correctly respects filters
- Fixed Simple Storage Box display count not updating when items are inserted via a storage network
- Fixed Storage Network inserting items into empty Simple Storage Boxes that had a filter set. (#32)
- Fixed double-click with an empty hand on a Storage Controller now correctly transfers matching items from inventory to the network
- Fixed crash when a contraption is unmounted while a Simple Storage Box screen is open
- Fixed issue with Storage Box transferring all items from player inventory on double click [Forge]
- Fixed issue where double-clicking on a Storage Box, Simple Storage box or Storage Controller would not register in a multiplayer server
- Fixed rendering issue where Simple Storage Boxes mounted on contraptions did not display the Void Upgrade icon
- Fixed issue where remaining Jetpack fuel was still displayed while jumping after fuel was depleted
- Fixed Simple Storage Box filter icon not updating on contraptions while the menu was open
- Fixed Simple Storage Box tooltip displaying void upgrade item twice [Forge]
- Fixed Simple Storage Boxes not saving their inventory when dropped [Forge] 

---
## 1.1.4 - 2025-10-11
## Changes
- **Feeder Upgrade**
  - Added `feederHealthThreshold` and `feederHungerLevel` config options for more control over when to auto-feed.
  - No longer feeds player an Ominous Bottle. [NeoForge]
- **Refill Upgrade**
  - Now only refills placeable block items. 
  - Added a new tag: `refill_blacklist` — items with this tag will be ignored by the Refill Upgrade.
  - Added a new config option to blacklist blocks, with support for wildcards to blacklist entire namespaces (i.e. minecraft:*)

## Bug Fixes
- Fixed an issue where backpack upgrades did not recognize the Right Ctrl key when toggling.
- Fixed the Void Upgrade being extracted from the Simple Storage Box by automation (hopper, chutes, funnels, etc.).
- Fixed an issue where the Simple Storage Box would not update its status.
- Fixed block items being placed in front of a Simple Storage Box when inserting them.
- Fixed server crash related to client-side class loading. [Forge]
- Fixed a server disconnection issue when Feeder Upgrade attempted to auto-feed player Chorus Fruit. [Forge]

---
## 1.1.3 - 2025-10-05
## Changes
- Updated Elytra boost system to allow configurable boost speed multiplier.
- Moved the following configuration options to the “Flight Upgrade” category *(Note: existing settings will not migrate)*
  - elytraBoostEnabled, elytraBoostMultiplier, jetpackMiningPenalty

## Bug Fixes
- Fixed Storage Box not correctly applying filter when using list or attribute filters.
- Fixed remaining air persisting after landing instead of fading out.
- Fixed item duplication when Backpack, Storage Box or Simple Storage Box GUI is open and block is destroyed or not in range of player. (#23)
- Fixed memory leak within `StorageBoxMountedStorage` and `SimpleStorageBoxMountedStorage`. (#24)
- Fixed player incorrectly taking falling damage when jumping just before landing with an equipped Backpack flight upgrade.
- Fixed Crafter injecting items into Simple Storage Box upgrade slots. (#25) [NeoForge]
- Fixed `BackpackHelper` to work correctly when multiple Curios "back" slots are present. (#26) — Thanks @MoePus
- Fixed potential server lag caused by excessive block updates from Simple Storage Boxes.
- Fixed an issue where players would rise upward after teleporting while hovering with the Backpack Flight upgrade.

---
## 1.1.2 - 2025-09-09
## Changes
- **Backpack Integration**
  - Bows, Crossbows, and Potato Cannons can now use projectiles (arrows & food) stored in an equipped backpack *(configurable)*.
  - The Create Toolbox can now check equipped backpack contents when returning items *(configurable)*.
  - JEI/EMI/REI recipe transfer can now pull ingredients from an equipped backpack when crafting with the 2×2 inventory grid.
- **Backpack Upgrades**
  - The Magnet Upgrade now visually pulls items toward the player *(when worn)* or toward the backpack *(when placed in the world)*.
  - The Flight Upgrade has been completely re-written to improve responsiveness.
  - The mining penalty while flying with the Flight Upgrade can now be disabled *(configurable)*.
- **Compatibility**
  - Added compatibility with the Construction Wand/Sticks mod for Backpacks *(when not worn)*, Storage Boxes, and Simple Storage Boxes (#12).
- **Item Sorting**: 
  - Items with a custom name are now sorted after items with the default name.

## Bug Fixes
- Fixed Storage Box display not updating correctly when used on a contraption.
- Potato Cannon now moves into item storage of backpack when quick-moved.
- Quick-moving non-stackable items (potions, enchanted books, music discs) now correctly stacks in backpacks.
- Fixed Void Upgrade in Simple Storage Boxes incorrectly filling all available slots. (#11)
- Fixed Chutes and Smart Chutes not transferring items into Storage Controllers and Storage Interfaces after a world reload. [NeoForge]
- Fixed crash when querying slot limits on a Storage Network. (#13) [NeoForge]
- Further fixes to the Flight Upgrade to improve desynchronization issues. (#14)
- Fixed duplication bug when upgrading backpacks of the same tier using recipe viewers’ "max craft" function. (#15)
- Fixed random crash when Storage Boxes are mounted on contraptions. (#16) — Thanks @MoePus
- Fixed automation not respecting Storage Box filters.
- Fixed flight upgrade thrust sound to play correctly when launching from the ground.
- Fixed particles when using the flight upgrade in lava.
- Fixed duplication bug when using a Passer Block with a Simple Storage Box. (#17)
- Fixed crafting grid ingredient placement — now correctly respects symmetrical recipes.
- Fixed server lag caused by excessive block updates from Storage Boxes. (#18)
- Fixed internal data structure of Simple Storage Boxes — existing boxes will migrate automatically on first load.
- Fixed Void Upgrade in Simple Storage Boxes incorrectly stacking above 1 in the void slot.
- Fixed rendering of text and filter items on Storage and Simple Storage Boxes when mounted on contraptions.
- Fixed void icon rendering backwards on Simple Storage Boxes.
- Fixed network protocol error when interacting with Storage Boxes and Simple Storage Boxes in spectator mode.
- Fixed log spam when interacting with a Simple Storage Box mounted to a contraption with no filter set (#19). [NeoForge]
- Fixed issue sending server packet from server with a Simple Storage Box mounted to a contraption (#20). [NeoForge] — Thanks @MoePus
- Various other minor fixes and general code cleanup.

---
## 1.1.1 - 2025-07-27
## Bug Fixes

- Fixed issue where the Backpack Flight upgrade would behave erratically after player respawn following a death.
- Fixed game crash when a packager attempts to unpack items into a disconnected Storage Interface. (#8) - Thanks @mbegenau
- Fixed issue where Passer blocks were not transferring items into or out of vanilla composters.
- Fixed issue where quick-moving tools from the first player inventory slot would incorrectly place them into item storage.
- Resolved missing item tags when quick-moving items into the backpack tool storage. [NeoForge]
- Fixed right-click behaviour on backpack slots; now moves either half the max stack size or half the slot count, whichever is smaller.
- Fixed issue where mining a Shulker Box with Ore Mining Upgrade enabled would cause it to lose its contents.
- Fixed an issue where block placement sounds would not play when placing blocks on any side of Storage Boxes, Simple Storage Boxes, or Storage Controllers.
- Fixed game crash when using the Backpack Pickblock Upgrade on a Metal Girder Encased Shaft. [NeoForge]

---
## 1.1.0 - 2025-07-18
## New Features
- **Backpack Upgrades:**
  - **Ore Mining Upgrade** - Mine an entire cluster of ore at once. Hold the modifier keybind to mine any connected blocks (up to 64) of the same type (configurable).
  - **Torch Deployer Upgrade** - Load up your backpack with Torches and never stumble in the dark again! Automatically places a torch from the backpack if the local light level is too low (configurable).
  
## Changes
- **Jetpack Upgrade:** Added configuration option to enable/disable the bobbing effect when hovering.
- **Recipe Viewers:** Better integration with JEI/EMI/REI.
  - Crafting recipes can now pull items direct from the equipped backpack as well as player inventory.
- **Storage Network Rewrite:** Significant internal refactor to improve compatibility with other mods. (#6)
  - New configuration setting allows control over whether empty Simple Storage Boxes can accept new items when part of a Storage Network.

## Bug Fixes
- Added missing blocks to #breakable_with_any_tool tag.
- Fixed missing recipes for Cardboard and Weathered Iron Storage Boxes. [Forge]
- Fixed Packager not respecting void mode on Cardboard and Weathered Iron boxes.
- Fixed Pale Oak Simple Storage Box not functioning correctly with Packagers.
- Fixed Storage Box and Simple Storage Box tooltip numbers incorrectly rendering when Backpack screen open.
- Fixed Backpack stack multiplier incorrectly being applied before placing as a block in the world. (#4) [NeoForge]
- Fixed crash when quick moving a deactivated Backpack upgrade to the player inventory. [NeoForge]
- Fixed duplication bug when unequipping disabled Backpack upgrades. [Forge]
- Fixed desynchronization issue when using Jetpack upgrade with high network latency. (#5)
- Fixed Storage Box not sorting when mounted to a contraption. [Forge]
- Fixed game crash when connecting a Passer to a vanilla chest. (#7) [NeoForge]
- Fixed duplication bug when using a Passer with a Packager to extract packages.
- Various other minor bug fixes and general code cleanup.

---
## 1.0.4 — 2025-06-15
## Bug Fixes
- Fixed game crash when game window resized while Backpack screen is open. (#3) [Forge]

## Changes
- Improved Feeder upgrade logic for detecting harmful food effects.
- Feeder upgrade no longer feeds Chorus Fruit to the player by default (configurable).

---
## 1.0.3 — 2025-06-12
## Bug Fixes
- Feeder upgrade now properly evaluates harmful food effects [NeoForge]
- Fixed items not stacking to the maximum capacity of a Simple Storage Box with a Storage Network. [NeoForge]
- Fixed game crash on launch if KubeJS was present (#2) [NeoForge]

---
## 1.0.2 — 2025-06-07
## Bug Fixes
- Fixed NBT error when opening a Backpack from player inventory [NeoForge]

---
## 1.0.1 — 2025-06-07 / 2025-06-12
## Bug Fixes
- Fixed missing keybind descriptions in language file. [NeoForge]
- Fixed items not being saved to the Backpack unless first placed as a block. [Forge]
- Fixed items not stacking to the maximum capacity of a Simple Storage Box with a Storage Network. [Forge]

---
## 1.0.0 — 2025-05-09 / 2025-05-19
## Changes
- **Create 6.0:**
  - Support added for Create 6.0.3+.
  - New Storage Box variants: Cardboard and Weathered Iron.
- **Ponder Scenes:** Updated to support new Ponder API.
- **Packager Support:**
  - Can be attached to Backpacks, Storage Boxes, Simple Storage Boxes, Storage Controllers and Storage Interfaces to pack/unpack packages.
  - Attach a Stock Link to connect a Storage Network to a logistics network.
- **Optional Dependency:**
  - Vanilla Backport: Support added for Pale Oak Simple Storage Box and Pale Oak Trim when mod present.

## Known Issues
- When a Storage Network has multiple Packagers with Stock Links attached, the Stock Keeper will display incorrect
  inventory totals for the network. ([Create#7554](https://github.com/Creators-of-Create/Create/issues/7554))
- If a threshold switch is attached to a Storage Network component (Storage Controller/Interface), incorrect values
  are displayed for Items/Stacks. ([Create#7688](https://github.com/Creators-of-Create/Create/issues/7688))

---
## 0.22 — 2025-03-21
## New Features
- **GUI Overlay for Backtanks:** Displays remaining air time while flying (configurable).
- **New Ponder Scenes:** Added for most items in Create: Storage to aid understanding.
- **"Elytra Boost" Added:**
  - Hold "jump" while gliding with an Elytra and the Flight upgrade equipped, or toggle with "hover."
  - Boosting consumes backtank air at a faster rate (configurable).
- **Feeder Upgrade Notification:** Displays a message when the Feeder upgrade feeds an item to the player (configurable).
- **Sortable Storage:** Middle-click to sort items in Backpacks, Storage Boxes, and Simple Storage Boxes. The order is customizable.

## Changes
- **Backpack Enhancements:**
  - Item stacking modified for Backpack tiers: now stacks up to 2x, 4x, 8x, 16x, or 32x the max item stack.
  - Backpacks can now be equipped in the chest slot or back slot (if Curios is installed).
  - Feeder upgrade avoids feeding items with negative effects (e.g., hunger or poison).
- **Flight Upgrade:** Adjusted for smoother, more natural momentum.
- **Tool Swap Upgrade:**
  - Now configurable to prioritize Silk Touch and specific blocks.
- **Smart Passer:** Now react to redstone signals to stop passing items.
- **Simple Storage Box:** Can now be used/interacted with on contraptions for bulk storage.
- **Storage Box Enhancements:**
  - No longer requires a dedicated slot for voiding items in void mode.
  - Indicator light turns purple when void mode is active.
  - Refined player interactions with Storage Boxes and Simple Storage Boxes.
  - Can now be used/interacted with on contraptions for bulk storage.
- **Storage Controller:** Lights up when at least one Simple Storage Box is connected to the network.
- **Tooltip Enhancements:** Now have a more "Create" aesthetic.
- **Recipe Tweaks:** Minor adjustments and balance improvements.

## Bug Fixes
- **Simple Storage Box:** Items on the display no longer render backwards.
- **Storage Interface:** Properly disconnects from the storage network when removed.
- **Feeder Upgrade:** Stores leftover items in Backpack (or drops if no space) after consumption (e.g., Honey Bottles, "bowl" foods).
- **Flight Upgrade:**
  - No longer deactivates when other players join the server.
  - Displays bubble particles instead of clouds while "flying" underwater.
  - Projectiles (e.g., arrows, eggs, potions) now fire at the correct angle when flying.
- **Tool Swap Upgrade:** No longer infinitely swaps when multiple tools of the same type are present in the backpack.
- **Visual Fixes:**
  - Backpack particles now correctly display to other players on multiplayer servers.
  - Backpacks render only if equipped and visible on multiplayer servers.
- **Other Fixes:**
  - Gravity behaves correctly when wearing a Backpack underwater.

---
## 0.20 — 2025-02-01
- _Initial migration from Fabric release_.