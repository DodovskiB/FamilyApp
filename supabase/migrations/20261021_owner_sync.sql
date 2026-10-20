-- Phase 1: synchronize one authenticated owner's devices. Sharing is a later migration.
begin;
create sequence if not exists public.shopping_sync_revision;
create table if not exists public.shopping_sync_records (
  owner_id uuid not null references auth.users(id) on delete cascade,
  entity text not null check (entity in ('list','catalog','item')),
  id uuid not null,
  body jsonb not null check (jsonb_typeof(body)='object'),
  deleted boolean not null default false,
  revision bigint not null default nextval('public.shopping_sync_revision'),
  primary key(owner_id,entity,id)
);
create index if not exists shopping_sync_records_cursor on public.shopping_sync_records(owner_id,revision);
create table if not exists public.shopping_sync_operations (
  owner_id uuid not null references auth.users(id) on delete cascade,
  operation_id uuid not null,
  request_hash text not null,
  primary key(owner_id,operation_id)
);
alter table public.shopping_sync_records enable row level security;
alter table public.shopping_sync_operations enable row level security;
revoke all on public.shopping_sync_records,public.shopping_sync_operations from anon,authenticated;
revoke all on sequence public.shopping_sync_revision from anon,authenticated;
grant select on public.shopping_sync_records to authenticated;
do $$ begin
  if not exists (select 1 from pg_policies where schemaname='public'
    and tablename='shopping_sync_records' and policyname='shopping_sync_owner_read') then
    create policy shopping_sync_owner_read on public.shopping_sync_records for select to authenticated
      using (owner_id=(select auth.uid()));
  end if;
end $$;

