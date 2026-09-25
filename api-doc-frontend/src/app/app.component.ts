import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormularioComponent } from './components/formulario/formulario.component';
import { ProgressoComponent } from './components/progresso/progresso.component';
import { ResultadoComponent } from './components/resultado/resultado.component';
import { TarefaService } from './services/tarefa.service';

type Estado = 'formulario' | 'processando' | 'concluido' | 'erro';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormularioComponent, ProgressoComponent, ResultadoComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  estado: Estado = 'formulario';
  codigoTarefa = '';
  urlRepositorio = '';
  statusAtual = '';
  mensagemAtual = '';
  conteudoMarkdown = '';
  mensagemErro = '';

  constructor(private tarefaService: TarefaService) {}

  onDisparar(url: string): void {
    this.urlRepositorio = url;
    this.estado = 'processando';
    this.statusAtual = '';
    this.mensagemAtual = 'Criando tarefa...';

    this.tarefaService.criar(url).subscribe({
      next: (tarefa) => {
        this.codigoTarefa = tarefa.codigo;
        this.assinarEventos(tarefa.codigo);
      },
      error: (err) => {
        this.estado = 'erro';
        this.mensagemErro =
          err?.error?.mensagemErro ||
          err?.error?.message ||
          'Não foi possível conectar ao servidor. Verifique se o backend está rodando.';
      },
    });
  }

  private assinarEventos(codigo: string): void {
    this.tarefaService.acompanharEventos(codigo).subscribe({
      next: ({ status, data }) => {
        this.statusAtual = status;
        this.mensagemAtual = data;

        if (status === 'CONCLUIDO') {
          this.buscarDocumento(codigo);
        } else if (status === 'FALHOU') {
          this.estado = 'erro';
          this.mensagemErro = data || 'Falha durante o processamento da tarefa.';
        }
      },
      error: () => {
        this.estado = 'erro';
        this.mensagemErro = 'Conexão com o servidor perdida. Tente novamente.';
      },
    });
  }

  private buscarDocumento(codigo: string): void {
    this.tarefaService.buscarDocumento(codigo).subscribe({
      next: (conteudo) => {
        this.conteudoMarkdown = conteudo;
        this.estado = 'concluido';
      },
      error: () => {
        this.estado = 'erro';
        this.mensagemErro = 'Documentação gerada, mas não foi possível carregá-la. Tente baixar o arquivo.';
      },
    });
  }

  onReiniciar(): void {
    this.estado = 'formulario';
    this.codigoTarefa = '';
    this.urlRepositorio = '';
    this.statusAtual = '';
    this.mensagemAtual = '';
    this.conteudoMarkdown = '';
    this.mensagemErro = '';
  }
}
