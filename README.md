# Reborn - Dying Will (0.4.1)

NeoForge 21.1 / Minecraft 1.21.1. Requires Curios (9.x for 1.21.1).

Milestone 1 laid the foundation: the flame system, the Dying Will form ladder, Flame Gems, the basic resonator
and Sky's ring kit. Milestone 2 (0.2.0) added the other six Sky-set flames' ring kits, the Ring Forge and flame pills.
0.4.1 adds weapon-move animations. 0.4.0 finished the graphics pass: flame-coloured infusion glints, weapon move and hit effects, and infused shields' on-block effects (below). Milestone 3 (0.3.0) added flame infusion: the Infuse toggle, the Infusion Anvil and infused weapon moves.
Every number is a placeholder in the server config files `reborn_dying_will-server.toml` and `reborn_dying_will-kits.toml`.

## What's in it

**Flames and capacity**
- All fifteen flames exist in the data model (Sky set, Earth set, Night). The seven Sky-set flames have their ring kits;
  Earth and Night already have capacity, ranks, Dying Will states and the HUD.
- Capacity per flame in FV, ranks E to S by the capacity bands, growth by spending flame
  (4% of FV spent times the rank factor, x1.5 below 30% health or in a Dying Will form).
- Pool regenerates 0.5%/s, 1.5%/s after standing still for 3 seconds.
- Death scars every unlocked flame by 3% (cap 15%); scar heals as you spend flame (5 FV per 1 FV of scar).
- Switching flames: press Y to open the flame selection screen and pick one. 30 s cooldown, empties the pool, not allowed during a form.

**Awakening**
- Near death: lethal damage to an unawakened player leaves them at half a heart in Dying Will and reveals
  their natural flame (fixed by UUID). Not for /kill or the void.
- Dying Will Pill: the first one only works at low health. After that, any health.
- Flame pills (one per Sky-set flame): an awakened player at low health who eats one awakens that flame, switches
  to it with a full pool, and a tick later enters Dying Will in it (if not already in a form). At higher health nothing happens and the pill isn't used.
  Recipe: Dying Will Pill + Sky gold ingot, Storm blaze powder, Rain prismarine crystals, Sun glistering melon slice,
  Lightning lightning rod, Cloud phantom membrane, Mist fermented spider eye.
- Dying Will Pistol + Bullets: a hit player enters Dying Will, awakening them at any health (PvP behind a config option).
  A hit mob gets a frenzy.

**Forms**
- Dying Will: 30 s, +5 to 10 s per entry, capped at 3 min; boost 1.5x plus 0.05x per rank above E
  (attack, speed, mining speed, knockback resistance); entering restores 10% of the pool.
  Ends in the vanilla Weakness effect (level set in config, default I): 60 s, -3 to 5 s per entry, floor 3 s.
  Entering while weak pauses the weakness and adds the next one on top. Milk doesn't clear it.
  20 s shared cooldown between pills/bullets.
- Each form is a status effect (Dying Will, Hyper Dying Will, Ultimate Dying Will) with its icon and timer in the
  inventory and top-right; the effect's timer is the form's timer. No swirl particles.
- Release key (V) ends your current form early; the weakness still follows. Can be turned off in config.
- Hyper Dying Will: unlocks when Dying Will reaches its cap. Hotkey toggle. Drains 1%/s (0.4%/s at full mastery),
  ends with fatigue. Mastery: 10 tiers of 600 x tier seconds, earned only while active (10 s after spending
  flame or dealing/taking damage). Boost 2.0x + 0.05x per tier + rank bonus.
- Ultimate Dying Will: after mastery is full, an extra tier of the same length. Then a pill or bullet during Hyper
  enters Ultimate: 3 min, flame costs nothing, 5x boost, then 1 hour of Weakness II and the extra tier resets.
- A forehead flame in the active flame's colour shows on anyone in a form (other players see it too).

