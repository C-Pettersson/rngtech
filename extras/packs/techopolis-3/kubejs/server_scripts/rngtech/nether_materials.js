// RNGTech in Techopolis 3: RNGTech Stages 4-6 without the Nether.
// Techopolis opens the Nether only after Ultimate Technium, so Blaze Rods and Blaze Powder come late.
// RNGTech's Steel, Aluminum, Sparksteel, and Titanium Heat Cores need Blaze Rods, so its fuel and gas
// power would wait for the Nether. Those Heat Cores now also take RNGTech's Ignition Catalyst, which the
// Bronze Heat Core already accepts. The Ammonia and Reforming Catalyst Beds also take Iron or Nickel Dust,
// the metals those catalysts use in real plants. The Exotic Heat Core keeps its Blaze Rods for Stage 8.
// KubeJS has no schema for RNGTech's recipe types, so this rewrites the recipe JSON and registers it again
// under the same id.

const BLAZE_ROD = '{"item":"minecraft:blaze_rod"}'
const BLAZE_POWDER = '{"item":"minecraft:blaze_powder"}'
const HEAT_CORES = ['steel', 'aluminum', 'sparksteel', 'titanium'].map(metal => `rngtech:${metal}_heat_core`)
const CATALYST_BEDS = {
  'rngtech:ammonia_catalyst_bed': 'c:dusts/iron',
  'rngtech:reforming_catalyst_bed': 'c:dusts/nickel'
}

ServerEvents.recipes(event => {
  var rewritten = []
  HEAT_CORES.forEach(id => {
    event.forEachRecipe({ id: id }, recipe => {
      var text = String(recipe.json).split(BLAZE_ROD).join(`[${BLAZE_ROD},{"item":"rngtech:ignition_catalyst"}]`)
      rewritten.push({ id: id, json: text })
    })
  })
  Object.keys(CATALYST_BEDS).forEach(id => {
    event.forEachRecipe({ id: id }, recipe => {
      var text = String(recipe.json).split(BLAZE_POWDER).join(`[${BLAZE_POWDER},{"tag":"${CATALYST_BEDS[id]}"}]`)
      rewritten.push({ id: id, json: text })
    })
  })
  rewritten.forEach(entry => event.custom(JsonUtils.fromString(entry.json)).id(entry.id))
  console.info(`RNGTech: ${rewritten.length} recipes no longer need the Nether`)
})
