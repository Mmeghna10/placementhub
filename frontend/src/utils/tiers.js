// Placement tiers, lowest first, as the API names them.
export const TIERS = [
  { value: 'REGULAR', label: 'Regular' },
  { value: 'DREAM', label: 'Dream' },
  { value: 'SUPER_DREAM', label: 'Super-dream' },
];

export const tierLabel = (tier) => TIERS.find((t) => t.value === tier)?.label ?? tier;
