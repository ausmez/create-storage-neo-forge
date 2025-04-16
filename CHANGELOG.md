# Changelog

## 1.0.0 - 2025-04-09

## Changes
- **Create 6.0.0:** Support added for Create 6.0.3+
- **Ponder Scenes:** Updated to support new Ponder API
- **Packager Support:**
  - Can be attached to Backpacks, Storage Boxes, Simple Storage Boxes, Storage Controllers and Storage Interfaces to pack/unpack packages.
  - Attach a Stock Link to connect a Storage Network to a logistics network. 

## Known Issues
- When a Storage Network has multiple Packagers with Stock Links attached, the Stock Keeper will display incorrect
  inventory totals for the network ([Create#7554](https://github.com/Creators-of-Create/Create/issues/7554))
- If a threshold switch is attached to a Storage Network component (Storage Controller/Interface), incorrect values
  are displayed for Items/Stacks ([Create#7688](https://github.com/Creators-of-Create/Create/issues/7688))
  

## 0.22 - 2025-03-01

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
- **Storage Box Enhancements:**
  - No longer requires a dedicated slot for voiding items in void mode.
  - Indicator light turns purple when void mode is active.
  - Refined player interactions with Storage Boxes and Simple Storage Boxes.
- **Storage Controller:** Lights up when at least one Simple Storage Box is connected to the network.
- **Tooltip Enhancements:** Now have a more "Create" aesthetic.
- **Recipe Tweaks:** Minor adjustments and balance improvements.

## Bug Fixes
- **Storage Box:** Can be used on contraptions for bulk storage.
- **Simple Storage Box:** Items on the display no longer render backwards
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


## 0.20 - 2024-12-01

- _Initial migration from Fabric release_