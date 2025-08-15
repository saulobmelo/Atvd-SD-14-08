# Sistema Distribuído de Monitoramento de Recursos

**Stack**: Java 21, Sockets (TCP/UDP), Java RMI.

## Como compilar

- Clone o repositório
- Abra o projeto na sua IDE
- Clique na opção de terminal na sua IDE
- Execute esse código
```bash
src/src/run_all_windows
```
- Será executado o arquivo .bat com todo o processo
> O cliente só imprime mensagens cujo prefixo seja `[TOKEN:<token>]`, recebidas do líder via multicast.

## Estrutura
- `leader/`: orquestração (heartbeat, election bully, clock sync, RMI aggregator, multicast, auth)
- `node/`: serviços dos nós (RMI getStatus, heartbeat responder, clock sync server, bully server)
- `client/`: autenticação + recepção multicast
- `common/`: modelos e utilitários (LamportClock, NodeId, ResourceStatus, Snapshot, Messages)

## Testes sugeridos
- Derrube o líder; observe eleição automática e continuidade da publicação.
- Adicione dois clientes autenticados; valide a recepção simultânea via multicast.
- Compare relógios Lamport antes/depois do `ClockSync` nos logs.