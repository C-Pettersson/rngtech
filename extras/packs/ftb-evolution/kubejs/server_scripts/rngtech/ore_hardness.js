// RNGTech in FTB Evolution: keep RNGTech ore hardness on RNGTech and vanilla ores.
// RNGTech's hardness tags also include the shared #c:ores/* tags. In Evolution those tags hold the
// FTB Materials, Mekanism, Modern Industrialization, Oritech, XyCraft, EnderIO, Occultism, and
// Create Propulsion ores, and every pickaxe that is not an RNGTech Modular tool reaches hardness 2,
// so those ores would break without drops.

const RNGTECH_ORE_HARDNESS = {
  1: ['minecraft:iron_ore', 'minecraft:deepslate_iron_ore', 'minecraft:copper_ore', 'minecraft:deepslate_copper_ore'],
  2: ['rngtech:tin_ore', 'rngtech:deepslate_tin_ore', 'rngtech:zinc_ore', 'rngtech:deepslate_zinc_ore',
    'minecraft:gold_ore', 'minecraft:deepslate_gold_ore', 'minecraft:nether_gold_ore'],
  3: ['rngtech:nickel_ore', 'rngtech:deepslate_nickel_ore', 'rngtech:lead_ore', 'rngtech:deepslate_lead_ore',
    'rngtech:silver_ore', 'rngtech:deepslate_silver_ore'],
  4: ['rngtech:aluminum_ore', 'rngtech:deepslate_aluminum_ore', 'rngtech:osmium_ore', 'rngtech:deepslate_osmium_ore'],
  5: ['rngtech:titanium_ore', 'rngtech:deepslate_titanium_ore'],
  6: ['rngtech:tungsten_ore', 'rngtech:deepslate_tungsten_ore', 'rngtech:platinum_ore', 'rngtech:deepslate_platinum_ore'],
  7: [],
  8: ['rngtech:naquadah_ore', 'rngtech:deepslate_naquadah_ore']
}

ServerEvents.tags('block', event => {
  Object.keys(RNGTECH_ORE_HARDNESS).forEach(level => {
    const tag = `rngtech:ore_hardness/level_${level}`
    event.removeAll(tag)
    RNGTECH_ORE_HARDNESS[level].forEach(block => event.add(tag, block))
  })
})
