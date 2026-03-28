# DISP Group 2

This github repo will house the source code and relevant models/diagrams produced by Group 2 for the DISP module, essentially acting as a portfolio.

### Camunda 8 service task foundation

Initial Java worker foundation has been added under [camunda-worker-foundation](camunda-worker-foundation).

- Maven project: [camunda-worker-foundation/pom.xml](camunda-worker-foundation/pom.xml)
- Worker stubs: [camunda-worker-foundation/src/main/java/au/edu/group2/disp/workers/FoundationWorkers.java](camunda-worker-foundation/src/main/java/au/edu/group2/disp/workers/FoundationWorkers.java)
- Config template: [camunda-worker-foundation/.env.example](camunda-worker-foundation/.env.example)
- Testing outline: [docs/camunda8-testing-outline.md](docs/camunda8-testing-outline.md)
- Traceability matrix: [docs/service-task-traceability.md](docs/service-task-traceability.md)
- GitHub sync playbook: [docs/camunda-github-sync-playbook.md](docs/camunda-github-sync-playbook.md)

The worker app is configured for Camunda 8 SaaS and keeps deployment disabled by default, because the BPMN is already managed in your university cluster.

### Socio-Technical Model

This model can be found in the `socio_technical_model.txt` file and can be accessed using:
1. Downloading the file
2. Accessing the [piStar Tool](https://www.cin.ufpe.br/~jhcp/pistar/tool/#).
3. Selecting 'Load Model'
4. Choosing the downloaded `socio_technical_model.txt`.