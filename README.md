This is a Forge port of Foxynotail's Create:Storage addon from his latest Create YouTube series, with some minor
tweaks and enhancements to work better in a multiplayer environment.

Changes in this version over Fabric:
- Backpack will stack items up to 2x, 4x, 8x, 16x, or 32x the maximum item stack (dependent on backpack type)
- Backpack can be equipped in either the chest slot or back slot (when Curios installed)
- Backpack feeder upgrade will take negative food effects into account when feeding player
    - i.e. if side effect is Hunger or Poison, it won't auto feed the player the food item
- Added a GUI overlay that displays the amount of time left in the backtanks when flying (configurable)
- Tool Swap upgrade 
- All tooltips have been overhauled to have more of a "create" feel
- Implemented ponder scenes for most items
- Minor tweaks to a few recipes
- Smart Passers can accept a redstone signal to stop passing items
- Storage Box no longer requires a dedicated slot for voiding items
- Storage Box light will turn purple when void mode is enabled
- Storage controller will now light up when at least 1 Simple Storage Box has been added to the Storage Network
- Added "Elytra Boost" to Jetpack upgrade!
  - Hold "jump" while gliding with a Jetpack upgrade equipped. Boosting will consume backtank air at a much faster rate! (configurable)


---

This mod features a handful of Storage Boxes, each with different sized inventories as well as a handful of new upgradable backpacks.

# Storage Boxes
- Industrial Iron, Andesite, Copper, Brass and Hardened Storage Boxes
- Each has a filter slot on the front to filter which items can be added or taken from
- Features a screen on the front of each box showing how full the storage is
- Each box has an indicator light to show if the box is full or empty
- Boxes can be interacted with by the player to add or take items without having to open the GUI

# Simple Storage Boxes
- Common Wood Type Varieties (all have same attributes)
- Each can hold 2048* items as default
- Can only hold one type of item
- Void Ability
    - To apply void upgrade: interact with a Simple Storage Void Upgrade
    - To remove void upgrade: interact with a Simple Storage Void Upgrade
        - when box has Void Upgrade
        - or remove via the menu
- Capacity Upgrades
    - Capacity can be increased with Simple Storage Capacity Upgrades
    - Max 9 Capacity Upgrades can be added
    - Each capacity upgrade doubles current capacity
    - Capacity Upgrades can be removed using the menu
- Filter is set automatically when items are added
    - To remove Filter: interact with a Wrench (only when box is empty)
- Menu can be opened by interacting with an empty hand while sneaking

*Note: Capacity is 32x Max Stack Size of items contained.*  
*For Example: Logs will have a capacity of 2048, Ender Pearls will limit capacity to 512 and Buckets of Water will limit capacity to 32*

## Simple Storage Box Upgrades
### Void Upgrade:
- Once Simple Storage Box is full, additional items inserted will be voided (deleted)
### Capacity Upgrade:
- Doubles the current capacity of the Simple Storage Box

# Storage Trim / Casing
- Basic blocks with connected textures that match the Simple Storage Box variants
- Connects simple storage boxes with controllers and interface blocks

# Storage Networks
## Simple Storage Controller
- Used as a master input and output for a Simple Storage Network
- Creates a Storage Network when placed from Simple Storage Boxes connected by Storage Trim
- Items can be inserted and extracted from the network by the player (use / attack)
- Items can be inserted and extracted from the network via hoppers / chutes / funnels etc

## Simple Storage Interface
- Works similarly to the Storage Controller with exceptions
- Cannot be interacted with directly by the player (no manual insert / extract)
- Does not create a network (Network requires Controller)
- Items can be inserted and extracted from the network via hoppers / chutes / funnels etc

# Backpacks
- Industrial Iron, Andesite, Copper, Brass and Hardened Backpacks
- Each backpack can hold 6 upgrades which affect how the backpack works when worn
- Different variations of the backpack hold different amounts of items per slot
- Each has 3 storage compartments:
    - Main Storage your bulk items and can be interacted with by hoppers / chutes etc.
    - A safe Tool Storage compartment for your tools and precious items (cannot be interacted with by hoppers)
    - Upgrade Slots

## Backpack Upgrades
### Magnet Upgrade:
Works when worn on the player's back or placed on the floor
Pulls item into the backpack's main inventory compartment from up to 5 blocks away

### Item Pickup Upgrade:
Similar to the Magnet upgrade but only works when the back pack is worn
When the player touches an item entity, this puts the items into the backpack instead of the player inventory

### Pick Block Upgrade:
Pick block items from your backpack

### Refill Upgrade:
Refills the main hand or off-hand items from the backpack if they're available

### Tool Swap Upgrade:
Swaps out any tool held in the player's main hand for the best available tool or weapon when mining a block or hitting an entity

### Feeder Upgrade:
Automatically feeds the player fro the backpack when the player is hungry enough to eat

### Jetpack Upgrade:
Turns any backpack into a fully functional jetpack with flight and hovering abilities.
Uses Create Backtanks for fuel (must be inside the backpack)

### Fall Damage Upgrade:
Prevents the player taking fall damage while wearing the backpack

# Passer Blocks
## Basic Passer
- Passes items from one container to another
- Has no intermediary storage or inventory
- Can pass items horizontally and vertically depending on rotation
- Can be rotated in all directions using a Wrench
- Will transfer 1 item at a time (like a hopper)

## Smart Passer
- Can be filtered using Create filters
- Can transfer up to 64 items at a time
- Amount to pass is selectable using Create interface. (Hold filter slot and change amount)