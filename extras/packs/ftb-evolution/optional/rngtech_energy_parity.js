// RNGTech in FTB Evolution, optional: raise RNGTech's recipe-driven generators toward Evolution's FE scale.
// Copy into kubejs/server_scripts/rngtech/ to enable. Processing time is unchanged, so FE/t scales with FE per fuel.
// At default stats this gives about 360 FE/t from Syngas, 1,200 FE/t from an Ammonia Fuel Cell, and 3,000 FE/t
// from Vacuum Collapse, the range of Powah's Hardened, Blazing, and Niotic Furnators that cost far less to build.
// The Solid Fuel Burner and Solar Panels have no recipe energy and cannot be changed from a script.

const RNGTECH_GENERATOR_ENERGY = {
  'rngtech:gas_combustion': 1.5,
  'rngtech:ammonia_power_cycle': 2.0,
  'rngtech:vacuum_collapse': 3.0
}

ServerEvents.recipes(event => {
  var rescaled = []
  Object.keys(RNGTECH_GENERATOR_ENERGY).forEach(type => {
    event.forEachRecipe({ type: type }, recipe => {
      var json = JSON.parse(String(recipe.json))
      json.energy = Math.round(json.energy * RNGTECH_GENERATOR_ENERGY[type])
      rescaled.push({ id: String(recipe.getId()), json: json })
    })
  })
  rescaled.forEach(entry => event.custom(entry.json).id(entry.id))
  console.info(`RNGTech: rescaled energy on ${rescaled.length} generator recipes`)
})
