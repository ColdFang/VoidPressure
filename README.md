# Void Pressure

Void Pressure is a configurable Minecraft mod that introduces a global pressure value shared across the world. Pressure mainly increases through entity deaths and can trigger events, effects, or world changes once defined thresholds are reached.

The mod is designed as a framework. It does not enforce a specific gameplay loop but instead provides systems that can be freely configured and combined to create custom mechanics.

The idea for Void Pressure originated while creating a Skyblock modpack, but the system works just as well in normal survival worlds or other custom game modes.

Void Pressure is currently in beta. The core systems are stable and usable, but the mod is still under active development and will be expanded with future updates.

Pressure is stored globally per world. Player kills and passive deaths can be balanced separately, per-entity overrides are supported, and pressure can optionally decay over time. A rolling 60-second window tracks how much pressure was gained and lost recently and is used to calculate the current pressure trend.

At configurable pressure values, different kinds of events can be triggered. Supported event types include command execution, potion or attribute effects, and mob modifications. Events can trigger at exact values or repeat at defined intervals and may be configured as one-time or persistent. All events are defined via configuration and can be edited in-game.

Void Pressure also includes a Pressure Altar that allows players to actively reduce pressure by sacrificing items. Offerings are fully configurable, the ritual can optionally cost XP, and a cooldown can be applied. The altar system is independent from the event system and acts as a player-controlled counterbalance.

An in-game GUI is included for overview and configuration. It can be opened with Shift + V and shows the current pressure value, recent pressure changes, and the 60-second trend. Event lists and overrides can be edited directly, and the HUD position can be moved or reset in-game. Most changes do not require a restart.

Void Pressure intentionally avoids hardcoded gameplay behavior. The mod provides tracking, trigger logic, and structured systems, while the actual gameplay impact is defined entirely through configuration. This makes it well suited for modpacks and custom game designs.
