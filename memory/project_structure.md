---
name: Structure projets — client custom + serveur Spigot
description: User runs the OriginsFight server (Spigot 1.8.8) with plugin OriginsFightCore, plus a custom 1.8.9 client (MavenMCP). Multiple sibling Bukkit/Bungee projects.
type: project
---

L'utilisateur (leziink / blixoww) gère le serveur PvP faction **OriginsFight** (anciennement RedConflict) — Spigot 1.8.8. Le **plugin principal** est `OriginsFightCore` (`fr.originsfight.OriginsFightCore`), Maven, dépendances : spigot-api 1.8.8-R0.1-SNAPSHOT, WorldEdit/WorldGuard 6.x, VaultAPI, Saber-Factions.

Côté joueur, il développe aussi un **client custom 1.8.9** via MCP (`MavenMCP-1.8.9-master`) — design philosophy minimal/Lunar-style (cf. `feedback_ui_design.md`).

Sibling Bukkit projects au même niveau (`C:\Users\Valentin\Desktop\JAVA MC\Project\IntelliJ\`) :
Anticlean, CJobs, ConodiaSanction, Duel, Duelz, FactionEvent, HUB, Halloween, LauncherMC, MelonTop, Mod 1.7, MooneriaItems, MySpawner, Nefazia, OriginsFightCore, RedFaction, SaraziaEvent, SkyBlockOptions, StaffChat, Trading, Utils.

**Why:** Quand l'utilisateur parle de "serveur", "PvP", "knockback", "combat", c'est sur **OriginsFightCore** (Spigot 1.8.8) qu'il faut bosser, **pas** sur MavenMCP. Le client custom ne peut pas modifier le KB sans flag anti-cheat.

**How to apply:**
- Pour toute logique gameplay/PvP/combat/économie/factions/staff serveur → cibler `C:\Users\Valentin\Desktop\JAVA MC\Project\IntelliJ\OriginsFightCore\`.
- Conventions du plugin : packages `fr.originsfight.<feature>`, messages dans `RC.java` (constantes statiques avec préfixe `§8[§c§lRedConflict§8]`), config dans `src/main/resources/config.yml` (sections commentées en français), enregistrement dans `OriginsFightCore#onEnable` (commands via `registerCommands`, listeners via `registerListeners` ou bloc dédié).
- Pour les tweaks UI/HUD/animations/cosmétique client → cibler `MavenMCP-1.8.9-master\src\main\java\net\minecraft\client\gui\ui\` (système de widgets déjà en place : CPS, Reach, ArmorGroup, etc.).
- Respecter le style minimal Lunar côté UI client.
