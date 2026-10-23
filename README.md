# Reborn - Dying Will (0.2.0)

NeoForge 21.1 / Minecraft 1.21.1. Requires Curios (9.x for 1.21.1).

Milestone 1 laid the foundation: the flame system, the Dying Will form ladder, Flame Gems, the basic resonator
and Sky's ring kit. Milestone 2 (0.2.0) adds the other six Sky-set flames' ring kits, the Ring Forge and flame pills.
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
- Flame pills (one per Sky-set flame): an awakened player at low health who eats one awakens that flame too
  (and enters Dying Will if they can). At higher health nothing happens and the pill isn't used.
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

## Keys (rebindable, "Reborn - Dying Will" category)
Z Primary, X Secondary, C Signature, H Hyper, V Release, Y choose flame.

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
Infusion and the Infusion Anvil, signatures, gloves, Box Workbench, level 2/3 resonators, canon weapons, chameleon,
boxes, Gear, Earth/Night unlock routes and kits, Vindice, combination flames, machines.
Placeholder textures throughout.

## Building
JDK 21, then `./gradlew build` -> `build/libs/reborn_dying_will-0.1.0.jar`.
`./gradlew runGameTestServer` runs the automated rule checks (not shipped in the jar).
