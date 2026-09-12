# Rune Ruin

**Rune Ruin** is a WIP NeoForge mod for **Minecraft 26.2.0** (NeoForge `26.2.0.59`).

![](.github/assets/preview.png)

# World Structure

```
                                          + Y = 512
             /````\                       | 
 TOP LAYER   \_  _/  /````\        .__.   | - - - 217 blocks below the build limit
               ||    \_  _/        |  |   |
           ..****...___||         _|__| --|-- ~25 blocks high - dripstone/deep/dark
       _..`             ``''~~''``        |                          caves biomes
      `|| __                              | Y = 296
       || ##`''***~'```''```''***~'```' --|-- ~10 blocks high - blooming caves ceiling
       || ##      BLOOMING CAVES          |                                     biomes
       || ##         LAYER   /````\       | - - - 75 blocks high
       || ##   /``\          \_  _/       |
       || ##.****./.____       ||  _..* --|-- ~25 blocks high - blooming caves biomes
~~''``` ``##            ``''~~''``        |
                                          | Y = 215
```''~*~''```''***~'```''```''***~'```' --|-- ~10 blocks high - dripstone/deep/dark
        |     *| |       DEEP CAVES   |   |                       caves ceiling biomes
       *\        /         LAYER      /   |
         |*     |*                  *|    | - - - 75 blocks high
                      /                   |
   /\      ..****..._/ \           _..* --|-- ~25 blocks high - dripstone/deep/dark
~~''``` ```             ``''.    |       |                          caves biomes
                            ||    `|      | Y = 134
```''~*~''```''***~'```''```||     ``' --|-- ~10 blocks high - hot/ice/lost caves
 LOST CAVES    ||           ||            |                       ceiling biomes
   LAYER       ||     *     ||         *  |
        ___    ||     **    ||       ***  | - - - 75 blocks high
 / / / /0 0\   |**   **     ||**    **    |
 \ \ \ \___/.****.../\__   *|**    /\.* --|-- ~25 blocks high - hot/ice/lost caves
~~''``` ^^^             ``''~~''``        |                          biomes
                                          | Y = 53
```''~*~''```''***~'```''```''***~'```' --|-- ~10 blocks high - void ceiling biomes
                         <>               |
 <>    `         `                <>      | - - - 50 blocks high
     VOID LAYER          `                + Y = 0
```

## TODO

- [ ] **TOP** Layer
  - [ ] Add more biomes, such as a yellow one.
  - [X] Improve generation of the top layer so it isn't too flat.
  - [X] Improve pillar generation and put runes on them.
  - [X] Improve rune designs.
- [ ] **Blooming Caves** Layer
  - [ ] Add a `Hive` biome consisting of a large stone hanging from the top layer, and add other types of hanging stones.
  - [ ] Add a `Frog` boss and magical abilities.
  - [ ] Add a `Pirate Ship` boss, a related invasion, and its crew.
  - [X] Divide into two biomes: jungle and stone forest.
  - [X] Add another type of glowing flora to the dark parts of the Blooming Caves beneath the top layer.
- [ ] **Deep Caves** Layer
  - [ ] Add more spike types (mossy, stone, etc.).
  - [ ] Add more variety to buds on inverted trees.
    - [ ] Bird nests
    - [ ] Mini-lake?
    - [ ] Spider nests?
  - [ ] Add a magenta mushroom biome with giant worms.
  - [ ] Add a regular mushroom biome with gnomes.
  - [ ] Add a spider cave biome with a giant spider boss.
- [ ] **Lost Caves** Layer
  - [ ] Add an ice biome.
  - [ ] Add Giant Goblets that extend beyond the Lost Caves layer and contain their own ecosystems.
  - [X] Add a lava biome.
- [ ] **Void** Layer
  - [ ] Add stars made of stardust blocks. Stardust is a useful material that is difficult to mine above the abyss.
  - [ ] Maybe add a radioactive flesh biome.
- [ ] **Equipment**
  - [ ] Add a `Blowpipe` that shoots `mossberries` or `glowberries` and applies an effect to enemies.
  - [ ] Add a `ring` that doubles the damage all tools deal to poisoned enemies, so it can be used with the `blowpipe` or a `sword`.
  - [ ] Add a `Grappling Hook`.
  - [ ] Add other rock-climbing equipment.
