# Packworks

NeoForge 1.21.1 / Java 21 mod that adds two sibling container-automation blocks:

- **Unpacker** — unloads supported containers into item-output slots and separates the emptied container.
- **Packer** — packs loose items into supported containers and outputs the filled container.

## Supported containers

Packworks works by itself with vanilla containers:

- Vanilla shulker boxes
- Vanilla bundles

Optional integration:

- Traveler's Backpack items from `travelersbackpack`, when Traveler's Backpack is installed

Traveler's Backpack is **not required** to use Packworks. If it is not installed, Packworks still works with shulker boxes and bundles.

Nested containers are **not** recursively packed or unpacked. A shulker/backpack/bundle stored inside another supported container is handled as a normal item.

## Unpacker automation

```text
Top side:    full / partially-full container input
Front side:  empty container output
Bottom side: extracted item output
```

## Packer automation

```text
Top side:    loose item input
Front side:  supported container input
Bottom side: packed container output
```

## Recipes

Both blocks use iron ingots, a hopper, chests, and a barrel.

The Unpacker uses a comparator in the bottom-middle recipe slot.

The Packer uses redstone in the bottom-middle recipe slot.

## Build

Use Java 21.

```bash
./gradlew build
```

The jar will be in `build/libs/` and is configured to build as:

```text
Packworks-1.0.0-NeoForge-1.21.1.jar
```
