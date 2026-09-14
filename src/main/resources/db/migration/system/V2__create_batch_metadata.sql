-- Based on Spring Batch 6.0.5 schema-postgresql.sql (Apache-2.0).
-- Framework column types and sequences are retained for compatibility.
create schema system;

create table system.batch_job_instance (
	job_instance_id bigint  not null constraint pk_batch_job_instance primary key,
	version bigint,
	job_name varchar(100) not null,
	job_key varchar(32) not null,
	constraint uk_batch_job_instance_job_name_job_key unique (job_name, job_key)
) ;

create table system.batch_job_execution (
	job_execution_id bigint  not null constraint pk_batch_job_execution primary key,
	version bigint,
	job_instance_id bigint not null,
	create_time timestamp not null,
	start_time timestamp default null,
	end_time timestamp default null,
	status varchar(10),
	exit_code varchar(2500),
	exit_message varchar(2500),
	last_updated timestamp,
	constraint fk_batch_job_execution_batch_job_instance foreign key (job_instance_id)
	references system.batch_job_instance(job_instance_id)
) ;

create table system.batch_job_execution_params (
	job_execution_id bigint not null,
	parameter_name varchar(100) not null,
	parameter_type varchar(100) not null,
	parameter_value varchar(2500),
	identifying char(1) not null,
	constraint fk_batch_job_execution_params_batch_job_execution foreign key (job_execution_id)
	references system.batch_job_execution(job_execution_id)
) ;

create table system.batch_step_execution (
	step_execution_id bigint  not null constraint pk_batch_step_execution primary key,
	version bigint not null,
	step_name varchar(100) not null,
	job_execution_id bigint not null,
	create_time timestamp not null,
	start_time timestamp default null,
	end_time timestamp default null,
	status varchar(10),
	commit_count bigint,
	read_count bigint,
	filter_count bigint,
	write_count bigint,
	read_skip_count bigint,
	write_skip_count bigint,
	process_skip_count bigint,
	rollback_count bigint,
	exit_code varchar(2500),
	exit_message varchar(2500),
	last_updated timestamp,
	constraint fk_batch_step_execution_batch_job_execution foreign key (job_execution_id)
	references system.batch_job_execution(job_execution_id)
) ;

create table system.batch_step_execution_context (
	step_execution_id bigint not null constraint pk_batch_step_execution_context primary key,
	short_context varchar(2500) not null,
	serialized_context text,
	constraint fk_batch_step_execution_context_batch_step_execution foreign key (step_execution_id)
	references system.batch_step_execution(step_execution_id)
) ;

create table system.batch_job_execution_context (
	job_execution_id bigint not null constraint pk_batch_job_execution_context primary key,
	short_context varchar(2500) not null,
	serialized_context text,
	constraint fk_batch_job_execution_context_batch_job_execution foreign key (job_execution_id)
	references system.batch_job_execution(job_execution_id)
) ;

create sequence system.batch_step_execution_seq maxvalue 9223372036854775807 no cycle;
create sequence system.batch_job_execution_seq maxvalue 9223372036854775807 no cycle;
create sequence system.batch_job_instance_seq maxvalue 9223372036854775807 no cycle;

comment on schema system is 'Framework-owned technical metadata';
comment on table system.batch_job_instance is 'Spring Batch job instance metadata';
comment on table system.batch_job_execution is 'Spring Batch job execution metadata';
comment on table system.batch_job_execution_params is 'Spring Batch job execution params metadata';
comment on table system.batch_step_execution is 'Spring Batch step execution metadata';
comment on table system.batch_step_execution_context is 'Spring Batch step execution context metadata';
comment on table system.batch_job_execution_context is 'Spring Batch job execution context metadata';
