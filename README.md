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

The worker app now defaults to **Camunda 8 Self-Managed (local)** at `localhost` and keeps deployment disabled by default.

### Run Camunda 8 locally (Docker Compose)

Reference: Camunda docs for Docker Compose local setup:  
https://docs.camunda.io/docs/self-managed/setup/deploy/local/docker-compose/

1. Download and extract the Camunda Docker Compose distribution from the docs release link.
2. In the extracted folder, start Camunda:
   `docker compose up -d`
3. Open Operate at `http://localhost:8080/operate` (default user: `demo`, password: `demo`).
4. Deploy your BPMN files (`*.bpmn` in repo root) to the local cluster from Camunda Modeler using:
   - gRPC endpoint: `http://localhost:26500`
   - REST endpoint: `http://localhost:8080`
   - Authentication: none (for local quickstart)
5. Start Java workers:
   - `cd camunda-worker-foundation`
   - `mvn spring-boot:run`

To switch workers back to SaaS, set `SPRING_PROFILES_ACTIVE=saas` and provide `CAMUNDA_CLIENT_ID`, `CAMUNDA_CLIENT_SECRET`, `CAMUNDA_CLUSTER_ID`, and `CAMUNDA_REGION`.

### Socio-Technical Model

This model can be found in the `socio_technical_model.txt` file and can be accessed using:
1. Downloading the file
2. Accessing the [piStar Tool](https://www.cin.ufpe.br/~jhcp/pistar/tool/#).
3. Selecting 'Load Model'
4. Choosing the downloaded `socio_technical_model.txt`.
