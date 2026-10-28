// RNGTech in Techopolis 3: ore blocks stay a Techopolis resource.
// Techopolis makes every ore drop itself and removes all smelting and crushing of ore blocks. An ore is
// worth little on its own: 32 go into a Dimensional Infuser for one Dimensional Ore. RNGTech's Crusher
// turns one ore block into 3 Crushed metal, and its Furnace smelts ore blocks, Diamond and Emerald ore
// included, from Stage 0. This script removes those recipes for every ore except the three only RNGTech
// places. RNGTech still crushes raw metal, which Techopolis makes from Strainer ore pieces.

const RNGTECH_ONLY_ORES = ['titanium', 'tungsten', 'naquadah']
const ORE_BLOCK_TYPES = ['rngtech:crusher', 'rngtech:furnace']

function takesPackOre(text) {
  const tags = text.match(/"tag":"c:ores\/[a-z_]+"/g) || []
  return tags.some(tag => RNGTECH_ONLY_ORES.indexOf(tag.slice(14, -1)) < 0)
}

ServerEvents.recipes(event => {
  var removed = []
  ORE_BLOCK_TYPES.forEach(type => {
    event.forEachRecipe({ type: type }, recipe => {
      if (takesPackOre(String(recipe.json))) removed.push(String(recipe.getId()))
    })
  })
  removed.forEach(id => event.remove({ id: id }))
  console.info(`RNGTech: removed ${removed.length} recipes that process ore blocks`)
})
