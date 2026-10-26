import { screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ADMIN, renderPage } from '../test/render';
import { getPlacementOverview } from '../api/adminApi';
import PlacementOverview from './PlacementOverview';

vi.mock('../api/adminApi');

describe('Placement overview', () => {
  it('shows the placement rate, tiers, outcomes and top companies', async () => {
    getPlacementOverview.mockResolvedValue({
      data: {
        students: 40, placedStudents: 30, placementRate: 0.75,
        placedByTier: { REGULAR: 20, DREAM: 8, SUPER_DREAM: 2 },
        offers: { OFFERED: 3, ACCEPTED: 30, DECLINED: 4, EXPIRED: 1, RELEASED: 2, WITHDRAWN: 5 },
        acceptanceRate: 0.86, avgHoursToDecide: 19.5, openings: 45, filled: 30, autoRefills: 6,
        topCompanies: [{ company: 'Northwind Labs', hires: 9 }],
      },
    });
    renderPage(<PlacementOverview />, { route: '/admin/dashboard', user: ADMIN });

    const section = await screen.findByRole('region', { name: 'Placement overview' });
    expect(within(section).getByText('75%')).toBeInTheDocument();
    expect(within(section).getByText('30 of 40 students placed')).toBeInTheDocument();
    expect(within(section).getByText('19.5 h')).toBeInTheDocument();
    expect(within(section).getByText('Super-dream')).toBeInTheDocument();
    expect(within(section).getByText('Released (upgraded)').closest('li')).toHaveTextContent('2');
    expect(within(section).getByText('Northwind Labs').closest('li')).toHaveTextContent('9');
  });
});