**Gems and resonator**
- Flame Gem Ore (stone and deepslate, iron pickaxe, Y -56 to 48, rarer than iron, more common than diamond).
- Basic Flame Resonator: unattuned gem surrounded by 8 iron ingots.
  - Right-click with a gem: insert it. Sneak + right-click empty-handed: take it out.
  - Right-click empty-handed: start/stop feeding it from your pool (10 FV/s while you stay within 5 blocks).
    The first flame fed attunes the gem. Attuned gems also collect 1 FV/s ambient.
  - A basic resonator stops at D-rank (49,999 FV). E-rank at 1,000 FV, D at 10,000.
- Gems are tinted in their flame's colour and show FV and next rank in the tooltip.

**Ring Forge**
- Crafted from 5 iron ingots around an E-rank gem over 3 smooth stone (the gem must be E-rank or better).
- 4 iron ingots + a ranked, attuned gem = a Flame Ring of that flame and grade. Add a chain for a Chained Flame Ring
  (Curios necklace slot). A finished ring + a chain = the same ring on a chain. Old Blank Rings from 0.1 count as 4 iron.
- Rings work in the off-hand, a Curios ring slot or (chained) the necklace slot. Highest grade wins.
- Effective rank = the lower of your rank and the ring's grade. Ring abilities need a ring of your active flame;
  passives work without one, at E.

**Sky's innate kit**
- Harmony (passive, E): while you're in a form, allies within 10 blocks get 25% of your boost
  (teammates and your tamed animals; with no teams, other teamless players).
- Flame Burst (Primary, ring, D): blast in front of you, 3% of capacity.
- Sky Shield (Secondary, ring, C): toggle, 5% + 1%/s, absorbs 2 hearts at C plus 2 per rank above.

**Storm: Disintegration**
- Passive: Storm damage ignores 10% of armor plus 5% per rank (35% at S), and only half of it can be absorbed by
  other flames' shields such as Sky Shield.
- Storm Bolt (Primary, D, 3%): a fast energy shot, range 16. Storm Burst (Secondary, C, 5%): destroys enemy projectiles
  within 3 blocks and throws enemies back.

**Rain: Tranquility**
- Passive: Rain hits add Tranquility stacks (-5% movement and attack speed each; cap 3 + your rank, so 8 at S; they fall
  off one per second after 3 s). At full stacks the target is Calmed for 3 s: its positive effects (Dying Will forms
  included), Hyper drain and toggle upkeep run twice as fast, and a calmed mob loses its target. With a Rain ring worn,
  you don't stay on fire.
- Calm Field (Primary, D, 5% + 1%/s, hold the key): radius 4; enemies gain a stack per second, enemy projectiles slow to
  half speed, fires go out. Flame Parry (Secondary, C, 4%): a half-second window that negates one melee hit and gives
  the attacker full stacks.

**Sun: Activation**
- Passive: an extra half-heart every 4 s at E down to 2 s at S; harmful effects wear off 25% faster plus 5% per rank.
- Activation Pulse (Primary, D, 4%): heals 2 hearts over 5 s (+1 per rank above D) and grows crops and saplings within 3
  blocks. Rally (Secondary, C, 6%): heals you and allies within 8 blocks 3 hearts (+1 per rank above C) and clears one
  harmful effect each (never the weakness a Dying Will form leaves).

**Lightning: Hardening**
- Passive: +1 armor toughness and 10% knockback resistance at E (+0.5 and 5% per rank); armor and shields wear 25%
  slower; Lightning hits chain to a nearby enemy for 40% (a second chain at C, a third at A); the pool refills twice as
  fast in thunderstorms.
