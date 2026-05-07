# Unpacker

NeoForge 1.21.1 / Java 21 mod that adds an **Unpacker** block.

## What it does

The Unpacker unloads supported container items into a hopper below and separates the emptied container item to the front output side.

Supported direct input containers:

- Traveler's Backpack items from `travelersbackpack`
- Vanilla shulker boxes
- Vanilla bundles

Nested containers are **not** recursively unpacked. A shulker/backpack/bundle stored inside another supported container is extracted as a normal item.

## Machine layout

```text
Input container / hopper / pipe
             ↓
        [ Unpacker ] → chest/barrel in front receives emptied containers
             ↓
           Hopper
             ↓
   chest below receives unloaded contents
```

## Side behavior

- Top side: insertion only; accepts supported containers only.
- Bottom side: extraction only; exposes only the contents inside the current input container.
- Front side: extraction only; emptied containers are pushed into an inventory in front once every 8 ticks.
- Other sides: no automation.

## Traveler's Backpack behavior

The mod uses the Traveler's Backpack item-handler capability exposed by Traveler's Backpack. It unloads normal backpack storage only and does not touch fluid tanks, upgrade slots, or tool slots.

## Recipe

```text
Iron Ingot | Hopper | Iron Ingot
Chest      | Barrel | Chest
Iron Ingot | Comparator | Iron Ingot
```

## Build

Use Java 21.

```bash
./gradlew build
```

The jar will be in `build/libs/`.
