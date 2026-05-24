import { Component } from '@angular/core';
import { FipeComponent } from './features/fipe/fipe.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FipeComponent],
  template: `
    <header class="app-header">
      <h1>Tabela FIPE — Histórico de Preços</h1>
    </header>
    <main class="app-content">
      <app-fipe></app-fipe>
    </main>
  `,
  styles: [`
    .app-header {
      background-color: #1565c0;
      color: white;
      padding: 16px 32px;
      box-shadow: 0 2px 4px rgba(0,0,0,.3);

      h1 {
        margin: 0;
        font-size: 1.4rem;
        font-weight: 500;
        letter-spacing: .5px;
      }
    }

    .app-content {
      max-width: 1200px;
      margin: 32px auto;
      padding: 0 24px;
    }
  `],
})
export class AppComponent {}

