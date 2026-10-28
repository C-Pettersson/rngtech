// RNGTech in Techopolis 3: keep other mods' recipes as they were before RNGTech.
// Immersive Engineering ships Tungsten and Titanium recipes that stay empty until a mod fills
// #c:ores/tungsten and #c:ores/titanium. With RNGTech they turn on: the Engineer's Hammer crushes
// Tungsten Ore by hand, the Arc Furnace skips RNGTech's Stage 7 smelting, and the Excavator digs both
// ores with no ore hardness. This script removes those recipes and the two Excavator entries.

const IE_TUNGSTEN_RECIPES = [
  'immersiveengineering:crafting/hammercrushing_tungsten',
  'immersiveengineering:crafting/raw_hammercrushing_tungsten',
  'immersiveengineering:arcfurnace/ore_tungsten',
  'immersiveengineering:arcfurnace/raw_ore_tungsten',
  'immersiveengineering:arcfurnace/dust_tungsten'
]
const IE_MINERALS = {
  'immersiveengineering:mineral/laterite': 'c:ores/titanium',
  'immersiveengineering:mineral/wolframite': 'c:ores/tungsten'
}

ServerEvents.recipes(event => {
  IE_TUNGSTEN_RECIPES.forEach(id => event.remove({ id: id }))
  Object.keys(IE_MINERALS).forEach(id => {
    event.forEachRecipe({ id: id }, recipe => {
      var json = JSON.parse(String(recipe.json))
      json.ores = json.ores.filter(ore => ore.output.tag != IE_MINERALS[id])
      event.custom(json).id(id)
    })
  })
  console.info('RNGTech: removed Immersive Engineering Tungsten and Titanium recipes')
})
