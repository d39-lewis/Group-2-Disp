# Camunda GitHub sync playbook (service-task teams)

## Official docs

- Git sync: https://docs.camunda.io/docs/components/modeler/web-modeler/git-sync/
- Process applications: https://docs.camunda.io/docs/components/modeler/web-modeler/process-applications/
- Create process application: https://docs.camunda.io/docs/components/modeler/web-modeler/process-applications/create-a-process-application/
- Deploy process application: https://docs.camunda.io/docs/components/modeler/web-modeler/process-applications/deploy-process-application/
- Process application versioning: https://docs.camunda.io/docs/components/modeler/web-modeler/process-applications/process-application-versioning/
- Process application pipeline: https://docs.camunda.io/docs/components/modeler/web-modeler/process-applications/process-application-pipeline/

## Recommended team setup

1. Create one process application in Web Modeler for the operational model.
2. Set your main BPMN to `daniel_local.bpmn`.
3. Connect GitHub sync to this repository and target branch:
   - feature work branch: `feat/service-task-foundation`
   - release branch: `main`
4. Keep service-task code and BPMN changes in the same branch/PR.
5. Use **Sync with GitHub** frequently (small changes, fewer conflicts).

## What to remember

- Process applications deploy as one bundle (BPMN/DMN/forms/resources in app scope).
- External forms can be deployed with the app; linked external BPMN/DMN are not deployed by that bundle.
- Single-file deployment is not the process-app model.
- Versioning is app-level; set version tags before review/deploy checkpoints.

## "Always latest BPMN" without chaos

Use this policy:

- **Dev testing:** run workers against latest deployed version in Dev cluster.
- **Formal testing/demo:** pin to a process application version (known snapshot).
- **Production/release:** only deploy reviewed process application versions.

This gives fast iteration in Dev and reproducibility for assessment demos.

## Service-task anti-drift checklist

1. Every `zeebe:taskDefinition type` in BPMN has a matching worker method.
2. Variable contracts are documented in `docs/service-task-traceability.md`.
3. Any BPMN change that touches task types requires worker update in same PR.
4. Before demo: verify Camunda Operate instance uses intended process version.