- Harden (Primary, D, 4%): 5 s of 25% less damage and no knockback. Static Discharge (Secondary, C, 5%): a shock of
  radius 4 that damages and stuns for 1 s (stunned creatures can't move, jump or land blows).

**Cloud: Propagation**
- Passive: Cloud hits have a 10% chance (+5% per rank) to echo a moment later for half damage.
- Cloud Spread (Primary, D, 4%): your next projectile from any weapon (bow, crossbow, trident, snowball...) splits into
  three; the copies can't be picked up. Needle Burst (Secondary, C, 6%): 16 flame needles in every direction, each able
  to echo.

**Mist: Construction**
- Passive: mobs notice you from 15% closer (+5% per rank); Mist hits have a 10% chance (+5% per rank) to confuse
  (a mob turns on the nearest other creature; a player gets Nausea).
- Decoy (Primary, D, 5%): a copy of you, in your skin and gear, that mobs go after for 8 s. Phantom Terrain (Secondary,
  C, 5% + 0.5% per block): up to 8 solid illusion blocks for 10 s. Look at the ground for a wall (4 wide, 2 high);
  look ahead or out over a drop for a bridge from your feet.

Not yet: each flame's infusion effects, signatures and S-rank abilities (they need infused weapons or Gear), Storm-infused
tools mining faster, Cloud-infused tools' extra drops, and flame bullets.

## Flame infusion
**Two ways to infuse**
- Infuse toggle (G): lends your active flame to whatever is in your main hand (bare fists too) while a ring of that flame
  is worn. 70% strength, rank = your ring's effective rank, and each hit or shot costs 0.25% of capacity (it switches
  off when the pool runs dry).
- Infusion Anvil (crafted: 3 iron blocks over a D-rank gem over 3 iron ingots): a weapon, bow, crossbow, gun or shield
  plus a ranked, attuned gem = the item permanently infused with that flame at that rank (the gem is used up; infusing
  again replaces it). Armor takes only Lightning. Works while its flame is your active flame, at the lower of your rank
  and the gem's rank. Infused items glow and say what they carry.

**Weapon categories** (item tags `reborn_dying_will:infusion/<category>`, so packs can sort other mods' weapons; untagged
swords, axes, maces, tridents, bows, crossbows and shields are recognised anyway). An infused weapon's moves take the
Primary and Secondary keys ahead of the ring kit. Primary needs D and costs 3%; Secondary needs C and costs 5%.

| Category | Primary | Secondary |
| --- | --- | --- |
| Blade | Flame Slash: arc in front, radius 4 | Counter Stance: the next melee hit within 1 s is turned back at 1.5x |
| Heavy | Ground Slam: radius 3, knocks up | Overhead Strike: hold to charge (1.5 s), release for 1.5x to 3x |
| Polearm | Piercing Thrust: everything on a 6-block line | Sweeping Spin: radius 3.5 with knockback |
| Short | Strike Flurry: four quick hits | Dash Strike: dash 5 blocks, hitting what's in the way |
| Whip | Lash: pulls the target to you | Bind: roots the target (Cloud: spreads to a second enemy) |
| Bow | Flame Arrow: no ammunition | Spread Shot: five arrows |
| Gun | Flame Round: no ammunition | Burst Fire: three rounds |
| Shield | Brace: 2 s blocking frontal hits, sending projectiles back | Charging Bash (Lightning: chains to every enemy nearby) |
| Fists | Flame Punch | Charged Punch: hold to charge |

All move damage is flame damage, so each flame's property rides on it. Storm's shots fly 50% further and faster; Sun's
are small and quick, and Sun's fists hit 25% harder.

**Infused normal hits and shots**: normal hits deal +10% damage plus 5% per rank (toggle +5%) and carry the flame:
Storm melee leaves 3 s of armor-ignoring decay; Storm shots get +50% speed and one more pierce. Rain adds a Tranquility
stack (blades two). Sun heals you for 5% of the damage (fists 10%, with extra knockback); Sun shots are faster with an
irregular kick. Lightning hits chain; Lightning shields send blocked projectiles back; Lightning armor adds toughness.
Cloud hits can echo (short weapons twice as often); Cloud shots split in two at 60% damage after about 10 blocks.
Mist hits can confuse (polearms twice as often); Mist shots fly with a harmless illusory twin. Sky hits knock back
harder and restore 1% of capacity to allied players within 6 blocks.

