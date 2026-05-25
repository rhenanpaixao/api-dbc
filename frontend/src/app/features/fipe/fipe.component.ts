import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
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
    MatInputModule,
    MatAutocompleteModule,
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

  filteredBrands: Brand[] = [];
  filteredModels: VehicleModel[] = [];

  readonly brandDisplayCtrl = new FormControl<Brand | string | null>(null);
  readonly modelDisplayCtrl = new FormControl<VehicleModel | string | null>({ value: null, disabled: true });

  // Used by mat-autocomplete [displayWith] to show the name instead of [object Object].
  // Works for both Brand and VehicleModel since both share the { name: string } shape.
  readonly displayName = (value: { name: string } | string | null): string => {
    if (!value) return '';
    return typeof value === 'string' ? value : value.name;
  };

  readonly form = this.fb.nonNullable.group({
    vehicleType: ['' as VehicleType, Validators.required],
    brandId: ['', Validators.required],
    modelId: ['', Validators.required],
  });

  ngOnInit(): void {
    // Subscriptions to display controls: filter lists as user types
    this.brandDisplayCtrl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        if (typeof value !== 'string') return; // Brand object set by Angular Material on selection
        const filter = value.toLowerCase();
        this.filteredBrands = this.brands.filter(b => b.name.toLowerCase().includes(filter));
        if (value === '') {
          this.form.controls.brandId.setValue('');
        }
      });

    this.modelDisplayCtrl.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(value => {
        if (typeof value !== 'string') return;
        const filter = value.toLowerCase();
        this.filteredModels = this.models.filter(m => m.name.toLowerCase().includes(filter));
        if (value === '') {
          this.form.controls.modelId.setValue('', { emitEvent: false });
        }
      });

    this.form.controls.brandId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(brandId => {
        if (brandId) {
          this.form.patchValue({ modelId: '' }, { emitEvent: false });
          this.modelDisplayCtrl.setValue(null, { emitEvent: false });
          this.filteredModels = [];
          this.modelDisplayCtrl.enable();
          this.vehicleHistory = null;
          this.loadModels(brandId);
        } else {
          this.modelDisplayCtrl.disable();
          this.modelDisplayCtrl.setValue(null, { emitEvent: false });
          this.filteredModels = [];
        }
      });
  }

  onVehicleTypeChange(): void {
    this.resetState();
    if (this.form.controls.vehicleType.value) {
      this.loadBrands();
    }
  }

  private resetState(): void {
    this.form.patchValue({ brandId: '', modelId: '' }, { emitEvent: false });
    this.brandDisplayCtrl.setValue(null, { emitEvent: false });
    this.modelDisplayCtrl.setValue(null, { emitEvent: false });
    this.modelDisplayCtrl.disable();
    this.brands = [];
    this.filteredBrands = [];
    this.filteredModels = [];
    this.models = [];
    this.vehicleHistory = null;
  }

  onBrandFocus(): void {
    this.filteredBrands = [...this.brands];
  }

  onBrandSelected(event: MatAutocompleteSelectedEvent): void {
    const brand = event.option.value as Brand;
    // Angular Material sets FormControl to the Brand object via onChange(brand).
    // Override it back to a string so DefaultValueAccessor.writeValue doesn't call toString().
    this.brandDisplayCtrl.setValue(brand.name, { emitEvent: false });
    this.form.controls.brandId.setValue(brand.code);
  }

  onModelFocus(): void {
    this.filteredModels = [...this.models];
  }

  onModelSelected(event: MatAutocompleteSelectedEvent): void {
    const model = event.option.value as VehicleModel;
    this.modelDisplayCtrl.setValue(model.name, { emitEvent: false });
    this.form.controls.modelId.setValue(model.code);
  }

  private loadBrands(): void {
    const vehicleType = this.form.controls.vehicleType.value;
    this.loadingBrands = true;
    this.errorMessage = null;

    this.fipeService.getBrands(vehicleType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
      next: brands => {
        this.brands = brands;
        this.filteredBrands = [...brands];
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

    this.fipeService.getModels(brandId, vehicleType)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
      next: models => {
        this.models = models;
        this.filteredModels = [...models];
        this.loadingModels = false;
      },
      error: () => {
        this.errorMessage = 'Não foi possível carregar os modelos.';
        this.loadingModels = false;
      },
    });
  }

  onClear(): void {
    this.resetState();
    this.form.controls.vehicleType.setValue('' as VehicleType, { emitEvent: false });
    this.errorMessage = null;
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    const { vehicleType, brandId, modelId } = this.form.getRawValue();
    this.loadingHistory = true;
    this.vehicleHistory = null;
    this.errorMessage = null;

    this.fipeService
      .getVehicleHistory(brandId, modelId, vehicleType)
      .pipe(takeUntilDestroyed(this.destroyRef))
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
