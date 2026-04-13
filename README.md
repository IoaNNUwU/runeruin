# Rune Ruin

**Rune Ruin** is WIP mod for **Minecraft 1.26.1**

![](.github/assets/preview.png)

# World Structure

```
                                          + Y = 512
             /````\                       | 
 TOP LAYER   \_  _/  /````\        .__.   | - - - 217 blocks below build limit
               ||    \_  _/        |  |   |
           ..****...___||         _|__| --|-- ~25 blocks of height - dripstone/deep/dark
        __`             ``''~~''``        |                          caves biomes
       /############################### --+ ↓ Y = 293  ↑ Y = 299 (5 blocks of height)
       || ##`''***~'```''```''***~'```' --|-- ~10 blocks height - blooming caves ceiling
       || ##      BLOOMING CAVES          |                                     biomes
       || ##         LAYER   /````\       | - - - 75 blocks of height
       || ##   /``\          \_  _/       |
       || ##.****./.____       ||  _..* --|-- ~25 blocks of height - blooming caves biomes
~~''``` ``##            ``''~~''``        |
####################################### --+ ↓ Y = 212  ↑ Y = 218
```''~*~''```''***~'```''```''***~'```' --|-- ~10 blocks height - dripstone/deep/dark
        |     *| |       DEEP CAVES   |   |                       caves ceiling biomes
       *\        /         LAYER      /   |
         |*     |*                  *|    | - - - 75 blocks of height
                      /                   |
   /\      ..****..._/ \           _..* --|-- ~25 blocks of height - dripstone/deep/dark
~~''``` ```             ``''~~''``        |                          caves biomes
####################################### --+ ↓ Y = 131  ↑ Y = 137
```''~*~''```''***~'```''```''***~'```' --|-- ~10 blocks height - hot/ice/lost caves
 LOST CAVES    ||           ||            |                       ceiling biomes
   LAYER       ||     *     ||         *  |
        ___    ||     **    ||       ***  | - - - 75 blocks of height
 / / / /0 0\   |**   **     ||**    **    |
 \ \ \ \___/.****.../\__   *|**    /\.* --|-- ~25 blocks of height - hot/ice/lost caves
~~''``` ^^^             ``''~~''``        |                          biomes
####################################### --+ ↓ Y = 50  ↑ Y = 56
```''~*~''```''***~'```''```''***~'```' --|-- ~10 blocks height - void ceiling biomes
                         <>               |
 <>    `         `                <>      | - - - 50 blocks of height
     VOID LAYER          `                + Y = 0
```

### TODO:

- [ ] **TOP** Layer
  - [ ] Add diamond-like block and change pillars to not have diamonds.
  - [ ] Add more biomes. Like yellow one.
  - [X] Improve generation of the top layer so it isn't too flat.
  - [X] Improve pillars generation and put runes on them.
- [ ] **Blooming Caves** Layer
  - [ ] Divide into 2 biomes: jungle and stone forest.
  - [ ] Make boulders generate only under the sun.
  - [ ] Add another type of glowing flora to glow in blooming caves under the top layer (where dark)
  - [ ] Add `Frog` boss and magic abilities
  - [ ] Add `Pirates Ship` boss/invasion and crew
- [ ] **Deep Caves** Layer
  - [ ] Add magenta mushrooms biome with big worms
- [ ] **Lost Caves** Layer
  - [ ] Add Lava biome
  - [ ] Add Ice biome
- [ ] **Void** layer
  - [ ] Add stars as small blocks of stardust - a useful material hard to mine above the abyss.
  - [ ] Add radioactive flesh biome maybe
- [ ] **Equipment**
  - [ ] Add `Grappling hook`
  - [ ] Add other rock climbing equipment.