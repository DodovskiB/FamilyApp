-- Group cancellation and bounded per-list/per-actor change watermarks.
begin;
create or replace function public.shopping_sharing_remove(list_id uuid, member_id uuid default null, invitation_id uuid default null)
returns void language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); address text; batch uuid;
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
    -- Resolve from the owner's original row even on a retry. Never cancel an
    -- unrelated owner's group or an already accepted grant.
    select i.group_id into batch from public.shopping_list_invitations i
      where i.id=invitation_id and i.owner_id=subject and i.list_id=shopping_sharing_remove.list_id;
    update public.shopping_list_invitations i set state='cancelled'
      where i.owner_id=subject and i.state='pending' and
      ((batch is not null and i.group_id=batch) or
       (batch is null and i.id=invitation_id and i.list_id=shopping_sharing_remove.list_id));
  end if;
end $$;

create sequence public.shopping_activity_revision;
create table public.shopping_list_activity (
  owner_id uuid not null references auth.users(id) on delete cascade,
  list_id uuid not null,
  actor_id uuid not null references auth.users(id) on delete cascade,
  revision bigint not null,
  primary key(owner_id,list_id,actor_id)
);
alter table public.shopping_list_activity enable row level security;
revoke all on public.shopping_list_activity from public,anon,authenticated;
revoke all on sequence public.shopping_activity_revision from public,anon,authenticated;

create function public.shopping_record_activity() returns trigger
language plpgsql security definer set search_path='' as $$
declare actor uuid:=auth.uid(); target uuid; previous uuid;
begin
  if actor is null then return new; end if;
  if tg_table_name='shopping_sync_records' then
    if new.entity not in ('list','item') then return new; end if;
    if tg_op='UPDATE' then
      if new.body is not distinct from old.body and new.deleted=old.deleted then return new; end if;
      if old.entity='item' then previous:=(old.body->>'list_id')::uuid; end if;
    end if;
    target:=case when new.entity='list' then new.id else (new.body->>'list_id')::uuid end;
  else
    if tg_op='UPDATE' then
      if new.link_url is not distinct from old.link_url and new.photo_path is not distinct from old.photo_path then return new; end if;
    elsif new.link_url is null and new.photo_path is null then return new;
    end if;
    select (r.body->>'list_id')::uuid into target from public.shopping_sync_records r
      where r.owner_id=new.owner_id and r.entity='item' and r.id=new.item_id and not r.deleted;
  end if;
  if target is not null then
    insert into public.shopping_list_activity values(new.owner_id,target,actor,nextval('public.shopping_activity_revision'))
    on conflict(owner_id,list_id,actor_id) do update set revision=excluded.revision;
  end if;
  if previous is not null and previous is distinct from target then
    insert into public.shopping_list_activity values(new.owner_id,previous,actor,nextval('public.shopping_activity_revision'))
    on conflict(owner_id,list_id,actor_id) do update set revision=excluded.revision;
  end if;
  return new;
end $$;
revoke all on function public.shopping_record_activity() from public,anon,authenticated;
create trigger shopping_record_activity after insert or update on public.shopping_sync_records
  for each row execute function public.shopping_record_activity();
create trigger shopping_media_activity after insert or update on public.shopping_item_media
  for each row execute function public.shopping_record_activity();

create function public.shopping_activity_snapshot() returns jsonb
language plpgsql stable security definer set search_path='' as $$
declare subject uuid:=auth.uid(); result jsonb;
begin
  if subject is null then raise exception 'Authentication required' using errcode='42501'; end if;
  select coalesce(jsonb_agg(jsonb_build_object('id',s.id,'revision',s.revision)),'[]'::jsonb) into result from (
    select l.id,coalesce(max(a.revision),0) as revision from public.shopping_sync_records l
    left join public.shopping_list_activity a on a.owner_id=l.owner_id and a.list_id=l.id and a.actor_id<>subject
    where l.entity='list' and not l.deleted and exists(select 1 from public.shopping_list_members m
      where m.owner_id=l.owner_id and m.list_id=l.id and (l.owner_id=subject or m.member_id=subject))
    group by l.owner_id,l.id
  ) s;
  return result;
end $$;
revoke all on function public.shopping_activity_snapshot() from public,anon;
grant execute on function public.shopping_activity_snapshot() to authenticated;
commit;
