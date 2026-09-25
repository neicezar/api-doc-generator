import { Component, EventEmitter, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-formulario',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './formulario.component.html',
  styleUrl: './formulario.component.scss',
})
export class FormularioComponent {
  @Output() disparar = new EventEmitter<string>();

  urlRepositorio = '';
  erro = '';

  onSubmit(): void {
    const url = this.urlRepositorio.trim();

    if (!url) {
      this.erro = 'Informe a URL do repositório.';
      return;
    }

    if (!url.startsWith('https://github.com/')) {
      this.erro = 'A URL deve ser um repositório público do GitHub (https://github.com/...).';
      return;
    }

    this.erro = '';
    this.disparar.emit(url);
  }
}