Not yet: signatures (X-Burner, Disintegration Beam, Downpour, Maximum Cannon, Lightning Bulwark, Cloud Multiplication,
Illusion Realm, Hallucination), gloves, Storm/Cloud-infused tools' mining effects.

## Flame looks
- Every active form draws a flame on the forehead in the picked look. It stands just in front of the brow (clear of hair
  and helmets) and always turns to face the camera, so there is no seam or flat edge. Each tongue is shaded across its
  width with soft edges, a brighter core and a glow, so it reads as round. Dying Will is jagged and fast; Hyper is calmer
  and smoother; Ultimate is Hyper at 1.3x.
- Each flame throws its own motes (sparks, rising drops, chips, leaves, bubbles, sand...). Earth-set flames have upright
  bands of script circling the head; Earth's Y flame has two crossing bands. Night is a dark core with a violet rim.
- Flame shots are glowing bodies with a comet tail pointing back along their flight, plus a mote trail (Lightning's is a
  green crackle web, Sun's a ray burst, Earth's a Y).
- Client config "Flame looks": show on/off, particle density, size, raise, forward, script rings. The game's Particles
  setting also thins the motes. Players more than 64 blocks away are skipped.
- Shader pack mode (AUTO / ALWAYS / NEVER): with an Iris shader pack on, AUTO draws flames, shots and script bands with
  vanilla entity render types (the glow goes through the same program as spider eyes), which shader packs are written
  to handle. ALWAYS/NEVER force one path so you can compare per pack. "Shader pack exceptions" lists packs (any part
  of the file name) that AUTO keeps on the normal path; MakeUp is listed by default.

### 0.4.1 changes
- **Move animations** replace the plain arm swing, in third person (what others see) and first person (your view):
  blade slash sweeps diagonally from high on one side to low on the other with the crescent; punches jab straight
  out; bows use the bow-drawing pose then let go; shield brace raises the shield for the whole brace and the bash
  drives it forward with a lean; the blade counter holds the sword across the body; polearm thrust winds up overhead
  and drives forward and down; polearm spin turns the whole body; heavy slam goes overhead then down; charging holds
  the weapon overhead (heavy) or the fist drawn back, released into the strike; short-blade flurry alternates stabs,
  the dash leans forward; whip lash and bind wind up overhead and flick; guns aim with a recoil kick per shot.
  Left-handed players get mirrored moves. Short blades, whips and guns have no items yet, so those are untested.
- How: the server sends one tiny packet per move to the player and everyone tracking them; the pose is applied by an
  optional mixin on the humanoid model (if it fails to apply, moves just play without a pose), and the first-person
  hand is drawn by the mod during a move. One access-transformer line exposes the game's first-person arm drawing.

### 0.4.0 changes
- **Infusion glint:** infused items shimmer in their flame's colour only while the infusion is live for whoever holds
  them (their active flame matches, or the Infuse toggle lends it); dormant items show no glint (unless enchanted).
  Slots show a small flame mark, coloured when live and grey when dormant, and the tooltip says Live or Dormant.
  New infusions no longer force the vanilla enchantment glint. (Drawn through a small optional mixin on the item
  renderer; if it ever fails to apply, items simply keep vanilla glints.)
