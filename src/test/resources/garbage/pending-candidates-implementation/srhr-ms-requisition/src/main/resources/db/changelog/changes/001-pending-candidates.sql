--liquibase formatted sql

--changeset srhr:pending-candidates-001
create table pending_candidates (
    id bigserial primary key,
    candidate_id varchar(64) not null,
    vacancy_id varchar(64) not null,
    full_name varchar(512) not null,
    approver_pernr varchar(32) not null,
    created_at timestamptz not null default now(),
    constraint uq_pending_candidates_snapshot unique (approver_pernr, candidate_id, vacancy_id)
);

create table pending_candidates_events (
    id bigserial primary key,
    pending_candidate_id bigint not null,
    event_code varchar(128) not null,
    status_date timestamptz not null,
    days integer,
    expiration_zone integer,
    constraint fk_pending_candidates_events_candidate
        foreign key (pending_candidate_id)
        references pending_candidates(id)
        on delete cascade
);

create index idx_pending_candidates_approver_page
    on pending_candidates(approver_pernr, id);

create index idx_pending_candidates_approver_name
    on pending_candidates(approver_pernr, lower(full_name));

create index idx_pending_candidates_events_candidate_sort
    on pending_candidates_events(pending_candidate_id, status_date, id);

create index idx_pending_candidates_events_code_candidate
    on pending_candidates_events(event_code, pending_candidate_id);

--rollback drop table if exists pending_candidates_events; drop table if exists pending_candidates;
