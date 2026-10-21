-- Owner/Admin access. Existing owner sync stays compatible; personal catalogs stay private.
begin;
create unique index if not exists shopping_sync_global_identity on public.shopping_sync_records(entity,id);
create table public.shopping_list_members (
  owner_id uuid not null references auth.users(id) on delete cascade,
  list_id uuid not null,
  member_id uuid not null references auth.users(id) on delete cascade,
  primary key(owner_id,list_id,member_id), check(owner_id<>member_id)
);
create table public.shopping_list_invitations (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users(id) on delete cascade,
  list_id uuid not null, email text not null,
  state text not null default 'pending' check(state in ('pending','accepted','declined','cancelled')),
  expires_at timestamptz not null default now()+interval '7 days',
  unique(owner_id,list_id,email)
);
alter table public.shopping_list_members enable row level security;
alter table public.shopping_list_invitations enable row level security;
revoke all on public.shopping_list_members,public.shopping_list_invitations from public,anon,authenticated;

-- Email comes from the authenticated, confirmed server account, never a request parameter/JWT email override.
create function public.shopping_sharing_email() returns text
language sql stable security definer set search_path='' as $$
  select lower(btrim(email)) from auth.users where id=auth.uid() and email_confirmed_at is not null
$$;
revoke all on function public.shopping_sharing_email() from public,anon,authenticated;

create function public.shopping_sharing_invite(list_id uuid, email text) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); address text:=lower(btrim(email));
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended(subject::text,0));
  if not exists(select 1 from public.shopping_sync_records r where r.owner_id=subject and r.entity='list'
    and r.id=list_id and not r.deleted) then raise exception 'Owner required' using errcode='42501'; end if;
  if address is null or length(address)>320 or address !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$'
    or address=public.shopping_sharing_email() then raise exception 'Enter another valid email'; end if;
  -- A repeated pending request is idempotent; a declined/cancelled/expired invitation can be renewed.
  insert into public.shopping_list_invitations as i(owner_id,list_id,email) values(subject,list_id,address)
    on conflict on constraint shopping_list_invitations_owner_id_list_id_email_key do update set state='pending',expires_at=now()+interval '7 days'
    where i.state<>'pending' or i.expires_at<=now();
end $$;

create function public.shopping_sharing_respond(invitation_id uuid, accept boolean) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); invitation public.shopping_list_invitations%rowtype; owner uuid;
begin
  if subject is null or accept is null then raise exception 'Authentication required' using errcode='42501'; end if;
  select owner_id into owner from public.shopping_list_invitations where id=invitation_id;
  if owner is null then raise exception 'Invitation unavailable' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended(owner::text,0));
  select * into invitation from public.shopping_list_invitations where id=invitation_id;
  if invitation.email is distinct from public.shopping_sharing_email() or subject=owner then
    raise exception 'Invitation unavailable' using errcode='42501'; end if;
  if (invitation.state='accepted' and accept) or (invitation.state='declined' and not accept) then return; end if;
  if invitation.state<>'pending' or invitation.expires_at<=now() or not exists(
    select 1 from public.shopping_sync_records where owner_id=owner and entity='list' and id=invitation.list_id and not deleted)
    then raise exception 'Invitation unavailable' using errcode='42501'; end if;
  if accept then
    insert into public.shopping_list_members values(owner,invitation.list_id,subject) on conflict do nothing;
  end if;
  update public.shopping_list_invitations set state=case when accept then 'accepted' else 'declined' end where id=invitation_id;
end $$;

create function public.shopping_sharing_remove(list_id uuid, member_id uuid default null, invitation_id uuid default null)
returns void language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); address text;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  perform pg_advisory_xact_lock(hashtextextended(subject::text,0));
  if not exists(select 1 from public.shopping_sync_records r where r.owner_id=subject and r.entity='list'
    and r.id=list_id and not r.deleted) then raise exception 'Owner required' using errcode='42501'; end if;
  if member_id is not null then
    delete from public.shopping_list_members m where m.owner_id=subject and m.list_id=shopping_sharing_remove.list_id and m.member_id=shopping_sharing_remove.member_id;
    select lower(btrim(email)) into address from auth.users where id=member_id;
    update public.shopping_list_invitations i set state='cancelled' where i.owner_id=subject
      and i.list_id=shopping_sharing_remove.list_id and i.email=address;
  end if;
  if invitation_id is not null then
    update public.shopping_list_invitations i set state='cancelled' where i.id=invitation_id and i.owner_id=subject
      and i.list_id=shopping_sharing_remove.list_id and i.state='pending';
  end if;
end $$;

