# Reborn - Dying Will (0.1.0, first milestone)

NeoForge 21.1 / Minecraft 1.21.1. Requires Curios (9.x for 1.21.1).

This milestone is the foundation: the flame system, the Dying Will form ladder, Flame Gems, the basic resonator,
and the Sky Flame's ring kit. Every number is a placeholder in `serverconfig/reborn_dying_will-server.toml`.

## What's in it

**Flames and capacity**
- All fifteen flames exist in the data model (Sky set, Earth set, Night). Only Sky has abilities so far; the others
  already have capacity, ranks, Dying Will states and the HUD.
- Capacity per flame in FV, ranks E to S by the capacity bands, growth by spending flame
  (4% of FV spent times the rank factor, x1.5 below 30% health or in a Dying Will form).
- Pool regenerates 0.5%/s, 1.5%/s after standing still for 3 seconds.
- Death scars every unlocked flame by 3% (cap 15%); scar heals as you spend flame (5 FV per 1 FV of scar).
- Switching flames: 30 s cooldown, empties the pool, not allowed during a form.

**Awakening**
- Near death: lethal damage to an unawakened player leaves them at half a heart in Dying Will and reveals
  their natural flame (fixed by UUID). Not for /kill or the void.
- Dying Will Pill: the first one only works at low health. After that, any health.
- Dying Will Pistol + Bullets: a hit player enters Dying Will, awakening them at any health (PvP behind a config option).
  A hit mob gets a frenzy.

**Forms**
- Dying Will: 30 s, +5 to 10 s per entry, capped at 3 min; boost 1.5x plus 0.05x per rank above E
  (attack, speed, mining speed, knockback resistance); entering restores 10% of the pool.
  Ends in weakness (-30% attack, -20% speed): 60 s, -3 to 5 s per entry, floor 3 s. Entering while weak
  pauses the weakness and adds the next one on top. 20 s shared cooldown between pills/bullets.
- Hyper Dying Will: unlocks when Dying Will reaches its cap. Hotkey toggle. Drains 1%/s (0.4%/s at full mastery),
  ends with fatigue. Mastery: 10 tiers of 600 x tier seconds, earned only while active (10 s after spending
  flame or dealing/taking damage). Boost 2.0x + 0.05x per tier + rank bonus.
- Ultimate Dying Will: after mastery is full, an extra tier of the same length. Then a pill or bullet during Hyper
  enters Ultimate: 3 min, flame costs nothing, 5x boost, then 1 hour of weakness and the extra tier resets.
- A forehead flame in the active flame's colour shows on anyone in a form (other players see it too).

**Gems and resonator**
- Flame Gem Ore (stone and deepslate, iron pickaxe, Y -56 to 48, rarer than iron, more common than diamond).
- Basic Flame Resonator: unattuned gem surrounded by 8 iron ingots.
  - Right-click with a gem: insert it. Sneak + right-click empty-handed: take it out.
  - Right-click empty-handed: start/stop feeding it from your pool (10 FV/s while you stay within 5 blocks).
    The first flame fed attunes the gem. Attuned gems also collect 1 FV/s ambient.
  - A basic resonator stops at D-rank (49,999 FV). E-rank at 1,000 FV, D at 10,000.
- Gems are tinted in their flame's colour and show FV and next rank in the tooltip.

**Rings (temporary crafting)**
The Ring Forge isn't built yet, so for now:
- Blank Ring: 4 iron nuggets in a diamond.
- Blank Ring + a ranked, attuned gem (crafting grid) = a Flame Ring of that flame and grade.
- Flame Ring + chain = Chained Flame Ring for the Curios necklace slot.
- Rings work in the off-hand, a Curios ring slot or (chained) the necklace slot. Highest grade wins.
- Effective rank = the lower of your rank and the ring's grade.

**Sky's innate kit**
- Harmony (passive, E): while you're in a form, allies within 10 blocks get 25% of your boost
  (teammates and your tamed animals; with no teams, other teamless players).
- Flame Burst (Primary, ring, D): blast in front of you, 3% of capacity.
- Sky Shield (Secondary, ring, C): toggle, 5% + 1%/s, absorbs 2 hearts at C plus 2 per rank above.

## Keys (rebindable, "Reborn - Dying Will" category)
Z Primary, X Secondary, C Signature, H Hyper, Y switch flame.

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
/reborn givegem <player> <flame> <rank>
```

## Recipes
- Dying Will Pill (x4): Flame Gem + sugar + gunpowder.
- Dying Will Bullet (x2): pill + iron nugget + gunpowder.
- Dying Will Pistol: 3 iron ingots over 1 planks (left).

## Not in this milestone
Infusion, the other flames' kits, gloves, Ring Forge, Box Workbench, Infusion Anvil, level 2/3 resonators,
canon weapons, chameleon, boxes, Gear, Earth/Night unlock routes, Vindice, combination flames, machines.
Placeholder textures throughout.

## Building
JDK 21, then `./gradlew build` -> `build/libs/reborn_dying_will-0.1.0.jar`.
`./gradlew runGameTestServer` runs the automated rule checks (not shipped in the jar).
