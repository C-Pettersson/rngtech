---
navigation:
  title: "Rules"
  parent: affix-sorter.md
  position: 0
  icon: rngtech:affix_sorter
---

# Rules

Open the **Rules** tab to write rules. A rule has a route, a set of conditions, and an on/off switch. An item matches a rule when it meets every condition that the rule sets. A condition that you do not set matches every item.

The sorter checks the rules from the top. The first enabled rule that matches decides the route. An item that matches no rule takes the default route. The default route is Reject. Click the default route under the list to change it.

| Condition | Matches |
|---|---|
| Items | Up to six items. Click **+** to add the item in the input slot. Click an item to remove it. |
| Tag | One item tag. Click to cycle through the tags of the rule's items. |
| Stage | A range of item stages. |
| Rarity | Any of the selected rarities. |
| Affixes | Up to six affixes, each at or above a tier. Set how many of them the item must have. |
| Quality | The item's Quality, the sum of its affix tiers, at or above a value. |
| RP | Refinement Potential at or above a value. |
| Roll % | A Unique's roll quality at or above a value. |
| Corrupted, Mirrored | Either state, only items in the state, or only items outside it. |

Put a Reject rule above a Pass rule to make an exception. For example, reject Corrupted items first, then pass Rare items.

Turn on **Pulse** to make a rule send a short redstone pulse when it decides an item. The pulse comes out of every face that is not a Pass or Reject face. Use it to stop your crafting line when you get the item you want.
