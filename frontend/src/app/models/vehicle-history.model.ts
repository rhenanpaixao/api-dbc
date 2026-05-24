export interface YearlyPrice {
  year: number;
  price: number;
  priceFormatted: string;
  priceDifference: number | null;
  priceDifferenceFormatted: string | null;
  changePercentage: number | null;
  changePercentageFormatted: string | null;
  comparedToYear: number | null;
}

export interface VehicleHistory {
  brand: string;
  model: string;
  fuel: string;
  priceHistory: YearlyPrice[];
}
