import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MarkdownComponent } from 'ngx-markdown';
import { TarefaService } from '../../services/tarefa.service';

@Component({
  selector: 'app-resultado',
  standalone: true,
  imports: [CommonModule, MarkdownComponent],
  templateUrl: './resultado.component.html',
  styleUrl: './resultado.component.scss',
})
export class ResultadoComponent {
  @Input() conteudo = '';
  @Input() codigo = '';
  @Input() urlRepositorio = '';
  @Output() reiniciar = new EventEmitter<void>();

  copiado = false;

  constructor(private tarefaService: TarefaService) {}

  copiar(): void {
    navigator.clipboard.writeText(this.conteudo).then(() => {
      this.copiado = true;
      setTimeout(() => (this.copiado = false), 2000);
    });
  }

  download(): void {
    const url = this.tarefaService.urlDownload(this.codigo);
    const a = document.createElement('a');
    a.href = url;
    a.download = `documentacao-${this.codigo}.md`;
    a.click();
  }

  nomeRepositorio(): string {
    try {
      return new URL(this.urlRepositorio).pathname.replace(/^\//, '');
    } catch {
      return this.urlRepositorio;
    }
  }
}
