import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TarefaResponse } from '../models/tarefa.model';

@Injectable({ providedIn: 'root' })
export class TarefaService {
  private readonly baseUrl = 'http://localhost:8080/api/tarefas';

  constructor(private http: HttpClient) {}

  criar(urlRepositorio: string): Observable<TarefaResponse> {
    return this.http.post<TarefaResponse>(this.baseUrl, { urlRepositorio });
  }

  /**
   * Assina o stream SSE da tarefa e emite um evento a cada mudança
   * de status. Usa a API nativa EventSource do navegador — não passa
   * pelo HttpClient porque SSE é uma conexão persistente de texto,
   * não uma requisição HTTP normal.
   */
  acompanharEventos(codigo: string): Observable<{ status: string; data: string }> {
    return new Observable(observer => {
      const source = new EventSource(`${this.baseUrl}/${codigo}/events`);
      const statuses = ['BAIXANDO', 'EXTRAINDO', 'GERANDO', 'CONCLUIDO', 'FALHOU'];

      statuses.forEach(status => {
        source.addEventListener(status, (e: MessageEvent) => {
          observer.next({ status, data: e.data });
          if (status === 'CONCLUIDO' || status === 'FALHOU') {
            source.close();
            observer.complete();
          }
        });
      });

      source.onerror = () => {
        source.close();
        observer.error('Conexão com o servidor perdida.');
      };

      return () => source.close();
    });
  }

  buscarDocumento(codigo: string): Observable<string> {
    return this.http.get(`${this.baseUrl}/${codigo}/documento`, {
      responseType: 'text',
    });
  }

  urlDownload(codigo: string): string {
    return `${this.baseUrl}/${codigo}/documento`;
  }
}