-- Full authorized snapshot: also discovers newly accepted access below an old incremental cursor.
-- Only the list/entry snapshots are shared; no private catalog records or catalog UUIDs.
create function public.shopping_sharing_snapshot() returns jsonb
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); result jsonb;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  select jsonb_build_object(
    'access',coalesce((select jsonb_agg(jsonb_build_object('id',r.id,'owner_id',r.owner_id,'role',
      case when r.owner_id=subject then 'owner' else 'admin' end))
      from public.shopping_sync_records r where r.entity='list' and not r.deleted and
      (r.owner_id=subject or exists(select 1 from public.shopping_list_members m where m.owner_id=r.owner_id and m.list_id=r.id and m.member_id=subject))),'[]'::jsonb),
    'records',coalesce((select jsonb_agg(jsonb_build_object('entity',r.entity,'id',r.id,
      'body',case when r.entity='item' then r.body||jsonb_build_object('catalog_id',null) else r.body end,
      'deleted',r.deleted,'revision',r.revision)) from public.shopping_sync_records r
      where r.owner_id<>subject and r.entity in ('list','item') and exists(
        select 1 from public.shopping_list_members m join public.shopping_sync_records l on
          l.owner_id=m.owner_id and l.entity='list' and l.id=m.list_id and not l.deleted
        where m.member_id=subject and m.owner_id=r.owner_id and m.list_id=case when r.entity='list' then r.id else (r.body->>'list_id')::uuid end)),'[]'::jsonb),
    'invitations',coalesce((select jsonb_agg(jsonb_build_object('id',i.id,'list_id',i.list_id,'name',r.body->>'name',
      'email',i.email,'state',i.state,'expires_at',i.expires_at,'incoming',i.owner_id<>subject))
      from public.shopping_list_invitations i join public.shopping_sync_records r on r.owner_id=i.owner_id and r.entity='list' and r.id=i.list_id and not r.deleted
      where i.state='pending' and i.expires_at>now() and (i.owner_id=subject or i.email=public.shopping_sharing_email())),'[]'::jsonb),
    'members',coalesce((select jsonb_agg(jsonb_build_object('list_id',m.list_id,'id',m.member_id,'email',u.email))
      from public.shopping_list_members m join auth.users u on u.id=m.member_id
      join public.shopping_sync_records r on r.owner_id=m.owner_id and r.entity='list' and r.id=m.list_id and not r.deleted
      where m.owner_id=subject),'[]'::jsonb)
  ) into result;
  return result;
end $$;

create or replace function public.shopping_sync_apply_internal(target_owner uuid, operations jsonb)
returns void language plpgsql security definer set search_path='' as $$
declare
  subject uuid := target_owner;
  op jsonb; kind text; action text; row_id uuid; op_id uuid; patch jsonb;
  existing public.shopping_sync_records%rowtype;
  data jsonb; previous_hash text; parent uuid; product uuid; redirected uuid; hops integer;
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
          -- Explicitly merged list tombstones redirect old devices' queued item writes.
          hops:=0;
          loop
            select (body->>'merged_into')::uuid into redirected from public.shopping_sync_records
              where owner_id=subject and entity='list' and id=parent and deleted and body ? 'merged_into';
            exit when redirected is null;
            parent:=redirected; hops:=hops+1;
            if hops>32 then raise exception 'Invalid list redirect'; end if;
          end loop;
          data:=jsonb_set(data,'{list_id}',to_jsonb(parent::text));
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
revoke all on function public.shopping_sync_apply_internal(uuid,jsonb) from public,anon,authenticated;
create or replace function public.shopping_sync_apply(operations jsonb)
returns void language sql security definer set search_path='' as $$
  select public.shopping_sync_apply_internal(auth.uid(),operations)
$$;


create function public.shopping_sharing_apply(owner_id uuid, list_id uuid, operations jsonb) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); op jsonb; existing public.shopping_sync_records%rowtype; cleaned jsonb:='[]'; patch jsonb;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  if jsonb_typeof(operations) is distinct from 'array' or jsonb_array_length(operations)>100
    or octet_length(operations::text)>1048576 then raise exception 'Invalid batch'; end if;
  perform pg_advisory_xact_lock(hashtextextended(owner_id::text,0));
  if not exists(select 1 from public.shopping_list_members m where m.owner_id=shopping_sharing_apply.owner_id
      and m.list_id=shopping_sharing_apply.list_id and m.member_id=subject)
    or not exists(select 1 from public.shopping_sync_records r where r.owner_id=shopping_sharing_apply.owner_id
      and r.entity='list' and r.id=list_id and not r.deleted)
    then raise exception 'List access removed' using errcode='42501'; end if;
  for op in select value from jsonb_array_elements(operations) loop
    patch:=op->'body';
    if op->>'entity'='list' then
      if (op->>'id')::uuid<>list_id or op->>'action'<>'update' or patch-array['name','sort_order']<>'{}'::jsonb then
        raise exception 'Owner required' using errcode='42501'; end if;
    elsif op->>'entity'='item' then
      select * into existing from public.shopping_sync_records r where r.owner_id=shopping_sharing_apply.owner_id
        and r.entity='item' and r.id=(op->>'id')::uuid;
      if existing.id is not null and (existing.body->>'list_id')::uuid is distinct from list_id then
        raise exception 'Wrong list' using errcode='42501'; end if;
      if op->>'action'='insert' then
        if (patch->>'list_id')::uuid is distinct from list_id then raise exception 'Wrong list' using errcode='42501'; end if;
        patch:=patch||jsonb_build_object('catalog_id',null);
      elsif existing.id is null or patch ? 'list_id' or patch ? 'catalog_id' then
        raise exception 'Wrong list' using errcode='42501';
      end if;
      op:=jsonb_set(op,'{body}',patch);
    else raise exception 'Private catalog' using errcode='42501'; end if;
    cleaned:=cleaned||jsonb_build_array(op);
  end loop;
  perform public.shopping_sync_apply_internal(owner_id,cleaned);
end $$;
revoke all on function public.shopping_sharing_invite(uuid,text),public.shopping_sharing_respond(uuid,boolean),
  public.shopping_sharing_remove(uuid,uuid,uuid),public.shopping_sharing_snapshot(),public.shopping_sharing_apply(uuid,uuid,jsonb) from public,anon;
grant execute on function public.shopping_sharing_invite(uuid,text),public.shopping_sharing_respond(uuid,boolean),
  public.shopping_sharing_remove(uuid,uuid,uuid),public.shopping_sharing_snapshot(),public.shopping_sharing_apply(uuid,uuid,jsonb) to authenticated;
commit;
