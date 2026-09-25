export type StatusTarefa =
  | 'PENDENTE'
  | 'BAIXANDO'
  | 'EXTRAINDO'
  | 'GERANDO'
  | 'CONCLUIDO'
  | 'FALHOU';

export interface TarefaResponse {
  codigo: string;
  status: StatusTarefa;
  urlRepositorio: string;
  urlArtefato: string | null;
  mensagemErro: string | null;
}
