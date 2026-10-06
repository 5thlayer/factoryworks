# Mutation system

The idea spawned from a reading of Factorio's quality mechanics. I started sketching how quality works with this notation:

- A -> A+ -> A++ ...
(with A being the base item and A+ being N+1 quality tier)

Then I did the same with Factorio's recycling mechanics which uses this formula:

0.25 * i/o + r

> [...] where i is the number of items used as ingredients, o is the number of items returned by the recipe, and r is a random number that is greater than or equal to 0 but less than 1).
> On average, this returns exactly 25% of the items needed to craft one item of the same type as the recycled item. For example, recycling a processing unit always gives 5 electronic circuits while having a 50% chance of returning one advanced circuit.

We can use a similar notation as the quality:

- A -> B + C (in a recycling machine)
(where B and C quantities depend on the formula above, they can be 0)

Doing the same with Factorio's spoilage mechanics:

- A -> B (with time passed, or in a machine to accelerate the process)

I then started searching for a 4th mechanics combining the 3 other mechanics:

- A -> X (with exposure to specific environmental conditions or in a dedicated machine)

I call this the mutation system which is more similar to alchemy than real science but involves exposure to radiation or a nez fictional energy.

I initially envisioned a system where uranium enrichment would be used as a means to store gamma rays or other ionizing rays to mutate items. But then I decided to use a more SF angle and created a fictional *void energy*.

Void energy would act similarly to *UU matter* in other tech mods, in which you convert energy to matter with different costs related to the complexity of the final product.

In my vision, the space between MC blocks is a source of potential energy which can be harvested by placing block. To avoid creating an infinite loop, placing block like this is a permanent action: placing a block of iron produces a certain amount of void energy and turns iron into either bedrock or a custom indestructible block.

Specific blocks (mostly storage blocks from ingots/dusts, but customizable with a tag system) have a density value used to determine the void energy potential. Tentative list:

1. air 0
2. dirt (and variants), sand, gravel 1
3. stone variants 2
4. resource storage blocks (metal, redstone, etc.) 3
5. obsidian 4
6. diamond block 5
7. netherite 6

Void energy use is the mod end game mechanics, but its production can be started early in the mod progression. It acts like a resource sink and a completionist goal and allows powering specific end game tier equipment (void armors, void tools|weapons) and machines (void harvesting requires void energy itself). It is also used to feed the mutation system.

The mutation mechanics works similarly to recycling. The idea is that each item is part of either a tiered family (iron, copper, gold, etc., lapis, emerald, diamond, netherite) and/or a crafting recipe (jukebox -> diamond). Processing an item in a mutation chamber (temp name) has a chance to produce a higher tier item in the family or a desirable ingredient (diamond chestplate -> diamonds). 