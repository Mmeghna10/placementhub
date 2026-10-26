import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { ADMIN, apiError, renderPage } from '../../test/render';
import { getPlacementPolicy, updatePlacementPolicy } from '../../api/adminApi';
import PlacementRules from './PlacementRules';

vi.mock('../../api/adminApi');

const open = () => renderPage(<PlacementRules />, { route: '/admin/placement-rules', user: ADMIN });

describe('Placement rules page', () => {
  it('loads the rules and saves a new maximum', async () => {
    getPlacementPolicy.mockResolvedValue({ data: { allowUpgrades: true, maxAcceptedOffers: 2 } });
    updatePlacementPolicy.mockResolvedValue({ data: { allowUpgrades: true, maxAcceptedOffers: 3 } });
    open();

    const max = await screen.findByLabelText('Maximum offers a student may accept');
    expect(max).toHaveValue(2);
    const user = userEvent.setup();
    await user.clear(max);
    await user.type(max, '3');
    await user.click(screen.getByRole('button', { name: /save rules/i }));

    expect(updatePlacementPolicy).toHaveBeenCalledWith({ allowUpgrades: true, maxAcceptedOffers: 3 });
    expect(await screen.findByRole('status')).toHaveTextContent(/saved/i);
  });

  it('switching upgrades off disables the maximum ("one student, one job")', async () => {
    getPlacementPolicy.mockResolvedValue({ data: { allowUpgrades: true, maxAcceptedOffers: 2 } });
    updatePlacementPolicy.mockResolvedValue({ data: { allowUpgrades: false, maxAcceptedOffers: 2 } });
    open();

    const user = userEvent.setup();
    await user.click(await screen.findByRole('checkbox', { name: /allow upgrades/i }));
    expect(screen.getByLabelText('Maximum offers a student may accept')).toBeDisabled();
    await user.click(screen.getByRole('button', { name: /save rules/i }));

    expect(updatePlacementPolicy).toHaveBeenCalledWith({ allowUpgrades: false, maxAcceptedOffers: 2 });
  });

  it("shows the server's validation message", async () => {
    getPlacementPolicy.mockResolvedValue({ data: { allowUpgrades: true, maxAcceptedOffers: 2 } });
    updatePlacementPolicy.mockRejectedValue(apiError(400, { code: 'VALIDATION_FAILED', error: 'At most 5 accepted offers' }));
    open();

    await userEvent.setup().click(await screen.findByRole('button', { name: /save rules/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent('At most 5 accepted offers');
  });
});
