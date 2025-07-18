# Changelog

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