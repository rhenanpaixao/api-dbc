import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Brand } from '../models/brand.model';
import { VehicleModel } from '../models/vehicle-model.model';
import { VehicleHistory } from '../models/vehicle-history.model';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class FipeService {

  private readonly baseUrl = environment.apiUrl;
  private readonly http = inject(HttpClient);

  getBrands(vehicleType = 'cars'): Observable<Brand[]> {
    const params = new HttpParams().set('vehicleType', vehicleType);
    return this.http.get<Brand[]>(`${this.baseUrl}/brands`, { params });
  }

  getModels(brandId: string, vehicleType = 'cars'): Observable<VehicleModel[]> {
    const params = new HttpParams().set('vehicleType', vehicleType);
    return this.http.get<VehicleModel[]>(`${this.baseUrl}/brands/${brandId}/models`, { params });
  }

  getVehicleHistory(
    brandId: string,
    modelId: string,
    vehicleType = 'cars'
  ): Observable<VehicleHistory> {
    const params = new HttpParams().set('vehicleType', vehicleType);
    return this.http.get<VehicleHistory>(
      `${this.baseUrl}/vehicles/${brandId}/${modelId}`,
      { params }
    );
  }
}
