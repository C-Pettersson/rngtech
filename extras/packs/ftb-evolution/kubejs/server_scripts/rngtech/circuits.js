// RNGTech in FTB Evolution: RNGTech recipes take RNGTech circuits only.
// RNGTech asks for #c:circuits/<tier>, which in Evolution also holds Mekanism's control circuits.
// A Metallurgic Infuser makes those from osmium, redstone, diamond, and refined obsidian, so the
// Stage 6-8 materials in RNGTech's Elite and Ultimate Electric Circuits would stop gating anything.
// Other mods keep accepting RNGTech circuits through the shared tags.
// KubeJS has no schema for RNGTech's recipe types, so replaceInput misses most of them. This script
// rewrites the recipe JSON instead and registers it again under the same id.

const RNGTECH_CIRCUITS = {
  basic: 'rngtech:basic_electric_circuit',
  advanced: 'rngtech:advanced_electric_circuit',
  elite: 'rngtech:elite_electric_circuit',
  ultimate: 'rngtech:ultimate_electric_circuit'
}

function rngtechOwnCircuits(text) {
  Object.keys(RNGTECH_CIRCUITS).forEach(tier => {
    text = text.split(`"tag":"c:circuits/${tier}"`).join(`"item":"${RNGTECH_CIRCUITS[tier]}"`)
  })
  return text
}

ServerEvents.recipes(event => {
  var rewritten = []
  event.forEachRecipe({ mod: 'rngtech' }, recipe => {
    var text = String(recipe.json)
    if (text.indexOf('"tag":"c:circuits/') >= 0) {
      rewritten.push({ id: String(recipe.getId()), json: rngtechOwnCircuits(text) })
    }
  })
  rewritten.forEach(entry => event.custom(JsonUtils.fromString(entry.json)).id(entry.id))
  console.info(`RNGTech: ${rewritten.length} recipes now take RNGTech circuits only`)
})
