# Phase 19 Hotfix 1 — Gravity-lift collision correction

## Problem

The animated lift path attempted to use `Entity#noClip` on the server. That flag is not reliably synchronised to a normal player's client in Minecraft 1.12.2, so client-side collision prediction could keep the player embedded in the solid source or destination level block.

## Correction

Gravity-lift travel is now a server-authoritative phase jump between the two paired level blocks:

- Stand on an **up** level and press jump.
- Stand on a **down** level and hold sneak.
- The entity is placed at the safe standing position above the paired level.
- The destination requires two clear blocks above it.
- The existing one-second cooldown prevents immediate return trips.
- Non-player living entities still activate automatically.

This deliberately prioritises reliable transport over the previous incremental movement animation. A later visual-effects pass can add particles, sounds, or a client animation without relying on collision-unsafe movement.

## Regression checks

1. Put two gravity-lift levels on the same X/Z with an empty shaft.
2. Set the lower level to `up` and the upper level to `down`.
3. Jump on the lower level and confirm arrival on top of the upper level.
4. Hold sneak on the upper level and confirm arrival on top of the lower level.
5. Confirm the player is not embedded in either block.
6. Place a block directly above a destination and confirm the lift refuses that unsafe trip.
7. Repeat on a dedicated server as a non-operator.
