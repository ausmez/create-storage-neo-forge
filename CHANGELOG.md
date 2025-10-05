# Changelog

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