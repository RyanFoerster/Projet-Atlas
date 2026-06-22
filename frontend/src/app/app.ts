import { Component, inject, signal, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

type ApiStatus = 'loading' | 'UP' | 'DOWN' | 'unreachable';

@Component({
  selector: 'app-root',
  imports: [],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App implements OnInit {
  private readonly http = inject(HttpClient);

  // En dev, le backend Spring Boot tourne sur 8080 (le frontend sur 4200).
  // L'appel cross-origin valide la configuration CORS côté Spring (cf. application.yml).
  private static readonly HEALTH_URL = 'http://localhost:8080/actuator/health';

  protected readonly apiStatus = signal<ApiStatus>('loading');

  ngOnInit(): void {
    this.http.get<{ status: string }>(App.HEALTH_URL).subscribe({
      next: (res) => this.apiStatus.set(res.status === 'UP' ? 'UP' : 'DOWN'),
      error: () => this.apiStatus.set('unreachable'),
    });
  }
}
