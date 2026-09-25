import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Passo {
  status: string;
  label: string;
  descricao: string;
  estado: 'pendente' | 'ativo' | 'concluido' | 'erro';
}

@Component({
  selector: 'app-progresso',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './progresso.component.html',
  styleUrl: './progresso.component.scss',
})
export class ProgressoComponent implements OnChanges {
  @Input() statusAtual = '';
  @Input() mensagem = '';
  @Input() urlRepositorio = '';

  passos: Passo[] = [
    { status: 'BAIXANDO',  label: 'Baixando',   descricao: 'Clonando repositório do GitHub', estado: 'pendente' },
    { status: 'EXTRAINDO', label: 'Extraindo',  descricao: 'Analisando código com JavaParser', estado: 'pendente' },
    { status: 'GERANDO',   label: 'Gerando',    descricao: 'Gerando documentação com LLM', estado: 'pendente' },
    { status: 'CONCLUIDO', label: 'Concluído',  descricao: 'Documentação pronta!', estado: 'pendente' },
  ];

  ngOnChanges(): void {
    this.atualizarPassos();
  }

  private atualizarPassos(): void {
    const ordem = ['BAIXANDO', 'EXTRAINDO', 'GERANDO', 'CONCLUIDO'];
    const indiceAtual = ordem.indexOf(this.statusAtual);

    this.passos = this.passos.map((passo, i) => {
      const indicePasso = ordem.indexOf(passo.status);
      let estado: Passo['estado'] = 'pendente';

      if (this.statusAtual === 'FALHOU' && indicePasso === indiceAtual - 1) {
        estado = 'erro';
      } else if (indicePasso < indiceAtual) {
        estado = 'concluido';
      } else if (indicePasso === indiceAtual) {
        estado = this.statusAtual === 'CONCLUIDO' ? 'concluido' : 'ativo';
      }

      return { ...passo, estado };
    });
  }

  nomeRepositorio(): string {
    try {
      const url = new URL(this.urlRepositorio);
      return url.pathname.replace(/^\//, '');
    } catch {
      return this.urlRepositorio;
    }
  }
}
