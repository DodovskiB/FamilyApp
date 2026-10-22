-- Grouped invitations remain per-list grants, with old-client single-list compatibility.
begin;
alter table public.shopping_list_invitations add column group_id uuid;
create index shopping_invitation_group on public.shopping_list_invitations(group_id) where group_id is not null;
create function public.shopping_sharing_invite_many(list_ids uuid[],email text) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); address text:=lower(btrim(email)); ids uuid[]; batch uuid:=gen_random_uuid(); recipient uuid;
begin
 if subject is null then raise exception 'Authentication required' using errcode='42501';end if;
 if list_ids is null or cardinality(list_ids) not between 1 and 100 or array_position(list_ids,null) is not null then raise exception 'Choose lists';end if;
 select array_agg(distinct x order by x) into ids from unnest(list_ids) x;
 perform pg_advisory_xact_lock(hashtextextended(subject::text,0));
 if (select count(*) from public.shopping_sync_records r where r.owner_id=subject and r.entity='list' and r.id=any(ids) and not r.deleted)<>cardinality(ids)
 then raise exception 'Owner required' using errcode='42501';end if;
 if address is null or length(address)>320 or address !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' or address=public.shopping_sharing_email()
 then raise exception 'Enter another valid email';end if;
 select u.id into recipient from auth.users u where lower(btrim(u.email))=address and u.email_confirmed_at is not null limit 1;
 insert into public.shopping_list_invitations as i(owner_id,list_id,email,group_id)
 select subject,x,address,batch from unnest(ids) x where not exists(
 select 1 from public.shopping_list_members m where m.owner_id=subject and m.list_id=x and m.member_id=recipient)
 on conflict on constraint shopping_list_invitations_owner_id_list_id_email_key do update
 set state='pending',expires_at=now()+interval '7 days',group_id=excluded.group_id;
end $$;
create function public.shopping_sharing_respond_many(group_id uuid,accept boolean) returns void
language plpgsql security definer set search_path='' as $$
declare subject uuid:=auth.uid(); address text:=public.shopping_sharing_email(); owner uuid; ids uuid[];
begin
 if subject is null or accept is null then raise exception 'Authentication required' using errcode='42501';end if;
 select i.owner_id into owner from public.shopping_list_invitations i where i.group_id=shopping_sharing_respond_many.group_id and i.email=address limit 1;
 if owner is null or owner=subject then raise exception 'Invitation unavailable' using errcode='42501';end if;
 perform pg_advisory_xact_lock(hashtextextended(owner::text,0));
 select array_agg(i.id) into ids from public.shopping_list_invitations i join public.shopping_sync_records r
 on r.owner_id=i.owner_id and r.entity='list' and r.id=i.list_id and not r.deleted
 where i.group_id=shopping_sharing_respond_many.group_id and i.owner_id=owner and i.email=address and i.state='pending' and i.expires_at>now();
 if ids is null then
  if exists(select 1 from public.shopping_list_invitations i where i.group_id=shopping_sharing_respond_many.group_id and i.owner_id=owner and i.email=address)
  and not exists(select 1 from public.shopping_list_invitations i where i.group_id=shopping_sharing_respond_many.group_id and i.owner_id=owner and i.email=address
  and i.state is distinct from case when accept then 'accepted' else 'declined' end) then return;end if;
  raise exception 'Invitation unavailable' using errcode='42501';
 end if;
 if accept then insert into public.shopping_list_members(owner_id,list_id,member_id)
 select owner,i.list_id,subject from public.shopping_list_invitations i where i.id=any(ids) on conflict do nothing;end if;
 update public.shopping_list_invitations i set state=case when accept then 'accepted' else 'declined' end where i.id=any(ids);
end $$;
create or replace function public.shopping_sharing_snapshot() returns jsonb
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
      'group_id',i.group_id,'email',i.email,'state',i.state,'expires_at',i.expires_at,'incoming',i.owner_id<>subject))
      from public.shopping_list_invitations i join public.shopping_sync_records r on r.owner_id=i.owner_id and r.entity='list' and r.id=i.list_id and not r.deleted
      where i.state='pending' and i.expires_at>now() and (i.owner_id=subject or i.email=public.shopping_sharing_email())),'[]'::jsonb),
    'members',coalesce((select jsonb_agg(jsonb_build_object('list_id',m.list_id,'id',m.member_id,'email',u.email))
      from public.shopping_list_members m join auth.users u on u.id=m.member_id
      join public.shopping_sync_records r on r.owner_id=m.owner_id and r.entity='list' and r.id=m.list_id and not r.deleted
      where m.owner_id=subject),'[]'::jsonb)
  ) into result;
  return result;
end $$;

revoke all on function public.shopping_sharing_invite_many(uuid[],text),public.shopping_sharing_respond_many(uuid,boolean) from public,anon;
grant execute on function public.shopping_sharing_invite_many(uuid[],text),public.shopping_sharing_respond_many(uuid,boolean) to authenticated;
commit;