create or replace function public.shopping_sync_apply(operations jsonb)
returns void language plpgsql security definer set search_path='' as $$
declare
  subject uuid := auth.uid();
  op jsonb; kind text; action text; row_id uuid; op_id uuid; patch jsonb;
  existing public.shopping_sync_records%rowtype;
  data jsonb; previous_hash text; parent uuid; product uuid;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  if jsonb_typeof(operations) is distinct from 'array' or jsonb_array_length(operations)>100
     or octet_length(operations::text)>1048576 then raise exception 'Invalid operation batch'; end if;
  -- All changes for an account are serialized, including cursor reads. A cursor cannot skip
  -- an earlier uncommitted transaction. No user-supplied owner ID is accepted.
  perform pg_advisory_xact_lock(hashtextextended(subject::text,0));
  for op in select value from jsonb_array_elements(operations) loop
    kind:=op->>'entity'; action:=op->>'action'; row_id:=(op->>'id')::uuid;
    op_id:=(op->>'operation_id')::uuid; patch:=op->'body';
    if kind is null or kind not in ('list','catalog','item') or action is null
       or action not in ('insert','update','delete') or row_id is null or op_id is null
       or jsonb_typeof(patch) is distinct from 'object' then raise exception 'Invalid operation'; end if;
    select request_hash into previous_hash from public.shopping_sync_operations
      where owner_id=subject and operation_id=op_id;
    if found then
      if previous_hash<>md5(op::text) then raise exception 'Operation ID reused'; end if;
      continue;
    end if;
    select * into existing from public.shopping_sync_records where owner_id=subject and entity=kind and id=row_id;
    if action='delete' then
      -- Tombstones prevent delayed offline edits from resurrecting removed records.
      insert into public.shopping_sync_records(owner_id,entity,id,body,deleted)
        values(subject,kind,row_id,'{}',true)
        on conflict(owner_id,entity,id) do update set deleted=true,revision=nextval('public.shopping_sync_revision');
      if kind='list' then
        update public.shopping_sync_records set deleted=true,revision=nextval('public.shopping_sync_revision')
          where owner_id=subject and entity='item' and not deleted and body->>'list_id'=row_id::text;
      elsif kind='catalog' then
        update public.shopping_sync_records set body=jsonb_set(body,'{catalog_id}','null'::jsonb),
          revision=nextval('public.shopping_sync_revision')
          where owner_id=subject and entity='item' and not deleted and body->>'catalog_id'=row_id::text;
      end if;
    elsif (action='insert' and existing.id is not null) or existing.deleted then
      null; -- A retried initial import does not overwrite subsequent edits or tombstones.
    else
      if action='update' and existing.id is null then raise exception 'Missing record'; end if;
      if kind='list' then
        if patch - array['name','sort_order'] <> '{}'::jsonb then raise exception 'Invalid list fields'; end if;
      elsif kind='catalog' then
        if patch - array['title','category','link_url'] <> '{}'::jsonb then raise exception 'Invalid catalog fields'; end if;
      else
        if patch - array['title','category','qty','qty_delta','checked','active','list_id','catalog_id'] <> '{}'::jsonb
          then raise exception 'Invalid item fields'; end if;
        if action='update' and patch ? 'qty' then raise exception 'Quantity updates require a delta'; end if;
        if action='insert' and patch ? 'qty_delta' then raise exception 'Initial quantity required'; end if;
      end if;
      data:=coalesce(existing.body,'{}'::jsonb) || (patch-'qty_delta');
      if kind='item' and action='update' and patch ? 'qty_delta' then
        data:=jsonb_set(data,'{qty}',to_jsonb(least(2147483647::bigint,greatest(1::bigint,
          (existing.body->>'qty')::bigint+(patch->>'qty_delta')::bigint))));
      end if;
      if kind='list' then
        if jsonb_typeof(data->'name') is distinct from 'string' or length(btrim(data->>'name'))=0
          or length(data->>'name')>512 or jsonb_typeof(data->'sort_order') is distinct from 'number'
          then raise exception 'Invalid list'; end if;
        perform (data->>'sort_order')::integer;
      else
        if jsonb_typeof(data->'title') is distinct from 'string' or length(btrim(data->>'title'))=0
          or length(data->>'title')>512 or jsonb_typeof(data->'category') is distinct from 'string'
          or length(data->>'category')>512 then raise exception 'Invalid product'; end if;
        if kind='catalog' then
          if data->>'link_url' is not null and (length(data->>'link_url')>8192
            or (data->>'link_url') !~ '^https?://') then raise exception 'Invalid link'; end if;
        else
          if jsonb_typeof(data->'qty') is distinct from 'number' or (data->>'qty')::bigint not between 1 and 2147483647
            or jsonb_typeof(data->'checked') is distinct from 'boolean'
            or jsonb_typeof(data->'active') is distinct from 'boolean' then raise exception 'Invalid item'; end if;
          parent:=(data->>'list_id')::uuid; product:=(data->>'catalog_id')::uuid;
          if parent is null then raise exception 'Parent list required'; end if;
          if not exists(select 1 from public.shopping_sync_records where owner_id=subject
            and entity='list' and id=parent and not deleted) then
            insert into public.shopping_sync_operations values(subject,op_id,md5(op::text));
            continue; -- Offline insert/edit against a deleted/unavailable list is not resurrected.
          end if;
          if product is not null and not exists(select 1 from public.shopping_sync_records
            where owner_id=subject and entity='catalog' and id=product and not deleted) then
            data:=jsonb_set(data,'{catalog_id}','null'::jsonb);
          end if;
        end if;
      end if;
      insert into public.shopping_sync_records(owner_id,entity,id,body) values(subject,kind,row_id,data)
        on conflict(owner_id,entity,id) do update set body=excluded.body,revision=nextval('public.shopping_sync_revision');
    end if;
    insert into public.shopping_sync_operations values(subject,op_id,md5(op::text));
  end loop;
end $$;

create or replace function public.shopping_sync_pull(after_revision bigint default 0)
returns jsonb language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); result jsonb;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  if after_revision<0 then raise exception 'Invalid cursor'; end if;
  perform pg_advisory_xact_lock(hashtextextended(subject::text,0));
  select coalesce(jsonb_agg(jsonb_build_object('entity',entity,'id',id,'body',body,
    'deleted',deleted,'revision',revision) order by revision),'[]'::jsonb) into result
    from (select * from public.shopping_sync_records where owner_id=subject and revision>after_revision
      order by revision limit 500) records;
  return result;
end $$;
revoke all on function public.shopping_sync_apply(jsonb),public.shopping_sync_pull(bigint) from public,anon;
grant execute on function public.shopping_sync_apply(jsonb),public.shopping_sync_pull(bigint) to authenticated;
commit;
