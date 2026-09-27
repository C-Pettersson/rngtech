# Balance Mode

Status: Implemented

Balance Mode is a Battery Chassis prefix that enables `CHARGE_BALANCER` behavior.

## Effect

When the chassis receives FE, it spreads the stored energy across eligible installed Battery Cells instead of filling one slot first. When the chassis outputs FE or loses stored FE, it draws from eligible installed cells instead of emptying one slot first.

The behavior respects each cell's remaining capacity, stored FE, input rate, and output rate. If one cell cannot accept or provide its share, the remainder is redistributed across the other eligible cells.

Balance Mode changes routing behavior only. It does not add capacity, transfer rate, burst output, or efficiency.

## Eligibility

| Profile | Eligible |
|---|---|
| Battery Chassis | Yes |
| Battery Cell | No |
| Processing machine block | No |
| Machine part | No |

## Slot

| Slot | Tier | Notes |
|---|---:|---|
| Prefix | Single-tier | Rolls as the `Balanced` prefix and occupies a normal prefix slot. |

## Related Pages

- [Battery Chassis](../content/battery-chassis.md)
- [Affix Generation](../systems/affix-generation.md)
- [Modifier Eligibility](../reference/modifier-eligibility.md)
