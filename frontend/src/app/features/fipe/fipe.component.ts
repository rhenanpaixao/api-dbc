import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';

import { FipeService } from '../../services/fipe.service';
import { Brand } from '../../models/brand.model';
import { VehicleModel } from '../../models/vehicle-model.model';
import { VehicleHistory, YearlyPrice } from '../../models/vehicle-history.model';

type VehicleType = 'cars' | 'motorcycles' | 'trucks';

interface VehicleTypeOption {
  value: VehicleType;
  label: string;
}

@Component({
  selector: 'app-fipe',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatCardModule,
    MatIconModule,
    MatDividerModule,
  ],
  templateUrl: './fipe.component.html',
  styleUrl: './fipe.component.scss',
})
export class FipeComponent implements OnInit {
  private readonly fipeService = inject(FipeService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  readonly vehicleTypes: VehicleTypeOption[] = [
    { value: 'cars', label: 'Carros' },
    { value: 'motorcycles', label: 'Motos' },
    { value: 'trucks', label: 'Caminhões' },
  ];

  brands: Brand[] = [];
  models: VehicleModel[] = [];
  vehicleHistory: VehicleHistory | null = null;

  loadingBrands = false;
  loadingModels = false;
  loadingHistory = false;
  errorMessage: string | null = null;

  readonly displayedColumns = ['year', 'price', 'difference', 'percentage', 'comparedTo'];

  readonly form = this.fb.nonNullable.group({
    vehicleType: ['cars' as VehicleType, Validators.required],
    brandId: ['', Validators.required],
    modelId: ['', Validators.required],
  });

  ngOnInit(): void {
    this.form.controls.modelId.disable();
    this.loadBrands();

    this.form.controls.vehicleType.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.form.patchValue({ brandId: '', modelId: '' });
        this.form.controls.modelId.disable();
        this.models = [];
        this.vehicleHistory = null;
        this.loadBrands();
      });

    this.form.controls.brandId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(brandId => {
        if (brandId) {
          this.form.patchValue({ modelId: '' });
          this.form.controls.modelId.enable();
          this.vehicleHistory = null;
          this.loadModels(brandId);
        } else {
          this.form.controls.modelId.disable();
        }
      });
  }

  private loadBrands(): void {
    const vehicleType = this.form.controls.vehicleType.value;
    this.loadingBrands = true;
    this.errorMessage = null;

    this.fipeService.getBrands(vehicleType).subscribe({
      next: brands => {
        this.brands = brands;
        this.loadingBrands = false;
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar as marcas. Verifique se o backend está rodando.';
        this.loadingBrands = false;
      },
    });
  }

  private loadModels(brandId: string): void {
    const vehicleType = this.form.controls.vehicleType.value;
    this.loadingModels = true;
    this.errorMessage = null;

    this.fipeService.getModels(brandId, vehicleType).subscribe({
      next: models => {
        this.models = models;
        this.loadingModels = false;
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar os modelos.';
        this.loadingModels = false;
      },
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    const { vehicleType, brandId, modelId } = this.form.getRawValue();
    this.loadingHistory = true;
    this.vehicleHistory = null;
    this.errorMessage = null;

    this.fipeService
      .getVehicleHistory(brandId, modelId, vehicleType)
      .subscribe({
        next: history => {
          this.vehicleHistory = history;
          this.loadingHistory = false;
        },
        error: err => {
          this.errorMessage =
            err.error?.message ?? 'Erro ao buscar o histórico de preços.';
          this.loadingHistory = false;
        },
      });
  }

  variationClass(row: YearlyPrice): string {
    if (row.changePercentage === null) return '';
    return row.changePercentage >= 0 ? 'positive' : 'negative';
  }
}
