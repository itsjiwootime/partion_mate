# Repository Agent Guide

## Default Working Rules
- Use `docs/agent-roadmap.md` as the default execution backlog unless the user explicitly overrides the order.
- Prefer finishing one task card at a time. Keep each turn scoped to a single task card whenever possible.
- After completing a task card, update its status and add short implementation notes in `docs/agent-roadmap.md`.
- Do not claim performance improvements, stability gains, or production readiness without a reproducible test or benchmark result.

## ADR Requirement
- Every meaningful development task that introduces a technical decision, tradeoff, or architectural change must create or update an ADR under `docs/adr/`.
- Use the template in `docs/adr/TEMPLATE.md`.
- Recommended filename format: `ADR-YYYYMMDD-short-slug.md`.
- Each ADR must include the following sections exactly:
  - `## 배경`
  - `## 결정사항`
  - `## 대안`
  - `## 결과`

## ADR Content Rules
- `배경`
  - 어떤 기술적 문제 또는 선택이 필요했는지 작성한다.
- `결정사항`
  - 어떤 결정을 내렸고 그 이유는 무엇인지 작성한다.
- `대안`
  - 고려한 다른 선택지와 각각의 장단점을 작성한다.
- `결과`
  - 긍정적, 부정적, 중립적 트레이드오프를 모두 작성한다.

## Documentation Sync
- If implementation changes the roadmap status, architecture direction, data model, or API behavior, sync the related Markdown files in the same task.
- If a decision changes later, update the existing ADR or create a superseding ADR and link both documents.