- **Flame effects:** a new effect packet draws, in each flame's colours and style: slash crescents (blade, polearm
  spin, short-blade flurry), thrust and dash streaks, slam rings (heavy, charged strikes, binds), whip tethers,
  shield barriers (brace, blade counter, shield bash, blocks), charge glow in the hand, muzzle flashes, and a hit mark on
  every flame hit (Sky star, Storm cross, Rain ripple, Sun rays, Lightning green bolts, Cloud echo rings, Mist swirl,
  Night collapse; Earth-set flames use their counterpart's mark). Lightning chains are green arcs. Weapon moves swing
  the arm. One small packet per effect, to players within 48 blocks; the client keeps at most 192 effects alive.
- **Infused shields on block:** Sky gives you and allies within 6 blocks Absorption I for 8 s; Storm wears a melee
  attacker's armor (2 durability per point blocked); Rain puts a Tranquility stack on them; Sun heals 30% of the
  blocked damage; Lightning still sends projectiles back; Cloud echoes 50% of the blocked damage onto the attacker;
  Mist has a 60% chance to daze them (confuse). All in `reborn_dying_will-kits.toml` under `shieldBlock`.
- Flame shots now use their size (Sun's are smaller).
- Script bands sit halfway between the brow and the flame's middle.
- Cleanup: removed the old dust-particle move visuals and their helpers, the white electric-spark chain arcs, the dust
  echo and witch confuse particles (all replaced by flame effects), and unused imports. No ids or items changed.

### 0.3.4 changes
- Shader pack exceptions list (default: MakeUp), since MakeUp looks worse on the shader-pack path.
- Script bands sit at the height of the flame's middle and are wider, clearing helmet corners; Earth's two bands cross
  at a shallower angle.
- The flame faces the viewer correctly in the inventory doll (GUI views are orthographic).

### 0.3.3 changes
- Shader pack mode added (fixes banded flames under Complementary Reimagined/Unbound; untested here, needs Iris).

### 0.3.2 changes
- Flame position: 0.08 lower than 0.3.1 (your tested offset, now the default) and pushed forward past helmets. The old
  "heightOffset" setting is replaced by "raise" and "forward" (both 0 by default; the old value is ignored).
- Head flames are camera-facing, softly shaded and glowing instead of two crossed flat planes.
- Script bands stand upright around the head instead of lying flat.
- Motes now spawn at the flame itself (including when looking up or down) and use smoother, higher-resolution sprites.
- Shots gained a glowing body and comet tail (0.3.1 shots could hide inside the body of whoever fired them).
- Dev only (not in the jar): `src/dev` holds a screenshot harness (`gradle runDevClient -Pdevshots`) used to check
  the looks in a real client before release.

## Keys (rebindable, "Reborn - Dying Will" category)
Z Primary, X Secondary, C Signature, H Hyper, V Release, Y choose flame, G Infuse, J toggle the ability panel.

## Display (client config: config/reborn_dying_will-client.toml, or Mods > Reborn - Dying Will > Config)
- Ability panel: the active flame's passive and abilities with their current keys; anything you can't use yet is greyed
  out with the reason (no ring, or the rank it needs). Show/hide, position (8 anchors), offsets, scale, background.
- Flame meter: position (8 anchors), offsets, horizontal or vertical, length, thickness, labels on/off.
- Mist decoys show the caster's visible Curios (any mod's), including cosmetic slots.

## Testing commands (op level 2)
```
/reborn info <player>
/reborn unlock|lock|active <player> <flame>
/reborn capacity set|add <player> <flame> <amount>
/reborn rank <player> <flame> <E..S>
/reborn pool fill <player>
/reborn mastery set <player> <seconds> | max <player> | ultimate <player>
/reborn entries set <player> <count>
/reborn scar clear <player>
/reborn weakness clear <player> | set <player> <seconds>
/reborn cooldowns clear <player>
/reborn form end <player>     (as if it ran out: weakness follows)
/reborn form clear <player>   (ends it with no weakness)
/reborn givegem <player> <flame> <rank>
```

## Other recipes
- Dying Will Pill (x4): Flame Gem + sugar + gunpowder.
- Dying Will Bullet (x2): pill + iron nugget + gunpowder.
- Dying Will Pistol: 3 iron ingots over 1 planks (left).

## Not in this version
Signatures, gloves, Box Workbench, level 2/3 resonators, canon weapons, chameleon,
boxes, Gear, Earth/Night unlock routes and kits, Vindice, combination flames, machines.
Placeholder textures throughout.

## Building
JDK 21, then `./gradlew build` -> `build/libs/reborn_dying_will-0.1.0.jar`.
`./gradlew runGameTestServer` runs the automated rule checks (not shipped in the jar).
